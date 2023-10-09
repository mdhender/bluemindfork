/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class UidSearchTests {

	private static final int PORT = 1143;
	private static final String TOPIC_ES_INDEXING_COUNT = "es.indexing.count";

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP + "," + PopulateHelper.FAKE_CYRUS_IP_2);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP + "," + PopulateHelper.FAKE_CYRUS_IP_2);
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@AfterClass
	public static void afterClass() {
		System.clearProperty("node.local.ipaddr");
		System.clearProperty("imap.local.ipaddr");
		System.clearProperty("ahcnode.fail.https.ok");
	}

	private String domainUid;
	private String loginUid;
	private AtomicInteger indexedCount;

	@Before
	public void setUp() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(pipo, esServer);
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);
		ElasticsearchTestHelper.getInstance().beforeTest();

		domainUid = "test.devenv";
		loginUid = "user" + System.currentTimeMillis();
		PopulateHelper.addDomain(domainUid);
		PopulateHelper.addUser(loginUid, domainUid);

		indexedCount = new AtomicInteger();
		VertxPlatform.eventBus().consumer(TOPIC_ES_INDEXING_COUNT, msg -> {
			indexedCount.addAndGet(1);
		});
	}

	@After
	public void tearDown() throws Exception {
		System.err.println("===== AFTER starts =====");
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
		System.err.println("===== AFTER ends =====");
	}

	@Test
	public void testGetSeen() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setSeen(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(10, uids.size());
			assertTrue(uids.contains(11));
		}
	}

	@Test
	public void testGetAll() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setAll(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(32, uids.size());
		}
	}

	@Test
	public void testGetNotSeen() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotSeen(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(22, uids.size());
			assertTrue(uids.contains(16));
			assertTrue(uids.contains(37));
		}
	}

	@Test
	public void testGetUnSeen() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setUnseenOnly(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(12 + 8, uids.size());
			assertTrue(uids.contains(16));
			assertTrue(uids.contains(35));
		}
	}

	@Test
	public void testBadKeyword() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setRawCommand("SEEN FLAGGED TOTO");
			sq.setUnseenOnly(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(0, uids.size());
		}
	}

	@Test
	public void testAllUnseenUndeleted() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setRawCommand("ALL UNSEEN UNDELETED");
			sq.setUnseenOnly(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(22, uids.size());
			assertTrue(uids.contains(16));
			assertTrue(uids.contains(37));
		}
	}

	@Test
	public void testGetMalformedSequence() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setRawCommand("99:*$");
			sq.setUnseenOnly(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(0, uids.size());
		}
	}

	// NOT UNSEEN means SEEN messages
	@Test
	public void testGetNotUnSeen() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotUnseen(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(10, uids.size());
			assertTrue(uids.contains(6));
		}
	}

	@Test
	public void testGetAnswered() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setAnswered(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(12, uids.size());
			assertTrue(uids.contains(16));
		}
	}

	@Test
	public void testGetNotAnswered() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotAnswered(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(20, uids.size());
			assertTrue(uids.contains(6));
			assertTrue(uids.contains(37));
		}
	}

	@Test
	public void testGetUnAnswered() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setUnanswered(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(20, uids.size());
			assertTrue(uids.contains(6));
			assertTrue(uids.contains(15));
			assertTrue(uids.contains(28));
			assertTrue(uids.contains(37));
		}
	}

	// NOT UNSANSWERED means ANSWERED messages
	@Test
	public void testGetNotUnAnswered() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotUnanswered(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(12, uids.size());
			assertTrue(uids.contains(16));
		}
	}

	@Test
	public void testGetDraft() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setDraft(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(10, uids.size());
			assertTrue(uids.contains(28));
		}
	}

	@Test
	public void testGetNotDraft() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotDraft(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(22, uids.size());
			assertTrue(uids.contains(6));
			assertTrue(uids.contains(10));
		}
	}

	@Test
	public void testGetUnDraft() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setUndraft(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(22, uids.size());
			assertTrue(uids.contains(6));
			assertTrue(uids.contains(11));
		}
	}

	@Test
	public void testGetNotUnDraft() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotUndraft(true);
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(10, uids.size());
			assertTrue(uids.contains(28));
		}
	}

	@Test
	public void testGetLarger() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setLarger(1000);
			sc.select("INBOX");
			setupBoxContentWithSizes(12, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(8, uids.size());
			assertTrue(uids.contains(13));
		}
	}

	@Test
	public void testGetNotLarger() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotLarger(1000);
			sc.select("INBOX");
			setupBoxContentWithSizes(12, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(12, uids.size());
			assertTrue(uids.contains(1));
		}
	}

	@Test
	public void testGetSmaller() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setSmaller(1000);
			sc.select("INBOX");
			setupBoxContentWithSizes(12, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(12, uids.size());
			assertTrue(uids.contains(12));
		}
	}

	@Test
	public void testGetNotSmaller() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotSmaller(1000);
			sc.select("INBOX");
			setupBoxContentWithSizes(12, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(8, uids.size());
			assertTrue(uids.contains(13));
		}
	}

	@Test
	public void testGetBefore() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setBefore(Date.from(Instant.now().minus(10, ChronoUnit.DAYS)));
			sc.select("INBOX");
			setupBoxContentWithDates(0, 10, 5, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(15, uids.size());
			assertTrue(uids.contains(8));
		}
	}

	@Test
	public void testGetNotBefore() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotBefore(Date.from(Instant.now().minus(10, ChronoUnit.DAYS)));
			sc.select("INBOX");
			setupBoxContentWithDates(0, 10, 5, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(10, uids.size());
			assertTrue(uids.contains(16));
			assertTrue(uids.contains(25));
		}
	}

	@Test
	public void testGetSentBefore() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setSentBefore(Date.from(Instant.now().minus(15, ChronoUnit.DAYS)));
			sc.select("INBOX");
			setupBoxContentWithDates(0, 10, 5, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(10, uids.size());
			assertTrue(uids.contains(4));
		}
	}

	@Test
	public void testGetNotSentBefore() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotSentBefore(Date.from(Instant.now().minus(15, ChronoUnit.DAYS)));
			sc.select("INBOX");
			setupBoxContentWithDates(0, 10, 5, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(15, uids.size());
			assertTrue(uids.contains(16));
		}
	}

	@Test
	public void testGetSentSince() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setSentSince(Date.from(Instant.now().minus(15, ChronoUnit.DAYS)));
			sc.select("INBOX");
			setupBoxContentWithDates(0, 10, 5, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(15, uids.size());
			assertTrue(uids.contains(11));
		}
	}

	@Test
	public void testGetNotSentSince() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotSentSince(Date.from(Instant.now().minus(15, ChronoUnit.DAYS)));
			sc.select("INBOX");
			setupBoxContentWithDates(0, 10, 5, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(10, uids.size());
			assertTrue(uids.contains(4));
		}
	}

	@Test
	public void testGetSentOn() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setSentOn(Date.from(Instant.now().minus(15, ChronoUnit.DAYS)));
			sc.select("INBOX");
			setupBoxContentWithDates(0, 10, 5, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(5, uids.size());
			assertTrue(uids.contains(11));
		}

	}

	@Test
	public void testGetNotSentOn() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotSentOn(Date.from(Instant.now().minus(15, ChronoUnit.DAYS)));
			sc.select("INBOX");
			setupBoxContentWithDates(0, 10, 5, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(20, uids.size());
			assertTrue(uids.contains(1));
			assertTrue(uids.contains(16));
		}
	}

	@Test
	public void testGetOn() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setOn(Date.from(Instant.now().minus(10, ChronoUnit.DAYS)));
			sc.select("INBOX");
			setupBoxContentWithDates(0, 10, 5, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(10, uids.size());
			assertTrue(uids.contains(16));
		}
	}

	@Test
	public void testGetNotOn() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotOn(Date.from(Instant.now().minus(15, ChronoUnit.DAYS)));
			sc.select("INBOX");
			setupBoxContentWithDates(0, 10, 5, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(20, uids.size());
			assertTrue(uids.contains(1));
			assertTrue(uids.contains(10));
		}
	}

	@Test
	public void testGetAfter() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setAfter(Date.from(Instant.now().minus(15, ChronoUnit.DAYS)));
			sc.select("INBOX");
			setupBoxContentWithDates(0, 10, 5, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(15, uids.size());
			assertTrue(uids.contains(11));
			assertTrue(uids.contains(22));
		}
	}

	@Test
	public void testGetNotAfter() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotAfter(Date.from(Instant.now().minus(10, ChronoUnit.DAYS)));
			sc.select("INBOX");
			setupBoxContentWithDates(0, 10, 5, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(15, uids.size());
			assertTrue(uids.contains(3));
		}
	}

	@Test
	public void testGetToSylvain() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setTo("<sylvain@zz.com>");
			sc.select("INBOX");
			setupBoxContentWithPeople(3, 4, 5);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(3, uids.size());
			assertTrue(uids.contains(1));
			assertTrue(uids.contains(2));
		}
	}

	@Test
	public void testGetToSYLVAIN() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setTo("SYLVAIN BLUEMIND");
			sc.select("INBOX");
			setupBoxContentWithPeople(4, 6, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(4, uids.size());
			assertTrue(uids.contains(1));
			assertTrue(uids.contains(4));
		}
	}

	@Test
	public void testGetNotToSylvain() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotTo("Sylvain Bluemind");
			sc.select("INBOX");
			setupBoxContentWithPeople(4, 6, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(14, uids.size());
			assertTrue(uids.contains(5));
		}
	}

	@Test
	public void testGetCcSylvain() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setCc("Sylvain Bluemind");
			sc.select("INBOX");
			setupBoxContentWithPeople(4, 6, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(6, uids.size());
			assertTrue(uids.contains(5));
			assertTrue(uids.contains(10));
		}
	}

	@Test
	public void testGetNotCcSylvain() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotCc("<sylvain@zz.com>");
			sc.select("INBOX");
			setupBoxContentWithPeople(4, 6, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(12, uids.size());
			assertTrue(uids.contains(1));
		}
	}

	@Test
	public void testGetCcSYLVAIN() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setCc("SYLVAIN BLUEMIND");
			sc.select("INBOX");
			setupBoxContentWithPeople(4, 6, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(6, uids.size());
			assertTrue(uids.contains(5));
		}
	}

	@Test
	public void testGetFromThomas() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setFrom("<thomas@zz.com>");
			sc.select("INBOX");
			setupBoxContentWithPeople(4, 6, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(10, uids.size());
			assertTrue(uids.contains(1));
			assertTrue(uids.contains(4));
		}
	}

	@Test
	public void testGetFromTHOMAS() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setFrom("THOMAS CATALDO");
			sc.select("INBOX");
			setupBoxContentWithPeople(4, 6, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(10, uids.size());
			assertTrue(uids.contains(1));
			assertTrue(uids.contains(5));
		}
	}

	@Test
	public void testGetNotFromThomas() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotFrom("Thomas Cataldo");
			sc.select("INBOX");
			setupBoxContentWithPeople(4, 6, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(8, uids.size());
			assertTrue(uids.contains(11));
		}
	}

	@Test
	public void testGetCcDavid() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setCc("David Bluemind");
			sc.select("INBOX");
			setupBoxContentWithPeople(4, 6, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(4, uids.size());
			assertTrue(uids.contains(1));
		}
	}

	@Test
	public void testGetNotCcDavid() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotCc("David");
			sc.select("INBOX");
			setupBoxContentWithPeople(4, 6, 8);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(14, uids.size());
			assertTrue(uids.contains(5));
		}
	}

	@Test
	public void testGetSubjectContainsToto() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setSubject("un premier sujet toto");
			sc.select("INBOX");
			setupBoxContentWithSubjects(4, 5, 6);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(4, uids.size());
			assertTrue(uids.contains(2));
		}
	}

	@Test
	public void testGetSubjectContainsTOTO() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setSubject("UN PREMIER SUJET TOTO");
			sc.select("INBOX");
			setupBoxContentWithSubjects(4, 5, 6);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(4, uids.size());
			assertTrue(uids.contains(2));
		}
	}

	@Test
	public void testGetSubjectContainsTiti() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setSubject("un premier sujet titi");
			sc.select("INBOX");
			setupBoxContentWithSubjects(4, 5, 6);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(6, uids.size());
		}
	}

	@Test
	public void testGetSubjectNotContainsToto() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotSubject("un premier sujet toto");
			sc.select("INBOX");
			setupBoxContentWithSubjects(4, 5, 6);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(11, uids.size());
			assertTrue(uids.contains(5));
			assertTrue(uids.contains(10));
		}
	}

	@Test
	public void testGetSubjectContainsTotoTutu() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setSubject("un premier sujet");
			sc.select("INBOX");
			setupBoxContentWithSubjects(4, 6, 5);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(15, uids.size());
		}
	}

	@Test
	public void testGetBodyContainsSentence() throws IMAPException, IOException, InterruptedException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setBody("this is message about my");
			sc.select("INBOX");
			setupBoxContentWithBodies(4, 5);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(9, uids.size());
		}
	}

	@Test
	public void testGetBodyContainsSentenceAmount200() throws IMAPException, IOException, InterruptedException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setBody("this is message about my 200");
			sc.select("INBOX");
			setupBoxContentWithBodies(4, 6);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(4, uids.size());
			assertTrue(uids.contains(1));
		}
	}

	@Test
	public void testGetBodyContainsSentenceAmount200Uppercase()
			throws IMAPException, IOException, InterruptedException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setBody("THIS IS MESSAGE ABOUT MY 200");
			sc.select("INBOX");
			setupBoxContentWithBodies(4, 6);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(4, uids.size());
			assertTrue(uids.contains(1));
		}
	}

	@Test
	public void testGetTextContainsSentenceAmount200() throws IMAPException, IOException, InterruptedException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setText("this is message about my 200");
			sc.select("INBOX");
			setupBoxContentWithBodies(5, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(5, uids.size());
			assertTrue(uids.contains(5));
		}
	}

	@Test
	public void testGetBodyNotContainsSentenceAmount500() throws IMAPException, IOException, InterruptedException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotBody("this is message about my 500");
			sc.select("INBOX");
			setupBoxContentWithBodies(4, 6);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(4, uids.size());
			assertTrue(uids.contains(1));
		}
	}

	@Test
	public void testGetTextNotContainsSentence() throws IMAPException, IOException, InterruptedException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotText("this is another kind");
			sc.select("INBOX");
			setupBoxContentWithBodies(5, 6);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(11, uids.size());
		}
	}

	@Test
	public void testGetTextNotContainsSentenceAmount500() throws IMAPException, IOException, InterruptedException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotText("this is message about my 500");
			sc.select("INBOX");
			setupBoxContentWithBodies(4, 6);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(4, uids.size());
			assertTrue(uids.contains(1));
		}
	}

	@Test
	public void testGetHeaderXBMExternalID() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.headerMatch("X-BM-ExternalID", "B9EE016A-9988-45AB-AA39-46B79C459D6B");
			sc.select("INBOX");
			setupBoxContentWithHeaders(2, 3, 4);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(2, uids.size());
			assertTrue(uids.contains(1));
		}
	}

	@Test
	public void testGetHeaderXBMQuality() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.headerMatch("X-BM-Quality", "test");
			sc.select("INBOX");
			setupBoxContentWithHeaders(4, 6, 5);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(5, uids.size());
			assertTrue(uids.contains(13));
		}
	}

	@Test
	public void testGetHeaderEmptyBody() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.headerMatch("X-BM-Quality", "");
			sc.select("INBOX");
			setupBoxContentWithHeaders(2, 3, 4);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(4, uids.size());
			assertTrue(uids.contains(6));
			assertTrue(uids.contains(7));
			assertTrue(uids.contains(8));
			assertTrue(uids.contains(9));
		}
	}

	@Test
	public void testGetHeaderEmptyBodyOther() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.headerMatch("X-BM-Quality", "");
			sc.select("INBOX");
			setupBoxContentWithHeaders(4, 6, 5);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(5, uids.size());
			assertTrue(uids.contains(12));
		}
	}

	@Test
	public void testGetNotHeaderXBMExternalIDEmpty() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.headerNotMatch("X-BM-ExternalID", "");
			sc.select("INBOX");
			setupBoxContentWithHeaders(2, 3, 4);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(4, uids.size());
			assertTrue(uids.contains(8));
		}
	}

	@Test
	public void testGetUidSeqUnbounded() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setUidSeq("30:*");
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(8, uids.size());
			assertTrue(uids.contains(31));
			assertFalse(uids.contains(5));
		}
	}

	@Test
	public void testGetOrSeenDraft() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setOr("SEEN", "FLAGGED");
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(10, uids.size());
			assertTrue(uids.contains(6));
			assertTrue(uids.contains(7));
			assertTrue(uids.contains(8));
			assertTrue(uids.contains(9));
			assertTrue(uids.contains(10));
			assertTrue(uids.contains(11));
			assertTrue(uids.contains(12));
			assertTrue(uids.contains(13));
			assertTrue(uids.contains(14));
			assertTrue(uids.contains(15));
		}
	}

	@Test
	public void testGetOrSeenFlaggedUidSeq() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setRawCommand("NOT DELETED OR SEEN FLAGGED UID 2:*");
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(10, uids.size());
			assertTrue(uids.contains(6));
			assertTrue(uids.contains(7));
			assertTrue(uids.contains(8));
			assertTrue(uids.contains(9));
			assertTrue(uids.contains(10));
			assertTrue(uids.contains(11));
			assertTrue(uids.contains(12));
			assertTrue(uids.contains(13));
			assertTrue(uids.contains(14));
			assertTrue(uids.contains(15));
		}
	}

	@Test
	public void testGetOrHeaderTotoHeaderXBMExternalID() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setOr("HEADER X-BM-ExternalID \"B9EE016A-9988-45AB-AA39-46B79C459D6B\"",
					"HEADER X-BM-ExternalID \"C0FF127B-9988-45AB-AA39-46B79C459D6B\"");
			sc.select("INBOX");
			setupBoxContentWithHeaders(4, 5, 6);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(9, uids.size());
			assertTrue(uids.contains(2));
			assertTrue(uids.contains(5));
		}
	}

	@Test
	public void testGetUidSequence() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setUidSeq("1,3:4,5,20:*");
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(18, uids.size());
			assertFalse(uids.contains(1));
			assertFalse(uids.contains(2));
			assertFalse(uids.contains(3));
			assertFalse(uids.contains(4));
			assertFalse(uids.contains(5));
			assertFalse(uids.contains(10));
			assertFalse(uids.contains(11));
			assertFalse(uids.contains(19));
			assertTrue(uids.contains(20));
		}
	}

	@Test
	public void testGetNotUidSequence() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotUidSeq("1,3:4,5,8:*");
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(2, uids.size());
			assertTrue(uids.contains(6));
			assertTrue(uids.contains(7));
		}
	}

	@Test
	public void testGetUidSequenceOtherTry() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setUidSeq("*:35,4:7");
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(5, uids.size());
			assertFalse(uids.contains(4));
			assertFalse(uids.contains(5));
			assertTrue(uids.contains(6));
			assertTrue(uids.contains(7));
			assertFalse(uids.contains(8));
			assertTrue(uids.contains(35));
			assertTrue(uids.contains(36));
			assertTrue(uids.contains(37));
		}
	}

	@Test
	public void testGetUidSequenceKeepGreatestUid() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setUidSeq("999:*");
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			List<Integer> uids = (List<Integer>) sc.uidSearch(sq);
			assertEquals(1, uids.size());
			assertEquals(37, (int) uids.get(0));
		}
	}

	@Test
	public void testGetUidSequenceKeepGreatestUidOther() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setUidSeq("99:100");
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			List<Integer> uids = (List<Integer>) sc.uidSearch(sq);
			assertEquals(1, uids.size());
			assertEquals(37, (int) uids.get(0));
		}
	}

	@Test
	public void testGetUidSeq() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setUidSeq("5:10");
			sc.select("INBOX");
			setupBoxContent(5, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(5, uids.size());
			assertTrue(uids.contains(6));
			assertTrue(uids.contains(10));
		}
	}

	@Test
	public void testGetUidSeqUpsideDown() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setUidSeq("10:5");
			sc.select("INBOX");
			setupBoxContent(4, 10, 12, 10);
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(6, uids.size());
			assertTrue(uids.contains(5));
			assertTrue(uids.contains(10));
		}
	}

	@Ignore("Must modify code to allow custom flags to be taken into account for StoreClient")
	@Test
	public void testGetKeyword() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setKeyword("Bmarchived");
			sc.select("INBOX");
			IMAPByteSource ibs = getUtf8Rfc822Message();
			for (int i = 0; i < 2; i++) {
				sc.append("INBOX", ibs.source().openStream(), new FlagsList());
			}
			for (int i = 0; i < 3; i++) {
				sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList(Flag.SEEN.toString())));
			}
			for (int i = 0; i < 4; i++) {
				sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList(Flag.BMARCHIVED.toString())),
						Date.from(Instant.now().minus(700, ChronoUnit.DAYS)));
			}
			ibs.close();
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(4, uids.size());
			assertTrue(uids.contains(8));
		}
	}

	@Ignore("Must modify code to allow custom flags to be taken into account for StoreClient")
	@Test
	public void testGetNotKeyword() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotKeyword("Toto");
			sc.select("INBOX");
			IMAPByteSource ibs = getUtf8Rfc822Message();
			for (int i = 0; i < 3; i++) {
				sc.append("INBOX", ibs.source().openStream(), new FlagsList());
			}
			for (int i = 0; i < 4; i++) {
				sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList(Flag.SEEN.toString())));
			}
			for (int i = 0; i < 5; i++) {
				sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList("Toto")),
						Date.from(Instant.now().minus(700, ChronoUnit.DAYS)));
			}
			ibs.close();
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(7, uids.size());
			assertTrue(uids.contains(1));
			assertTrue(uids.contains(6));
		}
	}

	@Ignore("Must modify code to allow custom flags to be taken into account for StoreClient")
	@Test
	public void testGetUnkeyword() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setUnkeyword("Toto");
			sc.select("INBOX");
			IMAPByteSource ibs = getUtf8Rfc822Message();
			for (int i = 0; i < 3; i++) {
				sc.append("INBOX", ibs.source().openStream(), new FlagsList());
			}
			for (int i = 0; i < 4; i++) {
				sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList(Flag.SEEN.toString())));
			}
			for (int i = 0; i < 5; i++) {
				sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList("Toto")),
						Date.from(Instant.now().minus(700, ChronoUnit.DAYS)));
			}
			ibs.close();
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(7, uids.size());
			assertTrue(uids.contains(1));
		}
	}

	@Ignore("Must modify code to allow custom flags to be taken into account for StoreClient")
	@Test
	public void testGetNotUnkeyword() throws IMAPException, InterruptedException, IOException {
		try (StoreClient sc = newStore(false)) {
			SearchQuery sq = new SearchQuery();
			sq.setNotUnKeyword("Toto");
			sc.select("INBOX");
			IMAPByteSource ibs = getUtf8Rfc822Message();
			for (int i = 0; i < 3; i++) {
				sc.append("INBOX", ibs.source().openStream(), new FlagsList());
			}
			for (int i = 0; i < 4; i++) {
				sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList(Flag.SEEN.toString())));
			}
			for (int i = 0; i < 5; i++) {
				sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList("Toto")),
						Date.from(Instant.now().minus(700, ChronoUnit.DAYS)));
			}
			ibs.close();
			Collection<Integer> uids = sc.uidSearch(sq);
			assertEquals(5, uids.size());
			assertTrue(uids.contains(8));
		}
	}

	@Test
	public void appendThenCustomFlag() throws Exception {
		try (StoreClient sc = newStore(false)) {

			List<Integer> addedMails = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				int added = sc.append("INBOX", new ByteArrayInputStream("From: gg@gmail.com\r\n".getBytes()),
						new FlagsList());
				assertTrue(added > 0);
				addedMails.add(added);
			}
			sc.select("INBOX");
			System.err.println(addedMails);
			List<Integer> toTagMails = addedMails.subList(1, 5);
			toTagMails.forEach(added -> {
				TaggedResult tagged = sc.tagged("uid store " + added + " (titi" + System.currentTimeMillis() + ")");
				assertTrue(tagged.isOk());
			});

			SecurityContext securityContext = new SecurityContext("abc123", loginUid, Collections.emptyList(),
					Collections.emptyList(), "test.devenv");
			Sessions.get().put("abc123", securityContext);
			ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(securityContext);
			IMailboxFolders foldersApi = prov.instance(IMailboxFolders.class, "test.devenv", "user." + loginUid);
			ItemValue<MailboxFolder> inbox = foldersApi.byName("INBOX");
			IMailboxItems recApi = prov.instance(IMailboxItems.class, inbox.uid);
			ContainerChangeset<Long> all = recApi.changesetById(0L);
			for (Long itemId : all.created) {
				ItemValue<MailboxItem> rec = recApi.getCompleteById(itemId);
				System.err.println(rec.value);
			}
			SearchQuery sq = new SearchQuery();
			sq.setUnkeyword("titi");
			Collection<Integer> uids = sc.uidSearch(sq);
		}
	}

	private IMAPByteSource getUtf8Rfc822Message() {
		String m = "From: Thomas Cataldo <thomas@zz.com>\r\n" + "Subject: test message " + System.currentTimeMillis()
				+ "\r\n" + "MIME-Version: 1.0\r\n" + "Content-Type: text/plain; CHARSET=UTF-8\r\n\r\n"
				+ "Hi, this is message about my 300€ from the casino.\r\n\r\n";
		Random r = new Random();
		int val = r.nextInt(1000);
		StringBuilder sb = new StringBuilder(val * 50 + 500);
		sb.append(m);
		for (int i = 0; i < val; i++) {
			sb.append(i).append("\r\n");
		}
		return IMAPByteSource.wrap(sb.toString().getBytes());
	}

	private IMAPByteSource getUtf8Rfc822MessageWithHeader(String header) {
		String m = "From: Thomas Cataldo <thomas@zz.com>\r\n" + header + "\r\n" + "Subject: test message "
				+ System.currentTimeMillis() + "\r\n" + "MIME-Version: 1.0\r\n"
				+ "Content-Type: text/plain; CHARSET=UTF-8\r\n\r\n"
				+ "Hi, this is message about my 300€ from the casino.\r\n\r\n";
		Random r = new Random();
		int val = r.nextInt(1000);
		StringBuilder sb = new StringBuilder(val * 50 + 500);
		sb.append(m);
		for (int i = 0; i < val; i++) {
			sb.append(i).append("\r\n");
		}
		return IMAPByteSource.wrap(sb.toString().getBytes());
	}

	private IMAPByteSource getUtf8Rfc822MessageWithAmount(int amount) {
		String m = "From: Thomas Cataldo <thomas@zz.com>\r\n" + "Subject: test message " + System.currentTimeMillis()
				+ "\r\n" + "MIME-Version: 1.0\r\n" + "Content-Type: text/plain; CHARSET=UTF-8\r\n\r\n"
				+ "Hi, this is message about my " + amount + "€ from the casino.\r\n\r\n";
		Random r = new Random();
		int val = r.nextInt(1000);
		StringBuilder sb = new StringBuilder(val * 50 + 500);
		sb.append(m);
		for (int i = 0; i < val; i++) {
			sb.append(i).append("\r\n");
		}
		return IMAPByteSource.wrap(sb.toString().getBytes());
	}

	private IMAPByteSource getUtf8Rfc822MessageWithSize(int size) {
		String m = "From: Thomas Cataldo <thomas@zz.com>\r\n" + "Subject: test message " + System.currentTimeMillis()
				+ "\r\n" + "MIME-Version: 1.0\r\n" + "Content-Type: text/plain; CHARSET=UTF-8\r\n\r\n"
				+ "Hi, this is message about my 300€ from the casino.\r\n\r\n";
		int val = 1000;
		StringBuilder sb = new StringBuilder(val * size);
		sb.append(m);
		for (int i = 0; i < size; i++) {
			sb.append(i).append("\r\n");
		}
		return IMAPByteSource.wrap(sb.toString().getBytes());
	}

	private IMAPByteSource getUtf8Rfc822MessageSubject(String subject) {
		String m = "From: Thomas Cataldo <thomas@zz.com>\r\n" + "Subject: " + subject + "\r\n" + "MIME-Version: 1.0\r\n"
				+ "Content-Type: text/plain; CHARSET=UTF-8\r\n\r\n"
				+ "Hi, this is message about my 300€ from the casino.\r\n\r\n";
		Random r = new Random();
		int val = r.nextInt(1000);
		StringBuilder sb = new StringBuilder(val * 50 + 500);
		sb.append(m);
		for (int i = 0; i < val; i++) {
			sb.append(i).append("\r\n");
		}
		return IMAPByteSource.wrap(sb.toString().getBytes());
	}

	private IMAPByteSource getUtf8Rfc822Message(String from, String to, String cc, String header) {
		String m = "From: " + from + "\r\n" + "To: " + to + "\r\n" + "Cc: " + cc + "\r\n" + "Subject: test message "
				+ System.currentTimeMillis() + "\r\n" + "MIME-Version: 1.0\r\n"
				+ "Content-Type: text/plain; CHARSET=UTF-8\r\n\r\n"
				+ "Hi, this is message about my 300€ from the casino.\r\n\r\n";
		Random r = new Random();
		int val = r.nextInt(1000);
		StringBuilder sb = new StringBuilder(val * 50 + 500);
		sb.append(m);
		for (int i = 0; i < val; i++) {
			sb.append(i).append("\r\n");
		}
		return IMAPByteSource.wrap(sb.toString().getBytes());
	}

	private StoreClient newStore(boolean tls) {
		StoreClient store = new StoreClient("127.0.0.1", PORT, loginUid + "@" + domainUid, loginUid);
		boolean login = store.login(tls);
		assertTrue(login);
		if (!login) {
			fail("login failed for " + login + " / " + loginUid);
		}
		return store;
	}

	void setupBoxContent(int deleted, int seen, int answered, int draft) {
		IMAPByteSource ibs = getUtf8Rfc822Message();
		try (StoreClient sc = newStore(false)) {
			IntStream.range(0, deleted).forEach((i) -> {
				try {
					sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList(Flag.DELETED.toString())));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			IntStream.range(0, seen).forEach((i) -> {
				try {
					sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList(Flag.SEEN.toString())));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			IntStream.range(0, answered).forEach((i) -> {
				try {
					sc.append("INBOX", ibs.source().openStream(),
							FlagsList.of(Arrays.asList(Flag.ANSWERED.toString())));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			IntStream.range(0, draft).forEach((i) -> {
				try {
					sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList(Flag.DRAFT.toString())));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
		ibs.close();
	}

	void setupBoxContentWithDates(int dateMinus700Days, int dateMinus20Days, int dateMinus15Days, int dateMinus10Days) {

		try (StoreClient sc = newStore(false)) {
			IntStream.range(0, dateMinus700Days).forEach((i) -> {
				try (IMAPByteSource ibs = getUtf8Rfc822Message()) {
					sc.append("INBOX", ibs.source().openStream(), new FlagsList(),
							Date.from(Instant.now().minus(700, ChronoUnit.DAYS)));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			IntStream.range(0, dateMinus20Days).forEach((i) -> {
				try (IMAPByteSource ibs = getUtf8Rfc822Message()) {
					sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList(Flag.SEEN.toString())),
							Date.from(Instant.now().minus(20, ChronoUnit.DAYS)));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			IntStream.range(0, dateMinus15Days).forEach((i) -> {
				try (IMAPByteSource ibs = getUtf8Rfc822Message()) {
					sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList(Flag.ANSWERED.toString())),
							Date.from(Instant.now().minus(15, ChronoUnit.DAYS)));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			IntStream.range(0, dateMinus10Days).forEach((i) -> {
				try (IMAPByteSource ibs = getUtf8Rfc822Message()) {
					sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList(Flag.DRAFT.toString())),
							Date.from(Instant.now().minus(10, ChronoUnit.DAYS)));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	void setupBoxContentWithSizes(int messageSize1, int messageSize10000) {
		IMAPByteSource ibsSmall = getUtf8Rfc822MessageWithSize(1);
		IMAPByteSource ibsLarge = getUtf8Rfc822MessageWithSize(10_000);

		try (StoreClient sc = newStore(false)) {
			IntStream.range(0, messageSize1).forEach((i) -> {
				try {
					sc.append("INBOX", ibsSmall.source().openStream(), new FlagsList(),
							Date.from(Instant.now().minus(700, ChronoUnit.DAYS)));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			IntStream.range(0, messageSize10000).forEach((i) -> {
				try {
					sc.append("INBOX", ibsLarge.source().openStream(),
							FlagsList.of(Arrays.asList(Flag.SEEN.toString())),
							Date.from(Instant.now().minus(20, ChronoUnit.DAYS)));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
		ibsSmall.close();
		ibsLarge.close();
	}

	void setupBoxContentWithPeople(int ccDavid, int ccSylvain, int fromSylvain) {
		int indexedAtStart = indexedCount.get();
		IMAPByteSource ibs = getUtf8Rfc822Message("Thomas Cataldo <thomas@zz.com>", "David Bluemind <david@zz.com>",
				"Sylvain Bluemind <sylvain@zz.com>", "X-BM-Quality:test");
		IMAPByteSource ibs1 = getUtf8Rfc822Message("Thomas Cataldo <thomas@zz.com>",
				"Sylvain Bluemind <sylvain@zz.com>", "David Bluemind <david@zz.com>",
				"X-BM-ExternalID:B9EE016A-9988-45AB-AA39-46B79C459D6B");
		IMAPByteSource ibs2 = getUtf8Rfc822Message("Sylvain Bluemind <sylvain@zz.com>", "David Bluemind <david@zz.com>",
				"Thomas Cataldo <thomas@zz.com>", "X-BM-Quality:test");
		try (StoreClient sc = newStore(false)) {
			IntStream.range(0, ccDavid).forEach((i) -> {
				try {
					sc.append("INBOX", ibs1.source().openStream(), new FlagsList());
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			IntStream.range(0, ccSylvain).forEach((i) -> {
				try {
					sc.append("INBOX", ibs.source().openStream(), FlagsList.of(Arrays.asList(Flag.SEEN.toString())));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			IntStream.range(0, fromSylvain).forEach((i) -> {
				try {
					sc.append("INBOX", ibs2.source().openStream(),
							FlagsList.of(Arrays.asList(Flag.ANSWERED.toString())));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

		}
		ibs.close();
		ibs1.close();
		ibs2.close();
	}

	void setupBoxContentWithBodies(int body200, int body500) {
		IMAPByteSource ibsCcAmountBody200 = getUtf8Rfc822MessageWithAmount(200);
		IMAPByteSource ibsCcAmountBody500 = getUtf8Rfc822MessageWithAmount(500);
		try (StoreClient sc = newStore(false)) {
			IntStream.range(0, body200).forEach((i) -> {
				try {
					sc.append("INBOX", ibsCcAmountBody200.source().openStream(), new FlagsList());
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			IntStream.range(0, body500).forEach((i) -> {
				try {
					sc.append("INBOX", ibsCcAmountBody500.source().openStream(),
							FlagsList.of(Arrays.asList(Flag.SEEN.toString())));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

		}
		ibsCcAmountBody200.close();
		ibsCcAmountBody500.close();
	}

	void setupBoxContentWithHeaders(int header1, int header2, int headerTest) {
		IMAPByteSource ibsWithHeader = getUtf8Rfc822MessageWithHeader(
				"X-BM-ExternalID:B9EE016A-9988-45AB-AA39-46B79C459D6B");
		IMAPByteSource ibsWithHeader1 = getUtf8Rfc822MessageWithHeader(
				"X-BM-ExternalID:C0FF127B-9988-45AB-AA39-46B79C459D6B");
		IMAPByteSource ibsWithHeaderXBMQuality = getUtf8Rfc822MessageWithHeader("X-BM-Quality:test");
		try (StoreClient sc = newStore(false)) {
			IntStream.range(0, header1).forEach((i) -> {
				try {
					sc.append("INBOX", ibsWithHeader.source().openStream(), new FlagsList());
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			IntStream.range(0, header2).forEach((i) -> {
				try {
					sc.append("INBOX", ibsWithHeader1.source().openStream(), new FlagsList());
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			IntStream.range(0, headerTest).forEach((i) -> {
				try {
					sc.append("INBOX", ibsWithHeaderXBMQuality.source().openStream(),
							FlagsList.of(Arrays.asList(Flag.SEEN.toString())));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

		}
		ibsWithHeader.close();
		ibsWithHeader1.close();
		ibsWithHeaderXBMQuality.close();
	}

	void setupBoxContentWithSubjects(int toto, int tutu, int titi) {
		IMAPByteSource ibsCcSubjectToto = getUtf8Rfc822MessageSubject("un premier sujet toto");
		IMAPByteSource ibsCcSubjectTutu = getUtf8Rfc822MessageSubject("un premier sujet tutu");
		IMAPByteSource ibsCcSubjectTiti = getUtf8Rfc822MessageSubject("un premier sujet titi");
		try (StoreClient sc = newStore(false)) {
			IntStream.range(0, toto).forEach((i) -> {
				try {
					sc.append("INBOX", ibsCcSubjectToto.source().openStream(), new FlagsList());
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			IntStream.range(0, tutu).forEach((i) -> {
				try {
					sc.append("INBOX", ibsCcSubjectTutu.source().openStream(),
							FlagsList.of(Arrays.asList(Flag.SEEN.toString())));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			IntStream.range(0, titi).forEach((i) -> {
				try {
					sc.append("INBOX", ibsCcSubjectTiti.source().openStream(),
							FlagsList.of(Arrays.asList(Flag.SEEN.toString())));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
		ibsCcSubjectToto.close();
		ibsCcSubjectTutu.close();
		ibsCcSubjectTiti.close();
	}
}
