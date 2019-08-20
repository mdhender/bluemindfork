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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.streams.ReadStream;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.DispositionType;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.ImportMailboxItemSet;
import net.bluemind.backend.mail.api.ImportMailboxItemSet.MailboxItemId;
import net.bluemind.backend.mail.api.ImportMailboxItemsStatus;
import net.bluemind.backend.mail.api.ImportMailboxItemsStatus.ImportStatus;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MailboxItem.SystemFlag;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.SeenUpdate;
import net.bluemind.backend.mail.api.utils.PartsWalker;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationAnnotations;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationArtifacts;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.backend.mail.replica.api.MailboxAnnotation;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.MailboxSub;
import net.bluemind.backend.mail.replica.api.QuotaRoot;
import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.backend.mail.replica.service.ReplicationEvents;
import net.bluemind.backend.mail.replica.service.tests.ReplicationEventsRecorder.Hierarchy;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.ContainerHierarchyNode;
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
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.sessions.Sessions;
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

public class ReplicationStackTests extends AbstractRollingReplicationTests {

	private String apiKey;
	protected String partition;
	protected String mboxRoot;

	@BeforeClass
	public static void oneShotBefore() {
		System.setProperty("es.mailspool.count", "1");
	}

	@Before
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
		CyrusPartition part = CyrusPartition.forServerAndDomain(cyrusReplication.server(), domainUid);
		this.partition = part.name;
		this.mboxRoot = "user." + userUid.replace('.', '^');

		this.apiKey = "sid";
		SecurityContext secCtx = new SecurityContext("sid", userUid, Collections.emptyList(), Collections.emptyList(),
				domainUid);
		Sessions.get().put(apiKey, secCtx);

