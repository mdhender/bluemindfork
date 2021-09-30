/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.cyrus.replication.client.SyncClient;
import net.bluemind.backend.mail.api.Conversation;
import net.bluemind.backend.mail.api.DispositionType;
import net.bluemind.backend.mail.api.IBaseMailboxFolders;
import net.bluemind.backend.mail.api.IItemsTransfer;
import net.bluemind.backend.mail.api.IMailConversation;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.IUserInbox;
import net.bluemind.backend.mail.api.ImportMailboxItemSet;
import net.bluemind.backend.mail.api.ImportMailboxItemSet.MailboxItemId;
import net.bluemind.backend.mail.api.ImportMailboxItemsStatus;
import net.bluemind.backend.mail.api.ImportMailboxItemsStatus.ImportStatus;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.MessageBody.Recipient;
import net.bluemind.backend.mail.api.MessageBody.RecipientKind;
import net.bluemind.backend.mail.api.flags.FlagUpdate;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.api.utils.PartsWalker;
import net.bluemind.backend.mail.parsing.Bodies;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationAnnotations;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationArtifacts;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.backend.mail.replica.api.MailboxAnnotation;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.MailboxSub;
import net.bluemind.backend.mail.replica.api.QuotaRoot;
import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.backend.mail.replica.service.ReplicationEvents;
import net.bluemind.backend.mail.replica.service.internal.ItemsTransferService;
import net.bluemind.backend.mail.replica.service.internal.ReplicatedDataExpirationService;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.mime.MimeTree;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ReplicationStackTests extends AbstractRollingReplicationTests {

	private static final ItemFlagFilter UNREAD_NOT_DELETED = ItemFlagFilter.create().mustNot(ItemFlag.Deleted,
			ItemFlag.Seen);

	@Before
	@Override
	public void before() throws Exception {
		super.before();

		imapAsUser(sc -> {
			int added = sc.append("INBOX", testEml(), new FlagsList());
			assertTrue(added > 0);
			sc.select("INBOX");
			Collection<MimeTree> bs = sc.uidFetchBodyStructure(Arrays.asList(added));
			MimeTree tree = bs.iterator().next();
			System.out.println("Mail " + added + " added:\n" + tree);
			return null;
		});

		IServiceProvider prov = provider();

		IMailboxFolders userMboxesApi = prov.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> found = userMboxesApi.all();
		long delay = System.currentTimeMillis();
		while (found.isEmpty()) {
			Thread.sleep(50);
			if (System.currentTimeMillis() - delay > 30000) {
				throw new TimeoutException("Wait for inbox took more than 30sec");
			}
			found = userMboxesApi.all();
		}

		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> iv : found) {
			if (iv.value.name.equals("INBOX")) {
				inbox = iv;
				break;
			}
		}
		assertNotNull(inbox);
		System.err.println("Wait for record in inbox...");
		IMailboxItems recordsApi = prov.instance(IMailboxItems.class, inbox.uid);
		ContainerChangeset<Long> allById = recordsApi.changesetById(0L);
		delay = System.currentTimeMillis();
		while (allById.created.isEmpty()) {
			Thread.sleep(50);
			if (System.currentTimeMillis() - delay > 30000) {
				throw new TimeoutException("Wait for record took more than 30sec");
			}
			allById = recordsApi.changesetById(0L);
		}

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		this.allocations = idAllocator.allocateOfflineIds(2);

		System.err.println("before() is complete, starting test...");
	}

	@Test
	public void testXConvModSeq() throws Exception {
		AtomicReference<String> ret = new AtomicReference<>();
		SyncClient sc = new SyncClient("127.0.0.1", 2501);
		sc.connect().thenCompose(v -> {
			return sc.authenticate("admin0", "admin");
		}).thenCompose(v -> {
			return sc.getMailboxes(domainUid + "!user." + userUid);
		}).thenAccept(v -> {
			ret.set(String.join("", v.dataLines));
		}).join();
		assertTrue(ret.get().contains("XCONVMODSEQ"));
	}

	@Test
	public void updateFlag() throws IMAPException, InterruptedException {
		System.err.println("********* Starting updateFlagTest code");
		IServiceProvider prov = provider();

		IMailboxFolders userMboxesApi = prov.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> found = userMboxesApi.all();
		assertNotNull(found);
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> iv : found) {
			System.out.println("Got " + iv.value.name);
			if (iv.value.name.equals("INBOX")) {
				inbox = iv;
				System.out.println("INBOX has uid " + iv.uid);
				break;
			}
		}
		assertNotNull(inbox);
		IMailboxItems recordsApi = prov.instance(IMailboxItems.class, inbox.uid);
		ContainerChangeset<Long> allById = recordsApi.changesetById(0L);
		long version = allById.version;
		System.out.println("Version is at " + version);
		ItemValue<MailboxItem> item = null;
		for (Long rec : allById.created) {
			item = recordsApi.getCompleteById(rec);
			break;
		}
		assertNotNull(item);

		IMailboxItems userRecordsApi = prov.instance(IMailboxItems.class, inbox.uid);
		MailboxItem updated = item.value;
		updated.flags = Arrays.asList(new MailboxItemFlag("$Junit" + System.currentTimeMillis()));
		System.out.println("UPDATE STARTS...............");
		userRecordsApi.updateById(item.internalId, updated);
		int count = 2;
		long time = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			updated.flags = Arrays.asList(new MailboxItemFlag("$Roberto" + System.currentTimeMillis()));
			Ack ack = userRecordsApi.updateById(item.internalId, updated);
			System.out.println("Item version is now " + ack.version);
		}
		time = System.currentTimeMillis() - time;
		System.out.println("avg per update: " + ((double) time) / count + "ms.");
	}

	@Test
	public void addFlagAlreadySet() throws Exception {

		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");

		ItemValue<MailboxItem> mail = addDraft(inbox);

		IMailboxItems itemsApi = provider().instance(IMailboxItems.class, inbox.uid);

		ItemValue<MailboxItem> mailboxItem = itemsApi.getCompleteById(mail.internalId);
		assertFalse(mailboxItem.value.flags.contains(MailboxItemFlag.System.Seen.value()));

		System.err.println("SEEN");
		itemsApi.addFlag(FlagUpdate.of(mail.internalId, MailboxItemFlag.System.Seen.value()));
		mailboxItem = itemsApi.getCompleteById(mail.internalId);
		assertTrue(mailboxItem.value.flags.contains(MailboxItemFlag.System.Seen.value()));

		System.err.println("SEEN AGAIN");
		itemsApi.addFlag(FlagUpdate.of(mail.internalId, MailboxItemFlag.System.Seen.value()));
		mailboxItem = itemsApi.getCompleteById(mail.internalId);
		assertTrue(mailboxItem.value.flags.contains(MailboxItemFlag.System.Seen.value()));

	}

	@Test
	public void addFlagUnknownMail() {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");
		IMailboxItems itemsApi = provider().instance(IMailboxItems.class, inbox.uid);
		Ack ack = itemsApi.addFlag(FlagUpdate.of(98765432L, MailboxItemFlag.System.Seen.value()));
		assertEquals(0L, ack.version);
	}

	@Test
	public void updateFlagsBatch() throws IMAPException, InterruptedException {

		IServiceProvider prov = provider();

		IMailboxFolders userMboxesApi = prov.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> found = userMboxesApi.all();
		assertNotNull(found);
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> iv : found) {
			System.out.println("Got " + iv.value.name);
			if (iv.value.name.equals("INBOX")) {
				inbox = iv;
				System.out.println("INBOX has uid " + iv.uid);
			}
		}
		assertNotNull(inbox);
		IMailboxItems recordsApi = prov.instance(IMailboxItems.class, inbox.uid);
		ContainerChangeset<Long> allById = recordsApi.changesetById(0L);
		long version = allById.version;
		System.out.println("Version is at " + version);

		List<Long> itemsId = allById.created.stream().map(rec -> recordsApi.getCompleteById(rec))
				.filter(mailboxItem -> !mailboxItem.value.flags.contains(MailboxItemFlag.System.Seen.value()))
				.map(mailboxItem -> mailboxItem.internalId).collect(Collectors.toList());

		assertNotNull(itemsId);

		IMailboxItems userRecordsApi = prov.instance(IMailboxItems.class, inbox.uid);
		System.out.println("**** Will update " + itemsId.size() + " item(s).");
		Ack updatedVersion = userRecordsApi.addFlag(FlagUpdate.of(itemsId, MailboxItemFlag.System.Seen.value()));
		System.out.println("Version is now " + updatedVersion.version);
	}

	@Test
	public void updateAnsweredFlag() {
		IServiceProvider prov = provider();

		IMailboxFolders userMboxesApi = prov.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> found = userMboxesApi.all();
		assertNotNull(found);

		ItemValue<MailboxFolder> inbox = found.stream().filter(iv -> iv.value.name.equals("INBOX")).findFirst().get();
		assertNotNull(inbox);

		IMailboxItems recordsApi = prov.instance(IMailboxItems.class, inbox.uid);
		ContainerChangeset<Long> allById = recordsApi.changesetById(0L);

		long oneMailId = allById.created.stream().findFirst().get().longValue();
		recordsApi.addFlag(FlagUpdate.of(oneMailId, MailboxItemFlag.System.Answered.value()));
		assertTrue(recordsApi.getCompleteById(oneMailId).value.flags.contains(MailboxItemFlag.System.Answered.value()));
	}

	@Test
	public void createDraft() throws InterruptedException, IOException {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");
		assertNotNull(inbox);
		addDraft(inbox);
	}

	@Test
	public void getToUpdateDecomposeParts() throws InterruptedException, IOException {
		cleanTmpParts();
		assertEquals(Bodies.getFolder("sid").list().length, 0);

		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");
		long id = addDraft(inbox).internalId;

		IMailboxItems recs = provider().instance(IMailboxItems.class, inbox.uid);
		MailboxItem item = recs.getForUpdate(id).value;
		Part struct = item.body.structure;

		PartsWalker<Object> walker = new PartsWalker<>(null);
		walker.visit((Object c, Part p) -> {
			if (!p.mime.startsWith("multipart/")) {
				assertFalse(isImapAddress(p.address));
			}
		}, struct);

		assertEquals(Bodies.getFolder("sid").list().length, 3);

		// assert that fetch works with tmp parts
		walker.visit((Object c, Part p) -> {
			if (p.mime.equals("text/html")) {
				final Part textHtmlPart = p;
				Stream partContent = recs.fetch(item.imapUid, textHtmlPart.address, textHtmlPart.encoding,
						textHtmlPart.mime, textHtmlPart.charset, textHtmlPart.fileName);
				byte[] bytes;
				try {
					bytes = fetchPart(partContent).getBytes();
					assertTrue(bytes.length > 0);
				} catch (InterruptedException e) {
					fail();
				}
			}
		}, struct);
	}

	@Test
	public void getToUpdateDecomposeTEXT() throws InterruptedException, IOException {
		cleanTmpParts();
		assertEquals(Bodies.getFolder("sid").list().length, 0);

		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");

		String emlPath = "data/simple_text_html.eml";
		try (InputStream inputStream = AbstractReplicatedMailboxesServiceTests.class.getClassLoader()
				.getResourceAsStream(emlPath)) {
			Objects.requireNonNull(inputStream, "Failed to open resource @ " + emlPath);
			addMailToFolder(inputStream, inbox.uid);
		} catch (Exception e) {
			throw new ServerFault(e);
		}

		IMailboxItems recs = provider().instance(IMailboxItems.class, inbox.uid);
		Part struct = recs.getForUpdate(allocations.globalCounter - 1).value.body.structure;

		// eml prerequisites
		assertTrue(struct.children.isEmpty() && !struct.mime.startsWith("multipart/"));

		assertTrue(!isImapAddress(struct.address) && struct.address != "TEXT");
		assertEquals(Bodies.getFolder("sid").list().length, 1);
	}

	private void cleanTmpParts() {
		File sidFolder = Bodies.getFolder("sid");
		File[] parts = sidFolder.listFiles();
		for (File part : parts) {
			part.delete();
		}
		sidFolder.delete();
	}

	private boolean isImapAddress(String address) {
		return CharMatcher.inRange('0', '9').or(CharMatcher.is('.')).matchesAllOf(address);
	}

	@Test
	public void createThenQuickDeleteExpunge() throws IMAPException, InterruptedException, IOException {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");
		assertNotNull(inbox);
		ItemValue<MailboxItem> added = addDraft(inbox);
		FlagsList fl = new FlagsList();
		fl.add(Flag.DELETED);
		List<Integer> toUpd = Arrays.asList((int) added.value.imapUid);
		System.err.println("Updating " + toUpd + " with flags " + fl);
		imapAsUser(sc -> {
			sc.select("INBOX");
			sc.uidStore(toUpd, fl, true);
			sc.uidExpunge(toUpd);
			return null;
		});
		IMailboxItems recs = provider().instance(IMailboxItems.class, inbox.uid);
		int retry = 0;
		do {
			ItemValue<MailboxItem> refetched = recs.getCompleteById(added.internalId);
			if (refetched.value.flags.contains(MailboxItemFlag.System.Deleted.value())) {
				break;
			}
			Thread.sleep(200);
		} while (retry++ < 10);
		assertTrue("record was not marked as deleted", retry < 10);
	}

	@Test
	public void createTextDraft() throws IMAPException, InterruptedException, IOException {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");
		assertNotNull(inbox);
		IMailboxItems recordsApi = provider().instance(IMailboxItems.class, inbox.uid);
		Stream forUpload = VertxStream.stream(Buffer.buffer("Coucou\r\n".getBytes()));
		String partId = recordsApi.uploadPart(forUpload);
		assertNotNull(partId);
		System.out.println("Got partId " + partId);
		MailboxItem item = MailboxItem.of("toto", Part.create(null, "text/plain", partId));
		item.flags = Arrays.asList(new MailboxItemFlag("Pouic"));
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange oneId = idAllocator.allocateOfflineIds(1);
		long expectedId = oneId.globalCounter;
		item.body.recipients = Arrays.asList(Recipient.create(RecipientKind.Originator, "myName", "myAddress@mail.com"),
				Recipient.create(RecipientKind.Primary, "primaryName", "primary@mail.com"));
		recordsApi.createById(expectedId, item);
		ItemValue<MailboxItem> reloaded = recordsApi.getCompleteById(expectedId);
		assertEquals(reloaded.value.body.recipients.size(), 2);
		Optional<Recipient> from = reloaded.value.body.recipients.stream()
				.filter(recipient -> recipient.kind.equals(RecipientKind.Originator)).findFirst();
		Optional<Recipient> to = reloaded.value.body.recipients.stream()
				.filter(recipient -> recipient.kind.equals(RecipientKind.Primary)).findFirst();
		if (!from.isPresent() || !to.isPresent()) {
			fail("Recipients are not replicated");
		}
		assertEquals("myName", from.get().dn);
		assertEquals("myAddress@mail.com", from.get().address);
		assertEquals("primaryName", to.get().dn);
		assertEquals("primary@mail.com", to.get().address);
		assertNotNull(reloaded);
		assertNotNull(reloaded.value.body.headers);
		Optional<Header> idHeader = reloaded.value.body.headers.stream()
				.filter(h -> h.name.equals(MailApiHeaders.X_BM_INTERNAL_ID)).findAny();
		assertTrue(idHeader.isPresent());
		assertEquals(userUid + "#" + InstallationId.getIdentifier() + ":" + expectedId, idHeader.get().firstValue());

	}

	@Test
	public void deleteNotEmptyFolder() {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		String folderName = "f" + System.currentTimeMillis();
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(2);
		long folderId = ids.globalCounter;
		MailboxFolder folder = new MailboxFolder();
		folder.fullName = folderName;
		folder.name = folderName;
		ItemIdentifier createAck = mboxesApi.createForHierarchy(folderId, folder);
		assertNotNull(createAck);
		ItemValue<MailboxFolder> folderItem = mboxesApi.byName(folderName);

		assertNotNull(folderItem);
		System.err.println("folder, id " + folderItem.internalId + ", " + folderItem);
		IMailboxItems recordsApi = provider().instance(IMailboxItems.class, folderItem.uid);
		Stream forUpload = VertxStream.stream(Buffer.buffer("Coucou\r\n".getBytes()));
		String partId = recordsApi.uploadPart(forUpload);
		assertNotNull(partId);
		MailboxItem item = MailboxItem.of("toto", Part.create(null, "text/plain", partId));
		long expectedId = folderId + 1;
		recordsApi.createById(expectedId, item);
		ItemValue<MailboxItem> reloaded = recordsApi.getCompleteById(expectedId);
		assertNotNull(reloaded);
		System.err.println("Before delete....");
		mboxesApi.deleteById(folderItem.internalId);
		System.err.println("Delete returned");

		String folderUid = folderItem.uid;

		folderItem = mboxesApi.byName(folderName);
		assertNull(folderItem);

		folderItem = mboxesApi.getComplete(folderUid);
		assertNull(folderItem);

	}

	@Test
	public void deleteFolderShouldDeleteSubFolders() {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		String folderName = "f" + System.currentTimeMillis();
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(2);
		long folderId = ids.globalCounter;
		MailboxFolder folder = new MailboxFolder();
		folder.fullName = folderName;
		folder.name = folderName;
		ItemIdentifier createAck = mboxesApi.createForHierarchy(folderId, folder);
		assertNotNull(createAck);
		ItemValue<MailboxFolder> folderItem = mboxesApi.byName(folderName);

		String subFolderName = "s" + System.currentTimeMillis();
		MailboxFolder subFolder = new MailboxFolder();
		subFolder.fullName = null;
		subFolder.name = subFolderName;
		subFolder.parentUid = folderItem.uid;
		createAck = mboxesApi.createForHierarchy(folderId + 1, subFolder);
		assertNotNull(createAck);
		ItemValue<MailboxFolder> subFolderItem = mboxesApi.getCompleteById(createAck.id);

		mboxesApi.deepDelete(folderItem.internalId);

		String folderUid = folderItem.uid;
		folderItem = mboxesApi.byName(folderName);
		assertNull(folderItem);
		folderItem = mboxesApi.getComplete(folderUid);
		assertNull(folderItem);
		String subFolderUid = subFolderItem.uid;
		folderItem = mboxesApi.byName(subFolderItem.value.fullName);
		assertNull(folderItem);
		folderItem = mboxesApi.getComplete(subFolderUid);
		assertNull(folderItem);
	}

	@Test
	public void createMailshareSubFolderSpeed() throws Exception {
		for (int i = 0; i < 100; i++) {

			String msName = "ms" + System.currentTimeMillis();
			String msUid = UUID.randomUUID().toString();

			IMailshare ms = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailshare.class,
					domainUid);
			Mailshare mailshare = new Mailshare();
			mailshare.name = msName;
			mailshare.emails = Arrays.asList(Email.create(msName + "@" + domainUid, true, true));
			mailshare.routing = Routing.internal;
			ms.create(msUid, mailshare);

			IContainerManagement cmgmt = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(msUid));
			List<AccessControlEntry> accessControlList = new ArrayList<>(cmgmt.getAccessControlList());
			AccessControlEntry entry = new AccessControlEntry();
			entry.subject = userUid;
			entry.verb = Verb.All;
			accessControlList.add(entry);
			cmgmt.setAccessControlList(accessControlList);

			IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, msName);
			ItemValue<MailboxFolder> root = mboxesApi.byName(msName);

			String folderName = "msf" + System.currentTimeMillis();
			IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
			IdRange ids = idAllocator.allocateOfflineIds(2);
			long folderId = ids.globalCounter;
			MailboxFolder folder = new MailboxFolder();
			System.err.println("Creating " + folderName + ", child of " + root);
			folder.name = folderName;
			folder.parentUid = root.uid; // NOSONAR
			ItemIdentifier createAck = mboxesApi.createForHierarchy(folderId, folder);
			ItemValue<MailboxFolder> folderItem = mboxesApi.getCompleteById(createAck.id);
			assertEquals("round " + i, folderName, folderItem.displayName);

			String subFolderName = "mss" + System.currentTimeMillis();
			MailboxFolder subFolder = new MailboxFolder();
			System.err.println("Creating " + subFolderName + ", child of " + folderItem);
			subFolder.fullName = null;
			subFolder.name = subFolderName;
			subFolder.parentUid = folderItem.uid;
			createAck = mboxesApi.createForHierarchy(folderId + 1, subFolder);
			ItemValue<MailboxFolder> subFolderItem = mboxesApi.getCompleteById(createAck.id);
			assertEquals("round " + i, subFolderName, subFolderItem.displayName);

		}
	}

	@Test
	public void deleteFolderShouldDeleteSubFoldersMailshare() throws Exception {
		String msName = "ms" + System.currentTimeMillis();
		String msUid = UUID.randomUUID().toString();

		IMailshare ms = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailshare.class,
				domainUid);
		Mailshare mailshare = new Mailshare();
		mailshare.name = msName;
		mailshare.emails = Arrays.asList(Email.create(msName + "@" + domainUid, true, true));
		mailshare.routing = Routing.internal;
		ms.create(msUid, mailshare);

		IContainerManagement cmgmt = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(msUid));
		List<AccessControlEntry> accessControlList = new ArrayList<>(cmgmt.getAccessControlList());
		AccessControlEntry entry = new AccessControlEntry();
		entry.subject = userUid;
		entry.verb = Verb.All;
		accessControlList.add(entry);
		cmgmt.setAccessControlList(accessControlList);

		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, msName);
		ItemValue<MailboxFolder> root = mboxesApi.byName(msName);
		assertNotNull(root);

		String folderName = "msf" + System.currentTimeMillis();
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(2);
		long folderId = ids.globalCounter;
		MailboxFolder folder = new MailboxFolder();
		System.err.println("Creating " + folderName + ", child of " + root);
		folder.name = folderName;
		folder.parentUid = root.uid; // NOSONAR
		ItemIdentifier createAck = mboxesApi.createForHierarchy(folderId, folder);
		assertNotNull(createAck);
		ItemValue<MailboxFolder> folderItem = mboxesApi.getCompleteById(createAck.id);
		assertEquals(folderName, folderItem.displayName);

		String subFolderName = "mss" + System.currentTimeMillis();
		MailboxFolder subFolder = new MailboxFolder();
		System.err.println("Creating " + subFolderName + ", child of " + folderItem);
		subFolder.fullName = null;
		subFolder.name = subFolderName;
		subFolder.parentUid = folderItem.uid;
		createAck = mboxesApi.createForHierarchy(folderId + 1, subFolder);
		assertNotNull(createAck);
		ItemValue<MailboxFolder> subFolderItem = mboxesApi.getCompleteById(createAck.id);
		assertEquals(subFolderName, subFolderItem.displayName);

		System.err.println("deepDelete starts for " + folderItem);
		mboxesApi.deepDelete(folderItem.internalId);
		System.err.println("deepDelete ends.");

		System.err.println("start checking for " + folderItem.value);
		String folderUid = folderItem.uid;
		folderItem = mboxesApi.byName(folderName);
		assertNull(folderItem);
		folderItem = mboxesApi.getComplete(folderUid);
		assertNull(folderItem);
		System.err.println("start checking for " + subFolderItem.value);

		String subFolderUid = subFolderItem.uid;
		folderItem = mboxesApi.byName(subFolderItem.value.fullName);
		assertNull(folderItem);
		folderItem = mboxesApi.getComplete(subFolderUid);
		assertNull(folderItem);
	}

	@Test
	public void deleteFolderWithUTF7Chars() {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		String folderName = "Messages Reçus " + System.currentTimeMillis();
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(2);
		long folderId = ids.globalCounter;
		MailboxFolder folder = new MailboxFolder();
		folder.fullName = folderName;
		folder.name = folderName;
		ItemIdentifier createAck = mboxesApi.createForHierarchy(folderId, folder);
		assertNotNull(createAck);
		System.err.println("Waiting after createForHierarchy...");

		ItemValue<MailboxFolder> folderItem = mboxesApi.byName(folderName);

		assertNotNull("Cannot find '" + folderName + "' by its name.", folderItem);

		System.err.println("Before delete of " + folderItem + "...");
		mboxesApi.deleteById(folderItem.internalId);
		System.err.println("Delete returned");

		String folderUid = folderItem.uid;

		folderItem = mboxesApi.byName(folderName);
		assertNull(folderItem);

		folderItem = mboxesApi.getComplete(folderUid);
		assertNull(folderItem);
	}

	@Test
	public void deleteFolderLookingLikeUTF7() {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		String folderName = "Ben & Nuts " + System.currentTimeMillis();
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(2);
		long folderId = ids.globalCounter;
		MailboxFolder folder = new MailboxFolder();
		folder.fullName = folderName;
		folder.name = folderName;
		ItemIdentifier createAck = mboxesApi.createForHierarchy(folderId, folder);
		assertNotNull(createAck);
		System.err.println("Waiting after createForHierarchy...");

		ItemValue<MailboxFolder> folderItem = mboxesApi.byName(folderName);

		assertNotNull("Cannot find '" + folderName + "' by its name.", folderItem);

		System.err.println("Before delete of " + folderItem + "...");
		mboxesApi.deleteById(folderItem.internalId);
		System.err.println("Delete returned");

		String folderUid = folderItem.uid;

		folderItem = mboxesApi.byName(folderName);
		assertNull(folderItem);

		folderItem = mboxesApi.getComplete(folderUid);
		assertNull(folderItem);

	}

	@Test
	public void createAlreadyExists() {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		String folderName = "f" + System.currentTimeMillis();

		MailboxFolder folder = new MailboxFolder();
		folder.fullName = folderName;
		folder.name = folderName;
		ItemIdentifier createAck = mboxesApi.createBasic(folder);
		assertNotNull(createAck);

		ItemIdentifier alreadyExists = mboxesApi.createBasic(folder);
		assertEquals(createAck, alreadyExists);
	}

	@Test
	public void createById() throws IMAPException, InterruptedException, IOException {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		String folderName = "f" + System.currentTimeMillis();

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange oneId = idAllocator.allocateOfflineIds(1);
		long expectedFolderId = oneId.globalCounter;
		MailboxFolder folder = new MailboxFolder();
		folder.fullName = folderName;
		folder.name = folderName;
		ItemIdentifier createAck = mboxesApi.createForHierarchy(expectedFolderId, folder);
		assertNotNull(createAck);
		ItemValue<MailboxFolder> folderItem = mboxesApi.byName(folderName);
		assertEquals(createAck.id, folderItem.internalId);
		assertEquals(createAck.uid, folderItem.uid);
		IContainersFlatHierarchy hierarchyApi = provider().instance(IContainersFlatHierarchy.class, domainUid, userUid);
		ItemValue<ContainerHierarchyNode> inHierarchy = hierarchyApi.getCompleteById(expectedFolderId);
		assertNotNull(inHierarchy);
		assertEquals(expectedFolderId, inHierarchy.internalId);
		System.err.println("Got node: " + inHierarchy);
	}

	@Test
	public void fetchParts() throws IMAPException, InterruptedException {

		Sessions.get().put("sid",
				new SecurityContext("sid", userUid, Collections.emptyList(), Collections.emptyList(), domainUid));

		ClientSideServiceProvider clientProv = ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", "sid");
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();

		assertNotNull(allBoxes);
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> iv : allBoxes) {
			System.out.println("Got " + iv.value.name);
			if (iv.value.name.equals("INBOX")) {
				inbox = iv;
				System.out.println("INBOX has uid " + iv.uid);
			}
		}
		assertNotNull(inbox);
		IMailboxItems recordsApi = clientProv.instance(IMailboxItems.class, inbox.uid);
		ContainerChangeset<Long> allById = recordsApi.changesetById(0L);
		long version = allById.version;
		System.out.println("Version is at " + version);
		ItemValue<MailboxItem> item = null;
		for (Long rec : allById.created) {
			item = recordsApi.getCompleteById(rec);
		}
		assertNotNull(item);

		MessageBody bodyVal = item.value.body;
		JsonObject js = new JsonObject(JsonUtils.asString(bodyVal.structure));
		System.out.println("Structure is " + js.encodePrettily());
		Stream partStream = recordsApi.fetch(item.value.imapUid, "3", null, null, null, null);
		fetchPart(partStream);
	}

	private Buffer fetchPart(Stream s) throws InterruptedException {
		ReadStream<Buffer> vertxPart = VertxStream.read(s);
		CountDownLatch cdl = new CountDownLatch(1);
		Buffer fullPartContent = Buffer.buffer();
		Vertx vx = VertxPlatform.getVertx();
		vx.setTimer(1, tid -> {
			System.out.println("In timer..." + tid);
			vertxPart.endHandler(v -> cdl.countDown());
			vertxPart.handler(fullPartContent::appendBuffer);
			vertxPart.endHandler(v -> cdl.countDown());
			vertxPart.resume();
		});
		assertTrue(cdl.await(2, TimeUnit.SECONDS));
		return fullPartContent;
	}

	@Test
	public void createFolderReplication() throws IMAPException, InterruptedException {
		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> box : allBoxes) {
			if (box.value.name.equals("INBOX")) {
				inbox = box;
				break;
			}
		}
		assertNotNull(inbox);
		MailboxReplica toCreate = new MailboxReplica();
		long time = System.currentTimeMillis() / 1000;
		toCreate.name = "create" + time;
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange oneId = idAllocator.allocateOfflineIds(1);
		ItemIdentifier created = mboxesApi.createForHierarchy(oneId.globalCounter, toCreate);
		System.out.println("Got a create of version " + created.version);
		ContainerChangeset<Long> changed = mboxesApi.changesetById(created.version - 1);
		long newItemId = changed.created.get(0);
		System.out.println("From changelog: itemId should be " + newItemId);
		ItemValue<MailboxFolder> foundItem = mboxesApi.getCompleteById(newItemId);
		System.out.println("Found " + foundItem.value.name);

		String newName = "update" + time;
		foundItem.value.name = newName;
		foundItem.value.fullName = newName;
		Ack updated = mboxesApi.updateById(foundItem.internalId, foundItem.value);
		System.out.println("version after update: " + updated.version);
	}

	@Test
	public void createFolderEndingWithSpace() throws IMAPException, InterruptedException {
		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> box : allBoxes) {
			if (box.value.name.equals("INBOX")) {
				inbox = box;
				break;
			}
		}
		assertNotNull(inbox);
		MailboxReplica toCreate = new MailboxReplica();
		long time = System.currentTimeMillis() / 1000;
		toCreate.name = "create" + time + " Février ";
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange oneId = idAllocator.allocateOfflineIds(1);
		System.err.println("Create starts...");
		ItemIdentifier created = mboxesApi.createForHierarchy(oneId.globalCounter, toCreate);
		System.out.println("Got a create of version " + created.version);
		ContainerChangeset<Long> changed = mboxesApi.changesetById(created.version - 1);
		long newItemId = changed.created.get(0);
		System.out.println("From changelog: itemId should be " + newItemId);
		ItemValue<MailboxFolder> foundItem = mboxesApi.getCompleteById(newItemId);
		System.out.println("Found " + foundItem.value.name);

		int newMail = imapAsUser(sc -> {
			int added = sc.append(toCreate.name, testEml(), new FlagsList());
			return added;
		});
		assertTrue(newMail > 0);

		oneId = idAllocator.allocateOfflineIds(1);
		MailboxReplica sub = new MailboxReplica();
		sub.name = toCreate.name + "/Toto espace ";
		ItemIdentifier createdSub = mboxesApi.createForHierarchy(oneId.globalCounter, sub);
		ItemValue<MailboxFolder> subItem = mboxesApi.getCompleteById(createdSub.id);
		System.err.println("Sub " + subItem.value);

		assertTrue(foundItem.value.name.endsWith(" "));
		assertTrue(foundItem.value.fullName.endsWith(" "));
		assertTrue(subItem.value.name.endsWith(" "));
		assertTrue(subItem.value.fullName.endsWith(" "));
	}

	@Test
	public void createSubThenUnsub() throws IMAPException, InterruptedException {
		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> box : allBoxes) {
			if (box.value.name.equals("INBOX")) {
				inbox = box;
				break;
			}
		}
		assertNotNull(inbox);
		MailboxReplica toCreate = new MailboxReplica();
		long time = System.currentTimeMillis() / 1000;
		toCreate.name = "create" + time;
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange oneId = idAllocator.allocateOfflineIds(1);
		ItemIdentifier created = mboxesApi.createForHierarchy(oneId.globalCounter, toCreate);
		System.out.println("Got a create of version " + created.version);
		ContainerChangeset<Long> changed = mboxesApi.changesetById(created.version - 1);
		long newItemId = changed.created.get(0);
		System.out.println("From changelog: itemId should be " + newItemId);
		ItemValue<MailboxFolder> foundItem = mboxesApi.getCompleteById(newItemId);
		System.out.println("Found " + foundItem.value.name);

		imapAsUser(sc -> {
			sc.subscribe(toCreate.name);
			return null;
		});

		Thread.sleep(200);

		ICyrusReplicationArtifacts cyrusArtifactsApi = clientProv.instance(ICyrusReplicationArtifacts.class,
				userUid + "@" + domainUid);
		Optional<MailboxSub> sub = cyrusArtifactsApi.subs().stream().filter(ms -> ms.mboxName.endsWith(toCreate.name))
				.findAny();
		assertTrue(sub.isPresent());

		imapAsUser(sc -> {
			sc.unsubscribe(toCreate.name);
			return null;
		});
		Thread.sleep(200);
		sub = cyrusArtifactsApi.subs().stream().filter(ms -> ms.mboxName.endsWith(toCreate.name)).findAny();
		assertFalse(sub.isPresent());
	}

	@Test
	public void setQuotaThenUnset() throws IMAPException, InterruptedException {
		IServiceProvider clientProv = provider();

		imapAsCyrusAdmin(sc -> {
			sc.setQuota("user/" + userUid + "@" + domainUid, 42);
			return null;
		});

		Thread.sleep(200);

		ICyrusReplicationArtifacts cyrusArtifactsApi = clientProv.instance(ICyrusReplicationArtifacts.class,
				userUid + "@" + domainUid);
		Optional<QuotaRoot> sub = cyrusArtifactsApi.quotas().stream().findAny();
		assertTrue(sub.isPresent());

		imapAsCyrusAdmin(sc -> {
			sc.setQuota("user/" + userUid + "@" + domainUid, 0);
			return null;
		});
		Thread.sleep(200);
		sub = cyrusArtifactsApi.quotas().stream().findAny();
		assertFalse(sub.isPresent());
	}

	@Test
	public void annotateFolder() throws IMAPException, InterruptedException {
		IServiceProvider clientProv = provider();

		imapAsUser(sc -> {
			sc.setMailboxAnnotation("INBOX", "/vendor/blue-mind/replication/id",
					ImmutableMap.of("value.priv", Long.toString(42)));
			return null;
		});

		Thread.sleep(200);

		ICyrusReplicationAnnotations cyrusAnnotationsApi = clientProv.instance(ICyrusReplicationAnnotations.class);
		List<MailboxAnnotation> annotated = cyrusAnnotationsApi.annotations(domainUid + "!" + mboxRoot);
		assertFalse(annotated.isEmpty());

		imapAsUser(sc -> {
			sc.setMailboxAnnotation("INBOX", "/vendor/blue-mind/replication/id",
					ImmutableMap.of("value.priv", Long.toString(43)));
			return null;
		});

		Thread.sleep(500);

	}

	@Test
	public void createFolderWithSpaces() throws IMAPException, InterruptedException {

		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> box : allBoxes) {
			if (box.value.name.equals("INBOX")) {
				inbox = box;
				break;
			}
		}
		assertNotNull(inbox);
		MailboxReplica toCreate = new MailboxReplica();
		long time = System.currentTimeMillis() / 1000;
		toCreate.name = "with spaces " + time;
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange oneId = idAllocator.allocateOfflineIds(1);
		ItemIdentifier created = mboxesApi.createForHierarchy(oneId.globalCounter, toCreate);
		System.out.println("Got a create of version " + created.version);
		ContainerChangeset<Long> changed = mboxesApi.changesetById(created.version - 1);
		long newItemId = changed.created.get(0);
		System.out.println("From changelog: itemId should be " + newItemId);
		ItemValue<MailboxFolder> foundItem = mboxesApi.getCompleteById(newItemId);
		System.out.println("Found " + foundItem.value.name);
		assertEquals(toCreate.name, foundItem.value.name);

		// when we update this empty folder too fast, replication will just
		// delete it
		// with apply unmailbox & create another one with the new name...
		System.err.println("Sleeping before update starts...");
		Thread.sleep(1000);

		String newName = "update spaces " + time;
		foundItem.value.name = newName;
		foundItem.value.fullName = newName;
		Ack updated = mboxesApi.updateById(foundItem.internalId, foundItem.value);
		System.out.println("version after update: " + updated.version);
		assertEquals(changed.version + 1, updated.version);
	}

	@Test
	public void createFolderWithNameAndParentUid() throws IMAPException, InterruptedException {

		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> box : allBoxes) {
			if (box.value.name.equals("INBOX")) {
				inbox = box;
				break;
			}
		}
		assertNotNull(inbox);
		MailboxReplica toCreate = new MailboxReplica();
		long time = System.currentTimeMillis() / 1000;
		toCreate.name = "parent" + time;
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(2);
		ItemIdentifier created = mboxesApi.createForHierarchy(ids.globalCounter, toCreate);
		System.out.println("Got a create of version " + created.version);
		ContainerChangeset<Long> changed = mboxesApi.changesetById(created.version - 1);
		long newItemId = changed.created.get(0);
		System.out.println("From changelog: itemId should be " + newItemId);
		ItemValue<MailboxFolder> foundItem = mboxesApi.getCompleteById(newItemId);
		System.out.println("Found " + foundItem.value.name);
		assertEquals(toCreate.name, foundItem.value.name);

		MailboxFolder child = new MailboxFolder();
		child.name = "sub" + time;
		child.parentUid = foundItem.uid;
		ItemIdentifier identifier = mboxesApi.createForHierarchy(ids.globalCounter + 1, child);
		ItemValue<MailboxFolder> reFetched = mboxesApi.getCompleteById(identifier.id);
		System.out.println("refetched: " + reFetched);
	}

	@Test
	public void imapDeletionsPropagation() throws IMAPException, InterruptedException {
		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> box : allBoxes) {
			if (box.value.name.equals("INBOX")) {
				inbox = box;
				break;
			}
		}
		assertNotNull(inbox);
		CountDownLatch mbUpdLock = expectMessages("mailreplica.mailbox.updated", 2);

		imapAsUser(sc -> {
			sc.select("INBOX");
			Collection<Integer> allUids = sc.uidSearch(new SearchQuery());
			FlagsList deleted = new FlagsList();
			deleted.add(Flag.DELETED);
			sc.uidStore(allUids, deleted, true);
			try {
				// leave time to get 2 replication messages
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			sc.uidExpunge(allUids);
			return null;
		});

		assertTrue(mbUpdLock.await(5, TimeUnit.SECONDS));

	}

	@Test
	public void unseenViaApi() throws InterruptedException {
		IServiceProvider prov = provider();
		IUserInbox inboxApi = prov.instance(IUserInbox.class, domainUid, userUid);
		imapAsUser(sc -> {
			sc.select("INBOX");
			FlagsList fl = new FlagsList();
			fl.add(Flag.SEEN);
			sc.uidStore("1:*", fl, true);
			return null;
		});
		Thread.sleep(500);
		int unseen = inboxApi.unseen();
		imapAsUser(sc -> {
			sc.select("INBOX");
			FlagsList fl = new FlagsList();
			fl.add(Flag.SEEN);
			sc.uidStore("1:*", fl, false);
			return null;
		});
		Thread.sleep(500);
		Integer newValue = inboxApi.unseen();
		System.err.println("unseen before: " + unseen + ", after " + newValue);
		assertTrue(newValue > unseen);
	}

	@Test
	public void mailshareCreateAndUpdate()
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailshare mailshareApi = prov.instance(IMailshare.class, domainUid);
		Mailshare mailshare = new Mailshare();
		mailshare.name = "shared" + System.currentTimeMillis();
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domainUid, true));
		mailshare.routing = Routing.internal;

		// setup events expectations
		CompletableFuture<MailboxReplicaRootDescriptor> onRoot = ReplicationEvents.onMailboxRootCreated();
		MailboxReplicaRootDescriptor expected = MailboxReplicaRootDescriptor.create(Namespace.shared, mailshare.name);
		Subtree sub = SubtreeContainer.mailSubtreeUid(domainUid, expected.ns, mailshare.name);
		String subtreeUid = sub.subtreeUid();
		System.err.println("On subtree update " + subtreeUid);
		CompletableFuture<ItemIdentifier> onSubtree = ReplicationEvents.onSubtreeUpdate(subtreeUid);
		CompletableFuture<Void> allEvents = CompletableFuture.allOf(onRoot, onSubtree);

		System.err.println("Before create.....");
		mailshareApi.create(mailshare.name, mailshare);

		IContainerManagement c = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(mailshare.name));
		List<AccessControlEntry> accessControlList = new ArrayList<>(c.getAccessControlList());
		accessControlList.add(AccessControlEntry.create(userUid, Verb.Write));
		c.setAccessControlList(accessControlList);

		allEvents.get(10, TimeUnit.SECONDS);
		MailboxReplicaRootDescriptor created = onRoot.get();
		assertNotNull(created);
		System.err.println("**** ROOT is " + created.ns + ", " + created.name + ", version: " + onSubtree.get());
		Thread.sleep(500);
		IMailboxFolders folders = provider().instance(IMailboxFolders.class, partition, mailshare.name);
		List<ItemValue<MailboxFolder>> allFolders = folders.all();
		ItemValue<MailboxFolder> sent = null;
		for (ItemValue<MailboxFolder> folder : allFolders) {
			System.out.println("Got " + folder.uid + ", " + folder.value.name);
			if ("Sent".equals(folder.value.name)) {
				sent = folder;
			}
		}
		assertNotNull(sent);
		imapAsCyrusAdmin(sc -> {
			CompletableFuture<ItemIdentifier> onMsSubtree = ReplicationEvents.onSubtreeUpdate(subtreeUid);
			sc.rename(mailshare.name + "/Sent@" + domainUid, mailshare.name + "/Middle/Sent@" + domainUid);
			onMsSubtree.get(20, TimeUnit.SECONDS);
			return null;
		});

		IDbReplicatedMailboxes fullFolders = provider().instance(IDbReplicatedMailboxes.class, partition,
				mailshare.name);
		List<ItemValue<MailboxReplica>> allFoldersFull = fullFolders.allReplicas();
		boolean found = false;
		for (ItemValue<MailboxReplica> folder : allFoldersFull) {
			System.out.println("Got " + folder.uid + ", " + folder.value.fullName);
			if ((mailshare.name + "/Middle/Sent").equals(folder.value.fullName)) {
				found = true;
			}
		}
		assertTrue(found);
	}

	@Test
	public void mailshareTransfers()
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException, IOException {
		ItemsTransferService.FORCE_CROSS = false;

		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailshare mailshareApi = prov.instance(IMailshare.class, domainUid);
		Mailshare mailshare = new Mailshare();
		mailshare.name = "shared" + System.currentTimeMillis();
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domainUid, true));
		mailshare.routing = Routing.internal;

		// setup events expectations
		CompletableFuture<MailboxReplicaRootDescriptor> onRoot = ReplicationEvents.onMailboxRootCreated();
		MailboxReplicaRootDescriptor expected = MailboxReplicaRootDescriptor.create(Namespace.shared, mailshare.name);
		Subtree sub = SubtreeContainer.mailSubtreeUid(domainUid, expected.ns, mailshare.name);
		String subtreeUid = sub.subtreeUid();
		System.err.println("On subtree update " + subtreeUid);
		CompletableFuture<ItemIdentifier> onSubtree = ReplicationEvents.onSubtreeUpdate(subtreeUid);
		CompletableFuture<Void> allEvents = CompletableFuture.allOf(onRoot, onSubtree);

		System.err.println("Before create.....");
		mailshareApi.create(mailshare.name, mailshare);

		IContainerManagement c = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(mailshare.name));
		List<AccessControlEntry> accessControlList = new ArrayList<>(c.getAccessControlList());
		accessControlList.add(AccessControlEntry.create(userUid, Verb.Write));
		c.setAccessControlList(accessControlList);

		allEvents.get(10, TimeUnit.SECONDS);
		MailboxReplicaRootDescriptor created = onRoot.get();
		assertNotNull(created);
		System.err.println("**** ROOT is " + created.ns + ", " + created.name + ", version: " + onSubtree.get());
		Thread.sleep(500);
		IMailboxFolders folders = provider().instance(IMailboxFolders.class, partition, mailshare.name);
		List<ItemValue<MailboxFolder>> allFolders = folders.all();
		ItemValue<MailboxFolder> sent = null;
		for (ItemValue<MailboxFolder> folder : allFolders) {
			System.out.println("Got " + folder.uid + ", " + folder.value.name);
			if ("Sent".equals(folder.value.name)) {
				sent = folder;
			}
		}
		assertNotNull(sent);
		int txSize = 5;
		List<Long> idsToTransfer = new ArrayList<>(txSize);
		for (int i = 0; i < txSize; i++) {
			ItemValue<MailboxItem> firstItem = addDraft(sent, mailshare.name);
			idsToTransfer.add(firstItem.internalId);
		}
		IMailboxFolders foldersApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> trash = foldersApi.byName("Trash");
		IItemsTransfer transferApi = provider().instance(IItemsTransfer.class, sent.uid, trash.uid);
		long time = System.currentTimeMillis();
		List<ItemIdentifier> copied = transferApi.copy(idsToTransfer);
		time = System.currentTimeMillis() - time;
		System.err.println("Copied " + txSize + " in " + time + "ms");
		assertNotNull(copied);
		assertEquals(txSize, copied.size());

		time = System.currentTimeMillis();
		List<ItemIdentifier> moved = transferApi.move(idsToTransfer);
		time = System.currentTimeMillis() - time;
		System.err.println("Moved " + txSize + " in " + time + "ms");
		assertNotNull(moved);
		assertEquals(txSize, moved.size());
		IMailboxItems items = provider().instance(IMailboxItems.class, trash.uid);
		for (ItemIdentifier id : moved) {
			ItemValue<MailboxItem> found = items.getCompleteById(id.id);
			System.err.println("found " + found);
			assertNotNull(found);
		}
	}

	@Test
	public void mailshareCrossBackend()
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException, IOException {
		ItemsTransferService.FORCE_CROSS = true;

		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailshare mailshareApi = prov.instance(IMailshare.class, domainUid);
		Mailshare mailshare = new Mailshare();
		mailshare.name = "shared" + System.currentTimeMillis();
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domainUid, true));
		mailshare.routing = Routing.internal;

		// setup events expectations
		CompletableFuture<MailboxReplicaRootDescriptor> onRoot = ReplicationEvents.onMailboxRootCreated();
		MailboxReplicaRootDescriptor expected = MailboxReplicaRootDescriptor.create(Namespace.shared, mailshare.name);
		Subtree sub = SubtreeContainer.mailSubtreeUid(domainUid, expected.ns, mailshare.name);
		String subtreeUid = sub.subtreeUid();
		System.err.println("On subtree update " + subtreeUid);
		CompletableFuture<ItemIdentifier> onSubtree = ReplicationEvents.onSubtreeUpdate(subtreeUid);
		CompletableFuture<Void> allEvents = CompletableFuture.allOf(onRoot, onSubtree);

		System.err.println("Before create.....");
		mailshareApi.create(mailshare.name, mailshare);

		IContainerManagement c = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(mailshare.name));
		List<AccessControlEntry> accessControlList = new ArrayList<>(c.getAccessControlList());
		accessControlList.add(AccessControlEntry.create(userUid, Verb.Write));
		c.setAccessControlList(accessControlList);

		allEvents.get(10, TimeUnit.SECONDS);
		MailboxReplicaRootDescriptor created = onRoot.get();
		assertNotNull(created);
		System.err.println("**** ROOT is " + created.ns + ", " + created.name + ", version: " + onSubtree.get());
		Thread.sleep(500);
		IMailboxFolders folders = provider().instance(IMailboxFolders.class, partition, mailshare.name);
		List<ItemValue<MailboxFolder>> allFolders = folders.all();
		ItemValue<MailboxFolder> sent = null;
		for (ItemValue<MailboxFolder> folder : allFolders) {
			System.out.println("Got " + folder.uid + ", " + folder.value.name);
			if ("Sent".equals(folder.value.name)) {
				sent = folder;
			}
		}
		assertNotNull(sent);
		int txSize = 50;
		List<Long> idsToTransfer = new ArrayList<>(txSize);
		for (int i = 0; i < txSize; i++) {
			ItemValue<MailboxItem> firstItem = addDraft(sent, mailshare.name);
			idsToTransfer.add(firstItem.internalId);
		}
		IMailboxFolders foldersApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> trash = foldersApi.byName("Trash");

		IItemsTransfer transferApi = provider().instance(IItemsTransfer.class, sent.uid, trash.uid);
		long time = System.currentTimeMillis();
		List<ItemIdentifier> copied = transferApi.copy(idsToTransfer);
		time = System.currentTimeMillis() - time;
		System.err.println("Copied " + txSize + " in " + time + "ms");
		assertNotNull(copied);
		assertEquals(txSize, copied.size());

		time = System.currentTimeMillis();
		List<ItemIdentifier> moved = transferApi.move(idsToTransfer);
		time = System.currentTimeMillis() - time;
		System.err.println("Moved " + txSize + " in " + time + "ms");
		assertNotNull(moved);
		assertEquals(txSize, moved.size());
	}

	@Test
	public void mailshareCreateSubfolder()
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailshare mailshareApi = prov.instance(IMailshare.class, domainUid);
		Mailshare mailshare = new Mailshare();
		mailshare.name = "shared" + System.currentTimeMillis();
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domainUid, true));
		mailshare.routing = Routing.internal;

		// setup events expectations
		CompletableFuture<MailboxReplicaRootDescriptor> onRoot = ReplicationEvents.onMailboxRootCreated();
		MailboxReplicaRootDescriptor expected = MailboxReplicaRootDescriptor.create(Namespace.shared, mailshare.name);
		Subtree sub = SubtreeContainer.mailSubtreeUid(domainUid, expected.ns, mailshare.name);
		String subtreeUid = sub.subtreeUid();
		System.err.println("On subtree update " + subtreeUid);
		CompletableFuture<ItemIdentifier> onSubtree = ReplicationEvents.onSubtreeUpdate(subtreeUid);
		CompletableFuture<Void> allEvents = CompletableFuture.allOf(onRoot, onSubtree);

		System.err.println("Before create.....");
		mailshareApi.create(mailshare.name, mailshare);
		allEvents.get(10, TimeUnit.SECONDS);
		MailboxReplicaRootDescriptor created = onRoot.get();
		assertNotNull(created);

		IContainerManagement aclApi = prov.instance(IContainerManagement.class,
				IMailboxAclUids.uidForMailbox(mailshare.name));
		System.err.println("Setting ACLs....");
		aclApi.setAccessControlList(Arrays.asList(AccessControlEntry.create(userUid, Verb.Write)));
		System.err.println("**** ROOT is " + created.ns + ", " + created.name + ", version: " + onSubtree.get());

		IMailboxFolders folders = provider().instance(IMailboxFolders.class, partition, mailshare.name);
		List<ItemValue<MailboxFolder>> allFolders = folders.all();
		ItemValue<MailboxFolder> root = null;
		for (ItemValue<MailboxFolder> folder : allFolders) {
			System.out.println("Got " + folder.uid + ", " + folder.value.name);
			if (mailshare.name.equals(folder.value.name)) {
				root = folder;
			}
		}
		assertNotNull(root);
		System.err.println("ROOT is " + root.uid + " " + root.value);

		MailboxFolder child = new MailboxFolder();
		child.name = "Eléments détectés" + System.currentTimeMillis();
		child.parentUid = root.uid;
		ItemIdentifier freshId = folders.createBasic(child);
		ItemValue<MailboxFolder> freshFolder = folders.getComplete(freshId.uid);
		assertNotNull(freshFolder);

		try {
			folders.createBasic(child);
			fail();
		} catch (Exception e) {
		}

		MailboxFolder grandChild = new MailboxFolder();
		grandChild.name = "Eléments détectés2" + System.currentTimeMillis();
		grandChild.parentUid = freshFolder.uid;
		ItemIdentifier freshId2 = folders.createBasic(grandChild);
		ItemValue<MailboxFolder> freshFolder2 = folders.getComplete(freshId2.uid);
		assertNotNull(freshFolder2);

		freshFolder.value.name = "updChild" + System.currentTimeMillis();
		freshFolder.value.fullName = null;
		System.err.println("rename child...");
		Ack ack = folders.updateById(freshFolder.internalId, freshFolder.value);
		assertNotNull(ack);
		assertTrue(ack.version > freshId.version);

		System.err.println("Before delete........");
		Thread.sleep(500);
		folders.deleteById(freshFolder.internalId);
		Thread.sleep(1000);
		ItemValue<MailboxFolder> exists = folders.getCompleteById(freshFolder.internalId);
		assertNull(exists);

		// nested case
		child = new MailboxFolder();
		child.name = "reChild" + System.currentTimeMillis();
		child.parentUid = root.uid;
		freshId = folders.createBasic(child);
		freshFolder = folders.getComplete(freshId.uid);
		assertNotNull(freshFolder);

		MailboxFolder subF = new MailboxFolder();
		subF.fullName = freshFolder.value.fullName + "/sub" + System.currentTimeMillis();
		subF.parentUid = freshFolder.uid;
		ItemIdentifier subFolder = folders.createBasic(subF);
		assertNotNull(subFolder);

		subF.fullName = freshFolder.value.fullName + "/upd" + System.currentTimeMillis();
		ack = folders.updateById(subFolder.id, subF);
		assertNotNull(ack);
		assertTrue(ack.version > subFolder.version);

		System.err.println("Pre nested delete....");
		Thread.sleep(500);
		folders.deleteById(subFolder.id);
		Thread.sleep(1000);
		ItemValue<MailboxFolder> subFound = folders.getCompleteById(subFolder.id);
		assertNull(subFound);
	}

	@Test
	public void mailshareRootFolderInception()
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailshare mailshareApi = prov.instance(IMailshare.class, domainUid);
		Mailshare mailshare = new Mailshare();
		mailshare.name = "shared" + System.currentTimeMillis();
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domainUid, true));
		mailshare.routing = Routing.internal;

		// setup events expectations
		CompletableFuture<MailboxReplicaRootDescriptor> onRoot = ReplicationEvents.onMailboxRootCreated();
		MailboxReplicaRootDescriptor expected = MailboxReplicaRootDescriptor.create(Namespace.shared, mailshare.name);
		Subtree sub = SubtreeContainer.mailSubtreeUid(domainUid, expected.ns, mailshare.name);
		String subtreeUid = sub.subtreeUid();
		System.err.println("On subtree update " + subtreeUid);
		CompletableFuture<ItemIdentifier> onSubtree = ReplicationEvents.onSubtreeUpdate(subtreeUid);
		CompletableFuture<Void> allEvents = CompletableFuture.allOf(onRoot, onSubtree);

		System.err.println("Before create.....");
		mailshareApi.create(mailshare.name, mailshare);
		allEvents.get(10, TimeUnit.SECONDS);
		MailboxReplicaRootDescriptor created = onRoot.get();
		assertNotNull(created);

		IContainerManagement aclApi = prov.instance(IContainerManagement.class,
				IMailboxAclUids.uidForMailbox(mailshare.name));
		System.err.println("Setting ACLs....");
		aclApi.setAccessControlList(Arrays.asList(AccessControlEntry.create(userUid, Verb.Write)));
		System.err.println("**** ROOT is " + created.ns + ", " + created.name + ", version: " + onSubtree.get());

		IMailboxFolders folders = provider().instance(IMailboxFolders.class, partition, mailshare.name);
		ItemValue<MailboxFolder> root = checkFolderTreeThenReturnRoot(mailshare, folders);

		MailboxFolder child = new MailboxFolder();
		child.name = mailshare.name;
		child.parentUid = root.uid;
		ItemIdentifier freshId = folders.createBasic(child);
		ItemValue<MailboxFolder> freshFolder = folders.getComplete(freshId.uid);
		final ItemValue<MailboxFolder> baseNested = freshFolder;
		assertNotNull(freshFolder);

		checkFolderTreeThenReturnRoot(mailshare, folders);

		LinkedList<ItemValue<MailboxFolder>> nestedCrap = new LinkedList<>();
		System.err.println("FRESH_ID: " + freshId);
		System.err.println("FRESH: " + freshFolder.uid + " " + freshFolder.value);
		for (int i = 0; i < 4; i++) {
			System.err.println("NESTING " + i);
			MailboxFolder nested = new MailboxFolder();
			nested.name = mailshare.name;
			nested.parentUid = freshFolder.uid;
			freshId = folders.createBasic(nested);
			freshFolder = folders.getComplete(freshId.uid);
			nestedCrap.add(freshFolder);

			checkFolderTreeThenReturnRoot(mailshare, folders);
			assertEquals("Created with wrong parent", nested.parentUid, freshFolder.value.parentUid);
		}

		System.err.println("Before delete........");
		Thread.sleep(500);

		while (!nestedCrap.isEmpty()) {
			ItemValue<MailboxFolder> toDel = nestedCrap.pollLast();
			System.err.println("DEL " + toDel.value);
			folders.deleteById(toDel.internalId);
			Thread.sleep(500);
		}

		folders.deleteById(baseNested.internalId);
		Thread.sleep(1000);
		ItemValue<MailboxFolder> exists = folders.getCompleteById(baseNested.internalId);
		assertNull(exists);

	}

	private ItemValue<MailboxFolder> checkFolderTreeThenReturnRoot(Mailshare mailshare, IBaseMailboxFolders folders) {
		List<ItemValue<MailboxFolder>> allFolders = folders.all();
		ItemValue<MailboxFolder> root = null;
		int nullParents = 0;
		for (ItemValue<MailboxFolder> folder : allFolders) {
			System.err.println("Got " + folder.uid + ": " + folder.value);
			if (folder.value.parentUid == null) {
				nullParents++;
			}
			if (mailshare.name.equals(folder.value.name)) {
				root = folder;
			}
		}
		assertNotNull(root);
		assertEquals("Only one folder should have a null parentUid", 1, nullParents);
		return root;
	}

	@Test
	public void mailshareFetchCompleteInSubfolder()
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException, IOException {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailshare mailshareApi = prov.instance(IMailshare.class, domainUid);
		Mailshare mailshare = new Mailshare();
		mailshare.name = "back" + System.currentTimeMillis();
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domainUid, true));
		mailshare.routing = Routing.internal;

		// setup events expectations
		CompletableFuture<MailboxReplicaRootDescriptor> onRoot = ReplicationEvents.onMailboxRootCreated();
		MailboxReplicaRootDescriptor expected = MailboxReplicaRootDescriptor.create(Namespace.shared, mailshare.name);
		Subtree sub = SubtreeContainer.mailSubtreeUid(domainUid, expected.ns, mailshare.name);
		String subtreeUid = sub.subtreeUid();
		System.err.println("On subtree update " + subtreeUid);
		CompletableFuture<ItemIdentifier> onSubtree = ReplicationEvents.onSubtreeUpdate(subtreeUid);
		CompletableFuture<Void> allEvents = CompletableFuture.allOf(onRoot, onSubtree);

		System.err.println("Before create.....");
		mailshareApi.create(mailshare.name, mailshare);
		allEvents.get(10, TimeUnit.SECONDS);
		MailboxReplicaRootDescriptor created = onRoot.get();
		assertNotNull(created);
		IContainerManagement aclApi = prov.instance(IContainerManagement.class,
				IMailboxAclUids.uidForMailbox(mailshare.name));
		System.err.println("Setting ACLs....");
		aclApi.setAccessControlList(Arrays.asList(AccessControlEntry.create(userUid, Verb.Write)));

		System.err.println("**** ROOT is " + created.ns + ", " + created.name + ", version: " + onSubtree.get());
		Thread.sleep(500);
		IMailboxFolders folders = provider().instance(IMailboxFolders.class, partition, mailshare.name);
		List<ItemValue<MailboxFolder>> allFolders = folders.all();
		ItemValue<MailboxFolder> sent = null;
		for (ItemValue<MailboxFolder> folder : allFolders) {
			System.out.println("Got " + folder.uid + ", " + folder.value.name);
			if ("Sent".equals(folder.value.name)) {
				sent = folder;
			}
		}
		assertNotNull(sent);
		ItemValue<MailboxItem> item = addDraft(sent, mailshare.name);
		IDbMailboxRecords recs = provider().instance(IDbMailboxRecords.class, sent.uid);
		Stream fetched = recs.fetchComplete(item.value.imapUid);
		assertNotNull(fetched);
		String asStr = GenericStream.streamToString(fetched);
		assertNotNull(asStr);
	}

	@Test
	public void mailshareCreateThenWriteDraft()
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException, IOException {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailshare mailshareApi = prov.instance(IMailshare.class, domainUid);
		Mailshare mailshare = new Mailshare();
		mailshare.name = "shared" + System.currentTimeMillis();
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domainUid, true));
		mailshare.routing = Routing.internal;

		// setup events expectations
		CompletableFuture<MailboxReplicaRootDescriptor> onRoot = ReplicationEvents.onMailboxRootCreated();
		MailboxReplicaRootDescriptor expected = MailboxReplicaRootDescriptor.create(Namespace.shared, mailshare.name);
		Subtree sub = SubtreeContainer.mailSubtreeUid(domainUid, expected.ns, mailshare.name);
		String subtreeUid = sub.subtreeUid();
		CompletableFuture<ItemIdentifier> onSubtree = ReplicationEvents.onSubtreeUpdate(subtreeUid);
		CompletableFuture<Void> allEvents = CompletableFuture.allOf(onRoot, onSubtree);

		mailshareApi.create(mailshare.name, mailshare);
		IMailboxes mboxesApi = prov.instance(IMailboxes.class, domainUid);
		List<AccessControlEntry> accessForUser = Arrays.asList(AccessControlEntry.create(userUid, Verb.Write));
		mboxesApi.setMailboxAccessControlList(mailshare.name, accessForUser);

		allEvents.get(4, TimeUnit.SECONDS);
		MailboxReplicaRootDescriptor created = onRoot.get();
		assertNotNull(created);
		System.err.println("**** ROOT is " + created.ns + ", " + created.name + ", version: " + onSubtree.get());
		Thread.sleep(500);
		IMailboxFolders folders = provider().instance(IMailboxFolders.class, partition, mailshare.name);
		List<ItemValue<MailboxFolder>> allFolders = folders.all();
		ItemValue<MailboxFolder> sent = null;
		ItemValue<MailboxFolder> root = null;
		for (ItemValue<MailboxFolder> folder : allFolders) {
			System.out.println("Got " + folder.uid + ", " + folder.value.name);
			if ("Sent".equals(folder.value.name)) {
				sent = folder;
			} else if (mailshare.name.equals(folder.value.name)) {
				root = folder;
			}
		}
		assertNotNull(sent);
		assertNotNull(root);
		IMailboxItems itemsApi = provider().instance(IMailboxItems.class, root.uid);

		ItemValue<MailboxItem> added = addDraft(root, mailshare.name);
		Part partsRoot = added.value.body.structure;
		PartsWalker<Void> walk = new PartsWalker<>(null);
		AtomicInteger partBytes = new AtomicInteger();
		walk.visit((ctx, part) -> {
			Stream fetched = itemsApi.fetch(added.value.imapUid, part.address, null, null, null, null);
			try {
				Buffer asBuffer = fetchPart(fetched);
				partBytes.addAndGet(asBuffer.length());
				System.out.println(part.address + " Received " + asBuffer.length() + " byte(s)");
			} catch (InterruptedException e) {
			}
		}, partsRoot);
		assertTrue(partBytes.get() > 10000);

		Thread.sleep(500);

		List<Long> unread = itemsApi.unreadItems();
		assertNotNull(unread);
		System.err.println("Found " + unread.size() + " unread item(s).");
		assertFalse(unread.isEmpty());
		List<ItemValue<MailboxItem>> refetch = itemsApi.multipleById(unread);
		List<Long> itemsId = refetch.stream().map(iv -> iv.internalId).collect(Collectors.toList());
		itemsApi.addFlag(FlagUpdate.of(itemsId, MailboxItemFlag.System.Seen.value()));
		Ack ack = itemsApi.addFlag(FlagUpdate.of(itemsId, new MailboxItemFlag("$MDNSent")));
		System.err.println("Got ack " + ack);
		refetch = itemsApi.multipleById(unread);
		unread = itemsApi.unreadItems();
		assertTrue(unread.isEmpty());
		for (ItemValue<MailboxItem> iv : refetch) {
			System.err.println("Delete " + iv);
			itemsApi.deleteById(iv.internalId);
		}
	}

	@Test
	public void renameFolderReplication() throws IMAPException, InterruptedException, IOException {
		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> box : allBoxes) {
			System.out.println("On name " + box.value.name);
			if (box.value.name.equals("INBOX")) {
				inbox = box;
				break;
			}
		}
		assertNotNull(inbox);
		MailboxReplica toCreate = new MailboxReplica();
		long time = System.currentTimeMillis() / 1000;
		toCreate.name = "create" + time;
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(3);
		ItemIdentifier created = mboxesApi.createForHierarchy(ids.globalCounter, toCreate);
		System.out.println("Got a create of version " + created.version);
		ContainerChangeset<Long> changed = mboxesApi.changesetById(created.version - 1);
		long newItemId = changed.created.get(0);
		System.out.println("From changelog: itemId should be " + newItemId);
		ItemValue<MailboxFolder> foundItem = mboxesApi.getCompleteById(newItemId);
		System.out.println("Found " + foundItem.value.name);
		addDraft(foundItem, ids.globalCounter + 1);

		String newName = "update" + time;
		foundItem.value.name = newName;
		foundItem.value.fullName = foundItem.value.name;
		Ack updated = mboxesApi.updateById(foundItem.internalId, foundItem.value);
		System.err.println("version after update: " + updated.version + " for " + foundItem.value.name);
		foundItem = mboxesApi.getCompleteById(foundItem.internalId);

		// an apply mailbox follows & re-update the folder
		Thread.sleep(500);

		CountDownLatch hierUpdLock = expectMessage("mailreplica.hierarchy.updated");
		String anotherName = "another" + time;
		System.err.println("Start imap rename from " + newName + " to " + anotherName);
		imapAsUser(sc -> {
			boolean renamed = sc.rename(newName, anotherName);
			System.err.println("renamed: " + renamed);
			return null;
		});
		assertTrue(hierUpdLock.await(30, TimeUnit.SECONDS));

		foundItem = mboxesApi.getCompleteById(foundItem.internalId);
		System.err.println("Found before add draft: " + foundItem);
		addDraft(foundItem, ids.globalCounter + 2);
	}

	@Test
	public void renameFolderToSameName() throws IMAPException, InterruptedException, IOException {
		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> box : allBoxes) {
			System.out.println("On name " + box.value.name);
			if (box.value.name.equals("INBOX")) {
				inbox = box;
				break;
			}
		}
		assertNotNull(inbox);
		MailboxReplica toCreate = new MailboxReplica();
		long time = System.currentTimeMillis() / 1000;
		toCreate.name = "create" + time;
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(3);
		ItemIdentifier created = mboxesApi.createForHierarchy(ids.globalCounter, toCreate);
		System.out.println("Got a create of version " + created.version);
		ContainerChangeset<Long> changed = mboxesApi.changesetById(created.version - 1);
		long newItemId = changed.created.get(0);
		System.out.println("From changelog: itemId should be " + newItemId);
		ItemValue<MailboxFolder> foundItem = mboxesApi.getCompleteById(newItemId);
		System.out.println("Found " + foundItem.value.name);
		addDraft(foundItem, ids.globalCounter + 1);

		Ack updated = mboxesApi.updateById(foundItem.internalId, foundItem.value);
		System.err.println("version after update: " + updated.version + " for " + foundItem.value.name);
		assertTrue(updated.version > foundItem.version);
	}

	@Test
	public void deleteDeepWithMultipleChildren() throws IMAPException, InterruptedException {
		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> box : allBoxes) {
			if (box.value.name.equals("INBOX")) {
				inbox = box;
				break;
			}
		}
		assertNotNull(inbox);
		MailboxReplica toCreate = new MailboxReplica();
		long time = System.currentTimeMillis() / 1000;
		toCreate.name = "create" + time;
		String base = toCreate.name;
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(3);
		ItemIdentifier created = mboxesApi.createForHierarchy(ids.globalCounter, toCreate);
		toCreate.name = base + "/a";
		created = mboxesApi.createForHierarchy(ids.globalCounter + 1, toCreate);
		toCreate.name = base + "/b";
		created = mboxesApi.createForHierarchy(ids.globalCounter + 2, toCreate);
		System.out.println("Got a create of version " + created.version);
		ItemValue<MailboxFolder> foundItem = mboxesApi.byName(base);
		System.out.println("Found " + foundItem.value.name);

		CountDownLatch hierUpdLock = expectMessages("mailreplica.hierarchy.updated", 3);

		System.err.println("deep delete start...");
		mboxesApi.deepDelete(foundItem.internalId);
		System.err.println("deep delete ends.");

		assertTrue("Expected 3 updates to occur on the hierarchy", hierUpdLock.await(10, TimeUnit.SECONDS));
		imapAsUser(sc -> {
			System.err.println("try imap listing...");
			ListResult foundFolders = sc.listAll();
			for (ListInfo f : foundFolders) {
				System.out.println(" * " + f.getName());
			}
			return null;
		});
		IDbReplicatedMailboxes fullApi = clientProv.instance(IDbReplicatedMailboxes.class, partition, mboxRoot);
		List<ItemValue<MailboxReplica>> allReplicas = fullApi.allReplicas();
		System.out.println("--------");
		for (ItemValue<MailboxReplica> iv : allReplicas) {
			System.out.println(" * " + iv.uid + " n: " + iv.value.name + " fn: " + iv.value.fullName + ", p: "
					+ iv.value.parentUid);
		}
	}

	@Test
	public void multipleDeleteRuns() throws IMAPException, InterruptedException {
		int cnt = 50;
		for (int i = 0; i < cnt; i++) {
			deleteDeepWithMultipleChildren();
			System.err.println("run " + (i + 1) + " / " + cnt);
		}
	}

	@Test
	public void deleteEmptyWithMultipleChildren() throws IMAPException, InterruptedException, IOException {
		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> box : allBoxes) {
			if (box.value.name.equals("INBOX")) {
				inbox = box;
				break;
			}
		}
		assertNotNull(inbox);
		MailboxReplica toCreate = new MailboxReplica();
		long time = System.currentTimeMillis() / 1000;
		toCreate.name = "create" + time;
		String base = toCreate.name;
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(3);
		ItemIdentifier created = mboxesApi.createForHierarchy(ids.globalCounter, toCreate);
		toCreate.name = base + "/a";
		created = mboxesApi.createForHierarchy(ids.globalCounter + 1, toCreate);
		toCreate.name = base + "/b";
		created = mboxesApi.createForHierarchy(ids.globalCounter + 2, toCreate);
		System.out.println("Got a create of version " + created.version);
		ItemValue<MailboxFolder> foundItem = mboxesApi.byName(base);
		System.out.println("Found " + foundItem.value.name);

		CountDownLatch hierUpdLock = expectMessages("mailreplica.hierarchy.updated", 3);

		addDraft(foundItem);

		mboxesApi.emptyFolder(foundItem.internalId);

		assertTrue("Expected 3 updates to occur on the hierarchy", hierUpdLock.await(10, TimeUnit.SECONDS));
		imapAsUser(sc -> {
			ListResult foundFolders = sc.listAll();
			for (ListInfo f : foundFolders) {
				System.out.println(" * " + f.getName());
			}
			return null;
		});
		IDbReplicatedMailboxes fullApi = clientProv.instance(IDbReplicatedMailboxes.class, partition, mboxRoot);
		List<ItemValue<MailboxReplica>> allReplicas = fullApi.allReplicas();
		System.out.println("--------");
		for (ItemValue<MailboxReplica> iv : allReplicas) {
			System.out.println(" * " + iv.uid + " n: " + iv.value.name + " fn: " + iv.value.fullName + ", p: "
					+ iv.value.parentUid);
		}
	}

	@Test
	public void emptyEmptyFolder() throws IMAPException, InterruptedException, IOException {
		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		MailboxReplica toCreate = new MailboxReplica();
		long time = System.currentTimeMillis() / 1000;
		toCreate.name = "create" + time;
		mboxesApi.createBasic(toCreate);
		ItemValue<MailboxFolder> foundItem = mboxesApi.byName(toCreate.name);

		mboxesApi.emptyFolder(foundItem.internalId);
	}

	@Test
	public void removeFirstLevelMessages() throws IMAPException, InterruptedException, IOException {
		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> box : allBoxes) {
			if (box.value.name.equals("INBOX")) {
				inbox = box;
				break;
			}
		}
		assertNotNull(inbox);
		MailboxReplica toCreate = new MailboxReplica();
		long time = System.currentTimeMillis() / 1000;
		toCreate.name = "create" + time;
		String base = toCreate.name;
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(3);
		ItemIdentifier created = mboxesApi.createForHierarchy(ids.globalCounter, toCreate);
		toCreate.name = base + "/a";
		created = mboxesApi.createForHierarchy(ids.globalCounter + 1, toCreate);
		toCreate.name = base + "/b";
		created = mboxesApi.createForHierarchy(ids.globalCounter + 2, toCreate);
		System.out.println("Got a create of version " + created.version);
		ItemValue<MailboxFolder> foundItem = mboxesApi.byName(base);
		System.out.println("Found " + foundItem.value.name);

		CountDownLatch hierUpdLock = expectMessages("mailreplica.hierarchy.updated", 1);

		addDraft(foundItem);

		mboxesApi.removeMessages(foundItem.internalId);

		assertTrue("Expected 1 update to occur on the hierarchy", hierUpdLock.await(10, TimeUnit.SECONDS));
		imapAsUser(sc -> {
			ListResult foundFolders = sc.listAll();
			for (ListInfo f : foundFolders) {
				System.out.println(" * " + f.getName());
			}
			return null;
		});
		IDbReplicatedMailboxes fullApi = clientProv.instance(IDbReplicatedMailboxes.class, partition, mboxRoot);
		List<ItemValue<MailboxReplica>> allReplicas = fullApi.allReplicas();
		System.out.println("--------");
		for (ItemValue<MailboxReplica> iv : allReplicas) {
			System.out.println(" * " + iv.uid + " n: " + iv.value.name + " fn: " + iv.value.fullName + ", p: "
					+ iv.value.parentUid);
		}
	}

	@Test
	public void renameFolderWithMultipleChildren() throws IMAPException, InterruptedException {
		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> box : allBoxes) {
			if (box.value.name.equals("INBOX")) {
				inbox = box;
				break;
			}
		}
		assertNotNull(inbox);
		MailboxReplica toCreate = new MailboxReplica();
		long time = System.currentTimeMillis() / 1000;
		toCreate.name = "create" + time;
		String base = toCreate.name;
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(3);
		ItemIdentifier created = mboxesApi.createForHierarchy(ids.globalCounter, toCreate);
		toCreate.name = base + "/a";
		created = mboxesApi.createForHierarchy(ids.globalCounter + 1, toCreate);
		toCreate.name = base + "/b";
		created = mboxesApi.createForHierarchy(ids.globalCounter + 2, toCreate);
		System.out.println("Got a create of version " + created.version);
		ItemValue<MailboxFolder> foundItem = mboxesApi.byName(base);
		System.out.println("Found " + foundItem.value.name);

		String newName = "update" + time;
		foundItem.value.name = newName;
		foundItem.value.fullName = newName;
		CountDownLatch hierUpdLock = expectMessages("mailreplica.hierarchy.updated", 3);

		Ack updated = mboxesApi.updateById(foundItem.internalId, foundItem.value);
		System.out.println("version after update: " + updated.version);

		assertTrue("Expected 3 updates to occur on the hierarchy", hierUpdLock.await(10, TimeUnit.SECONDS));
		imapAsUser(sc -> {
			ListResult foundFolders = sc.listAll();
			for (ListInfo f : foundFolders) {
				System.out.println(" * " + f.getName());
			}
			return null;
		});
		IDbReplicatedMailboxes fullApi = clientProv.instance(IDbReplicatedMailboxes.class, partition, mboxRoot);
		List<ItemValue<MailboxReplica>> allReplicas = fullApi.allReplicas();
		System.out.println("--------");
		for (ItemValue<MailboxReplica> iv : allReplicas) {
			System.out.println(" * " + iv.uid + " n: " + iv.value.name + " fn: " + iv.value.fullName + ", p: "
					+ iv.value.parentUid);
		}
	}

	@Test
	public void renameFolderChangesParent() throws IMAPException, InterruptedException {
		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> box : allBoxes) {
			if (box.value.name.equals("INBOX")) {
				inbox = box;
				break;
			}
		}
		assertNotNull(inbox);
		MailboxReplica toCreate = new MailboxReplica();
		long time = System.currentTimeMillis() / 1000;
		toCreate.name = "create" + time;
		String base = toCreate.name;
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(4);
		ItemIdentifier created = mboxesApi.createForHierarchy(ids.globalCounter, toCreate);
		toCreate.name = base + "/a";
		created = mboxesApi.createForHierarchy(ids.globalCounter + 1, toCreate);
		ItemValue<MailboxFolder> destParent = mboxesApi.getCompleteById(created.id);

		toCreate.name = base + "/b";
		created = mboxesApi.createForHierarchy(ids.globalCounter + 2, toCreate);
		String child = "child" + System.currentTimeMillis();
		toCreate.name = base + "/b/" + child;
		created = mboxesApi.createForHierarchy(ids.globalCounter + 3, toCreate);
		ItemValue<MailboxFolder> preRename = mboxesApi.getCompleteById(created.id);

		System.out.println("Got a create of version " + created.version);
		ItemValue<MailboxFolder> foundItem = mboxesApi.byName(base);
		System.out.println("Found " + foundItem.value.name);

		CountDownLatch hierUpdLock = expectMessages("mailreplica.hierarchy.updated", 1);
		imapAsUser(sc -> {
			sc.rename(base + "/b/" + child, base + "/a/" + child);
			return null;
		});
		assertTrue("Expected 1 update to occur on the hierarchy", hierUpdLock.await(10, TimeUnit.SECONDS));
		ItemValue<MailboxFolder> postRename = mboxesApi.getCompleteById(created.id);
		while (postRename == null || postRename.version == preRename.version) {
			Thread.sleep(100);
			postRename = mboxesApi.getCompleteById(created.id);
		}

		System.err.println("Before: " + preRename + ", after: " + postRename);
		assertNotEquals(preRename.value.parentUid, postRename.value.parentUid);
		assertEquals(postRename.value.parentUid, destParent.uid);

	}

	@Test
	public void renameFolderChangesParentToTopLevel() throws IMAPException, InterruptedException {
		IServiceProvider clientProv = provider();
		IMailboxFolders mboxesApi = clientProv.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();
		ItemValue<MailboxFolder> inbox = null;
		for (ItemValue<MailboxFolder> box : allBoxes) {
			if (box.value.name.equals("INBOX")) {
				inbox = box;
				break;
			}
		}
		assertNotNull(inbox);
		MailboxReplica toCreate = new MailboxReplica();
		long time = System.currentTimeMillis() / 1000;
		toCreate.name = "create" + time;
		String base = toCreate.name;
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(2);
		ItemIdentifier created = mboxesApi.createForHierarchy(ids.globalCounter, toCreate);
		toCreate.name = base + "/a" + time;
		created = mboxesApi.createForHierarchy(ids.globalCounter + 1, toCreate);
		ItemValue<MailboxFolder> toRename = mboxesApi.getCompleteById(created.id);

		System.out.println("Got a create of version " + created.version);
		ItemValue<MailboxFolder> foundItem = mboxesApi.byName(base);
		System.out.println("Found " + foundItem.value.name);

		String src = base + "/a" + time;
		String dst = "a" + time;
		CountDownLatch hierUpdLock = expectMessages("mailreplica.hierarchy.updated", 1);
		imapAsUser(sc -> {
			sc.rename(src, dst);
			return null;
		});
		assertTrue("Expected 1 update to occur on the hierarchy", hierUpdLock.await(10, TimeUnit.SECONDS));

		ItemValue<MailboxFolder> postRename = mboxesApi.getCompleteById(created.id);
		int iter = 0;
		while (iter++ < 50 && (postRename == null || postRename.version == toRename.version)) {
			Thread.sleep(100);
			System.out.println("waiting for rename of " + src + " to " + dst + " " + iter + "...");
			postRename = mboxesApi.getCompleteById(created.id);
		}
		assertFalse("took too long", iter >= 50);
		assertEquals(toRename.uid, postRename.uid);
		System.err.println("Before: " + toRename.value + ", after: " + postRename.value);
		assertNotEquals(toRename.value.parentUid, postRename.value.parentUid);
		assertEquals(null, postRename.value.parentUid);

	}

	@Test
	public void copyIn_SUCCESS() throws IOException, InterruptedException {

		IMailboxFolders foldersApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(7);
		long offlineId = ids.globalCounter;

		MailboxReplica mr = new MailboxReplica();
		String root = "root" + System.currentTimeMillis();
		mr.name = root;

		// create root
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src
		mr.name = mr.name + "/src";
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src/dst
		mr.name = mr.name + "/dst";
		foldersApi.createForHierarchy(offlineId++, mr);

		// append mail into root/src
		ItemValue<MailboxFolder> src = foldersApi.byName(root + "/src");
		long id = offlineId++;
		addDraft(src, id);

		long id2 = offlineId++;
		addDraft(src, id2);

		IMailboxItems itemApi = provider().instance(IMailboxItems.class, src.uid);

		// copy into root/src/dst
		long expectedId = offlineId++;
		long expectedId2 = offlineId++;

		ImportMailboxItemSet toCopy = ImportMailboxItemSet.copyIn(src.internalId,
				Arrays.asList(MailboxItemId.of(id), MailboxItemId.of(id2)),
				Arrays.asList(MailboxItemId.of(expectedId), MailboxItemId.of(expectedId2)));

		ItemValue<MailboxFolder> dst = foldersApi.byName(root + "/src/dst");
		ImportMailboxItemsStatus ret = foldersApi.importItems(dst.internalId, toCopy);

		assertEquals(ImportStatus.SUCCESS, ret.status);
		assertEquals(2, ret.doneIds.size());
		assertEquals(expectedId, ret.doneIds.get(0).destination);
		assertEquals(expectedId2, ret.doneIds.get(1).destination);

		// check
		itemApi = provider().instance(IMailboxItems.class, dst.uid);
		ItemValue<MailboxItem> copy = itemApi.getCompleteById(expectedId);
		assertNotNull(copy);

		copy = itemApi.getCompleteById(expectedId2);
		assertNotNull(copy);
	}

	@Test
	public void copyIn_SUCCESS_NoExpectedIds() throws IOException, InterruptedException {

		IMailboxFolders foldersApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(5);
		long offlineId = ids.globalCounter;

		MailboxReplica mr = new MailboxReplica();
		String root = "root" + System.currentTimeMillis();
		mr.name = root;

		// create root
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src
		mr.name = mr.name + "/src";
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src/dst
		mr.name = mr.name + "/dst";
		foldersApi.createForHierarchy(offlineId++, mr);

		// append mail into root/src
		ItemValue<MailboxFolder> src = foldersApi.byName(root + "/src");
		long id = offlineId++;
		addDraft(src, id);

		long id2 = offlineId++;
		addDraft(src, id2);

		IMailboxItems itemApi = provider().instance(IMailboxItems.class, src.uid);

		// copy into root/src/dst
		ImportMailboxItemSet toCopy = ImportMailboxItemSet.copyIn(src.internalId,
				Arrays.asList(MailboxItemId.of(id), MailboxItemId.of(id2)), Collections.emptyList());

		ItemValue<MailboxFolder> dst = foldersApi.byName(root + "/src/dst");
		ImportMailboxItemsStatus ret = foldersApi.importItems(dst.internalId, toCopy);

		assertEquals(ImportStatus.SUCCESS, ret.status);
		assertEquals(2, ret.doneIds.size());
		long expectedId = ret.doneIds.get(0).destination;
		long expectedId2 = ret.doneIds.get(1).destination;

		// check
		itemApi = provider().instance(IMailboxItems.class, dst.uid);
		ItemValue<MailboxItem> copy = itemApi.getCompleteById(expectedId);
		assertNotNull(copy);

		copy = itemApi.getCompleteById(expectedId2);
		assertNotNull(copy);
	}

	@Test
	public void copyIn_PARTIAL() throws IOException, InterruptedException {

		IMailboxFolders foldersApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(7);
		long offlineId = ids.globalCounter;

		MailboxReplica mr = new MailboxReplica();
		String root = "root" + System.currentTimeMillis();
		mr.name = root;

		// create root
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src
		mr.name = mr.name + "/src";
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src/dst
		mr.name = mr.name + "/dst";
		foldersApi.createForHierarchy(offlineId++, mr);

		// append mail into root/src
		ItemValue<MailboxFolder> src = foldersApi.byName(root + "/src");
		long id = offlineId++;
		addDraft(src, id);

		long id2 = offlineId++;
		addDraft(src, id2);

		IMailboxItems itemApi = provider().instance(IMailboxItems.class, src.uid);

		// copy into root/src/dst
		long expectedId = offlineId++;
		long expectedId2 = offlineId++;

		ImportMailboxItemSet toCopy = ImportMailboxItemSet.copyIn(src.internalId,
				Arrays.asList(MailboxItemId.of(id), MailboxItemId.of(id2), MailboxItemId.of(0L)),
				Arrays.asList(MailboxItemId.of(expectedId), MailboxItemId.of(expectedId2), MailboxItemId.of(0L)));

		ItemValue<MailboxFolder> dst = foldersApi.byName(root + "/src/dst");
		ImportMailboxItemsStatus ret = foldersApi.importItems(dst.internalId, toCopy);

		assertEquals(ImportStatus.PARTIAL, ret.status);
		assertEquals(2, ret.doneIds.size());
		assertEquals(expectedId, ret.doneIds.get(0).destination);
		assertEquals(expectedId2, ret.doneIds.get(1).destination);

		// check
		itemApi = provider().instance(IMailboxItems.class, dst.uid);
		ItemValue<MailboxItem> copy = itemApi.getCompleteById(expectedId);
		assertNotNull(copy);

		copy = itemApi.getCompleteById(expectedId2);
		assertNotNull(copy);

	}

	@Test
	public void copyIn_ERROR() {

		IMailboxFolders foldersApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(7);
		long offlineId = ids.globalCounter;

		MailboxReplica mr = new MailboxReplica();
		String root = "root" + System.currentTimeMillis();
		mr.name = root;

		// create root
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src
		mr.name = mr.name + "/src";
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src/dst
		mr.name = mr.name + "/dst";
		foldersApi.createForHierarchy(offlineId++, mr);

		ItemValue<MailboxFolder> src = foldersApi.byName(root + "/src");
		ImportMailboxItemSet toCopy = ImportMailboxItemSet.copyIn(src.internalId,
				Arrays.asList(MailboxItemId.of(0L), MailboxItemId.of(1L), MailboxItemId.of(2L)),
				Arrays.asList(MailboxItemId.of(0L), MailboxItemId.of(1L), MailboxItemId.of(2L)));

		ItemValue<MailboxFolder> dst = foldersApi.byName(root + "/src/dst");
		ImportMailboxItemsStatus ret = foldersApi.importItems(dst.internalId, toCopy);

		assertEquals(ImportStatus.ERROR, ret.status);
		assertTrue(ret.doneIds.isEmpty());

	}

	@Test
	public void copyIn_ExpectedIdsSizeDoesNotMatch() {
		IMailboxFolders foldersApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);

		ImportMailboxItemSet toCopy = ImportMailboxItemSet.copyIn(0L,
				Arrays.asList(MailboxItemId.of(0L), MailboxItemId.of(1L), MailboxItemId.of(2L)),
				Arrays.asList(MailboxItemId.of(0L)));

		try {
			foldersApi.importItems(0L, toCopy);
			fail();
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

	}

	@Test
	public void moveIn_SUCCESS_PERF() throws IOException, InterruptedException {
		int count = 20;
		IMailboxFolders foldersApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(4 * count + 50);
		long offlineId = ids.globalCounter;
		System.err.println("Allocated " + ids);

		MailboxReplica mr = new MailboxReplica();
		String root = "root" + System.currentTimeMillis();
		mr.name = root;

		// create root
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src
		mr.name = mr.name + "/src";
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src/dst
		mr.name = mr.name + "/dst";
		foldersApi.createForHierarchy(offlineId++, mr);

		ItemValue<MailboxFolder> src = foldersApi.byName(root + "/src");
		ItemValue<MailboxFolder> dst = foldersApi.byName(root + "/src/dst");

		long id = offlineId++;
		addDraft(src, id);

		long id2 = offlineId++;
		addDraft(src, id2);

		ItemValue<MailboxFolder> currentSrc = src;
		ItemValue<MailboxFolder> currentDest = dst;

		for (int i = 0; i < count; i++) {
			System.err.println("Loop " + (i + 1) + " / " + count + " with ranges " + ids);

			// move into root/src/dst
			long expectedId = offlineId++;
			long expectedId2 = offlineId++;

			ImportMailboxItemSet toMove = ImportMailboxItemSet.moveIn(currentSrc.internalId,
					Arrays.asList(MailboxItemId.of(id), MailboxItemId.of(id2)),
					Arrays.asList(MailboxItemId.of(expectedId), MailboxItemId.of(expectedId2)));

			System.err.println("Import to ids " + expectedId + " and " + expectedId2 + " starts..");
			ImportMailboxItemsStatus ret = foldersApi.importItems(currentDest.internalId, toMove);

			System.err.println("move result: " + ret);
			assertEquals(ImportStatus.SUCCESS, ret.status);
			assertEquals(2, ret.doneIds.size());

			// check
			IMailboxItems itemApi = provider().instance(IMailboxItems.class, currentDest.uid);
			ItemValue<MailboxItem> copy = itemApi.getCompleteById(expectedId);
			assertNotNull(copy);

			copy = itemApi.getCompleteById(expectedId2);
			assertNotNull(copy);

			id = expectedId;
			id2 = expectedId2;
			ItemValue<MailboxFolder> tmp = currentSrc;
			currentSrc = currentDest;
			currentDest = tmp;

		}
	}

	@Test
	public void moveIn_SUCCESS() throws IOException, InterruptedException {

		IMailboxFolders foldersApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(7);
		long offlineId = ids.globalCounter;

		MailboxReplica mr = new MailboxReplica();
		String root = "root" + System.currentTimeMillis();
		mr.name = root;

		// create root
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src
		mr.name = mr.name + "/src";
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src/dst
		mr.name = mr.name + "/dst";
		foldersApi.createForHierarchy(offlineId++, mr);

		// append mail into root/src
		ItemValue<MailboxFolder> src = foldersApi.byName(root + "/src");
		long id = offlineId++;
		ItemValue<MailboxItem> draf1 = addDraft(src, id);

		long id2 = offlineId++;
		ItemValue<MailboxItem> draft2 = addDraft(src, id2);
		System.err.println("Draft1 " + draf1.internalId + ", Draft2 " + draft2.internalId);

		// move into root/src/dst
		long expectedId = offlineId++;
		long expectedId2 = offlineId;

		ImportMailboxItemSet toMove = ImportMailboxItemSet.moveIn(src.internalId,
				Arrays.asList(MailboxItemId.of(id), MailboxItemId.of(id2)),
				Arrays.asList(MailboxItemId.of(expectedId), MailboxItemId.of(expectedId2)));

		ItemValue<MailboxFolder> dst = foldersApi.byName(root + "/src/dst");

		System.err.println("MoveIn " + toMove + "...");
		ImportMailboxItemsStatus ret = foldersApi.importItems(dst.internalId, toMove);

		assertEquals(ImportStatus.SUCCESS, ret.status);
		assertEquals(2, ret.doneIds.size());
		Set<Long> done = ret.doneIds.stream().map(m -> m.destination).collect(Collectors.toSet());
		assertTrue(done.contains(expectedId));
		assertTrue(done.contains(expectedId2));
		assertEquals(expectedId, ret.doneIds.get(0).destination);
		assertEquals(expectedId2, ret.doneIds.get(1).destination);

		// check
		IMailboxItems itemApi = provider().instance(IMailboxItems.class, dst.uid);
		ItemValue<MailboxItem> copy = itemApi.getCompleteById(expectedId);
		assertNotNull(copy);

		copy = itemApi.getCompleteById(expectedId2);
		assertNotNull(copy);

		// check delete from source
		itemApi = provider().instance(IMailboxItems.class, src.uid);
		ItemValue<MailboxItem> deleted = itemApi.getCompleteById(id);
		System.err.println("Deleted: " + deleted);
		assertTrue(deleted.value.flags.contains(MailboxItemFlag.System.Deleted.value()));

		deleted = itemApi.getCompleteById(id2);
		assertTrue(deleted.value.flags.contains(MailboxItemFlag.System.Deleted.value()));

	}

	@Test
	public void moveIn_PARTIAL() throws IOException, InterruptedException {

		IMailboxFolders foldersApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(7);
		long offlineId = ids.globalCounter;

		MailboxReplica mr = new MailboxReplica();
		String root = "root" + System.currentTimeMillis();
		mr.name = root;

		// create root
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src
		mr.name = mr.name + "/src";
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src/dst
		mr.name = mr.name + "/dst";
		foldersApi.createForHierarchy(offlineId++, mr);

		// append mail into root/src
		ItemValue<MailboxFolder> src = foldersApi.byName(root + "/src");
		long id = offlineId++;
		addDraft(src, id);

		long id2 = offlineId++;
		addDraft(src, id2);

		IMailboxItems itemApi = provider().instance(IMailboxItems.class, src.uid);

		// copy into root/src/dst
		long expectedId = offlineId++;
		long expectedId2 = offlineId++;

		ImportMailboxItemSet toMove = ImportMailboxItemSet.moveIn(src.internalId,
				Arrays.asList(MailboxItemId.of(id), MailboxItemId.of(id2), MailboxItemId.of(0L)),
				Arrays.asList(MailboxItemId.of(expectedId), MailboxItemId.of(expectedId2), MailboxItemId.of(0L)));

		ItemValue<MailboxFolder> dst = foldersApi.byName(root + "/src/dst");
		ImportMailboxItemsStatus ret = foldersApi.importItems(dst.internalId, toMove);

		assertEquals(ImportStatus.PARTIAL, ret.status);
		assertEquals(2, ret.doneIds.size());
		assertEquals(expectedId, ret.doneIds.get(0).destination);
		assertEquals(expectedId2, ret.doneIds.get(1).destination);

		// check
		itemApi = provider().instance(IMailboxItems.class, dst.uid);
		ItemValue<MailboxItem> copy = itemApi.getCompleteById(expectedId);
		assertNotNull(copy);

		copy = itemApi.getCompleteById(expectedId2);
		assertNotNull(copy);

		// check delete from source
		itemApi = provider().instance(IMailboxItems.class, src.uid);
		ItemValue<MailboxItem> deleted = itemApi.getCompleteById(id);
		assertTrue(deleted.value.flags.contains(MailboxItemFlag.System.Deleted.value()));
		deleted = itemApi.getCompleteById(id2);
		assertTrue(deleted.value.flags.contains(MailboxItemFlag.System.Deleted.value()));
	}

	@Test
	public void moveIn_ERROR() {

		IMailboxFolders foldersApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(7);
		long offlineId = ids.globalCounter;

		MailboxReplica mr = new MailboxReplica();
		String root = "root" + System.currentTimeMillis();
		mr.name = root;

		// create root
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src
		mr.name = mr.name + "/src";
		foldersApi.createForHierarchy(offlineId++, mr);

		// create root/src/dst
		mr.name = mr.name + "/dst";
		foldersApi.createForHierarchy(offlineId++, mr);

		ItemValue<MailboxFolder> src = foldersApi.byName(root + "/src");
		ImportMailboxItemSet toMove = ImportMailboxItemSet.moveIn(src.internalId,
				Arrays.asList(MailboxItemId.of(0L), MailboxItemId.of(1L), MailboxItemId.of(2L)),
				Arrays.asList(MailboxItemId.of(0L), MailboxItemId.of(1L), MailboxItemId.of(2L)));

		ItemValue<MailboxFolder> dst = foldersApi.byName(root + "/src/dst");
		ImportMailboxItemsStatus ret = foldersApi.importItems(dst.internalId, toMove);

		assertEquals(ImportStatus.ERROR, ret.status);
		assertTrue(ret.doneIds.isEmpty());

	}

	@Test
	public void testApiFixDispositionType() throws IMAPException, InterruptedException, IOException {
		// create inbox
		final IMailboxFolders mailboxFolders = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		final ItemValue<MailboxFolder> inbox = mailboxFolders.byName("INBOX");
		assertNotNull(inbox);

		// create APPLE-MAIL draft containing inline parts not displayed by
		// other client
		final ItemValue<MailboxItem> reloaded = addDraft(inbox);

		// check disposition types are fixed ( 0:text without disposition,
		// 1:image
		// attachment,
		// 2:image attachment)
		final List<Part> subParts = reloaded.value.body.structure.children;
		assertEquals(DispositionType.ATTACHMENT, subParts.get(1).dispositionType);
		assertEquals(DispositionType.ATTACHMENT, subParts.get(2).dispositionType);
		assertNull(subParts.get(0).dispositionType);

		// should have real attachments (it is based on disposition type)
		assertTrue(reloaded.value.body.structure.hasRealAttachments());
		assertEquals(2, reloaded.value.body.structure.nonInlineAttachments().size());
	}

	/**
	 * DISABLED
	 */
	public void softDelete() throws InterruptedException {
		IContainersFlatHierarchy hierarchyApi = provider().instance(IContainersFlatHierarchy.class, domainUid, userUid);

		ContainerChangeset<ItemVersion> changeset = hierarchyApi.filteredChangesetById(0L,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));

		String mbox = "test" + System.currentTimeMillis();
		imapAsUser(sc -> {
			sc.create(mbox);
			return null;
		});

		Thread.sleep(1000);

		changeset = hierarchyApi.filteredChangesetById(changeset.version,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));

		assertEquals(1, changeset.created.size());

		long folderId = changeset.created.get(0).id;

		imapAsUser(sc -> {
			sc.deleteMailbox(mbox);
			return null;
		});

		Thread.sleep(1000);

		long version = changeset.version;

		changeset = hierarchyApi.filteredChangesetById(version, ItemFlagFilter.create().mustNot(ItemFlag.Deleted));

		assertEquals(1, changeset.deleted.size());

		// not filtered changeset

		ContainerChangeset<String> nonFilteredChangeset = hierarchyApi.changeset(version);
		assertEquals(1, nonFilteredChangeset.updated.size());

		ItemValue<ContainerHierarchyNode> folder = hierarchyApi.getComplete(nonFilteredChangeset.updated.get(0));
		assertNotNull(folder);
		assertTrue(folder.flags.contains(ItemFlag.Deleted));

		// deleted changeset
		changeset = hierarchyApi.filteredChangesetById(0L, ItemFlagFilter.create().must(ItemFlag.Deleted));
		assertEquals(1, changeset.created.size());
		assertEquals(folderId, changeset.created.get(0).id);
		folder = hierarchyApi.getCompleteById(changeset.created.get(0).id);
		assertNotNull(folder);
		assertTrue(folder.flags.contains(ItemFlag.Deleted));
		assertTrue(folder.displayName.startsWith(mbox + "/"));

	}

	/**
	 * DISABLED
	 */
	public void softDelete_Mailshare() throws InterruptedException {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailshare mailshareApi = prov.instance(IMailshare.class, domainUid);

		String mailshareUid = "shared" + System.currentTimeMillis();

		Mailshare mailshare = new Mailshare();
		mailshare.name = mailshareUid;
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domainUid, true));
		mailshare.routing = Routing.internal;

		mailshareApi.create(mailshareUid, mailshare);

		IContainerManagement c = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(mailshareUid));
		List<AccessControlEntry> accessControlList = new ArrayList<>(c.getAccessControlList());
		accessControlList.add(AccessControlEntry.create(userUid, Verb.Write));
		c.setAccessControlList(accessControlList);

		Thread.sleep(1000);

		IContainersFlatHierarchy hierarchyApi = prov.instance(IContainersFlatHierarchy.class, domainUid, mailshareUid);
		IMailboxFolders foldersApi = provider().instance(IMailboxFolders.class, partition, mailshareUid);

		String mbox = "test" + System.currentTimeMillis();

		ContainerChangeset<ItemVersion> changeset = hierarchyApi.filteredChangesetById(0L,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));

		ContainerChangeset<ItemVersion> foldersChangeset = foldersApi.filteredChangesetById(0L,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));

		imapAsCyrusAdmin(sc -> {
			sc.create(mailshare.name + "/" + mbox + "@" + domainUid);
			return null;
		});
		Thread.sleep(1000);

		changeset = hierarchyApi.filteredChangesetById(changeset.version,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));

		assertEquals(1, changeset.created.size());

		long folderId = changeset.created.get(0).id;

		foldersChangeset = foldersApi.filteredChangesetById(foldersChangeset.version,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
		assertEquals(1, foldersChangeset.created.size());

		imapAsCyrusAdmin(sc -> {
			sc.deleteMailbox(mailshare.name + "/" + mbox + "@" + domainUid);
			return null;
		});
		Thread.sleep(1000);

		long version = changeset.version;
		changeset = hierarchyApi.filteredChangesetById(version, ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
		assertEquals(1, changeset.deleted.size());

		long foldersVersion = foldersChangeset.version;
		foldersChangeset = foldersApi.filteredChangesetById(foldersVersion,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
		assertEquals(1, foldersChangeset.deleted.size());

		//
		ContainerChangeset<String> nonFilteredChangeset = hierarchyApi.changeset(version);
		assertEquals(1, nonFilteredChangeset.updated.size());

		ItemValue<ContainerHierarchyNode> folder = hierarchyApi.getComplete(nonFilteredChangeset.updated.get(0));
		assertNotNull(folder);
		assertTrue(folder.flags.contains(ItemFlag.Deleted));

		// deleted changeset
		changeset = hierarchyApi.filteredChangesetById(0L, ItemFlagFilter.create().must(ItemFlag.Deleted));
		assertEquals(1, changeset.created.size());
		assertEquals(folderId, changeset.created.get(0).id);
		folder = hierarchyApi.getCompleteById(changeset.created.get(0).id);
		assertNotNull(folder);
		assertTrue(folder.flags.contains(ItemFlag.Deleted));
		assertTrue(folder.displayName.startsWith(mbox + "/"));

	}

	@Test
	public void renameMailbox() throws Exception {

		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailshare mailshareApi = prov.instance(IMailshare.class, domainUid);

		String mailshareUid = "shared" + System.currentTimeMillis();
		Mailshare mailshare = new Mailshare();
		mailshare.name = mailshareUid;
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domainUid, true));
		mailshare.routing = Routing.internal;

		CompletableFuture<MailboxReplicaRootDescriptor> onRoot = ReplicationEvents.onMailboxRootCreated();
		MailboxReplicaRootDescriptor expected = MailboxReplicaRootDescriptor.create(Namespace.shared, mailshare.name);
		Subtree sub = SubtreeContainer.mailSubtreeUid(domainUid, expected.ns, mailshare.name);
		String subtreeUid = sub.subtreeUid();
		System.err.println("On subtree update " + subtreeUid);
		CompletableFuture<ItemIdentifier> onSubtree = ReplicationEvents.onSubtreeUpdate(subtreeUid);
		CompletableFuture<Void> allEvents = CompletableFuture.allOf(onRoot, onSubtree);

		mailshareApi.create(mailshareUid, mailshare);
		allEvents.get(10, TimeUnit.SECONDS);

		Thread.sleep(4000);
		mailshare.name = mailshare.name + "-updated";
		System.err.println("Slept before renaming to " + mailshare.name);

		mailshareApi.update(mailshareUid, mailshare);

		Thread.sleep(2000);

		IDbReplicatedMailboxes fullFolders = provider().instance(IDbReplicatedMailboxes.class, partition,
				mailshare.name);
		List<ItemValue<MailboxReplica>> allFoldersFull = fullFolders.allReplicas();
		assertEquals(2, allFoldersFull.size());
		boolean found = false;
		boolean foundSent = false;

		for (ItemValue<MailboxReplica> folder : allFoldersFull) {
			System.out.println("Got " + folder.uid + ", " + folder.value.fullName);
			if ((mailshare.name + "/Sent").equals(folder.value.fullName)) {
				foundSent = true;
			}
			if (mailshare.name.equals(folder.value.fullName)) {
				found = true;
			}
		}

		assertTrue(found);
		assertTrue(foundSent);

	}

	@Test
	public void createById_Accent() {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		String folderName = "élo" + System.currentTimeMillis();

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange oneId = idAllocator.allocateOfflineIds(1);
		long expectedFolderId = oneId.globalCounter;
		MailboxFolder folder = new MailboxFolder();
		folder.name = folderName;
		ItemIdentifier createAck = mboxesApi.createForHierarchy(expectedFolderId, folder);

		imapAsUser(sc -> {
			ListResult all = sc.listAll();
			assertTrue(all.stream().anyMatch(f -> f.getName().equals(folderName)));
			return null;
		});

		assertNotNull(createAck);
		ItemValue<MailboxFolder> folderItem = mboxesApi.byName(folderName);
		assertNotNull(folderItem);
		assertEquals(createAck.id, folderItem.internalId);
		assertEquals(createAck.uid, folderItem.uid);
		IContainersFlatHierarchy hierarchyApi = provider().instance(IContainersFlatHierarchy.class, domainUid, userUid);
		ItemValue<ContainerHierarchyNode> inHierarchy = hierarchyApi.getCompleteById(expectedFolderId);
		assertNotNull(inHierarchy);
		assertEquals(expectedFolderId, inHierarchy.internalId);
		System.err.println("Got node: " + inHierarchy);
	}

	@Test
	public void multipleDeleteById() throws InterruptedException {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		String folderName = "coucou" + System.currentTimeMillis();

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange oneId = idAllocator.allocateOfflineIds(1);
		long expectedFolderId = oneId.globalCounter;
		MailboxFolder folder = new MailboxFolder();
		folder.name = folderName;
		mboxesApi.createForHierarchy(expectedFolderId, folder);

		imapAsUser(sc -> {
			ListResult all = sc.listAll();
			assertTrue(all.stream().anyMatch(f -> f.getName().equals(folderName)));
			for (int i = 0; i < 10; i++) {
				int added = sc.append(folderName, testEml(), new FlagsList());
				assertTrue(added > 0);
			}

			return null;
		});

		Thread.sleep(2000);

		IServiceProvider prov = provider();
		IMailboxFolders userMboxesApi = prov.instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> found = userMboxesApi.all();
		assertNotNull(found);
		ItemValue<MailboxFolder> mbox = null;
		for (ItemValue<MailboxFolder> iv : found) {
			System.out.println("Got " + iv.value.name);
			if (iv.value.name.equals(folderName)) {
				mbox = iv;
				break;
			}
		}
		assertNotNull(mbox);
		IMailboxItems recordsApi = prov.instance(IMailboxItems.class, mbox.uid);

		ContainerChangeset<Long> changeset = recordsApi.changesetById(0L);
		assertEquals(10, changeset.created.size());

		recordsApi.multipleDeleteById(changeset.created);

		Thread.sleep(2000);

		changeset = recordsApi.changesetById(changeset.version);
		assertEquals(10, changeset.updated.size());

		changeset.updated.forEach(up -> {
			ItemValue<MailboxItem> record = recordsApi.getCompleteById(up);
			assertTrue(record.value.flags.contains(MailboxItemFlag.System.Deleted.value()));
		});

	}

	@Test
	public void markFolderAsRead() throws InterruptedException {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		String folderName = "f" + System.currentTimeMillis();
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(2);
		long folderId = ids.globalCounter;
		MailboxFolder folder = new MailboxFolder();
		folder.fullName = folderName;
		folder.name = folderName;

		ItemIdentifier createAck = mboxesApi.createForHierarchy(folderId, folder);
		assertNotNull(createAck);

		ItemValue<MailboxFolder> folderItem = mboxesApi.byName(folderName);
		assertNotNull(folderItem);

		System.err.println("folder, id " + folderItem.internalId + ", " + folderItem);
		IMailboxItems recordsApi = provider().instance(IMailboxItems.class, folderItem.uid);
		Stream forUpload = VertxStream.stream(Buffer.buffer("Coucou\r\n".getBytes()));
		String partId = recordsApi.uploadPart(forUpload);
		assertNotNull(partId);

		MailboxItem item = MailboxItem.of("toto", Part.create(null, "text/plain", partId));
		long expectedId = folderId + 1;
		recordsApi.createById(expectedId, item);
		ItemValue<MailboxItem> message = recordsApi.getCompleteById(expectedId);
		assertNotNull(message);
		assertFalse(messageIsSeen(message));

		CountDownLatch hierUpdLock = expectMessages("mailreplica.hierarchy.updated", 1);

		System.err.println("Before marks as read....");
		mboxesApi.markFolderAsRead(folderItem.internalId);
		System.err.println("Mark as read done.");

		Count cnt = recordsApi.count(UNREAD_NOT_DELETED);
		System.err.println("perUser: " + cnt.total);

		assertTrue("Expected 1 update to occur on the hierarchy", hierUpdLock.await(10, TimeUnit.SECONDS));

		checkMessageIsSeen(message, folderItem.uid);
	}

	@Test
	public void testPerUserUnreadNotContainer() {
		IMailboxItems restCall = provider().instance(IMailboxItems.class, UUID.randomUUID().toString());
		try {
			Count unread = restCall.count(UNREAD_NOT_DELETED);
			fail("call should not be possible but got " + unread);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void markSharedFolderAsRead()
			throws InterruptedException, ExecutionException, TimeoutException, IOException {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailshare mailshareApi = prov.instance(IMailshare.class, domainUid);
		Mailshare mailshare = new Mailshare();
		mailshare.name = "shared" + System.currentTimeMillis();
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domainUid, true));
		mailshare.routing = Routing.internal;

		// setup events expectations
		CompletableFuture<MailboxReplicaRootDescriptor> onRoot = ReplicationEvents.onMailboxRootCreated();
		MailboxReplicaRootDescriptor expected = MailboxReplicaRootDescriptor.create(Namespace.shared, mailshare.name);
		Subtree sub = SubtreeContainer.mailSubtreeUid(domainUid, expected.ns, mailshare.name);
		String subtreeUid = sub.subtreeUid();
		System.err.println("On subtree update " + subtreeUid);
		CompletableFuture<ItemIdentifier> onSubtree = ReplicationEvents.onSubtreeUpdate(subtreeUid);
		CompletableFuture<Void> allEvents = CompletableFuture.allOf(onRoot, onSubtree);

		System.err.println("Before create.....");
		mailshareApi.create(mailshare.name, mailshare);

		IContainerManagement c = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(mailshare.name));
		List<AccessControlEntry> accessControlList = new ArrayList<>(c.getAccessControlList());
		accessControlList.add(AccessControlEntry.create(userUid, Verb.Write));
		c.setAccessControlList(accessControlList);

		allEvents.get(10, TimeUnit.SECONDS);
		MailboxReplicaRootDescriptor mailshareRoot = onRoot.get();
		assertNotNull(mailshareRoot);

		System.err.println(
				"**** ROOT is " + mailshareRoot.ns + ", " + mailshareRoot.name + ", version: " + onSubtree.get());
		Thread.sleep(500);
		IMailboxFolders folders = provider().instance(IMailboxFolders.class, partition, mailshare.name);
		ItemValue<MailboxFolder> sharedSent = folders.byName(mailshare.name + "/Sent");
		assertNotNull(sharedSent);
		System.err.println("sharedSent: " + sharedSent.value);

		int max = 3;
		List<ItemValue<MailboxItem>> messages = new ArrayList<>(max);
		for (int i = 0; i < max; i++) {
			ItemValue<MailboxItem> item = addDraft(sharedSent, mailshare.name);
			assertFalse(messageIsSeen(item));
			messages.add(item);
		}

		CountDownLatch hierUpdLock = expectMessages("mailreplica.hierarchy.updated", 1);

		System.err.println("Before marks as read....");
		folders.markFolderAsRead(sharedSent.internalId);
		System.err.println("Mark as read done.");

		assertTrue("Expected 1 update to occur on the hierarchy", hierUpdLock.await(10, TimeUnit.SECONDS));

		IMailboxItems mailboxItemsService = provider().instance(IMailboxItems.class, sharedSent.uid);

		Count perUser = mailboxItemsService.count(UNREAD_NOT_DELETED);
		System.err.println("perUserUnread is " + perUser.total);

		List<Long> messageIds = messages.stream().map(m -> m.internalId).collect(Collectors.toList());
		mailboxItemsService.multipleById(messageIds).forEach(message -> checkMessageIsSeen(message, sharedSent.uid));
	}

	@Test
	public void deleteFlag() throws IOException, InterruptedException {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");

		ItemValue<MailboxItem> mail = addDraft(inbox);

		IMailboxItems itemsApi = provider().instance(IMailboxItems.class, inbox.uid);
		ItemValue<MailboxItem> mailboxItem = itemsApi.getCompleteById(mail.internalId);
		assertFalse(mailboxItem.value.flags.contains(MailboxItemFlag.System.Seen.value()));

		itemsApi.addFlag(FlagUpdate.of(mail.internalId, MailboxItemFlag.System.Seen.value()));
		mailboxItem = itemsApi.getCompleteById(mail.internalId);
		assertTrue(mailboxItem.value.flags.contains(MailboxItemFlag.System.Seen.value()));

		itemsApi.deleteFlag(FlagUpdate.of(mail.internalId, MailboxItemFlag.System.Seen.value()));
		mailboxItem = itemsApi.getCompleteById(mail.internalId);
		assertFalse(mailboxItem.value.flags.contains(MailboxItemFlag.System.Seen.value()));
	}

	@Test
	public void deleteUnsetFlag() throws IOException, InterruptedException {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");

		ItemValue<MailboxItem> mail = addDraft(inbox);

		IMailboxItems itemsApi = provider().instance(IMailboxItems.class, inbox.uid);
		ItemValue<MailboxItem> mailboxItem = itemsApi.getCompleteById(mail.internalId);
		assertFalse(mailboxItem.value.flags.contains(MailboxItemFlag.System.Seen.value()));

		itemsApi.deleteFlag(FlagUpdate.of(mail.internalId, MailboxItemFlag.System.Seen.value()));

		mailboxItem = itemsApi.getCompleteById(mail.internalId);
		assertFalse(mailboxItem.value.flags.contains(MailboxItemFlag.System.Seen.value()));
	}

	@Test
	public void deleteFlagUnknownMail() {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");
		IMailboxItems itemsApi = provider().instance(IMailboxItems.class, inbox.uid);
		Ack ack = itemsApi.deleteFlag(FlagUpdate.of(98765432L, MailboxItemFlag.System.Seen.value()));
		assertEquals(0L, ack.version);
	}

	private boolean messageIsSeen(ItemValue<MailboxItem> message) {
		return message.value.flags.contains(MailboxItemFlag.System.Seen.value());
	}

	private void checkMessageIsSeen(ItemValue<MailboxItem> message, String folderUid) {
		IMailboxItems recordsApi = provider().instance(IMailboxItems.class, folderUid);
		long maxWait = 10000;
		long time = System.currentTimeMillis();
		long maxTime = time + maxWait;
		message = recordsApi.getCompleteById(message.internalId);
		while (!messageIsSeen(message) && time < maxTime) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
			}
			message = recordsApi.getCompleteById(message.internalId);
			time = System.currentTimeMillis();
		}
		System.err.println(String.format("Waited %dms %s", maxWait - (maxTime - time),
				time < maxTime ? "" : " (max wait reached)"));

		assertTrue("Expected the message to have a 'Seen' flag", messageIsSeen(message));
	}

	@Test
	public void testMailConversation() throws Exception {
		String user2Uid = PopulateHelper.addUser("user2", domainUid, Routing.internal);
		IMailConversation user1ConversationService = provider().instance(IMailConversation.class,
				IMailReplicaUids.conversationSubtreeUid(domainUid, userUid));
		IMailboxFolders user1MboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> user1Inbox = user1MboxesApi.byName("INBOX");
		ItemValue<MailboxFolder> user1SentBox = user1MboxesApi.byName("Sent");

		//
		// simulate user1 sends to user2 (should generate a conversation for
		// user1 in Sent)
		//
		long user1ItemId = createEml("data/user1_send_to_user2.eml", userUid, mboxRoot, "Sent");
		String user2MboxRoot = "user." + user2Uid.replace('.', '^');
		createEml("data/user1_send_to_user2.eml", user2Uid, user2MboxRoot, "INBOX");
		List<ItemValue<Conversation>> user1InboxConversations = user1ConversationService.byFolder(user1Inbox.uid,
				ItemFlagFilter.all());
		// a conversation already exists in INBOX due to #before method
		assertEquals(1, user1InboxConversations.size());
		List<ItemValue<Conversation>> user1SentConversations = user1ConversationService.byFolder(user1SentBox.uid,
				ItemFlagFilter.all());
		assertEquals(1, user1SentConversations.size());
		long conversationId = Long.parseUnsignedLong(user1SentConversations.get(0).uid, 16);
		long numberOfMessagesInConversation = user1SentConversations.get(0).value.messageRefs.size();
		assertEquals(1, numberOfMessagesInConversation);

		//
		// simulate user2 replies to user1 (should generate a conversation for
		// user1 in Inbox)
		//
		createEml("data/user2_reply_to_user1.eml", user2Uid, user2MboxRoot, "Sent");
		long user1ItemId2 = createEml("data/user2_reply_to_user1.eml", userUid, mboxRoot, "INBOX");
		user1InboxConversations = user1ConversationService.byFolder(user1Inbox.uid, ItemFlagFilter.all());
		assertEquals(2, user1InboxConversations.size());
		assertEquals(2, user1InboxConversations.get(1).value.messageRefs.size());
		assertEquals(user1ItemId, user1InboxConversations.get(1).value.messageRefs.get(0).itemId);
		assertEquals(user1ItemId2, user1InboxConversations.get(1).value.messageRefs.get(1).itemId);
		user1SentConversations = user1ConversationService.byFolder(user1SentBox.uid, ItemFlagFilter.all());
		assertEquals(1, user1SentConversations.size());
		assertEquals(conversationId, Long.parseUnsignedLong(user1InboxConversations.get(1).uid, 16));
		numberOfMessagesInConversation = user1InboxConversations.get(1).value.messageRefs.size();
		assertEquals(2, numberOfMessagesInConversation);

		//
		// simulate user1 sends another one to user2 (should not generate more
		// conversation for user1 in Inbox, but one more in Sent)
		//
		createEml("data/user1_send_another_to_user2.eml", userUid, mboxRoot, "Sent");
		createEml("data/user1_send_another_to_user2.eml", user2Uid, user2MboxRoot, "INBOX");
		user1InboxConversations = user1ConversationService.byFolder(user1Inbox.uid, ItemFlagFilter.all());
		assertEquals(2, user1InboxConversations.size());
		user1SentConversations = user1ConversationService.byFolder(user1SentBox.uid, ItemFlagFilter.all());
		assertEquals(2, user1SentConversations.size());
		assertNotEquals(conversationId, Long.parseUnsignedLong(user1SentConversations.get(1).uid, 16));

		//
		// move sent message to trash
		//
		IMailboxFolders foldersApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> trash = foldersApi.byName("Trash");
		IItemsTransfer transferApi = provider().instance(IItemsTransfer.class, user1SentBox.uid, trash.uid);
		List<ItemIdentifier> moved = transferApi.move(Arrays.asList(user1ItemId));
		assertNotNull(moved);
		user1SentConversations = user1ConversationService.byFolder(user1SentBox.uid, ItemFlagFilter.all());
		assertEquals(2, user1SentConversations.size());
		numberOfMessagesInConversation = user1SentConversations.get(0).value.messageRefs.size();
		assertEquals(1, numberOfMessagesInConversation);

		//
		// move reply message to trash
		//
		IItemsTransfer transferApi2 = provider().instance(IItemsTransfer.class, user1Inbox.uid, trash.uid);
		List<ItemIdentifier> moved2 = transferApi2.move(Arrays.asList(user1ItemId2));
		assertNotNull(moved2);
		user1InboxConversations = user1ConversationService.byFolder(user1Inbox.uid, ItemFlagFilter.all());
		assertEquals(2, user1InboxConversations.size());
		numberOfMessagesInConversation = user1InboxConversations.get(0).value.messageRefs.size();
		assertEquals(1, numberOfMessagesInConversation);

		//
		// clean up trash
		//
		provider().instance(IMailboxFolders.class, partition, mboxRoot).emptyFolder(trash.internalId);
		Thread.sleep(1000);
		TaskRef deleteExpiredTaskRef = new ReplicatedDataExpirationService(new BmTestContext(SecurityContext.SYSTEM),
				JdbcTestHelper.getInstance().getMailboxDataDataSource(), "bm/core").deleteExpired(0);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), deleteExpiredTaskRef);
		user1InboxConversations = user1ConversationService.byFolder(user1Inbox.uid, ItemFlagFilter.all());
		assertEquals(2, user1InboxConversations.size());
		numberOfMessagesInConversation = user1InboxConversations.get(0).value.messageRefs.size();
		assertEquals(1, numberOfMessagesInConversation);
	}

	/** Create a message in a synchronous way. */
	private long createEml(String emlPath, String userUid, String mboxRoot, String folderName) throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(emlPath)) {
			IServiceProvider provider = ServerSideServiceProvider.getProvider(new SecurityContext(userUid, userUid,
					Collections.<String>emptyList(), Collections.<String>emptyList(), domainUid));
			Stream stream = VertxStream.stream(Buffer.buffer(ByteStreams.toByteArray(in)));
			IMailboxFolders mailboxFolderService = provider.instance(IMailboxFolders.class, partition, mboxRoot);
			ItemValue<MailboxFolder> folder = mailboxFolderService.byName(folderName);
			IMailboxItems mailboxItemService = provider.instance(IMailboxItems.class, folder.uid);
			String partId = mailboxItemService.uploadPart(stream);
			Part fullEml = Part.create(null, "message/rfc822", partId);
			MessageBody messageBody = new MessageBody();
			messageBody.subject = "Subject_" + System.currentTimeMillis();
			messageBody.structure = fullEml;
			MailboxItem item = new MailboxItem();
			item.body = messageBody;
			IOfflineMgmt offlineMgmt = provider.instance(IOfflineMgmt.class, domainUid, userUid);
			IdRange oneId = offlineMgmt.allocateOfflineIds(1);
			long expectedId = oneId.globalCounter;
			mailboxItemService.createById(expectedId, item);
			return expectedId;
		}
	}

}
