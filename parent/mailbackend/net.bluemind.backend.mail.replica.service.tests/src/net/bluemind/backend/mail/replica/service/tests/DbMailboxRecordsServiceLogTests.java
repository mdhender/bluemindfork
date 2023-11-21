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

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.service.tests.compat.CyrusGUID;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.AuditLogUpdateStatus.MessageCriticity;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogStoreConfig;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.lib.elasticsearch.ESearchActivator;

public class DbMailboxRecordsServiceLogTests extends AbstractMailboxRecordsServiceTests<IDbMailboxRecords> {

	private final String dataStreamName = AuditLogStoreConfig.resolveDataStreamName(domainUid);

	private ItemValue<MailboxRecord> createBodyAndRecord(int imapUid, Date internalDate, String eml) {
		IDbMessageBodies mboxes = getBodies(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		ReadStream<Buffer> emlReadStream = openResource(eml);
		Stream bmStream = VertxStream.stream(emlReadStream);
		String bodyUid = CyrusGUID.randomGuid();
		mboxes.create(bodyUid, bmStream);

		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);
		assertNotNull(records);
		MailboxRecord mailboxRecord = new MailboxRecord();
		mailboxRecord.imapUid = imapUid;
		mailboxRecord.internalDate = internalDate;
		mailboxRecord.lastUpdated = mailboxRecord.internalDate;
		mailboxRecord.messageBody = bodyUid;
		mailboxRecord.flags = Arrays.asList(MailboxItemFlag.System.Draft.value());
		String mailUid = "uid." + imapUid;
		records.create(mailUid, mailboxRecord);

		return records.getComplete(mailUid);
	}

	private ItemValue<MailboxRecord> createBodyAndRecord(int imapUid, Date internalDate, String eml,
			List<MailboxItemFlag> flags) {
		IDbMessageBodies mboxes = getBodies(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		ReadStream<Buffer> emlReadStream = openResource(eml);
		Stream bmStream = VertxStream.stream(emlReadStream);
		String bodyUid = CyrusGUID.randomGuid();
		mboxes.create(bodyUid, bmStream);

		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);
		assertNotNull(records);
		MailboxRecord mailboxRecord = new MailboxRecord();
		mailboxRecord.imapUid = imapUid;
		mailboxRecord.internalDate = internalDate;
		mailboxRecord.lastUpdated = mailboxRecord.internalDate;
		mailboxRecord.messageBody = bodyUid;
		mailboxRecord.flags = flags;
		String mailUid = "uid." + imapUid;
		records.create(mailUid, mailboxRecord);

		return records.getComplete(mailUid);
	}

	@Test
	public void createAndUpdateMailboxRecordChangeFlags() throws ServerFault, ElasticsearchException, IOException {
		ItemValue<MailboxRecord> mailRecord1 = createBodyAndRecord(1, adaptDate(5), "data/sort_1.eml");
		ItemValue<MailboxRecord> mailRecord2 = createBodyAndRecord(2, adaptDate(10), "data/sort_2.eml");
		ItemValue<MailboxRecord> mailRecord3 = createBodyAndRecord(3, adaptDate(12), "data/sort_3.eml");

		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);

