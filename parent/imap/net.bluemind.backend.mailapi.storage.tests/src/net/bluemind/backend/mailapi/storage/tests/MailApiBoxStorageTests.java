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
package net.bluemind.backend.mailapi.storage.tests;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.backend.mailapi.storage.MailApiBoxStorage;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MailApiBoxStorageTests {

	private static final Logger logger = LoggerFactory.getLogger(MailApiBoxStorageTests.class);

	private String loginUser1 = "user1";
	private String domainUid;
	private String mailUser1;
	BmContext context;

	protected String INDEX_NAME = "mailspool_1";
	public String ALIAS = "mailspool_alias_" + loginUser1;

	ItemValue<Mailbox> mboxUser1;
	protected String apiKey;
	private SecurityContext secCtxUser1;
	private String user1Login;

	@BeforeClass
	public static void sysprop() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
	}

	@Before
	public void before() throws Exception {

		domainUid = "test" + System.currentTimeMillis() + ".lab";

		mailUser1 = loginUser1 + "@" + domainUid;
		apiKey = "sid";

		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		context = getContext();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		Assert.assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		Server impaServer = new Server();
		impaServer.tags = Collections.singletonList("mail/imap");
		impaServer.ip = PopulateHelper.FAKE_CYRUS_IP;

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt(esServer, impaServer);

		ElasticsearchTestHelper.getInstance().beforeTest(1);

		PopulateHelper.addDomain(domainUid, Routing.none);
		PopulateHelper.addUser(loginUser1, domainUid, Routing.internal);
		mboxUser1 = systemServiceProvider().instance(IMailboxes.class, domainUid).getComplete(loginUser1);

		this.secCtxUser1 = new SecurityContext(apiKey, user1Login, Collections.emptyList(), Collections.emptyList(),
				domainUid);

	}

	@Test
	public void testGetQuotaNoSpecificFlags() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, mailUser1, loginUser1)) {
			assertTrue(sc.login());

			// Create 100 mails
			IntStream.range(1, 100).forEach(i -> {
				int added = sc.append("INBOX", eml("emls/test_mail.eml"), new FlagsList());
				assertTrue(added > 0);
			});
			Thread.sleep(2_000);
			MailApiBoxStorage apiBoxStorage = new MailApiBoxStorage();
			MailboxQuota mailboxQuota = apiBoxStorage.getQuota(context, domainUid, mboxUser1);
			logger.info("{}@{} - user quota = {}", mboxUser1, domainUid, mailboxQuota.used);
			Assert.assertEquals(5206, mailboxQuota.used);
		}
	}

	@Test
	public void testGetQuotaWithSomeMailsFlagedDeleted() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, mailUser1, loginUser1)) {
			assertTrue(sc.login());

			// Create 100 mails
			IntStream.range(1, 100).forEach(i -> {
				FlagsList flags = new FlagsList();
				if (i % 10 == 0) {
					flags.add(Flag.DELETED);
				}
				int added = sc.append("INBOX", eml("emls/test_mail.eml"), flags);
				assertTrue(added > 0);
			});
			Thread.sleep(2_000);
			MailApiBoxStorage apiBoxStorage = new MailApiBoxStorage();
			MailboxQuota mailboxQuota = apiBoxStorage.getQuota(context, domainUid, mboxUser1);
			logger.info("{}@{} - user quota = {}", mboxUser1, domainUid, mailboxQuota.used);
			Assert.assertEquals(4732, mailboxQuota.used);
		}
	}

	@Test
	public void testGetMaxQuotaForUser() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, mailUser1, loginUser1)) {
			assertTrue(sc.login());

			// Create 100 mails
			IntStream.range(1, 100).forEach(i -> {
				int added = sc.append("INBOX", eml("emls/test_mail.eml"), new FlagsList());
				assertTrue(added > 0);
			});

			// Get mailbox and update quota
			IMailboxes mailboxesApi = systemServiceProvider().instance(IMailboxes.class, domainUid);
			var mailboxUser1Item = mailboxesApi.byEmail(mailUser1);
			mailboxUser1Item.value.quota = 204_800;
			logger.info("{} - update mailbox with quota {}", mailUser1, 204_800);
			mailboxesApi.update(mailboxUser1Item.uid, mailboxUser1Item.value);

			Thread.sleep(2_000);
			var retrievedMailboxUser1 = mailboxesApi.byEmail(mailUser1);

			MailApiBoxStorage apiBoxStorage = new MailApiBoxStorage();
			MailboxQuota mailboxQuota = apiBoxStorage.getQuota(context, domainUid, retrievedMailboxUser1);
			logger.info("{}@{} - user quota = {}", mboxUser1, domainUid, mailboxQuota.used);
			Assert.assertEquals(5206, mailboxQuota.used);
			Assert.assertEquals(204800, (int) mailboxQuota.quota);
		}
	}

	protected IServiceProvider systemServiceProvider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	protected IServiceProvider user1ServiceProvider() {
		return ServerSideServiceProvider.getProvider(secCtxUser1);
	}

	protected BmContext getContext() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
	}

	private InputStream eml(String resPath) {
		return MailApiBoxStorageTests.class.getClassLoader().getResourceAsStream(resPath);
	}

}
