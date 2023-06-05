/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.backend.mail.dataprotect;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.config.Token;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.dataprotect.sdsspool.SdsDataProtectSpool;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.serviceprovider.SPResolver;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class MailSdsBackup {
	private static final Logger logger = LoggerFactory.getLogger(MailSdsBackup.class);
	private final Path tempFolder;
	private final Path jsonIndex;
	private final SimpleDateFormat dateformat;

	private SystemConf sysconf;
	private Map<String, ISdsSyncStore> productionStores = new HashMap<>();
	private SdsDataProtectSpool backupStore = null;
	IServiceProvider serviceProvider;

	public MailSdsBackup(Path tempFolder) {
		dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateformat.setTimeZone(TimeZone.getTimeZone("GMT"));
		this.tempFolder = tempFolder;
		this.jsonIndex = tempFolder.resolve("index.json");
		serviceProvider = SPResolver.get().resolve(Token.admin0());
		sysconf = serviceProvider.instance(ISystemConfiguration.class).getValues();
	}

	public MailSdsBackup(Path tempFolder, Map<String, ISdsSyncStore> productionStores,
			SdsDataProtectSpool backupStore) {
		this(tempFolder);
		this.productionStores = productionStores;
		this.backupStore = backupStore;
	}

	public void backupMailbox(ItemValue<Domain> domain, ItemValue<DirEntry> de) {
		IUser userApi = serviceProvider.instance(IUser.class, domain.uid);
		IMailshare mailshareApi = serviceProvider.instance(IMailshare.class, domain.uid);

		if (Kind.USER.equals(de.value.kind)) {
			logger.info("backup single user ({})", de.value.email);
			ItemValue<User> user = userApi.getComplete(de.uid);
			try (MailSdsIndexWriter indexWriter = new MailSdsIndexWriter(jsonIndex)) {
				backupSdsUser(tempFolder, indexWriter, domain, user);
			} catch (IOException e) {
				logger.error("Unable to backup user {}: {}", user, e.getMessage(), e);
			}
		} else if (Kind.MAILSHARE.equals(de.value.kind)) {
			logger.info("backup single mailshare ({})", de.value.email != null ? de.value.email : de.value.displayName);
			ItemValue<Mailshare> mailshare = mailshareApi.getComplete(de.uid);
			try (MailSdsIndexWriter indexWriter = new MailSdsIndexWriter(jsonIndex)) {
				backupSdsMailshare(tempFolder, indexWriter, domain, mailshare);
			} catch (IOException e) {
				logger.error("Unable to backup mailshare {}: {}", mailshare, e.getMessage(), e);
			}
		} else if (Kind.DOMAIN.equals(de.value.kind)) {
			IDomains domainApi = serviceProvider.instance(IDomains.class);
			backupDomains(Arrays.asList(domainApi.get(de.uid)));
		} else {
			throw new ServerFault("Don't know how to backup direntry " + de);
		}
	}

	public Set<String> backupDomains(List<ItemValue<Domain>> domains) {
		logger.info("backup domains requested ({})",
				domains.stream().map(d -> d.value.defaultAlias).collect(Collectors.toList()));

		try (MailSdsIndexWriter indexWriter = new MailSdsIndexWriter(jsonIndex)) {
			for (ItemValue<Domain> domain : domains) {
				logger.info("backup domain {}", domain.value.defaultAlias);
				// For each user
				IDirectory dirApi = serviceProvider.instance(IDirectory.class, domain.uid);
				IUser userApi = serviceProvider.instance(IUser.class, domain.uid);
				IMailshare mailshareApi = serviceProvider.instance(IMailshare.class, domain.uid);
				ListResult<ItemValue<DirEntry>> users = dirApi.search(DirEntryQuery.filterKind(Kind.USER));
				ListResult<ItemValue<DirEntry>> mailshares = dirApi.search(DirEntryQuery.filterKind(Kind.MAILSHARE));

				users.values.stream().forEach(diruser -> {
					ItemValue<User> user = userApi.getComplete(diruser.uid);
					try {
						backupSdsUser(tempFolder, indexWriter, domain, user);
					} catch (IOException e) {
						logger.error("Unable to backup user {}: {}", user, e.getMessage(), e);
					}
				});
				mailshares.values.stream().forEach(dirmailshare -> {
					ItemValue<Mailshare> mailshare = mailshareApi.getComplete(dirmailshare.uid);
					try {
						backupSdsMailshare(tempFolder, indexWriter, domain, mailshare);
					} catch (IOException e) {
						logger.error("Unable to backup mailshare {}: {}", mailshare, e.getMessage(), e);
					}
				});
			}
		} catch (IOException e) {
			logger.error("Unable to open json index {} for writing", jsonIndex, e);
		}
		return Sets.newHashSet(tempFolder.toString());
	}

	private Path backupSdsUser(Path basePath, MailSdsIndexWriter index, ItemValue<Domain> domain, ItemValue<User> user)
			throws IOException {
		Path outputPath = Paths.get(basePath.toAbsolutePath().toString(),
				String.format("%s@%s.json", user.value.login, domain.value.defaultAlias));
		IMailboxes mailboxApi = serviceProvider.instance(IMailboxes.class, domain.uid);
		ItemValue<Mailbox> mailbox = mailboxApi.getComplete(user.uid);
		CyrusPartition part = CyrusPartition.forServerAndDomain(mailbox.value.dataLocation, domain.uid);
		ISdsSyncStore productionStore = productionStores.get(user.value.dataLocation);

		IDbReplicatedMailboxes mailboxapi = serviceProvider.instance(IDbReplicatedMailboxes.class, part.name,
				"user." + mailbox.uid.replace('.', '^'));
		generateSdsMailboxJson(outputPath, domain, mailbox.uid, user.value.login, mailbox.value, mailboxapi.all(),
				productionStore);
		index.add(mailbox.uid, outputPath);
		return outputPath;
	}

	private Path backupSdsMailshare(Path basePath, MailSdsIndexWriter index, ItemValue<Domain> domain,
			ItemValue<Mailshare> mailshare) throws IOException {
		Path outputPath = Paths.get(basePath.toAbsolutePath().toString(),
				String.format("mailshare_%s@%s.json", mailshare.value.name, domain.value.defaultAlias));
		Mailbox mailbox = mailshare.value.toMailbox();
		CyrusPartition part = CyrusPartition.forServerAndDomain(mailshare.value.dataLocation, domain.uid);
		IDbReplicatedMailboxes mailboxapi = serviceProvider.instance(IDbReplicatedMailboxes.class, part.name,
				mailshare.value.name);
		ISdsSyncStore productionStore = productionStores.get(mailshare.value.dataLocation);
		generateSdsMailboxJson(outputPath, domain, mailshare.uid, mailshare.value.name, mailbox, mailboxapi.all(),
				productionStore);
		index.add(mailshare.uid, outputPath);
		return outputPath;
	}

	private void generateSdsMailboxJson(Path outputPath, ItemValue<Domain> domain, String mailboxUid, String userLogin,
			Mailbox mailbox, List<ItemValue<MailboxFolder>> folders, ISdsSyncStore productionStore) throws IOException {
		Set<PosixFilePermission> backupPermissions = PosixFilePermissions.fromString("rw-------");

		try (OutputStream outStream = Files.newOutputStream(outputPath, StandardOpenOption.CREATE_NEW,
				StandardOpenOption.TRUNCATE_EXISTING);
				JsonGenerator generator = new JsonFactory().createGenerator(outStream, JsonEncoding.UTF8)) {
			Files.setPosixFilePermissions(outputPath, backupPermissions);
			generator.writeStartObject();
			generator.writeNumberField("version", 1);
			generator.writeStringField("kind", mailbox.type.name());
			generator.writeStringField("mailboxUid", mailboxUid);
			generator.writeStringField("login", userLogin);
			generator.writeStringField("domainUid", domain.uid);
			generator.writeStringField("domainName", domain.value.defaultAlias);
			generator.writeStringField("dataLocation", mailbox.dataLocation);

			generator.writeObjectFieldStart("backingstore");
			generator.writeStringField("archivekind", sysconf.stringValue(SysConfKeys.archive_kind.name()));
			generator.writeStringField("bucket", sysconf.stringValue(SysConfKeys.sds_s3_bucket.name()));
			generator.writeStringField("region", sysconf.stringValue(SysConfKeys.sds_s3_region.name()));
			generator.writeStringField("endpoint", sysconf.stringValue(SysConfKeys.sds_s3_endpoint.name()));
			generator.writeStringField("insecure", sysconf.stringValue(SysConfKeys.sds_s3_insecure.name()));
			generator.writeEndObject();

			generator.writeArrayFieldStart("folders");
			for (ItemValue<MailboxFolder> folder : folders) {
				generator.writeStartObject();
				generator.writeStringField("uid", folder.uid);
				generator.writeStringField("fullName", folder.value.fullName);
				generator.writeStringField("name", folder.value.name);
				generator.writeArrayFieldStart("messages");
				IDbMailboxRecords recordsApi = null;
				try {
					recordsApi = serviceProvider.instance(IDbMailboxRecords.class, folder.uid);
				} catch (ServerFault sf) {
					if (ErrorCode.NOT_FOUND.equals(sf.getCode())) {
						logger.error("Unable to backup user {} folder {}: folder uid={} not found", userLogin,
								folder.value.fullName, folder.uid);
					} else {
						throw sf;
					}
				}
				if (recordsApi != null) {
					generateSdsFolderContent(folder, generator, productionStore, recordsApi);
				}
				generator.writeEndArray();
				generator.writeEndObject();
			}
			generator.writeEndArray();
			generator.writeEndObject();
		}
	}

	private void generateSdsFolderContent(ItemValue<MailboxFolder> folder, JsonGenerator generator,
			ISdsSyncStore productionStore, IDbMailboxRecords recordsApi) {
		Lists.partition(recordsApi.imapIdSet("1:*", ""), 1000).stream().map(recordsApi::slice)
				.flatMap(Collection::stream).forEach(irecord -> {
					String guid = irecord.value.messageBody;
					Date date = irecord.value.internalDate;
					// This is just a safety against broken databases, not encountered in real life
					if (guid != null && !guid.isEmpty()) {
						try {
							generator.writeStartObject();
							generator.writeStringField("g", guid);
							generator.writeStringField("d", dateformat.format(date));
							generator.writeEndObject();
						} catch (IOException ie) {
							logger.warn("unable to generate json data for message_body_guid: {}", guid);
						}
						if (productionStore != null && backupStore != null) {
							Path fp = backupStore.livePath(guid);
							if (!fp.getParent().toFile().exists()) {
								try {
									Files.createDirectories(fp.getParent());
								} catch (IOException ie) {
									throw new ServerFault("unable to create directory " + fp.getParent());
								}
							}
							SdsResponse response = productionStore.downloadRaw(GetRequest.of("", guid, fp.toString()));
							if (!response.succeeded()) {
								logger.warn("unable to download guid {}: {}", guid, response);
							}
						}
					}
				});
	}
}
