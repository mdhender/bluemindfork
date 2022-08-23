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
package net.bluemind.imap.cyrus.swap.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusReplicationHelper;
import net.bluemind.backend.cyrus.replication.testhelper.SyncServerHelper;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class AbstractRollingReplicationTests {

	protected String cyrusIp;
	protected String domainUid;

	/**
	 * login local part == uid for unit tests
	 */
	protected String userUid;
	protected CyrusReplicationHelper cyrusReplication;
	protected String apiKey;

	protected String partition;
	protected String mboxRoot;
	protected IdRange allocations;
	protected ItemValue<MailboxFolder> userInbox;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@Before
	public void before() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();

		BmConfIni ini = new BmConfIni();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		this.cyrusIp = ini.get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		ItemValue<Server> cyrusServer = ItemValue.create("localhost", imapServer);
		CyrusService cyrusService = new CyrusService(cyrusServer);
		cyrusService.reset();

		PopulateHelper.initGlobalVirt(esServer, imapServer);
		ElasticsearchTestHelper.getInstance().beforeTest();
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		String unique = Long.toString(System.currentTimeMillis());
		domainUid = "test" + unique + ".lab";
		userUid = "user" + unique;

		// ensure the partition is created correctly before restarting cyrus
		PopulateHelper.addDomain(domainUid, Routing.none);

		System.err.println("Setup replication START");
		this.cyrusReplication = new CyrusReplicationHelper(cyrusIp);
		cyrusReplication.installReplication();
		System.err.println("Setup replication END");

		JdbcActivator.getInstance().addMailboxDataSource(cyrusReplication.server().uid,
				JdbcTestHelper.getInstance().getMailboxDataDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		MQ.init().get(30, TimeUnit.SECONDS);
		Topology.get();

		SyncServerHelper.waitFor();

		cyrusReplication.startReplication().get(5, TimeUnit.SECONDS);

		System.err.println("Start populate user " + userUid);
		PopulateHelper.addUser(userUid, domainUid, Routing.internal);

		this.apiKey = "sid";
		SecurityContext secCtx = new SecurityContext("sid", userUid, Collections.emptyList(), Collections.emptyList(),
				domainUid);
		Sessions.get().put(apiKey, secCtx);

		CyrusPartition part = CyrusPartition.forServerAndDomain(cyrusReplication.server(), domainUid);
		this.partition = part.name;
		this.mboxRoot = "user." + userUid.replace('.', '^');

		// Wait for INBOX
		for (int i = 0; i < 30; i++) {
			ItemValue<MailboxFolder> inbox = provider().instance(IDbReplicatedMailboxes.class, partition, mboxRoot)
					.byName("INBOX");
			if (inbox != null) {
				this.userInbox = inbox;
				break;
			} else {
				Thread.sleep(100);
			}
		}
		assertNotNull(userInbox);
	}

	@FunctionalInterface
	public static interface ImapActions<T> {

		T run(StoreClient sc) throws Exception;
	}

	public static interface ImapOperation extends ImapActions<Void> {

		default Void run(StoreClient sc) throws Exception {
			op(sc);
			return null;
		}

		void op(StoreClient sc) throws Exception;
	}

	protected <T> T asCyrusUser(ImapActions<T> actions) {
		return imapAction(userUid + "@" + domainUid, userUid, actions);
	}

	protected void doAsCyrusUser(ImapOperation actions) {
		imapAction(userUid + "@" + domainUid, userUid, actions);
	}

	protected <T> T imapAsCyrusAdmin(ImapActions<T> actions) {
		return imapAction("admin0", "admin", actions);
	}

	private <T> T imapAction(String imapLogin, String imapPass, ImapActions<T> actions) {
		try (StoreClient sc = new StoreClient(cyrusIp, 1143, imapLogin, imapPass)) {
			assertTrue(sc.login());
			return actions.run(sc);
		} catch (Exception e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}

	protected IServiceProvider provider() {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", apiKey);
	}

	@After
	public void after() throws Exception {
		System.err.println("test is over, time for after()");
		cyrusReplication.stopReplication().get(5, TimeUnit.SECONDS);
		JdbcTestHelper.getInstance().afterTest();
	}

}
