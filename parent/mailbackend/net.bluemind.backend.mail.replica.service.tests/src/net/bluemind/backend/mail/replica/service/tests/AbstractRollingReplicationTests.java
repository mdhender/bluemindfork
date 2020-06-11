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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusReplicationHelper;
import net.bluemind.backend.cyrus.replication.testhelper.SyncServerHelper;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.locator.LocatorVerticle;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.vertx.testhelper.Deploy;

public abstract class AbstractRollingReplicationTests {

	protected String cyrusIp;
	protected String domainUid;

	/**
	 * login local part == uid for unit tests
	 */
	protected String userUid;
	protected ReplicationEventsRecorder rec;
	protected CyrusReplicationHelper cyrusReplication;

	protected String uniqueUidPart() {
		return System.currentTimeMillis() + "";
	}

	@Before
	public void before() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();
		Deploy.verticles(false, LocatorVerticle::new).get(5, TimeUnit.SECONDS);

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

		String unique = uniqueUidPart();
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

		CountDownLatch cdl = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(ar -> {
			cdl.countDown();
		});
		boolean beforeTimeout = cdl.await(30, TimeUnit.SECONDS);
		assertTrue(beforeTimeout);

		MQ.init().get(30, TimeUnit.SECONDS);
		Topology.get();

		SyncServerHelper.waitFor();

		cyrusReplication.startReplication().get(5, TimeUnit.SECONDS);

		this.rec = new ReplicationEventsRecorder(VertxPlatform.getVertx());
		rec.recordUser(domainUid, userUid);

		System.err.println("Start populate user " + userUid);
		PopulateHelper.addUser(userUid, domainUid, Routing.internal);
	}

	@FunctionalInterface
	public static interface ImapActions<T> {

		T run(StoreClient sc) throws Exception;
	}

	protected <T> T imapAsUser(ImapActions<T> actions) {
		return imapAction(userUid + "@" + domainUid, userUid, actions);
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

	protected InputStream testEml() {
		return EmlTemplates.withRandomMessageId("with_inlines.ftl");
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
			Stream forUpload = VertxStream.stream(Buffer.buffer(ByteStreams.toByteArray(in)));
			String partId = recordsApi.uploadPart(forUpload);
			assertNotNull(partId);
			System.out.println("Got partId " + partId);
			Part fullEml = Part.create(null, "message/rfc822", partId);
			MessageBody brandNew = new MessageBody();
			brandNew.subject = "toto";
			brandNew.structure = fullEml;
			MailboxItem item = new MailboxItem();
			item.body = brandNew;
			item.flags = Arrays.asList(new MailboxItemFlag("Pouic"));
			long expectedId = id;
			System.err.println("Before create by id....." + id);
			recordsApi.createById(expectedId, item);
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

	protected IServiceProvider provider() {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", "sid");
	}

	@After
	public void after() throws Exception {
		System.err.println("test is over, time for after()");
		cyrusReplication.stopReplication().get(5, TimeUnit.SECONDS);
		JdbcTestHelper.getInstance().afterTest();
	}

}
