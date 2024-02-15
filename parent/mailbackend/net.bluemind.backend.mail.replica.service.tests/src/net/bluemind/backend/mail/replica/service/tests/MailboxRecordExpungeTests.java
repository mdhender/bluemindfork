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
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.IMailboxRecordExpunged;
import net.bluemind.backend.mail.replica.api.MailboxRecordExpunged;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.mime.MimeTree;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.role.api.BasicRoles;

public class MailboxRecordExpungeTests extends AbstractRollingReplicationTests {

	private static final String MARCO = "marco";
	private static Logger logger = LoggerFactory.getLogger(MailboxRecordExpungeTests.class);

	private IMailboxRecordExpunged expungeApi;

	private Container recordsContainer;
	private Container subtreeContainer;
	private Long mailItemId;
	private Long mailItemId2;

	@BeforeEach
	@Override
	public void before(TestInfo testInfo) throws Exception {
		super.before(testInfo);

		this.partition = domainUid.replace('.', '_');
		this.mboxRoot = "user." + userUid.replace('.', '^');

		this.apiKey = "sid";
		Sessions.get().put(apiKey,
				new SecurityContext("sid", userUid, Collections.emptyList(), Collections.emptyList(), domainUid));

		imapAsUser(sc -> {
			assertTrue(sc.create(MARCO));
			int added = sc.append(MARCO, testEml(), new FlagsList());
			int added2 = sc.append(MARCO, testEml(), new FlagsList());
			assertTrue(added > 0);
			sc.select(MARCO);
			Collection<MimeTree> bs = sc.uidFetchBodyStructure(Arrays.asList(added, added2));
			MimeTree tree = bs.iterator().next();
			logger.info(String.format("Mail %d added:%n %s", added, tree));
			logger.info(String.format("Mail %d added:%n %s", added2, tree));
			return null;
		});

		IMailboxFolders foldersApi = getFoldersApi();
		ItemValue<MailboxFolder> marco = foldersApi.byName(MARCO);
		IMailboxItems recApi = recApi(marco.uid);
		ContainerChangeset<Long> changes = recApi.changesetById(0L);
		assertEquals(2, changes.created.size());
		this.mailItemId = changes.created.get(0);
		this.mailItemId2 = changes.created.get(1);

		this.expungeApi = getExpungeApi(marco);

		setContainers(marco.uid);
	}

	private void insertExpungedMessage(long id) throws SQLException {
		try (Connection con = JdbcTestHelper.getInstance().getMailboxDataDataSource().getConnection();
				PreparedStatement stm = con
						.prepareStatement("INSERT INTO q_mailbox_record_expunged VALUES(?,?,?,?,now())")) {
			stm.setInt(1, (int) recordsContainer.id);
			stm.setInt(2, (int) subtreeContainer.id);
			stm.setLong(3, id);
			stm.setLong(4, id);
			int insert = stm.executeUpdate();
			assertEquals(1, insert);
		}
	}

	private void setContainers(String folderUid) {
		try {
			String uid = IMailReplicaUids.mboxRecords(folderUid);
			BmTestContext context = BmTestContext.context(apiKey, domainUid, BasicRoles.ROLE_MANAGE_MAILBOX);
			DataSource ds = DataSourceRouter.get(context, uid);

			ContainerStore cs = new ContainerStore(context, ds, context.getSecurityContext());
			recordsContainer = cs.get(uid);
			if (recordsContainer == null) {
				LoggerFactory.getLogger(this.getClass()).warn("Missing container {}", uid);
				fail();
			}

			IMailboxes mailboxesApi = provider().instance(IMailboxes.class, recordsContainer.domainUid);
			ItemValue<Mailbox> mailbox = mailboxesApi.getComplete(recordsContainer.owner);
			if (mailbox == null) {
				fail();
			}
			String subtreeContainerUid = IMailReplicaUids.subtreeUid(recordsContainer.domainUid, mailbox);
			subtreeContainer = cs.get(subtreeContainerUid);
			if (subtreeContainer == null) {
				LoggerFactory.getLogger(this.getClass()).warn("Missing subtree container {}", subtreeContainerUid);
				fail();
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Test
	public void testFetch() throws SQLException {
		insertExpungedMessage(mailItemId);
		List<MailboxRecordExpunged> fetch = expungeApi.fetch();
		assertEquals(1, fetch.size());
	}

	@Test
	public void testCount() throws SQLException {
		insertExpungedMessage(mailItemId);
		Count count = expungeApi.count(ItemFlagFilter.all());
		assertEquals(1, count.total);
	}

	@Test
	public void testDelete() throws SQLException {
		insertExpungedMessage(mailItemId);
		expungeApi.delete(mailItemId);
		Count count = expungeApi.count(ItemFlagFilter.all());
		assertEquals(0, count.total);
	}

	@Test
	public void testMultipleDelete() throws SQLException {
		insertExpungedMessage(mailItemId);
		insertExpungedMessage(mailItemId2);
		assertEquals(2, expungeApi.count(ItemFlagFilter.all()).total);

		List<Long> ids = Arrays.asList(mailItemId, mailItemId2);
		expungeApi.multipleDelete(ids);
		Count count = expungeApi.count(ItemFlagFilter.all());
		assertEquals(0, count.total);
	}

	@Override
	protected InputStream testEml() {
		return EmlTemplates.withRandomMessageId("small_eml.ftl");
	}

	@Override
	public IServiceProvider provider() {
		SecurityContext userSec = new SecurityContext("sid", userUid, Collections.emptyList(),
				Arrays.asList(BasicRoles.ROLE_MANAGE_MAILBOX), domainUid);
		return ServerSideServiceProvider.getProvider(userSec);
	}

	private IMailboxFolders getFoldersApi() {
		return provider().instance(IMailboxFolders.class, partition, mboxRoot);
	}

	private IMailboxItems recApi(String folderUid) {
		return provider().instance(IMailboxItems.class, folderUid);
	}

	private IMailboxRecordExpunged getExpungeApi(ItemValue<MailboxFolder> mailContainer) {
		return provider().instance(IMailboxRecordExpunged.class, mailContainer.uid);
	}
}
