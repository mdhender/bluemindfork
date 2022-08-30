/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cli.sds;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import com.google.common.collect.Lists;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.sds.store.loader.SdsStoreLoader;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemConf;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "restore", description = "Populate a mailbox using the mapping file in dataprotect")
public class RestoreSdsMappingCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("sds");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return RestoreSdsMappingCommand.class;
		}

	}

	private CliContext ctx;

	@Option(names = "--dry", description = "do not write the messages")
	boolean dry;

	@Option(names = "--rebuild-db", description = "rebuild the import db")
	boolean rebuildDb;

	@Parameters(paramLabel = "FILE", description = "json file to restore")
	File jsonFile;

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Override
	public void run() {
		JsonObject sdsMapping;
		ctx.info("Parsing {}", jsonFile);
		try {
			byte[] content = Files.readAllBytes(jsonFile.toPath());
			sdsMapping = new JsonObject(Buffer.buffer(content));
		} catch (IOException e) {
			throw new CliException(e);
		}

		String domainUid = sdsMapping.getString("domainUid");
		ItemValue<Mailbox> mbox = getMailbox(sdsMapping);
		LoginResponse sudo = authenticate(mbox);

		String userLogin = sdsMapping.getString("login");
		Set<String> objects = initMapDb(sdsMapping, sudo, userLogin, domainUid);

		String login = mbox.value.name + "@" + domainUid;
		ItemValue<Server> back = ctx.adminApi().instance(IServer.class, "default").getComplete(mbox.value.dataLocation);
		try (StoreClient sc = new StoreClient(back.value.address(), 1143, login, sudo.authKey)) {
			if (!sc.login()) {
				ctx.error("Failed to login to backend {} as ", back.value.address(), login);
				System.exit(1);
			}
			long restored = restoreFolders(sdsMapping, objects, sc);
			ctx.info("Restore is finished. We restored {} from object store.", restored);
		} catch (IMAPException | ParseException e) {
			throw new CliException(e);
		}

	}

	private long restoreFolders(JsonObject sdsMapping, Set<String> objects, StoreClient sc)
			throws IMAPException, ParseException {
		SystemConf sysconf = ctx.adminApi().instance(ISystemConfiguration.class).getValues();
		ISdsSyncStore sds = new SdsStoreLoader().forSysconf(sysconf)
				.orElseThrow(() -> new CliException("Failed to load sds store."));

		JsonArray folders = sdsMapping.getJsonArray("folders");

		int len = folders.size();
		FlagsList seen = new FlagsList();
		seen.add(Flag.SEEN);
		long restored = 0;
		for (int i = 0; i < len; i++) {
			JsonObject folder = folders.getJsonObject(i);
			String fn = folder.getString("fullName");
			if (!sc.select(fn)) {
				ctx.error("Failed to select {}", fn);
				continue;
			}
			JsonArray msgs = folder.getJsonArray("messages");
			int msgCount = msgs.size();
			for (int j = 0; j < msgCount; j++) {
				if (j % 1000 == 0) {
					int percent = j * 100 / msgCount;
					ctx.info("[{}]: {} / {} - {} %", fn, j, msgCount, percent);
				}
				JsonObject guidAndDate = msgs.getJsonObject(j);
				String sdsKey = guidAndDate.getString("g");
				if (objects.contains(sdsKey)) {
					ctx.info("Skip known object {}", sdsKey);
					continue;
				}

				Date appendDateTime = sdf.parse(guidAndDate.getString("d"));
				try {
					sdsGetImapAppend(sds, sc, seen, fn, sdsKey, appendDateTime);
					if (!dry) {
						objects.add(sdsKey);
					}
					restored++;
				} catch (Exception e) {
					ctx.warn("Failed to process {}", sdsKey);
				}
			}
		}
		return restored;
	}

	private Set<String> initMapDb(JsonObject sdsMapping, LoginResponse sudo, String login, String domain) {
		File dbFile = new File("restore-" + sdsMapping.getString("mailboxUid") + ".db");
		if (rebuildDb) {
			dbFile.delete();
		}
		DB db = DBMaker.fileDB(dbFile.getAbsolutePath()).transactionEnable().fileMmapEnable().make();
		Set<String> objects = db.hashSet("restored-objects", Serializer.STRING).createOrOpen();
		if (rebuildDb) {
			ctx.info("Rebuilding restoration sds keys from database.");
			loadObjectsFromDb(login, domain, sudo, sdsMapping, objects);
		}

		if (!objects.isEmpty()) {
			ctx.info("Resuming restoration with {} known sds keys.", objects.size());
		}

		return objects;
	}

	private void loadObjectsFromDb(String login, String domain, LoginResponse sudo, JsonObject sdsMapping,
			Set<String> objects) {
		String mboxRoot = "user." + login.replace('.', '^');
		String partition = domain.replace('.', '_');

		IMailboxFolders folderService = ctx.api(sudo.authKey).instance(IMailboxFolders.class, partition, mboxRoot);
		folderService.all().forEach(folderItem -> {
			if (backupContainsFolder(sdsMapping, folderItem.value.fullName)) {
				ctx.info("Rebuilding restoration sds keys from folder {}.", folderItem.value.fullName);
				IDbMailboxRecords recordService = ctx.api(sudo.authKey).instance(IDbMailboxRecords.class,
						folderItem.uid);
				ContainerChangeset<ItemVersion> allIds = recordService.filteredChangesetById(0l,
						ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
				if (allIds != null && !allIds.created.isEmpty()) {
					ctx.info("Found {} messages in folder {}", allIds.created.size(), folderItem.value.fullName);
					List<List<ItemVersion>> partitioned = Lists.partition(allIds.created, 500);
					for (List<ItemVersion> records : partitioned) {
						List<ItemValue<MailboxRecord>> asRecords = recordService
								.multipleGetById(records.stream().map(i -> i.id).collect(Collectors.toList()));
						objects.addAll(asRecords.stream().map(r -> r.value.messageBody).collect(Collectors.toList()));
					}
				}
			}
		});

	}

	private boolean backupContainsFolder(JsonObject sdsMapping, String fullName) {
		JsonArray folders = sdsMapping.getJsonArray("folders");
		int len = folders.size();

		for (int i = 0; i < len; i++) {
			JsonObject folder = folders.getJsonObject(i);
			String fn = folder.getString("fullName");
			if (fn.equals(fullName)) {
				return true;
			}

		}
		return false;

	}

	private LoginResponse authenticate(ItemValue<Mailbox> mbox) {
		IAuthentication authApi = ctx.adminApi().instance(IAuthentication.class);
		ctx.info("Sudo as " + mbox.value.defaultEmail().address);
		LoginResponse sudo = authApi.su(mbox.value.defaultEmail().address);
		if (sudo.authKey == null) {
			ctx.error("sudo failed.");
			System.exit(1);
		}
		return sudo;
	}

	private ItemValue<Mailbox> getMailbox(JsonObject js) {
		IMailboxes mboxApi = ctx.adminApi().instance(IMailboxes.class, js.getString("domainUid"));
		ItemValue<Mailbox> mbox = mboxApi.getComplete(js.getString("mailboxUid"));
		ctx.info("Working on {}", mbox);
		if (mbox == null) {
			ctx.error("Mailbox " + js.getString("mailboxUid") + " not found.");
			System.exit(1);
		}
		return mbox;
	}

	private void sdsGetImapAppend(ISdsSyncStore sds, StoreClient sc, FlagsList seen, String fn, String sdsKey,
			Date forAppend) throws IOException {
		GetRequest get = new GetRequest();
		File f = new File(sdsKey + ".sds");
		get.filename = f.getAbsolutePath();
		get.guid = sdsKey;
		SdsResponse resp = sds.download(get);
		ctx.info("{} -> {} ({})", sdsKey, f.getAbsolutePath(), resp);
		if (!dry) {
			try (InputStream in = Files.newInputStream(f.toPath())) {
				int added = sc.append(fn, in, seen, forAppend);
				ctx.info("{} restored as imapUid {}", sdsKey, added);
			}
		}
		Files.delete(f.toPath());
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
