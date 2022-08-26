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
import java.util.Optional;
import java.util.Set;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.core.container.model.ItemValue;
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

	@Parameters(paramLabel = "FILE", description = "json file to restore")
	File jsonFile;

	@Override
	public void run() {
		JsonObject js;
		ctx.info("Parsing {}", jsonFile);
		try {
			byte[] content = Files.readAllBytes(jsonFile.toPath());
			js = new JsonObject(Buffer.buffer(content));
		} catch (IOException e) {
			throw new CliException(e);
		}

		IMailboxes mboxApi = ctx.adminApi().instance(IMailboxes.class, js.getString("domainUid"));
		ItemValue<Mailbox> mbox = mboxApi.getComplete(js.getString("mailboxUid"));
		ctx.info("Working on {}", mbox);
		if (mbox == null) {
			ctx.error("Mailbox " + js.getString("mailboxUid") + " not found.");
			System.exit(1);
		}

		IAuthentication authApi = ctx.adminApi().instance(IAuthentication.class);
		ctx.info("Sudo as " + mbox.value.defaultEmail().address);
		LoginResponse sudo = authApi.su(mbox.value.defaultEmail().address);
		if (sudo.authKey == null) {
			ctx.error("sudo failed.");
			System.exit(1);
		}
		ItemValue<Server> back = ctx.adminApi().instance(IServer.class, "default").getComplete(mbox.value.dataLocation);
		SystemConf sysconf = ctx.adminApi().instance(ISystemConfiguration.class).getValues();

		File dbFile = new File("restore-" + js.getString("mailboxUid") + ".db");
		DB db = DBMaker.fileDB(dbFile.getAbsolutePath()).transactionEnable().fileMmapEnable().make();
		Set<String> objects = db.hashSet("restored-objects", Serializer.STRING).createOrOpen();

		if (!objects.isEmpty()) {
			ctx.info("Resuming resto with {} known sds keys.", objects.size());
		}

		ISdsSyncStore sds = new SdsStoreLoader().forSysconf(sysconf)
				.orElseThrow(() -> new CliException("Failed to load sds store."));

		String login = mbox.value.name + "@" + js.getString("domainUid");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long restored = 0;

		try (StoreClient sc = new StoreClient(back.value.address(), 1143, login, sudo.authKey)) {
			if (!sc.login()) {
				ctx.error("Failed to login to backend {} as ", back.value.address(), login);
				System.exit(1);
			}
			JsonArray folders = js.getJsonArray("folders");
			int len = folders.size();
			FlagsList seen = new FlagsList();
			seen.add(Flag.SEEN);
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

					Date forAppend = sdf.parse(guidAndDate.getString("d"));
					try {
						sdsGetImapAppend(sds, sc, seen, fn, sdsKey, forAppend);
						objects.add(sdsKey);
						restored++;
					} catch (Exception e) {
						ctx.warn("Failed to process {}", sdsKey);
					}
				}
			}
		} catch (IMAPException | ParseException e) {
			throw new CliException(e);
		}
		ctx.info("Restore is finished. We restored {} from object store.", restored);

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
