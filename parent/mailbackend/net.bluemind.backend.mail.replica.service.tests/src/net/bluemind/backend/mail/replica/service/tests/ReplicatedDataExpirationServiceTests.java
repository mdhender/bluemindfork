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
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.vertx.java.core.streams.ReadStream;

import net.bluemind.backend.cyrus.replication.testhelper.CyrusGUID;
import net.bluemind.backend.cyrus.replication.testhelper.InputStreamWrapper;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IReplicatedDataExpiration;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.service.internal.ReplicatedDataExpirationService;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.tests.BmTestContext;

public class ReplicatedDataExpirationServiceTests extends AbstractMailboxRecordsServiceTests<IDbMailboxRecords> {

	@Test
	public void testDeletingExpiredMailsOnEmptyTable() {
		try {
			getExpirationService().deleteExpired(7);
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

			TaskRef deleteExpired = getExpirationService().deleteExpired(7);
			TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), deleteExpired);

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

	private void createRecord(long imapUid, int lastUpdated, boolean expunged) {
		IDbMessageBodies mboxes = getBodies(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		ReadStream<InputStreamWrapper> emlReadStream = openResource("data/with_inlines.eml");
		Stream bmStream = VertxStream.stream(emlReadStream);
		String bodyUid = CyrusGUID.randomGuid();
		mboxes.create(bodyUid, bmStream);

		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);
		MailboxRecord record = new MailboxRecord();
		record.imapUid = imapUid;
		record.internalDate = new Date();
		record.lastUpdated = getDate(lastUpdated);
		record.messageBody = bodyUid;
		if (expunged) {
			record.internalFlags = Arrays.asList(InternalFlag.expunged);
		}
		String mailUid = "uid." + imapUid;
		records.create(mailUid, record);
	}

	private Date getDate(int lastUpdated) {
		LocalDate localDate = LocalDate.now();
		LocalDate adapted = localDate.minusDays(lastUpdated);
		return adapted.toDate();
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
				"bm/core");
	}

}
