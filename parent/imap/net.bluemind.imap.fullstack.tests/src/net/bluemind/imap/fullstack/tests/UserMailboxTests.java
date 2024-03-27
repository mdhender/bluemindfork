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
package net.bluemind.imap.fullstack.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.ByteStreams;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPByteSource;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.IMAPHeaders;
import net.bluemind.imap.InternalDate;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.TaggedResult;
import net.bluemind.imap.mime.MimeTree;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class UserMailboxTests {

	@BeforeClass
	public static void sysprop() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
	}

	private String userUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		Assert.assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt(pipo, esServer);
		String domUid = "devenv.blue";
		PopulateHelper.addDomain(domUid, Routing.internal);

		ElasticsearchTestHelper.getInstance().beforeTest();

		this.userUid = PopulateHelper.addUser("john", "devenv.blue", Routing.internal);
		assertNotNull(userUid);

		IMailboxes mboxes = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class,
				domUid);
		ItemValue<Mailbox> asMbox = mboxes.getComplete(userUid);
		asMbox.value.quota = 50000;
		mboxes.update(userUid, asMbox.value);

		StateContext.setInternalState(new RunningState());
		System.err.println("==== BEFORE ====");

	}

	@After
	public void after() throws Exception {
		System.err.println("===== AFTER =====");
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void userCannotConnect() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "JOHN")) {
			assertFalse(sc.login());
		}
	}

	@Test
	public void userCanConnect() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			for (int i = 0; i < 10; i++) {
				Thread.sleep(250);
				sc.noop();
			}

		}
	}

	@Test
	public void defaultFoldersExist() throws Exception {

		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			ListResult userFolders = sc.listAll();
			assertFalse("user folders list must not be empty", userFolders.isEmpty());
		}
	}

	@Test
	public void outlookEmptyXlist() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			TaggedResult result = sc.tagged("""
					XLIST "" ""
					""");
			assertTrue(result.isOk());
		}
	}

	@Test
	public void outlookListPrefix() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			sc.create("Top");
			sc.create("Top/Level");
			sc.create("Top/Level/Folder");
			TaggedResult results = sc.tagged("LIST \"\" \"Top/Level/*\"");
			assertTrue(results.isOk());

			results = sc.tagged("LIST \"\" \"Top/%/%\"");
			assertTrue(results.isOk());

			results = sc.tagged("LIST \"\" \"[ur.anus]/%/%\"");
			assertTrue(results.isOk());

			results = sc.tagged("LIST \"\" \"%\"");
			assertTrue(results.isOk());

		}
	}

	private InputStream eml() {
		StringBuilder sb = new StringBuilder();
		sb.append("From: john.grubber@die-hard.net\r\n");
		sb.append("To: simon-petter.gruber@die-hard.net\r\n");
		sb.append("Subject: McLane has a machine gun\r\n\r\n");
		sb.append("Oh oh oh !\r\n");
		return new ByteArrayInputStream(sb.toString().getBytes());
	}

	private InputStream bigEml() {
		StringBuilder sb = new StringBuilder();
		sb.append("From: john.grubber@die-hard.net\r\n");
		sb.append("To: simon-petter.gruber@die-hard.net\r\n");
		sb.append("Subject: McLane has a machine gun\r\n");
		sb.append("Content-Transfer-Encoding: base64\r\n");
		sb.append("Content-Type: application/octet-stream\r\n\r\n");
		byte[] rand = new byte[5 * 1024 * 1024];
		ThreadLocalRandom.current().nextBytes(rand);
		sb.append(Base64.getMimeEncoder().encodeToString(rand));
		sb.append("THE END\r\n");
		sb.append("\r\n");
		byte[] emlData = sb.toString().getBytes();
		return new ByteArrayInputStream(emlData);
	}

	private InputStream eml(String resPath) {
		return UserMailboxTests.class.getClassLoader().getResourceAsStream(resPath);
	}

	@Test
	public void appendThenfetchInternalDate() throws Exception {
		AtomicInteger addUid = new AtomicInteger();
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			int added = sc.append("INBOX", eml(), new FlagsList());
			assertTrue(added > 0);
			addUid.set(added);
			sc.select("INBOX");
			InternalDate[] dates = sc.uidFetchInternalDate("1:*");
			assertNotNull(dates);
		}
		SecurityContext sc = new SecurityContext("abc123", "john", Collections.emptyList(), Collections.emptyList(),
				"devenv.blue");
		Sessions.get().put("abc123", sc);
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(sc);
		IMailboxFolders foldersApi = prov.instance(IMailboxFolders.class, "devenv.blue", "user.john");
		ItemValue<MailboxFolder> inbox = foldersApi.byName("INBOX");
		IMailboxItems recApi = prov.instance(IMailboxItems.class, inbox.uid);
		Stream part = recApi.fetch(addUid.get(), "1", null, null, null, null);
		String data = GenericStream.streamToString(part);
		System.err.println("data: " + data);
		assertTrue(data.contains("Oh oh"));
	}

	@Test
	public void appendBig() throws Exception {
		AtomicInteger addUid = new AtomicInteger();
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			HashingInputStream hash = new HashingInputStream(Hashing.murmur3_32_fixed(), bigEml());
			int added = sc.append("INBOX", hash, new FlagsList());
			assertTrue(added > 0);
			addUid.set(added);
			assertTrue(sc.select("INBOX"));
			HashCode sourceHash = hash.hash();

			try (IMAPByteSource fetch12 = sc.uidFetchMessage(added)) {
				assertNotNull(fetch12);
				System.err.println("Got " + fetch12.size() + " byte(s)");
				try (HashingInputStream hashAfter = new HashingInputStream(Hashing.murmur3_32_fixed(),
						fetch12.source().openBufferedStream())) {
					byte[] data = ByteStreams.toByteArray(hashAfter);
					HashCode after = hashAfter.hash();
					String full = new String(data);
					System.err.println(full.substring(0, 256));
					System.err.println(full.substring(full.length() - 256, full.length()));

					System.err.println("data: " + data.length + " before: " + sourceHash + " after: " + after);
					assertEquals(sourceHash, after);
				}
			}
			FlagsList del = new FlagsList();
			del.add(Flag.DELETED);
			boolean done = sc.uidStore(Collections.singleton(added), del, true);
			assertTrue(done);
			sc.uidExpunge(Collections.singleton(added));
		}

	}

	@Test
	public void appendInline() throws Exception {
		AtomicInteger addUid = new AtomicInteger();
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			int added = sc.append("INBOX", eml("emls/sapin_inline.eml"), new FlagsList());
			assertTrue(added > 0);
			addUid.set(added);
			assertTrue(sc.select("INBOX"));
			try (IMAPByteSource fetch12 = sc.uidFetchPart(added, "1.2", null)) {
				assertNotNull(fetch12);
				System.err.println("Got " + fetch12.size() + " byte(s)");
				byte[] data = fetch12.source().read();
				System.err.println("data: " + data.length);
			}

			try (IMAPByteSource fetch12 = sc.uidFetchPart(added, "1.2.MIME", null)) {
				assertNotNull(fetch12);
				System.err.println("Got " + fetch12.size() + " byte(s)");
				byte[] data = fetch12.source().read();
				System.err.println("data: " + new String(data));
			}
		}
		SecurityContext sc = new SecurityContext("abc123", "john", Collections.emptyList(), Collections.emptyList(),
				"devenv.blue");
		Sessions.get().put("abc123", sc);
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(sc);
		IMailboxFolders foldersApi = prov.instance(IMailboxFolders.class, "devenv.blue", "user.john");
		ItemValue<MailboxFolder> inbox = foldersApi.byName("INBOX");
		IMailboxItems recApi = prov.instance(IMailboxItems.class, inbox.uid);
		Stream part11 = recApi.fetch(addUid.get(), "1.1", null, null, null, null);
		String data11 = GenericStream.streamToString(part11);
		System.err.println("data1.1: " + data11);

		Stream part12 = recApi.fetch(addUid.get(), "1.2", null, null, null, null);
		byte[] data12 = GenericStream.streamToBytes(part12);
		assertTrue(data12.length > 0);
		System.err.println("part 1.2 returned " + data12.length + " bytes");

	}

	@Test
	public void testUidSearchThenBS() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			int added = sc.append("INBOX", eml(), new FlagsList());
			assertTrue(added > 0);
			sc.select("INBOX");
			Collection<Integer> existing = sc.uidSearch(new SearchQuery());
			assertFalse(existing.isEmpty());
			Collection<MimeTree> structs = sc.uidFetchBodyStructure(existing);
			assertFalse(structs.isEmpty());
			MimeTree mt = structs.iterator().next();
			System.err.println(mt);
			Collection<IMAPHeaders> allHeads = sc.uidFetchHeaders(existing, "From", "To", "Cc", "Subject");
			IMAPHeaders heads = allHeads.iterator().next();
			System.err.println(heads.getRawHeaders());

			TaggedResult rcStyle = sc.tagged(
					"uid fetch 1 (uid rfc822.size flags internaldate bodystructure body.peek[header.fields (date from to subject content-type cc reply-to list-post disposition-notification-to x-priority x-bm-event x-bm-event-countered x-bm-resourcebooking x-bm-rsvp x-bm-folderuid x-bm-foldertype x-asterisk-callerid)])");
			assertTrue(rcStyle.isOk());

			TaggedResult rcStyleHead = sc.tagged("uid fetch 1 (uid body.peek[header])");
			assertTrue(rcStyleHead.isOk());

			IMAPByteSource thePart = sc.uidFetchPart(1, "1", null);
			assertNotNull(thePart);

		}
	}

	@Test
	public void testStoreNoParens() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			int added = sc.append("INBOX", eml(), new FlagsList());
			assertTrue(added > 0);
			sc.select("INBOX");
			TaggedResult resUidStore = sc.tagged("uid store " + added + " +flags \\seen");
			assertTrue(resUidStore.isOk());
			for (String s : resUidStore.getOutput()) {
				System.err.println("S: " + s);
			}

			TaggedResult resStore = sc.tagged("store 1 -flags \\seen");
			assertTrue(resStore.isOk());

			TaggedResult deleteBySeq = sc.tagged("store 1 +flags \\deleted");
			assertTrue(deleteBySeq.isOk());
			boolean fetched = false;
			for (String s : deleteBySeq.getOutput()) {
				System.err.println("S: " + s);
				fetched |= s.contains("FETCH");
			}
			assertTrue("deleted message should be fetched by non-silent store", fetched);

		}
	}

	@Test
	public void testDeletionsAreFetched() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			int one = sc.append("INBOX", eml(), new FlagsList());
			assertTrue(one > 0);
			int two = sc.append("INBOX", eml(), new FlagsList());
			assertTrue(two > 0);
			int three = sc.append("INBOX", eml(), new FlagsList());
			assertTrue(three > 0);

			sc.select("INBOX");
			Collection<FlagsList> full = sc.uidFetchFlags("1:*");
			assertEquals(3, full.size());

			FlagsList delFlag = FlagsList.of(Flag.DELETED, Flag.SEEN);
			boolean res = sc.uidStore("" + two, delFlag, true);
			assertTrue(res);

			Collection<FlagsList> withDel = sc.uidFetchFlags("1:*");
			assertEquals(withDel.size(), full.size());
			for (FlagsList fl : withDel) {
				System.err.println("fl: " + fl);
			}

			Collection<Integer> del = sc.uidSearchDeleted();
			assertEquals(1, del.size());

			SearchQuery sq = new SearchQuery();
			sq.setNotDeleted(false);
			Collection<Integer> basicSearch = sc.uidSearch(sq);
			assertEquals(3, basicSearch.size());
		}
	}

	@Test
	public void testDeletingFolderUpdatesQuota() throws IMAPException {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailboxes mboxApi = prov.instance(IMailboxes.class, "devenv.blue");
		MailboxQuota startQuota = mboxApi.getMailboxQuota(userUid);
		System.err.println("start at " + startQuota);
		ESearchActivator.refreshIndex("mailspool_*");
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			String fn = "big.folder." + System.nanoTime();
			assertTrue(sc.create(fn));
			FlagsList fl = new FlagsList();
			int added = sc.append(fn, bigEml(), fl);
			assertTrue(added > 0);

			ESearchActivator.refreshIndex("mailspool_*");

			MailboxQuota postAppend = mboxApi.getMailboxQuota(userUid);
			System.err.println("append at " + postAppend);
			assertTrue(postAppend.used > startQuota.used);

			assertTrue(sc.deleteMailbox(fn).isOk());

			ESearchActivator.refreshIndex("mailspool_*");

			MailboxQuota postDel = mboxApi.getMailboxQuota(userUid);
			System.err.println("afterDel at " + postDel);

			assertTrue(postDel.used < postAppend.used);

		}

	}

	@Test
	public void testEmptyTrashUpdatesQuota() throws IMAPException {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailboxes mboxApi = prov.instance(IMailboxes.class, "devenv.blue");
		MailboxQuota startQuota = mboxApi.getMailboxQuota(userUid);
		System.err.println("start at " + startQuota);
		ESearchActivator.refreshIndex("mailspool_*");
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			String fn = "Trash/big.folder." + System.nanoTime();
			assertTrue(sc.create(fn));
			FlagsList fl = new FlagsList();
			int added = sc.append(fn, bigEml(), fl);
			assertTrue(added > 0);

			ESearchActivator.refreshIndex("mailspool_*");

			MailboxQuota postAppend = mboxApi.getMailboxQuota(userUid);
			System.err.println("append at " + postAppend);
			assertTrue(postAppend.used > startQuota.used);

			// assertTrue(sc.deleteMailbox(fn).isOk());
			String subtree = IMailReplicaUids.subtreeUid("devenv.blue", Type.user, userUid);
			IMailboxFoldersByContainer folderApi = prov.instance(IMailboxFoldersByContainer.class, subtree);
			ItemValue<MailboxFolder> toEmpty = folderApi.byName("Trash");
			assertNotNull(toEmpty);
			folderApi.emptyFolder(toEmpty.internalId);

			ESearchActivator.refreshIndex("mailspool_*");

			MailboxQuota postDel = mboxApi.getMailboxQuota(userUid);
			System.err.println("afterDel at " + postDel);

			assertTrue(postDel.used < postAppend.used);

		}

	}

	@Test
	public void testSearchUnseenVersusUidsearchUnseen() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			String fn = "unseen" + System.nanoTime();
			sc.create(fn);
			assertTrue(sc.select(fn));
			FlagsList fl = new FlagsList();
			int one = sc.append(fn, eml(), fl);
			assertEquals(1, one);
			int two = sc.append(fn, eml(), fl);
			assertEquals(2, two);
			int three = sc.append(fn, eml(), fl);
			assertEquals(3, three);

			FlagsList del = new FlagsList();
			del.add(Flag.DELETED);
			sc.uidStore("2", del, true);
			sc.expunge();

			TaggedResult unseenRes = sc.tagged("search unseen");
			String searchUnseen = Arrays.stream(unseenRes.getOutput()).filter(s -> s.startsWith("* SEARCH")).findFirst()
					.orElseThrow();
			System.err.println("searchUnseen: " + searchUnseen);
			assertEquals("* SEARCH 1 2", searchUnseen);

			TaggedResult uidUnseenRes = sc.tagged("uid search unseen");
			String searchUidUnseen = Arrays.stream(uidUnseenRes.getOutput()).filter(s -> s.startsWith("* SEARCH"))
					.findFirst().orElseThrow();
			System.err.println("uidUnseen: " + searchUidUnseen);
			assertEquals("* SEARCH 1 3", searchUidUnseen);
		}
	}

	@Test
	public void testFlagUpdatesExtraWork() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			int added = sc.append("INBOX", eml(), new FlagsList());
			assertTrue(added > 0);
			int addedTwo = sc.append("INBOX", eml(), new FlagsList());
			assertTrue(addedTwo > 0);
			int addedThree = sc.append("INBOX", eml(), new FlagsList());
			assertTrue(addedThree > 0);
			sc.select("INBOX");

			int limit = 2500;
			long time = System.nanoTime();
			for (int i = 0; i < limit; i++) {
				TaggedResult resUidStore = sc.tagged("uid store " + added + " +flags \\seen");
				assertTrue(resUidStore.isOk());
				TaggedResult resUidStore2 = sc.tagged("uid store " + addedTwo + " +flags \\seen");
				assertTrue(resUidStore2.isOk());
				TaggedResult resUidStore3 = sc.tagged("uid store " + addedThree + " +flags \\seen");
				assertTrue(resUidStore3.isOk());
				TaggedResult resStore = sc.tagged("store 1,2,3 -flags \\seen");
				assertTrue(resStore.isOk());
				if (i % 100 == 0) {
					System.err.println((i + 1) + " / " + limit);
				}
			}
			Duration spent = Duration.ofNanos(System.nanoTime() - time);
			int changed = limit * 6;
			long secs = spent.toSeconds();
			System.err.println(
					"Spent " + spent.toSeconds() + "s for " + changed + " updates " + (changed / secs) + " per sec.");
		}
	}

}
