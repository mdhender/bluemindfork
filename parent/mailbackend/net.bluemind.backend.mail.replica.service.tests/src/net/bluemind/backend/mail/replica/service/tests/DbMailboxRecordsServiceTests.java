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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusGUID;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.SortDescriptor.Direction;
import net.bluemind.core.container.model.SortDescriptor.Field;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;

public class DbMailboxRecordsServiceTests extends AbstractMailboxRecordsServiceTests<IDbMailboxRecords> {

	@Test
	public void createBodyAndRecord() {
		ItemValue<MailboxRecord> loaded = createBodyAndRecord(1, new Date(), "data/with_inlines.eml");
		assertNotNull(loaded);
		MailboxItem value = loaded.value;
		System.out.println("Reloaded " + value.imapUid + ", flags: " + value.flags + ", body: " + value.body);
	}

	private ItemValue<MailboxRecord> createBodyAndRecord(int imapUid, Date internalDate, String eml) {
		IDbMessageBodies mboxes = getBodies(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		ReadStream<Buffer> emlReadStream = openResource(eml);
		Stream bmStream = VertxStream.stream(emlReadStream);
		String bodyUid = CyrusGUID.randomGuid();
		mboxes.create(bodyUid, bmStream);

		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);
		assertNotNull(records);
		MailboxRecord record = new MailboxRecord();
		record.imapUid = imapUid;
		record.internalDate = internalDate;
		record.lastUpdated = record.internalDate;
		record.messageBody = bodyUid;
		String mailUid = "uid." + imapUid;
		records.create(mailUid, record);

		return records.getComplete(mailUid);
	}

