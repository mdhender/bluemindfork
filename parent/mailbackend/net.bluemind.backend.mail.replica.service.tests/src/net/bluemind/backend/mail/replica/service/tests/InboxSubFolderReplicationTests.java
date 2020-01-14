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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vertx.java.core.buffer.Buffer;

import com.google.common.io.ByteStreams;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.ImportMailboxItemSet;
import net.bluemind.backend.mail.api.ImportMailboxItemSet.MailboxItemId;
import net.bluemind.backend.mail.api.ImportMailboxItemsStatus;
import net.bluemind.backend.mail.api.ImportMailboxItemsStatus.ImportStatus;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.backend.mail.replica.service.tests.ReplicationEventsRecorder.Hierarchy;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.mime.MimeTree;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;

public class InboxSubFolderReplicationTests extends AbstractRollingReplicationTests {

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

	protected IServiceProvider provider() {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", "sid");
	}

	@Test
	public void testCreateSubFolder() {

		String subFolderName = "INBOX/sub-folder-" + System.currentTimeMillis();
		imapAsUser(sc -> {
			sc.create(subFolderName);
			Thread.sleep(1000);
			return null;
		});

		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> allBoxes = mboxesApi.all();

		Optional<ItemValue<MailboxFolder>> inbox = allBoxes.stream().filter(f -> "INBOX".equals(f.value.name))
				.findFirst();
		assertTrue(inbox.isPresent());

		Optional<ItemValue<MailboxFolder>> subFolder = allBoxes.stream()
				.filter(f -> subFolderName.equals(f.value.fullName)).findFirst();
		assertTrue(subFolder.isPresent());

		assertEquals(inbox.get().uid, subFolder.get().value.parentUid);
		assertEquals(subFolderName, subFolder.get().value.fullName);

		long inboxCount = allBoxes.stream().filter(mailbox -> "INBOX".equals(mailbox.value.name)).count();
		assertEquals(1, inboxCount);

	}

	@Test
	public void testDeleteSubFolder() {

		String subFolderName = "INBOX/sub-folder-" + System.currentTimeMillis();
		imapAsUser(sc -> {
			sc.create(subFolderName);
			Thread.sleep(1000);
			return null;
		});

		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);

		assertNotNull(mboxesApi.byName(subFolderName));

		imapAsUser(sc -> {
			sc.deleteMailbox(subFolderName);
			Thread.sleep(1000);
			return null;
		});

