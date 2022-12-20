/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IReplicatedDataExpiration;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.service.internal.ReplicatedDataExpirationService;
import net.bluemind.backend.mail.replica.service.tests.compat.CyrusGUID;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ReplicatedDataExpirationServiceTests extends AbstractMailboxRecordsServiceTests<IDbMailboxRecords> {

	@Test
	public void testDeletingExpiredMailsOnEmptyTable() {
		try {
			getExpirationService().deleteExpiredExpunged(7);
		} catch (Exception e) {
			fail("Error while executing expiration task " + e.getMessage());
		}
	}

	@Test
	public void testMailExpiration() {
		try {
			createRecord(10, 15, true);
			createRecord(20, 5, true);
			createRecord(30, 15, false);
			createRecord(40, 5, false);

			assertEquals(4, getService(SecurityContext.SYSTEM).all().size());

			getExpirationService().deleteExpiredExpunged(10);

			try (Connection con = JdbcTestHelper.getInstance().getMailboxDataDataSource().getConnection();
					PreparedStatement stm = con.prepareStatement("SELECT count(*) from q_mailbox_record_expunged;")) {
				ResultSet executeQuery = stm.executeQuery();
				executeQuery.next();
				int count = executeQuery.getInt(1);
				assertEquals(1, count);
			}

			getService(SecurityContext.SYSTEM).all().forEach(rec -> {
				System.err.println("ENTRY: " + rec.value.imapUid);
			});

			List<ItemValue<MailboxRecord>> records = getService(SecurityContext.SYSTEM).all();
			assertEquals(3, records.size());

			for (ItemValue<MailboxRecord> itemValue : records) {
				assertFalse(itemValue.value.imapUid == 10);
			}

		} catch (Exception e) {
			fail("Error while executing expiration task " + e.getMessage());
		}
	}

	@Test
	public void testDeleteOrphanBodies() throws SQLException {
		IReplicatedDataExpiration expirationService = getExpirationService();
		MailboxRecord rec1 = null;
		try {
			rec1 = createRecord(10, 15, true);
			createRecord(20, 5, true);
			createRecord(30, 15, false);
			createRecord(40, 5, false);
			assertEquals(4, getService(SecurityContext.SYSTEM).all().size());

			expirationService.deleteExpiredExpunged(10);

			try (Connection con = JdbcTestHelper.getInstance().getMailboxDataDataSource().getConnection();
					PreparedStatement stm = con.prepareStatement("SELECT count(*) from q_mailbox_record_expunged;")) {
				ResultSet count = stm.executeQuery();
				count.next();
				assertEquals(1, count.getInt(1));
			}

			List<ItemValue<MailboxRecord>> records = getService(SecurityContext.SYSTEM).all();
			assertEquals(3, records.size());
			for (ItemValue<MailboxRecord> itemValue : records) {
				assertFalse(itemValue.value.imapUid == 10);
			}
		} catch (Exception e) {
			fail("Error while executing expiration task " + e.getMessage());
			return;
		}

		expirationService.deleteOrphanMessageBodies();

		/*
		 * t_message_body_purge_queue should have been filled by the trigger on
		 * t_mailbox_record
		 */
		try (Connection conn = JdbcTestHelper.getInstance().getMailboxDataDataSource().getConnection();
				PreparedStatement st = conn.prepareStatement(
						"SELECT message_body_guid, created, removed FROM t_message_body_purge_queue")) {
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				String guid = rs.getString(1);
				Timestamp created = rs.getTimestamp(2);
				Date removed = rs.getDate(3);
				System.err.println("guid: " + guid + " created: " + created + " removed: " + removed);
				assertEquals("\\x" + rec1.messageBody, guid);
				assertFalse(created == null);
				assertTrue(removed == null);
			}
		}

		/* Set the created column in the past */
		try (Connection conn = JdbcTestHelper.getInstance().getMailboxDataDataSource().getConnection();
				PreparedStatement st = conn.prepareStatement(
						"UPDATE t_message_body_purge_queue SET created = created - '30 days'::interval")) {
			st.executeUpdate();
		}

		expirationService.deleteOrphanMessageBodies();

		/*
		 * Try to remove entities from t_message_body_purge_queue older than 1 day, this
		 * should not do anything as entries are younger than that
		 */
		TaskRef t = expirationService.deleteMessageBodiesFromObjectStore(1);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), t);

		try (Connection conn = JdbcTestHelper.getInstance().getMailboxDataDataSource().getConnection();
				PreparedStatement st = conn.prepareStatement(
						"SELECT count(*) FROM t_message_body_purge_queue WHERE removed IS NOT NULL")) {
			ResultSet rs = st.executeQuery();
			rs.next();
			assertEquals(1, rs.getInt(1));
		}

		/* Set the removed column in the past */
		try (Connection conn = JdbcTestHelper.getInstance().getMailboxDataDataSource().getConnection();
				PreparedStatement st = conn.prepareStatement(
						"UPDATE t_message_body_purge_queue SET removed = removed - '30 days'::interval")) {
			st.executeUpdate();
		}

		t = expirationService.deleteMessageBodiesFromObjectStore(1);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), t);

		/* Now, all entries should have been removed */
		try (Connection conn = JdbcTestHelper.getInstance().getMailboxDataDataSource().getConnection();
				PreparedStatement st = conn.prepareStatement("SELECT count(*) FROM t_message_body_purge_queue")) {
			ResultSet rs = st.executeQuery();
			rs.next();
			assertEquals(0, rs.getInt(1));
		}
	}

	private MailboxRecord createRecord(long imapUid, int lastUpdated, boolean expunged) throws SQLException {
		IDbMessageBodies mboxes = getBodies(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		ReadStream<Buffer> emlReadStream = openResource("data/with_inlines.eml");
		Stream bmStream = VertxStream.stream(emlReadStream);
		String bodyUid = CyrusGUID.randomGuid();
		mboxes.create(bodyUid, bmStream);

		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);
		MailboxRecord mboxrecord = new MailboxRecord();
		mboxrecord.imapUid = imapUid;
		mboxrecord.internalDate = new Date();
		mboxrecord.lastUpdated = getDate(lastUpdated);
		mboxrecord.messageBody = bodyUid;
		String mailUid = "uid." + imapUid;
		records.create(mailUid, mboxrecord);
		if (expunged) {
			mboxrecord.internalFlags = Arrays.asList(InternalFlag.expunged);
			records.update(mailUid, mboxrecord);

			try (Connection con = JdbcTestHelper.getInstance().getMailboxDataDataSource().getConnection();
					PreparedStatement stm = con
							.prepareStatement("UPDATE q_mailbox_record_expunged set created = ? WHERE imap_uid = ?;")) {
				stm.setDate(1, getSqlDate(lastUpdated));
				stm.setLong(2, imapUid);
				int updated = stm.executeUpdate();
				assertEquals(1, updated);
			}
		}
		return mboxrecord;
	}

	private Date getDate(int lastUpdated) {
		LocalDate localDate = LocalDate.now();
		LocalDate adapted = localDate.minusDays(lastUpdated);
		return Date.from(adapted.atStartOfDay(ZoneId.systemDefault()).toInstant());

	}

	private java.sql.Date getSqlDate(int lastUpdated) {
		LocalDate localDate = LocalDate.now();
		LocalDate adapted = localDate.minusDays(lastUpdated);
		return java.sql.Date.valueOf(adapted);
	}

	protected IDbMailboxRecords getService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMailboxRecords.class, mboxUniqueId);
	}

	protected IDbMessageBodies getBodies(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMessageBodies.class, partition);
	}

	protected IReplicatedDataExpiration getExpirationService() {
		BmTestContext testCtx = new BmTestContext(SecurityContext.SYSTEM);
		return new ReplicatedDataExpirationService(testCtx, JdbcTestHelper.getInstance().getMailboxDataDataSource(),
				PopulateHelper.FAKE_CYRUS_IP);
	}

}