		long delay = System.currentTimeMillis();
		Hierarchy hierarchy = null;
		do {
			Thread.sleep(400);
			hierarchy = rec.hierarchy(domainUid, userUid);
			System.out.println("Hierarchy version is " + hierarchy.exactVersion);
			if (System.currentTimeMillis() - delay > 30000) {
				throw new TimeoutException("Hierarchy init took more than 20sec");
			}
		} while (hierarchy.exactVersion < 7);
		System.out.println("Hierarchy is now at version " + hierarchy.exactVersion);
		System.err.println("before is complete, starting test.");
	}

	@After
	public void after() throws Exception {
		System.err.println("Test is over, after starts...");
		super.after();
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
		for (SystemFlag f : item.value.systemFlags) {
			System.out.println("uid " + item.value.imapUid + ", f: " + f.name());
		}

		IMailboxItems userRecordsApi = prov.instance(IMailboxItems.class, inbox.uid);
		MailboxItem updated = item.value;
		updated.systemFlags = Collections.emptyList();
		updated.otherFlags = Arrays.asList("$Junit" + System.currentTimeMillis());
		System.out.println("UPDATE STARTS...............");
		userRecordsApi.updateById(item.internalId, updated);
		int count = 2;
		long time = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			updated.otherFlags = Arrays.asList("$Roberto" + System.currentTimeMillis());
			Ack ack = userRecordsApi.updateById(item.internalId, updated);
			System.out.println("Item version is now " + ack.version);
		}
		time = System.currentTimeMillis() - time;
		System.out.println("avg per update: " + ((double) time) / count + "ms.");
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
		ItemValue<MailboxItem> item = null;
		List<SeenUpdate> toUpdate = new LinkedList<>();
		for (Long rec : allById.created) {
			item = recordsApi.getCompleteById(rec);
			SeenUpdate su = new SeenUpdate();
			su.itemId = item.internalId;
			su.seen = !item.value.systemFlags.contains(SystemFlag.seen);
			System.out
					.println("uid " + item.value.imapUid + ", f: " + item.value.systemFlags + ", su.seen: " + su.seen);
			toUpdate.add(su);
		}
		assertNotNull(item);

		IMailboxItems userRecordsApi = prov.instance(IMailboxItems.class, inbox.uid);
		System.out.println("**** Will update " + toUpdate.size() + " item(s).");
		Ack updatedVersion = userRecordsApi.updateSeens(toUpdate);
		System.out.println("Version is now " + updatedVersion.version);
	}

	protected IServiceProvider provider() {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", "sid");
	}

	@Test
	public void createDraft() throws IMAPException, InterruptedException, IOException {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");
		assertNotNull(inbox);
		addDraft(inbox);
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
			if (refetched.value.systemFlags.contains(SystemFlag.deleted)) {
				break;
			}
			Thread.sleep(200);
		} while (retry++ < 10);
		assertTrue("record was not marked as deleted", retry < 10);
	}

	protected ItemValue<MailboxItem> addDraft(ItemValue<MailboxFolder> inbox) throws IOException, InterruptedException {
		return addDraft(inbox, userUid);
	}

	protected ItemValue<MailboxItem> addDraft(ItemValue<MailboxFolder> inbox, String owner)
			throws IOException, InterruptedException {
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, owner);
		IdRange oneId = idAllocator.allocateOfflineIds(1);
		return addDraft(inbox, oneId.globalCounter, owner);
	}

	protected ItemValue<MailboxItem> addDraft(ItemValue<MailboxFolder> inbox, long id)
			throws IOException, InterruptedException {
		return addDraft(inbox, id, userUid);
	}

	protected ItemValue<MailboxItem> addDraft(ItemValue<MailboxFolder> inbox, long id, String owner)
			throws IOException, InterruptedException {
		assertNotNull(inbox);
		IMailboxItems recordsApi = provider().instance(IMailboxItems.class, inbox.uid);
		try (InputStream in = testEml()) {
			Stream forUpload = VertxStream.stream(new Buffer(ByteStreams.toByteArray(in)));
			String partId = recordsApi.uploadPart(forUpload);
			assertNotNull(partId);
			System.out.println("Got partId " + partId);
			Part fullEml = Part.create(null, "message/rfc822", partId);
			MessageBody brandNew = new MessageBody();
			brandNew.subject = "toto";
			brandNew.structure = fullEml;
			MailboxItem item = new MailboxItem();
			item.body = brandNew;
			item.otherFlags = Arrays.asList("Pouic");
			long expectedId = id;
			System.err.println("Before create by id....." + id);
			recordsApi.createById(expectedId, item);
			System.err.println("OK YEAH YEAH");
			ItemValue<MailboxItem> reloaded = recordsApi.getCompleteById(expectedId);
			assertNotNull(reloaded);
			assertNotNull(reloaded.value.body.headers);
			Optional<Header> idHeader = reloaded.value.body.headers.stream()
					.filter(h -> h.name.equals(MailApiHeaders.X_BM_INTERNAL_ID)).findAny();
			assertTrue(idHeader.isPresent());
			assertEquals(owner + "#" + InstallationId.getIdentifier() + ":" + expectedId, idHeader.get().firstValue());
			return reloaded;
		}
	}

	@Test
	public void createTextDraft() throws IMAPException, InterruptedException, IOException {
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");
		assertNotNull(inbox);
		IMailboxItems recordsApi = provider().instance(IMailboxItems.class, inbox.uid);
		Stream forUpload = VertxStream.stream(new Buffer("Coucou\r\n".getBytes()));
		String partId = recordsApi.uploadPart(forUpload);
		assertNotNull(partId);
		System.out.println("Got partId " + partId);
		MailboxItem item = MailboxItem.of("toto", Part.create(null, "text/plain", partId));
		item.otherFlags = Arrays.asList("Pouic");
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange oneId = idAllocator.allocateOfflineIds(1);
		long expectedId = oneId.globalCounter;
		recordsApi.createById(expectedId, item);
		ItemValue<MailboxItem> reloaded = recordsApi.getCompleteById(expectedId);
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
		Stream forUpload = VertxStream.stream(new Buffer("Coucou\r\n".getBytes()));
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
		assertNotNull(folderItem);
		assertTrue(folderItem.flags.contains(ItemFlag.Deleted));

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
		assertNotNull(folderItem);
		assertTrue(folderItem.flags.contains(ItemFlag.Deleted));
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
		assertNotNull(folderItem);
		assertTrue(folderItem.flags.contains(ItemFlag.Deleted));

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
		Stream partStream = recordsApi.fetch(item.value.imapUid, "3", null, null, null);
		fetchPart(partStream);
		// System.out.println("HTML?\n" + fullPartContent.toString());

	}

	private Buffer fetchPart(Stream s) throws InterruptedException {
		ReadStream<?> vertxPart = VertxStream.read(s);
		CountDownLatch cdl = new CountDownLatch(1);
		Buffer fullPartContent = new Buffer();
		Vertx vx = VertxPlatform.getVertx();
		vx.setTimer(1, tid -> {
			System.out.println("In timer..." + tid);
			vertxPart.endHandler(v -> cdl.countDown());
			vertxPart.dataHandler(b -> {
				fullPartContent.appendBuffer(b);
			});
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
			if ("Middle/Sent".equals(folder.value.fullName)) {
				found = true;
			}
		}
		assertTrue(found);
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
		Thread.sleep(500);
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

		MailboxFolder child = new MailboxFolder();
		child.fullName = "child" + System.currentTimeMillis();
		child.parentUid = root.uid;
		ItemIdentifier freshId = folders.createBasic(child);
		ItemValue<MailboxFolder> freshFolder = folders.getComplete(freshId.uid);
		assertNotNull(freshFolder);

		freshFolder.value.name = "updChild" + System.currentTimeMillis();
		Ack ack = folders.updateById(freshFolder.internalId, freshFolder.value);
		assertNotNull(ack);
		assertTrue(ack.version > freshId.version);

		System.err.println("Before delete........");
		Thread.sleep(500);
		folders.deleteById(freshFolder.internalId);
		Thread.sleep(1000);
		ItemValue<MailboxFolder> exists = folders.getCompleteById(freshFolder.internalId);
		System.err.println("Got " + exists);
		assertTrue(exists.flags.contains(ItemFlag.Deleted));

		// nested case
		child = new MailboxFolder();
		child.fullName = "reChild" + System.currentTimeMillis();
		child.parentUid = root.uid;
		freshId = folders.createBasic(child);
		freshFolder = folders.getComplete(freshId.uid);
		assertNotNull(freshFolder);

		MailboxFolder subF = new MailboxFolder();
		subF.fullName = child.fullName + "/sub" + System.currentTimeMillis();
		subF.parentUid = freshFolder.uid;
		ItemIdentifier subFolder = folders.createBasic(subF);
		assertNotNull(subFolder);

		subF.fullName = child.fullName + "/upd" + System.currentTimeMillis();
		ack = folders.updateById(subFolder.id, subF);
		assertNotNull(ack);
		assertTrue(ack.version > subFolder.version);

		System.err.println("Pre nested delete....");
		Thread.sleep(500);
		folders.deleteById(subFolder.id);
		Thread.sleep(1000);
		ItemValue<MailboxFolder> subFound = folders.getCompleteById(subFolder.id);
		assertTrue(subFound.flags.contains(ItemFlag.Deleted));
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
			Stream fetched = itemsApi.fetch(added.value.imapUid, part.address, null, null, null);
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
		List<SeenUpdate> forUpdate = refetch.stream().map(iv -> SeenUpdate.of(iv.internalId, true, true))
				.collect(Collectors.toList());
		Ack ack = itemsApi.updateSeens(forUpdate);
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

	private CountDownLatch expectMessage(String vertxAddress) {
		return expectMessages(vertxAddress, 1, msg -> true);
	}

	private CountDownLatch expectMessages(String vertxAddress, int count) {
		return expectMessages(vertxAddress, count, msg -> true);
	}

	private CountDownLatch expectMessages(String vertxAddress, int count, Predicate<JsonObject> msgFilter) {
		CountDownLatch msgLock = new CountDownLatch(count);
		AtomicReference<Handler<Message<JsonObject>>> ref = new AtomicReference<>();
		Handler<Message<JsonObject>> h = (Message<JsonObject> msg) -> {
			JsonObject payload = msg.body();
			boolean matches = msgFilter.test(payload);
			System.out.println("GOT 1 (match: " + matches + ") (still expects "
					+ (msgLock.getCount() - (matches ? 1 : 0)) + "): " + payload.encodePrettily());
			if (matches) {
				msgLock.countDown();
				if (msgLock.getCount() == 0) {
					VertxPlatform.eventBus().unregisterHandler(vertxAddress, ref.get());
				}
			}
		};
		ref.set(h);
		VertxPlatform.eventBus().registerHandler(vertxAddress, h);
		return msgLock;
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

		mboxesApi.deepDelete(foundItem.internalId);

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

		Thread.sleep(1000);

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
		System.err.println("Before: " + toRename + ", after: " + postRename);
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
		int COUNT = 50;
		IMailboxFolders foldersApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(4 * COUNT + 50);
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
		IMailboxItems itemApi = provider().instance(IMailboxItems.class, src.uid);

		long id = offlineId++;
		addDraft(src, id);

		long id2 = offlineId++;
		addDraft(src, id2);

		ItemValue<MailboxFolder> currentSrc = src;
		ItemValue<MailboxFolder> currentDest = dst;

		for (int i = 0; i < COUNT; i++) {
			System.err.println("Loop " + (i + 1) + " / " + COUNT + " with ranges " + ids);

			// move into root/src/dst
			long expectedId = offlineId++;
			long expectedId2 = offlineId++;

			ImportMailboxItemSet toMove = ImportMailboxItemSet.moveIn(currentSrc.internalId,
					Arrays.asList(MailboxItemId.of(id), MailboxItemId.of(id2)),
					Arrays.asList(MailboxItemId.of(expectedId), MailboxItemId.of(expectedId2)));

			ImportMailboxItemsStatus ret = foldersApi.importItems(currentDest.internalId, toMove);

			assertEquals(ImportStatus.SUCCESS, ret.status);
			assertEquals(2, ret.doneIds.size());

			// check
			itemApi = provider().instance(IMailboxItems.class, currentDest.uid);
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
		addDraft(src, id);

		long id2 = offlineId++;
		addDraft(src, id2);

		IMailboxItems itemApi = provider().instance(IMailboxItems.class, src.uid);

		// move into root/src/dst
		long expectedId = offlineId++;
		long expectedId2 = offlineId++;

		ImportMailboxItemSet toMove = ImportMailboxItemSet.moveIn(src.internalId,
				Arrays.asList(MailboxItemId.of(id), MailboxItemId.of(id2)),
				Arrays.asList(MailboxItemId.of(expectedId), MailboxItemId.of(expectedId2)));

		ItemValue<MailboxFolder> dst = foldersApi.byName(root + "/src/dst");
		ImportMailboxItemsStatus ret = foldersApi.importItems(dst.internalId, toMove);

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

		// check delete from source
		itemApi = provider().instance(IMailboxItems.class, src.uid);
		ItemValue<MailboxItem> deleted = itemApi.getCompleteById(id);
		System.err.println("Deleted: " + deleted);
		assertTrue(deleted.value.systemFlags.contains(SystemFlag.deleted));

		deleted = itemApi.getCompleteById(id2);
		assertTrue(deleted.value.systemFlags.contains(SystemFlag.deleted));

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
		assertTrue(deleted.value.systemFlags.contains(SystemFlag.deleted));
		deleted = itemApi.getCompleteById(id2);
		assertTrue(deleted.value.systemFlags.contains(SystemFlag.deleted));
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
	public void testApiPreservesDispositionType() throws IMAPException, InterruptedException, IOException {
		// create inbox
		final IMailboxFolders mailboxFolders = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		final ItemValue<MailboxFolder> inbox = mailboxFolders.byName("INBOX");
		assertNotNull(inbox);

		// create draft containing inline parts
		final ItemValue<MailboxItem> reloaded = addDraft(inbox);

		// check disposition type is kept ( 0:text without disposition, 1:image
		// inline,
		// 2:image inline)
		final List<Part> subParts = reloaded.value.body.structure.children;
		assertEquals(DispositionType.INLINE, subParts.get(1).dispositionType);
		assertEquals(DispositionType.INLINE, subParts.get(2).dispositionType);
		assertNull(subParts.get(0).dispositionType);

		// should not have real attachments (it is based on disposition type)
		assertFalse(reloaded.value.body.structure.hasRealAttachments());
		assertEquals(0, reloaded.value.body.structure.nonInlineAttachments().size());
	}

	@Test
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

	@Test
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
			if ("Sent".equals(folder.value.fullName)) {
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
			assertTrue(record.value.systemFlags.contains(SystemFlag.deleted));
		});

	}

}