		assertNull(mboxesApi.byName(subFolderName));

	}

	@Test
	public void testRenameSubFolder() {

		String subFolderName = "INBOX/sub-folder-" + System.currentTimeMillis();
		imapAsUser(sc -> {
			sc.create(subFolderName);
			Thread.sleep(1000);
			sc.rename(subFolderName, subFolderName + "-updated");
			Thread.sleep(1000);
			return null;
		});

		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		assertNull(mboxesApi.byName(subFolderName));
		assertNotNull(mboxesApi.byName(subFolderName + "-updated"));

	}

	@Test
	public void testCopyIntoSubFolder() throws IOException {
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(7);
		long offlineId = ids.globalCounter;

		String subFolderName = "INBOX/sub-folder-" + System.currentTimeMillis();
		imapAsUser(sc -> {
			sc.create(subFolderName);
			Thread.sleep(1000);
			return null;
		});

		// append mail into INBOX
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");
		long id = offlineId++;
		addDraft(inbox, id, userUid);

		long id2 = offlineId++;
		addDraft(inbox, id2, userUid);

		// copy into sub folder
		long expectedId = offlineId++;
		long expectedId2 = offlineId++;

		ImportMailboxItemSet toCopy = ImportMailboxItemSet.copyIn(inbox.internalId,
				Arrays.asList(MailboxItemId.of(id), MailboxItemId.of(id2)),
				Arrays.asList(MailboxItemId.of(expectedId), MailboxItemId.of(expectedId2)));

		ItemValue<MailboxFolder> subFolder = mboxesApi.byName(subFolderName);
		assertNotNull(subFolder);
		ImportMailboxItemsStatus ret = mboxesApi.importItems(subFolder.internalId, toCopy);

		assertEquals(ImportStatus.SUCCESS, ret.status);
		assertEquals(2, ret.doneIds.size());
		assertEquals(expectedId, ret.doneIds.get(0).destination);
		assertEquals(expectedId2, ret.doneIds.get(1).destination);

		// check
		IMailboxItems itemApi = provider().instance(IMailboxItems.class, inbox.uid);
		itemApi = provider().instance(IMailboxItems.class, subFolder.uid);
		ItemValue<MailboxItem> item = itemApi.getCompleteById(expectedId);
		assertNotNull(item);

		item = itemApi.getCompleteById(expectedId2);
		assertNotNull(item);

	}

	@Test
	public void testMailshareCopyFromSubFolderToSubFolder() throws IOException {
		IMailshare ms = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailshare.class,
				domainUid);
		String msUid = UUID.randomUUID().toString();
		Mailshare mailshare = new Mailshare();
		mailshare.emails = Arrays.asList(Email.create("ms@" + domainUid, true));
		mailshare.name = "ms";
		mailshare.routing = Routing.internal;
		ms.create(msUid, mailshare);

		IContainerManagement cs = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(msUid));
		AccessControlEntry acl = AccessControlEntry.create(userUid, Verb.Write);
		cs.setAccessControlList(Arrays.asList(acl));

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(7);
		long offlineId = ids.globalCounter;

		String src = "src" + System.currentTimeMillis();
		String dest = "dest" + System.currentTimeMillis();
		imapAsUser(sc -> {
			sc.create("Dossiers partagés/ms/" + src);
			sc.create("Dossiers partagés/ms/" + dest);
			Thread.sleep(2000);
			return null;
		});

		// append mail into src
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, "ms");
		ItemValue<MailboxFolder> srcFolder = mboxesApi.byName(src);
		long id = offlineId++;
		addDraft(srcFolder, id, msUid);

		ItemValue<MailboxFolder> destFolder = mboxesApi.byName(dest);
		// copy into sub folder
		long expectedId = offlineId++;

		ImportMailboxItemSet toCopy = ImportMailboxItemSet.copyIn(srcFolder.internalId,
				Arrays.asList(MailboxItemId.of(id)), Arrays.asList(MailboxItemId.of(expectedId)));

		ImportMailboxItemsStatus ret = mboxesApi.importItems(destFolder.internalId, toCopy);

		assertEquals(ImportStatus.SUCCESS, ret.status);
		assertEquals(1, ret.doneIds.size());
		assertEquals(expectedId, ret.doneIds.get(0).destination);

		// check
		IMailboxItems itemApi = provider().instance(IMailboxItems.class, destFolder.uid);
		ItemValue<MailboxItem> item = itemApi.getCompleteById(expectedId);
		assertNotNull(item);
	}

	@Test
	public void testMailshareCopyFromSubFolderToRootFolder() throws IOException {
		IMailshare ms = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailshare.class,
				domainUid);
		String msUid = UUID.randomUUID().toString();
		Mailshare mailshare = new Mailshare();
		mailshare.emails = Arrays.asList(Email.create("ms@" + domainUid, true));
		mailshare.name = "ms";
		mailshare.routing = Routing.internal;
		ms.create(msUid, mailshare);

		IContainerManagement cs = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(msUid));
		AccessControlEntry acl = AccessControlEntry.create(userUid, Verb.Write);
		cs.setAccessControlList(Arrays.asList(acl));

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(7);
		long offlineId = ids.globalCounter;

		String src = "src" + System.currentTimeMillis();
		imapAsUser(sc -> {
			sc.create("Dossiers partagés/ms/" + src);
			Thread.sleep(2000);
			return null;
		});

		// append mail into src
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, "ms");
		ItemValue<MailboxFolder> srcFolder = mboxesApi.byName(src);
		long id = offlineId++;
		addDraft(srcFolder, id, msUid);

		ItemValue<MailboxFolder> destFolder = mboxesApi.byName("ms");
		// copy into sub folder
		long expectedId = offlineId++;

		ImportMailboxItemSet toCopy = ImportMailboxItemSet.copyIn(srcFolder.internalId,
				Arrays.asList(MailboxItemId.of(id)), Arrays.asList(MailboxItemId.of(expectedId)));

		ImportMailboxItemsStatus ret = mboxesApi.importItems(destFolder.internalId, toCopy);

		assertEquals(ImportStatus.SUCCESS, ret.status);
		assertEquals(1, ret.doneIds.size());
		assertEquals(expectedId, ret.doneIds.get(0).destination);

		// check
		IMailboxItems itemApi = provider().instance(IMailboxItems.class, destFolder.uid);
		ItemValue<MailboxItem> item = itemApi.getCompleteById(expectedId);
		assertNotNull(item);
	}

	@Test
	public void testMailshareCopyFromRootFolderToSubFolder() throws IOException {
		IMailshare ms = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailshare.class,
				domainUid);
		String msUid = UUID.randomUUID().toString();
		Mailshare mailshare = new Mailshare();
		mailshare.emails = Arrays.asList(Email.create("ms@" + domainUid, true));
		mailshare.name = "ms";
		mailshare.routing = Routing.internal;
		ms.create(msUid, mailshare);

		IContainerManagement cs = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(msUid));
		AccessControlEntry acl = AccessControlEntry.create(userUid, Verb.Write);
		cs.setAccessControlList(Arrays.asList(acl));

		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(7);
		long offlineId = ids.globalCounter;

		String dest = "dest" + System.currentTimeMillis();
		imapAsUser(sc -> {
			sc.create("Dossiers partagés/ms/" + dest);
			Thread.sleep(2000);
			return null;
		});

		// append mail into src
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, "ms");
		ItemValue<MailboxFolder> srcFolder = mboxesApi.byName("ms");
		long id = offlineId++;
		addDraft(srcFolder, id, msUid);

		ItemValue<MailboxFolder> destFolder = mboxesApi.byName(dest);
		// copy into sub folder
		long expectedId = offlineId++;

		ImportMailboxItemSet toCopy = ImportMailboxItemSet.copyIn(srcFolder.internalId,
				Arrays.asList(MailboxItemId.of(id)), Arrays.asList(MailboxItemId.of(expectedId)));

		ImportMailboxItemsStatus ret = mboxesApi.importItems(destFolder.internalId, toCopy);

		assertEquals(ImportStatus.SUCCESS, ret.status);
		assertEquals(1, ret.doneIds.size());
		assertEquals(expectedId, ret.doneIds.get(0).destination);

		// check
		IMailboxItems itemApi = provider().instance(IMailboxItems.class, destFolder.uid);
		ItemValue<MailboxItem> item = itemApi.getCompleteById(expectedId);
		assertNotNull(item);
	}

	@Test
	public void testMoveIntoSubFolder() throws IOException {
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange ids = idAllocator.allocateOfflineIds(7);
		long offlineId = ids.globalCounter;

		String subFolderName = "INBOX/sub-folder-" + System.currentTimeMillis();
		imapAsUser(sc -> {
			sc.create(subFolderName);
			Thread.sleep(1000);
			return null;
		});

		// append mail into INBOX
		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");
		long id = offlineId++;
		addDraft(inbox, id, userUid);

		long id2 = offlineId++;
		addDraft(inbox, id2, userUid);

		// move into sub folder
		long expectedId = offlineId++;
		long expectedId2 = offlineId++;

		ImportMailboxItemSet toMove = ImportMailboxItemSet.moveIn(inbox.internalId,
				Arrays.asList(MailboxItemId.of(id), MailboxItemId.of(id2)),
				Arrays.asList(MailboxItemId.of(expectedId), MailboxItemId.of(expectedId2)));

		ItemValue<MailboxFolder> subFolder = mboxesApi.byName(subFolderName);
		assertNotNull(subFolder);
		ImportMailboxItemsStatus ret = mboxesApi.importItems(subFolder.internalId, toMove);

		assertEquals(ImportStatus.SUCCESS, ret.status);
		assertEquals(2, ret.doneIds.size());
		assertEquals(expectedId, ret.doneIds.get(0).destination);
		assertEquals(expectedId2, ret.doneIds.get(1).destination);

		// check
		IMailboxItems itemApi = provider().instance(IMailboxItems.class, inbox.uid);
		itemApi = provider().instance(IMailboxItems.class, subFolder.uid);
		ItemValue<MailboxItem> item = itemApi.getCompleteById(expectedId);
		assertNotNull(item);

		item = itemApi.getCompleteById(expectedId2);
		assertNotNull(item);

	}

	protected ItemValue<MailboxItem> addDraft(ItemValue<MailboxFolder> inbox, long id, String owner)
			throws IOException {
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

}
