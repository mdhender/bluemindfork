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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Sets;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
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

	private static final String SELECT_GUID_QUERY = "SELECT encode(message_body_guid, 'hex') AS guid, internal_date " //
			+ "FROM t_mailbox_record JOIN t_container ON (t_mailbox_record.container_id = t_container.id) " //
			+ "WHERE t_mailbox_record.subtree_id = ? AND t_container.uid = ?";

	private ServerSideServiceProvider provider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	public MailSdsBackup(Path tempFolder) {
		ISystemConfiguration sysApi = provider().instance(ISystemConfiguration.class);
		this.sysconf = sysApi.getValues();
		dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateformat.setTimeZone(TimeZone.getTimeZone("GMT"));
		this.tempFolder = tempFolder;
		this.jsonIndex = tempFolder.resolve("index.json");
	}

	public Set<String> backupDomains(List<ItemValue<Domain>> domains) {
		logger.info("backup domains requested ({})",
				domains.stream().map(d -> d.value.defaultAlias).collect(Collectors.toList()));

		try (MailSdsIndexWriter indexWriter = new MailSdsIndexWriter(jsonIndex)) {
			for (ItemValue<Domain> domain : domains) {
				logger.info("backup domain {}", domain.value.defaultAlias);
				// For each user
				IDirectory dirApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IDirectory.class, domain.uid);
				IUser userApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
						domain.uid);
				IMailshare mailshareApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IMailshare.class, domain.uid);
				ListResult<ItemValue<DirEntry>> users = dirApi.search(DirEntryQuery.filterKind(Kind.USER));
				ListResult<ItemValue<DirEntry>> mailshares = dirApi.search(DirEntryQuery.filterKind(Kind.MAILSHARE));

				users.values.stream().forEach(diruser -> {
					ItemValue<User> user = userApi.getComplete(diruser.uid);
					try {
						backupSdsUser(tempFolder, indexWriter, domain, user);
					} catch (SQLException | IOException e) {
						logger.error("Unable to backup user {}: {}", user, e.getMessage(), e);
					}
				});
				mailshares.values.stream().forEach(dirmailshare -> {
					ItemValue<Mailshare> mailshare = mailshareApi.getComplete(dirmailshare.uid);
					try {
						backupSdsMailshare(tempFolder, indexWriter, domain, mailshare);
					} catch (SQLException | IOException e) {
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
			throws IOException, SQLException {
		Path outputPath = Paths.get(basePath.toAbsolutePath().toString(),
				String.format("%s@%s.json", user.value.login, domain.value.defaultAlias));
		IMailboxes mailboxApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class,
				domain.uid);
		ItemValue<Mailbox> mailbox = mailboxApi.getComplete(user.uid);
		CyrusPartition part = CyrusPartition.forServerAndDomain(mailbox.value.dataLocation, domain.uid);
		DataSource ds = JdbcActivator.getInstance().getMailboxDataSource(user.value.dataLocation);

		IDbReplicatedMailboxes mailboxapi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbReplicatedMailboxes.class, part.name, "user." + mailbox.uid.replace('.', '^'));
		generateSdsMailboxJson(ds, outputPath, domain, mailbox.uid, user.value.login, mailbox.value, mailboxapi.all(),
				getSubtreeContainerId(domain.uid, mailbox.value, mailbox.uid));
		index.add(mailbox.uid, outputPath);
		return outputPath;
	}

	private long getSubtreeContainerId(String domainUid, Mailbox mailbox, String mailboxUid) {
		IContainers containerApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainers.class);
		return containerApi.get(IMailReplicaUids.subtreeUid(domainUid, mailbox.type, mailboxUid)).internalId;
	}

	private Path backupSdsMailshare(Path basePath, MailSdsIndexWriter index, ItemValue<Domain> domain,
			ItemValue<Mailshare> mailshare) throws IOException, SQLException {
		Path outputPath = Paths.get(basePath.toAbsolutePath().toString(),
				String.format("mailshare_%s@%s.json", mailshare.value.name, domain.value.defaultAlias));
		Mailbox mailbox = mailshare.value.toMailbox();
		CyrusPartition part = CyrusPartition.forServerAndDomain(mailshare.value.dataLocation, domain.uid);
		DataSource ds = JdbcActivator.getInstance().getMailboxDataSource(mailshare.value.dataLocation);
		IDbReplicatedMailboxes mailboxapi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbReplicatedMailboxes.class, part.name, mailshare.value.name);
		generateSdsMailboxJson(ds, outputPath, domain, mailshare.uid, mailshare.value.name, mailbox, mailboxapi.all(),
				getSubtreeContainerId(domain.uid, mailbox, mailshare.uid));
		index.add(mailshare.uid, outputPath);
		return outputPath;
	}

	private void generateSdsMailboxJson(DataSource ds, Path outputPath, ItemValue<Domain> domain, String mailboxUid,
			String userLogin, Mailbox mailbox, List<ItemValue<MailboxFolder>> folders, long subtreeContainerId)
			throws SQLException, IOException {
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
//			generator.writeStringField("accesskey", sysconf.stringValue(SysConfKeys.sds_s3_access_key.name()));
//			generator.writeStringField("secretkey", sysconf.stringValue(SysConfKeys.sds_s3_secret_key.name()));
			generator.writeEndObject();

			generator.writeArrayFieldStart("folders");
			for (ItemValue<MailboxFolder> folder : folders) {
				generator.writeStartObject();
				generator.writeStringField("uid", folder.uid);
				generator.writeStringField("fullName", folder.value.fullName);
				generator.writeStringField("name", folder.value.name);
				generator.writeArrayFieldStart("messages");

				generateSdsFolderContent(ds, folder, subtreeContainerId, generator);

				generator.writeEndArray();
				generator.writeEndObject();
			}
			generator.writeEndArray();
			generator.writeEndObject();
		}
	}

	private void generateSdsFolderContent(DataSource ds, ItemValue<MailboxFolder> folder, long subtreeContainerId,
			JsonGenerator generator) throws SQLException, IOException {

		try (Connection conn = ds.getConnection(); PreparedStatement st = conn.prepareStatement(SELECT_GUID_QUERY)) {
			// TODO: do not compile, we need subtree id here
			st.setLong(1, subtreeContainerId);
			st.setString(2, "mbox_records_" + folder.uid);
			st.setMaxFieldSize(2048);
			try (ResultSet rs = st.executeQuery()) {
				while (rs.next()) {
					String guid = rs.getString(1);
					Date date = rs.getDate(2);
					// This is just a safety against broken databases, not encountered in real life
					if (guid != null && !guid.isEmpty()) {
						generator.writeStartObject();
						generator.writeStringField("g", guid);
						generator.writeStringField("d", dateformat.format(date));
						generator.writeEndObject();
					}
				}
			}
		}
	}
}