	@Test
	public void sortedIdsStrategy_date() {

		ItemValue<MailboxRecord> mailRecord1 = createBodyAndRecord(1, adaptDate(5), "data/sort_1.eml");
		ItemValue<MailboxRecord> mailRecord2 = createBodyAndRecord(2, adaptDate(10), "data/sort_2.eml");
		ItemValue<MailboxRecord> mailRecord3 = createBodyAndRecord(3, adaptDate(12), "data/sort_3.eml");

		// no sort or filter (only default applies)
		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);
		List<Long> sortedIds = records.sortedIds(null);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(2).longValue());

		// sort by date desc
		SortDescriptor sorted = new SortDescriptor();
		Field field = new Field();
		field.column = "internal_date";
		field.dir = Direction.Desc;
		sorted.fields = Arrays.asList(field);
		sortedIds = records.sortedIds(sorted);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(2).longValue());

		// sort by date asc
		field.dir = Direction.Asc;
		sorted.fields = Arrays.asList(field);
		sortedIds = records.sortedIds(sorted);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord3.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord1.internalId == sortedIds.get(2).longValue());
	}

	@Test
	public void sortedIdsStrategy_multi() {

		ItemValue<MailboxRecord> mailRecord1 = createBodyAndRecord(1, adaptDate(5), "data/sort_1.eml");
		ItemValue<MailboxRecord> mailRecord2 = createBodyAndRecord(2, adaptDate(10), "data/sort_2.eml");
		ItemValue<MailboxRecord> mailRecord3 = createBodyAndRecord(3, adaptDate(12), "data/sort_3.eml");

		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);

		// sort by date desc
		SortDescriptor sorted = new SortDescriptor();
		Field fieldDate = new Field();
		fieldDate.column = "internal_date";
		fieldDate.dir = Direction.Desc;
		Field fieldSuject = new Field();
		fieldSuject.column = "subject";
		fieldSuject.dir = Direction.Asc;
		sorted.fields = Arrays.asList(fieldDate, fieldSuject);
		List<Long> sortedIds = records.sortedIds(sorted);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(2).longValue());

		// sort by date asc
		fieldDate.dir = Direction.Asc;
		fieldSuject.dir = Direction.Desc;
		sorted.fields = Arrays.asList(fieldDate, fieldSuject);
		sortedIds = records.sortedIds(sorted);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord3.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord1.internalId == sortedIds.get(2).longValue());
	}

	@Test
	public void sortedIdsStrategy_subject() {

		ItemValue<MailboxRecord> mailRecord1 = createBodyAndRecord(1, adaptDate(5), "data/sort_1.eml");
		ItemValue<MailboxRecord> mailRecord2 = createBodyAndRecord(2, adaptDate(10), "data/sort_2.eml");
		ItemValue<MailboxRecord> mailRecord3 = createBodyAndRecord(3, adaptDate(12), "data/sort_3.eml");

		// no sort or filter (only default applies)
		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);
		List<Long> sortedIds = records.sortedIds(null);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(2).longValue());

		// sort by subject desc
		SortDescriptor sorted = new SortDescriptor();
		Field field = new Field();
		field.column = "subject";
		field.dir = Direction.Desc;
		sorted.fields = Arrays.asList(field);
		sortedIds = records.sortedIds(sorted);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord3.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord1.internalId == sortedIds.get(2).longValue());

		// sort by subject asc
		field.dir = Direction.Asc;
		sorted.fields = Arrays.asList(field);
		sortedIds = records.sortedIds(sorted);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(2).longValue());
	}

	@Test
	public void sortedIdsStrategy_sender() {

		ItemValue<MailboxRecord> mailRecord1 = createBodyAndRecord(1, adaptDate(5), "data/sort_1.eml");
		ItemValue<MailboxRecord> mailRecord2 = createBodyAndRecord(2, adaptDate(10), "data/sort_2.eml");
		ItemValue<MailboxRecord> mailRecord3 = createBodyAndRecord(3, adaptDate(12), "data/sort_3.eml");

		// no sort or filter (only default applies)
		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);
		List<Long> sortedIds = records.sortedIds(null);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(2).longValue());

		// sort by sender desc
		SortDescriptor sorted = new SortDescriptor();
		Field field = new Field();
		field.column = "sender";
		field.dir = Direction.Desc;
		sorted.fields = Arrays.asList(field);
		sortedIds = records.sortedIds(sorted);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord3.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord1.internalId == sortedIds.get(2).longValue());

		// sort by sender asc
		field.dir = Direction.Asc;
		sorted.fields = Arrays.asList(field);
		sortedIds = records.sortedIds(sorted);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(2).longValue());
	}

	@Test
	public void sortedIdsStrategy_size() {

		ItemValue<MailboxRecord> mailRecord1 = createBodyAndRecord(1, adaptDate(5), "data/sort_1.eml");
		ItemValue<MailboxRecord> mailRecord2 = createBodyAndRecord(2, adaptDate(10), "data/sort_2.eml");
		ItemValue<MailboxRecord> mailRecord3 = createBodyAndRecord(3, adaptDate(12), "data/sort_3.eml");

		// no sort or filter (only default applies)
		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);
		List<Long> sortedIds = records.sortedIds(null);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(2).longValue());

		// sort by size desc
		SortDescriptor sorted = new SortDescriptor();
		Field field = new Field();
		field.column = "size";
		field.dir = Direction.Desc;
		sorted.fields = Arrays.asList(field);
		sortedIds = records.sortedIds(sorted);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord3.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord1.internalId == sortedIds.get(2).longValue());

		// sort by size asc
		field.dir = Direction.Asc;
		sorted.fields = Arrays.asList(field);
		sortedIds = records.sortedIds(sorted);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(2).longValue());
	}

	@Test
	public void filterIdsStrategy_notDeleted() {

		ItemValue<MailboxRecord> mailRecord1 = createBodyAndRecord(1, adaptDate(5), "data/sort_1.eml");
		ItemValue<MailboxRecord> mailRecord2 = createBodyAndRecord(2, adaptDate(10), "data/sort_2.eml");
		ItemValue<MailboxRecord> mailRecord3 = createBodyAndRecord(3, adaptDate(12), "data/sort_3.eml");

		// no sort or filter (only default applies)
		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);
		List<Long> sortedIds = records.sortedIds(null);

		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(2).longValue());

		// after delete a record
		records.delete(mailRecord2.uid);

		// not deleted filter (default sort)
		sortedIds = records.sortedIds(null);
		assertNotNull(sortedIds);
		assertEquals(2, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(1).longValue());

		// no sort or filter (only default applies)
		SortDescriptor sorted = new SortDescriptor();
		sorted.filter = ItemFlagFilter.create().mustNot(ItemFlag.Deleted);
		sortedIds = records.sortedIds(sorted);
		assertNotNull(sortedIds);
		assertEquals(2, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(1).longValue());
	}

	@Test
	public void filterIdsStrategy_flagged() {

		ItemValue<MailboxRecord> mailRecord1 = createBodyAndRecord(1, adaptDate(5), "data/sort_1.eml");
		ItemValue<MailboxRecord> mailRecord2 = createBodyAndRecord(2, adaptDate(10), "data/sort_2.eml");
		ItemValue<MailboxRecord> mailRecord3 = createBodyAndRecord(3, adaptDate(12), "data/sort_3.eml");

		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);

		assertTrue(mailRecord1.value.flags.isEmpty());
		assertTrue(mailRecord2.value.flags.isEmpty());
		assertTrue(mailRecord3.value.flags.isEmpty());

		// no sort or filter (only default applies)
		List<Long> sortedIds = records.sortedIds(null);
		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(2).longValue());

		// important filter (default sort)
		SortDescriptor sorted = new SortDescriptor();
		ItemFlagFilter create = ItemFlagFilter.create().must(ItemFlag.Important).mustNot(ItemFlag.Deleted);
		sorted.filter = create;
		sortedIds = records.sortedIds(sorted);

		assertNotNull(sortedIds);
		assertTrue(sortedIds.isEmpty());

		// important filter (default sort) after update a mail as important
		mailRecord1.value.flags = Arrays.asList(MailboxItemFlag.System.Flagged.value());
		records.update(mailRecord1.uid, mailRecord1.value);
		sortedIds = records.sortedIds(sorted);

		assertNotNull(sortedIds);
		assertEquals(1, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
	}

	@Test
	public void filterIdsStrategy_unseen() {

		ItemValue<MailboxRecord> mailRecord1 = createBodyAndRecord(1, adaptDate(5), "data/sort_1.eml");
		ItemValue<MailboxRecord> mailRecord2 = createBodyAndRecord(2, adaptDate(10), "data/sort_2.eml");
		ItemValue<MailboxRecord> mailRecord3 = createBodyAndRecord(3, adaptDate(12), "data/sort_3.eml");

		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);

		// no sort or filter (only default applies)
		List<Long> sortedIds = records.sortedIds(null);
		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord2.internalId == sortedIds.get(1).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(2).longValue());

		// unseen filter (default sort)
		SortDescriptor sorted = new SortDescriptor();
		sorted.filter = ItemFlagFilter.create().mustNot(ItemFlag.Seen, ItemFlag.Deleted);
		sortedIds = records.sortedIds(sorted);
		assertNotNull(sortedIds);
		assertEquals(3, sortedIds.size());

		// unseen filter (default sort) after update a mail as seen
		mailRecord1.value.flags = Arrays.asList(MailboxItemFlag.System.Seen.value());
		records.update(mailRecord1.uid, mailRecord1.value);
		sortedIds = records.sortedIds(sorted);

		assertNotNull(sortedIds);
		assertEquals(2, sortedIds.size());
		assertTrue(mailRecord2.internalId == sortedIds.get(0).longValue());
		assertTrue(mailRecord3.internalId == sortedIds.get(1).longValue());
	}

	protected IDbMailboxRecords getService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMailboxRecords.class, mboxUniqueId);
	}

	protected IDbMessageBodies getBodies(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMessageBodies.class, partition);
	}

	private Date adaptDate(int daysBeforeNow) {
		LocalDate localDate = LocalDate.now();
		LocalDate adapted = localDate.minusDays(daysBeforeNow);
		return Date.from(adapted.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

}