		ElasticsearchClient esClient = ESearchActivator.getClient();

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
		mailRecord1.value.flags = Arrays.asList(MailboxItemFlag.System.Flagged.value(),
				MailboxItemFlag.System.Answered.value());
		mailRecord2.value.flags = Arrays.asList(MailboxItemFlag.System.Deleted.value(),
				MailboxItemFlag.System.Seen.value());
		mailRecord3.value.flags = Arrays.asList(MailboxItemFlag.System.Seen.value());
		records.update(mailRecord1.uid, mailRecord1.value);
		records.update(mailRecord2.uid, mailRecord2.value);
		records.update(mailRecord3.uid, mailRecord3.value);

		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("container.uid").value("mbox_records_" + mboxUniqueId))
									._toQuery())
							.must(TermQuery.of(t -> t.field("logtype").value("mailbox_records"))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 3L == response.hits().total().value();
		});
		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value("mbox_records_" + mboxUniqueId))
								._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value("mailbox_records"))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(2L, response.hits().total().value());

		AuditLogEntry firstAuditLogEntry = response.hits().hits().get(0).source();
		AuditLogEntry secondAuditLogEntry = response.hits().hits().get(1).source();

		assertEquals("first subject", firstAuditLogEntry.content.description());
		assertEquals("Removed Flags:\n\\Draft\nAdded Flags:\n\\Flagged,\\Answered\n", firstAuditLogEntry.updatemessage);
		assertEquals(MessageCriticity.MAJOR, firstAuditLogEntry.criticity);

		assertEquals("third subject", secondAuditLogEntry.content.description());
		assertEquals("Removed Flags:\n\\Draft\nAdded Flags:\n\\Seen\n", secondAuditLogEntry.updatemessage);
		assertEquals(MessageCriticity.MINOR, secondAuditLogEntry.criticity);

		response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value("mbox_records_" + mboxUniqueId))
								._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value("mailbox_records"))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Deleted.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(1L, response.hits().total().value());
		firstAuditLogEntry = response.hits().hits().get(0).source();
		assertEquals("second subject", firstAuditLogEntry.content.description());
	}

	@Test
	public void createAndUpdateMailboxRecordChangeFlagsReadToUnread()
			throws ServerFault, ElasticsearchException, IOException {
		ItemValue<MailboxRecord> mailRecord1 = createBodyAndRecord(1, adaptDate(5), "data/sort_1.eml",
				Arrays.asList(MailboxItemFlag.System.Seen.value()));

		IDbMailboxRecords records = getService(SecurityContext.SYSTEM);

		ElasticsearchClient esClient = ESearchActivator.getClient();

		List<Long> sortedIds = records.sortedIds(null);
		assertNotNull(sortedIds);
		assertEquals(1, sortedIds.size());
		assertTrue(mailRecord1.internalId == sortedIds.get(0).longValue());

		// Remove Seen flag
		mailRecord1.value.flags = Arrays.asList();
		records.update(mailRecord1.uid, mailRecord1.value);

		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("container.uid").value("mbox_records_" + mboxUniqueId))
									._toQuery())
							.must(TermQuery.of(t -> t.field("logtype").value("mailbox_records"))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("container.uid").value("mbox_records_" + mboxUniqueId))
									._toQuery())
							.must(TermQuery.of(t -> t.field("logtype").value("mailbox_records"))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value("mbox_records_" + mboxUniqueId))
								._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value("mailbox_records"))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(1L, response.hits().total().value());

		AuditLogEntry firstAuditLogEntry = response.hits().hits().get(0).source();

		assertEquals("first subject", firstAuditLogEntry.content.description());
		assertEquals("Removed Flags:\n\\Seen\n", firstAuditLogEntry.updatemessage);
		assertEquals(MessageCriticity.MINOR, firstAuditLogEntry.criticity);
	}

	@Test
	public void createMailboxRecordWithAttachmentVoicemailInvitation()
			throws ServerFault, ElasticsearchException, IOException {
		createBodyAndRecord(1, adaptDate(5), "data/with_voicemail.eml");

		ElasticsearchClient esClient = ESearchActivator.getClient();
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("container.uid").value("mbox_records_" + mboxUniqueId))
									._toQuery())
							.must(TermQuery.of(t -> t.field("logtype").value("mailbox_records"))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value("mbox_records_" + mboxUniqueId))
								._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value("mailbox_records"))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				AuditLogEntry.class);

		AuditLogEntry firstAuditLogEntry = response.hits().hits().get(0).source();
		assertEquals(2, firstAuditLogEntry.content.has().size());
	}

	protected IDbMailboxRecords getService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMailboxRecords.class, mboxUniqueId);
	}

	protected IDbMessageBodies getBodies(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMessageBodies.class, partition);
	}

}
