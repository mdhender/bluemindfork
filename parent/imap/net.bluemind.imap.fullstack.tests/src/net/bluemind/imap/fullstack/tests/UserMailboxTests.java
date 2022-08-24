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
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.ByteStreams;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
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
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class UserMailboxTests {

	@BeforeClass
	public static void sysprop() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList("mail/imap");

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt(pipo);
		String domUid = "devenv.blue";
		PopulateHelper.addDomain(domUid, Routing.internal);
		String userUid = PopulateHelper.addUser("john", "devenv.blue", Routing.internal);
		assertNotNull(userUid);

		StateContext.setInternalState(new RunningState());
		System.err.println("==== BEFORE ====");

	}

	@After
	public void after() throws Exception {
		System.err.println("===== AFTER =====");
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void userCanConnect() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1144, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			for (int i = 0; i < 10; i++) {
				Thread.sleep(250);
				sc.noop();
			}

		}
	}

	@Test
	public void defaultFoldersExist() throws Exception {

		try (StoreClient sc = new StoreClient("127.0.0.1", 1144, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			ListResult userFolders = sc.listAll();
			assertFalse("user folders list must not be empty", userFolders.isEmpty());
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
		try (StoreClient sc = new StoreClient("127.0.0.1", 1144, "john@devenv.blue", "john")) {
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
		try (StoreClient sc = new StoreClient("127.0.0.1", 1144, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			HashingInputStream hash = new HashingInputStream(Hashing.murmur3_32(), bigEml());
			int added = sc.append("INBOX", hash, new FlagsList());
			assertTrue(added > 0);
			addUid.set(added);
			assertTrue(sc.select("INBOX"));
			HashCode sourceHash = hash.hash();

			try (IMAPByteSource fetch12 = sc.uidFetchMessage(added)) {
				assertNotNull(fetch12);
				System.err.println("Got " + fetch12.size() + " byte(s)");
				try (HashingInputStream hashAfter = new HashingInputStream(Hashing.murmur3_32(),
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
		try (StoreClient sc = new StoreClient("127.0.0.1", 1144, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			int added = sc.append("INBOX", eml("emls/sapin_inline.eml"), new FlagsList());
			assertTrue(added > 0);
			addUid.set(added);
			assertTrue(sc.select("INBOX"));
			try (IMAPByteSource fetch12 = sc.uidFetchPart(added, "1.2")) {
				assertNotNull(fetch12);
				System.err.println("Got " + fetch12.size() + " byte(s)");
				byte[] data = fetch12.source().read();
				System.err.println("data: " + data.length);
			}

			try (IMAPByteSource fetch12 = sc.uidFetchPart(added, "1.2.MIME")) {
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
		try (StoreClient sc = new StoreClient("127.0.0.1", 1144, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			int added = sc.append("INBOX", eml(), new FlagsList());
			assertTrue(added > 0);
			sc.select("INBOX");
			Collection<Integer> existing = sc.uidSearch(new SearchQuery());
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

			IMAPByteSource thePart = sc.uidFetchPart(1, "1");
			assertNotNull(thePart);

		}
	}

}
