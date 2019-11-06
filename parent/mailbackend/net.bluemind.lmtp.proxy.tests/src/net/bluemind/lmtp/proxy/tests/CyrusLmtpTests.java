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
package net.bluemind.lmtp.proxy.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.vertx.java.core.Vertx;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.Constructor;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lmtp.testhelper.client.VertxLmtpClient;
import net.bluemind.lmtp.testhelper.model.FakeMailbox;
import net.bluemind.lmtp.testhelper.model.MailboxesModel;
import net.bluemind.locator.LocatorVerticle;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import net.bluemind.vertx.testhelper.Deploy;

public class CyrusLmtpTests extends AbstractChainTest {

	private String cyrusIp;
	private ItemValue<Server> backend;

	@Before
	public void before() throws Exception {
		super.before();
		// setup cyrus according to the mailbox model
		setupMailServices(MailboxesModel.get());
	}

	public void setupMailServices(MailboxesModel mdl) throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		Deploy.verticles(false, Constructor.of(LocatorVerticle::new, LocatorVerticle.class)).get(5, TimeUnit.SECONDS);

		BmConfIni ini = new BmConfIni();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		this.cyrusIp = ini.get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, imapServer);
		System.err.println("Deploying...");

		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);
		for (String dom : mdl.domains) {
			PopulateHelper.addDomain(dom, Routing.none);
		}

		final CompletableFuture<Void> spawn = new CompletableFuture<Void>();
		VertxPlatform.spawnVerticles(ar -> {
			System.out.println("Spawned.");
			if (ar.succeeded()) {
				spawn.complete(ar.result());
			} else {
				spawn.completeExceptionally(ar.cause());
			}
		});
		spawn.join();

		CyrusService service = new CyrusService(cyrusIp);
		this.backend = service.server();
		assertNotNull(backend);

		for (FakeMailbox fm : mdl.knownRecipients.values()) {
			String[] splitted = fm.email.split("@");
			String localPart = splitted[0];
			String domainPart = splitted[1];
			String uid = PopulateHelper.addUser(localPart, domainPart, Routing.internal);
			switch (fm.state) {
			case OverQuota:
				makeOverQuota(uid, localPart, domainPart);
				break;
			case OverQuotaOnNextMail:
				makeOverQuotaOnNextMail(uid, localPart, domainPart);
				break;
			case Fucked:
				makeFucked(localPart, domainPart);
				break;
			default:
				// do nothing
				break;
			}
		}
	}

	private void makeOverQuota(String uid, String localPart, String domainPart) {
		ServerSideServiceProvider admProv = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailboxes mboxesApi = admProv.instance(IMailboxes.class, domainPart);
		ItemValue<Mailbox> mbox = mboxesApi.byEmail(localPart + "@" + domainPart);
		assertNotNull(mbox);
		MailboxQuota quota = mboxesApi.getMailboxQuota(mbox.uid);
		System.out.println("Got quota, used: " + quota.used + ", set: " + quota.quota);
		if (quota.quota != null && quota.used > quota.quota) {
			System.out.println(localPart + " already over quota.");
			return;
		}

		StoreClient sc = new StoreClient(cyrusIp, 1143, localPart + "@" + domainPart, localPart);
		assertTrue(sc.login());
		int added = sc.append("Trash", resourceStream("data/basic_2attachments.eml"), new FlagsList());
		added = sc.append("INBOX", resourceStream("data/basic_2attachments.eml"), new FlagsList());
		assertTrue(added > 0);
		sc.logout();
		sc.close();
		IUser userApi = admProv.instance(IUser.class, domainPart);
		ItemValue<User> userItem = userApi.getComplete(uid);
		userItem.value.quota = 5; // 5KB
		userApi.update(uid, userItem.value);
	}

	private void makeOverQuotaOnNextMail(String uid, String localPart, String domainPart) {
		ServerSideServiceProvider admProv = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailboxes mboxesApi = admProv.instance(IMailboxes.class, domainPart);
		ItemValue<Mailbox> mbox = mboxesApi.byEmail(localPart + "@" + domainPart);
		assertNotNull(mbox);
		MailboxQuota quota = mboxesApi.getMailboxQuota(mbox.uid);
		IUser userApi = admProv.instance(IUser.class, domainPart);
		ItemValue<User> userItem = userApi.getComplete(uid);
		System.out.println("Got quota, used: " + quota.used + ", set: " + quota.quota);
		if (quota.quota != null) {
			userItem.value.quota = quota.used + 1;
			userApi.update(uid, userItem.value);
			quota = mboxesApi.getMailboxQuota(mbox.uid);
			System.err.println("UPD Quota now, used: " + quota.used + ", total: " + quota.quota);
			return;
		}

		StoreClient sc = new StoreClient(cyrusIp, 1143, localPart + "@" + domainPart, localPart);
		assertTrue(sc.login());
		int added = sc.append("INBOX", resourceStream("data/basic_2attachments.eml"), new FlagsList());
		assertTrue(added > 0);
		sc.logout();
		sc.close();
		userItem.value.quota = 5; // 5KB
		userApi.update(uid, userItem.value);
		quota = mboxesApi.getMailboxQuota(mbox.uid);
		userItem.value.quota = quota.used + 1;
		userApi.update(uid, userItem.value);
		quota = mboxesApi.getMailboxQuota(mbox.uid);
		System.err.println("SET Quota now, used: " + quota.used + ", total: " + quota.quota);
	}

	private void makeFucked(String localPart, String domainPart) {
		INodeClient nc = NodeActivator.get(cyrusIp);
		String partition = CyrusPartition.forServerAndDomain(backend, domainPart).name;
		String userFirst = localPart.charAt(0) + "";
		String domFirst = domainPart.charAt(0) + "";
		String path = "/var/spool/cyrus/meta/" + partition + "/domain/" + domFirst + "/" + domainPart + "/" + userFirst
				+ "/user/" + localPart + "/cyrus.index";
		System.err.println("Fucking path " + path);
		nc.deleteFile(path);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		super.after();
	}

	protected CompletableFuture<VertxLmtpClient> lmtpClient() {
		CompletableFuture<VertxLmtpClient> ret = new CompletableFuture<VertxLmtpClient>();
		Vertx vertx = VertxPlatform.getVertx();
		vertx.setTimer(1, tid -> {
			VertxLmtpClient client = new VertxLmtpClient(vertx, cyrusIp, 24);
			ret.complete(client);
		});
		return ret;
	}

}
