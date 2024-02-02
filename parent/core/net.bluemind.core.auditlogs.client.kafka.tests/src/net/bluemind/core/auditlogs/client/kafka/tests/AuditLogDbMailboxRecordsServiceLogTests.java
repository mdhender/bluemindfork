package net.bluemind.core.auditlogs.client.kafka.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Test;

import com.google.common.base.Strings;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.IAuditLogMgmt;
import net.bluemind.core.auditlogs.client.loader.AuditLogLoader;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemChangeLogEntry;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.service.ChangeLogUtil;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.tests.BmTestContext;

public class AuditLogDbMailboxRecordsServiceLogTests extends AbstractMailboxRecordsServiceTests<IDbMailboxRecords> {

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

	@Test
	public void createAndUpdateMailboxRecordChangeFlags() throws ServerFault, IOException {
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

		AuditLogLoader auditLogLoader = new AuditLogLoader();
		SecurityContext secCtx = new SecurityContext("sid", userUid, Collections.emptyList(), Collections.emptyList(),
				domainUid);
		auditLogLoader.getItemChangelogClient();
		String containerUid = "mbox_records_" + mboxUniqueId;
		IContainers containers = ServerSideServiceProvider.getProvider(secCtx).instance(IContainers.class);
		ContainerDescriptor desc = containers.get(containerUid);
		Container container = Container.create(desc.uid, desc.type, desc.name, desc.owner, desc.domainUid,
				desc.defaultContainer);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			ItemChangelog itemChangelog = ChangeLogUtil.getItemChangeLog(mailRecord1.uid, 0L, new BmTestContext(secCtx),
					container);
			return 6L == itemChangelog.entries.size();
		});
		ItemChangelog itemChangelog = ChangeLogUtil.getItemChangeLog(mailRecord1.uid, 0L, new BmTestContext(secCtx),
				container);

		assertEquals(6, itemChangelog.entries.size());

		assertEquals(3L, itemChangelog.entries.stream().filter(e -> e.type.equals(Type.Created)).toList().size());
		assertEquals(2L, itemChangelog.entries.stream().filter(e -> e.type.equals(Type.Updated)).toList().size());
		assertEquals(1L, itemChangelog.entries.stream().filter(e -> e.type.equals(Type.Deleted)).toList().size());
	}

	@Test
	public void createMailboxRecordWithAttachmentVoicemailInvitation() throws ServerFault, IOException {
		ItemValue<MailboxRecord> mailRecord1 = createBodyAndRecord(1, adaptDate(5), "data/with_voicemail.eml");

		AuditLogLoader auditLogProvider = new AuditLogLoader();
		IAuditLogMgmt auditLogManager = auditLogProvider.getManager();

		AuditLogLoader auditLogLoader = new AuditLogLoader();
		SecurityContext secCtx = new SecurityContext("sid", userUid, Collections.emptyList(), Collections.emptyList(),
				domainUid);
		auditLogLoader.getItemChangelogClient();
		String containerUid = "mbox_records_" + mboxUniqueId;
		IContainers containers = ServerSideServiceProvider.getProvider(secCtx).instance(IContainers.class);
		ContainerDescriptor desc = containers.get(containerUid);
		Container container = Container.create(desc.uid, desc.type, desc.name, desc.owner, desc.domainUid,
				desc.defaultContainer);

		assertTrue(auditLogManager.hasAuditLogBackingStore(domainUid));
		Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> {
			ItemChangelog itemChangelog = ChangeLogUtil.getItemChangeLog(mailRecord1.uid, 0L, new BmTestContext(secCtx),
					container);
			return 1L == itemChangelog.entries.size();
		});
		ItemChangelog itemChangelog = ChangeLogUtil.getItemChangeLog(mailRecord1.uid, 0L, new BmTestContext(secCtx),
				container);

		assertEquals(1, itemChangelog.entries.size());
		ItemChangeLogEntry firstAuditLogEntry = itemChangelog.entries.get(0);
		assertEquals(mailRecord1.uid, firstAuditLogEntry.itemUid);
		assertEquals(Type.Created, firstAuditLogEntry.type);

		itemChangelog = ChangeLogUtil.getItemChangeLog(mailRecord1.uid, 0L, new BmTestContext(secCtx), container);
		assertEquals(1, itemChangelog.entries.size());

		auditLogManager.removeAuditLogBackingStore(domainUid);
		assertFalse(auditLogManager.hasAuditLogBackingStore(domainUid));
	}

	protected IDbMailboxRecords getService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMailboxRecords.class, mboxUniqueId);
	}

	protected IDbMessageBodies getBodies(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMessageBodies.class, partition);
	}

	class CyrusGUID {

		private CyrusGUID() {
		}

		private static final Random r = new Random();

		public static String randomGuid() {
			String left = UUID.randomUUID().toString().replace("-", "");
			String right = Strings.padStart(Integer.toHexString(r.nextInt()), 8, '0');
			return left + right;
		}

	}
}
