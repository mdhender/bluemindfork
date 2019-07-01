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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;

import net.bluemind.backend.mail.replica.api.ICyrusReplicationAnnotations;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationArtifacts;
import net.bluemind.backend.mail.replica.api.MailboxAnnotation;
import net.bluemind.backend.mail.replica.api.MailboxSub;
import net.bluemind.backend.mail.replica.api.QuotaRoot;
import net.bluemind.backend.mail.replica.api.SeenOverlay;
import net.bluemind.backend.mail.replica.api.SieveScript;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class CyrusArtifactsServiceTests {

	protected String apiKey;
	private BmTestContext userContext;

	@BeforeClass
	public static void oneShotBefore() {
		System.setProperty("es.mailspool.count", "1");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		BmConfIni ini = new BmConfIni();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		Server imapServer = new Server();
		imapServer.ip = ini.get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer, esServer);
		ElasticsearchTestHelper.getInstance().beforeTest();

		PopulateHelper.addDomain("test.lab", Routing.none);

		PopulateHelper.addUser("user", "test.lab", Routing.internal);
		PopulateHelper.addUser("german.pr0n", "test.lab", Routing.internal);

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();
		this.apiKey = "test-session";
		Sessions.get().put(apiKey, SecurityContext.SYSTEM);
		this.userContext = BmTestContext.contextWithSession("user-session", "user", "test.lab");
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSieve() {
		ICyrusReplicationArtifacts service = getArtifactsService(SecurityContext.SYSTEM);
		SieveScript ss = new SieveScript("user@test.lab", "qsd.sieve", 123, false);
		service.storeScript(ss);
		List<SieveScript> found = service.sieves();
		assertEquals(1, found.size());
		assertFalse(found.get(0).isActive);

		ss.isActive = true;
		service.storeScript(ss);
		found = service.sieves();
		assertEquals(1, found.size());
		assertTrue(found.get(0).isActive);

		service.deleteScript(ss);
		found = service.sieves();
		assertEquals(0, found.size());
	}

	@Test
	public void testSeen() {
		ICyrusReplicationArtifacts service = getArtifactsService(SecurityContext.SYSTEM);
		SeenOverlay seen = new SeenOverlay();
		seen.userId = "user@test.lab";
		seen.uniqueId = "1234";
		seen.lastChange = 1;
		seen.lastUid = 2;
		seen.lastRead = 3;
		seen.seenUids = "1,2,3";
		service.storeSeen(seen);
		List<SeenOverlay> found = service.seens();
		assertEquals(1, found.size());
	}

	@Test
	public void testSub() {
		ICyrusReplicationArtifacts service = getArtifactsService(SecurityContext.SYSTEM);
		MailboxSub sub = new MailboxSub("user@test.lab", "german.pr0n");
		service.storeSub(sub);
		List<MailboxSub> found = service.subs();
		assertEquals(1, found.size());
		service.deleteSub(found.get(0));
		found = service.subs();
		assertTrue(found.isEmpty());
	}

	@Test
	public void testQuotas() {
		ICyrusReplicationArtifacts service = getArtifactsService(SecurityContext.SYSTEM);
		QuotaRoot sub = new QuotaRoot("test.lab!user.user", 42);
		service.storeQuota(sub);
		List<QuotaRoot> found = service.quotas();
		assertEquals(1, found.size());
		service.deleteQuota(found.get(0));
		found = service.quotas();
		assertTrue(found.isEmpty());
	}

	@Test
	public void testAnnotations() {
		ICyrusReplicationAnnotations service = getAnnotationsService(SecurityContext.SYSTEM);
		MailboxAnnotation ma = new MailboxAnnotation();
		ma.mailbox = "test.lab!user.user";
		ma.userId = "user@test.lab";
		ma.entry = "the_key";
		ma.value = "id";
		service.storeAnnotation(ma);
		ma.entry = "other_key";
		ma.value = "42";
		service.storeAnnotation(ma);
		service.storeAnnotation(ma);
		List<MailboxAnnotation> search = service.annotations(ma.mailbox);
		assertEquals(2, search.size());
		service.deleteAnnotation(ma);
	}

	@Test
	public void testAnnotationsAsUser() {
		ICyrusReplicationAnnotations service = getAnnotationsService(userContext.getSecurityContext());
		MailboxAnnotation ma = new MailboxAnnotation();
		ma.mailbox = "test.lab!user.another";
		ma.userId = "another@test.lab";
		ma.entry = "the_key";
		ma.value = "id";
		try {
			service.storeAnnotation(ma);
			fail("another does not exist");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}

		MailboxAnnotation noRights = new MailboxAnnotation();
		noRights.mailbox = "test.lab!user.german^pr0n";
		noRights.userId = "german.pr0n@test.lab";
		noRights.entry = "the_key";
		noRights.value = "id";
		try {
			service.storeAnnotation(noRights);
			fail("ACLs should block the store annotation call");
		} catch (ServerFault sf) {
			sf.printStackTrace();
			assertEquals(ErrorCode.PERMISSION_DENIED, sf.getCode());
		}

		MailboxAnnotation myBox = new MailboxAnnotation();
		myBox.mailbox = "test.lab!user.user.Sent";
		myBox.userId = "user@test.lab";
		myBox.entry = "the_key";
		myBox.value = "id";
		service.storeAnnotation(myBox);

	}

	protected ICyrusReplicationArtifacts getArtifactsService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(ICyrusReplicationArtifacts.class, "user@test.lab");
	}

	protected ICyrusReplicationAnnotations getAnnotationsService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(ICyrusReplicationAnnotations.class);
	}

}
