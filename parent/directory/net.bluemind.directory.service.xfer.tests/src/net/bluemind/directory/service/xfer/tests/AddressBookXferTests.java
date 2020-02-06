package net.bluemind.directory.service.xfer.tests;
/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.addressbook.api.VCard.Organizational.Member;
import net.bluemind.addressbook.api.VCardChanges;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class AddressBookXferTests {

	private String domainUid = "bm.lan";
	private String userUid = "test" + System.currentTimeMillis();
	private String shardIp;
	private SecurityContext context;

	@BeforeClass
	public static void oneShotBefore() {
		System.setProperty("es.mailspool.count", "1");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		ElasticsearchTestHelper.getInstance().beforeTest();

		Server imapServer = new Server();
		imapServer.ip = new BmConfIni().get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		Server pg2 = new Server();
		shardIp = new BmConfIni().get("pg2");
		pg2.ip = shardIp;
		pg2.tags = Lists.newArrayList("mail/shard");

		PopulateHelper.initGlobalVirt(imapServer, pg2);
		PopulateHelper.createTestDomain(domainUid, imapServer);
		PopulateHelper.addUser(userUid, domainUid);

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		JdbcTestHelper.getInstance().initNewServer(pg2.ip);

		context = new SecurityContext("user", userUid, Arrays.<String>asList(), Arrays.<String>asList(), domainUid);

		Sessions.get().put(context.getSessionId(), context);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testXferAB() {

		String containerUid = IAddressBookUids.defaultUserAddressbook(userUid);

		IAddressBook service = ServerSideServiceProvider.getProvider(context).instance(IAddressBook.class,
				containerUid);

		VCard card = defaultVCard();
		service.create("c1", card);
		service.create("c2", card);

		String uid3 = "testcreate_" + System.nanoTime();

		card.kind = Kind.group;
		card.identification.formatedName = FormatedName.create("test25");

		String uid4 = "testcreate_" + System.nanoTime();
		VCard group = defaultVCard();
		group.kind = Kind.group;
		String uid5 = "testcreate_" + System.nanoTime();
		group.organizational.member = Arrays.asList(Member.create(containerUid, uid5, "fakeName", "fake@email.la"));
		VCardChanges changes = VCardChanges.create(
				// add
				Arrays.asList(VCardChanges.ItemAdd.create(uid3, defaultVCard()),
						// Create group before member
						VCardChanges.ItemAdd.create(uid4, group), VCardChanges.ItemAdd.create(uid5, defaultVCard())

				),
				// modify
				Arrays.asList(VCardChanges.ItemModify.create("c1", card)),
				// delete
				Arrays.asList(VCardChanges.ItemDelete.create("c2")));

		service.updates(changes);

		// initial container state
		int nbItems = service.allUids().size();
		assertEquals(4, nbItems);
		long version = service.getVersion();
		assertEquals(7, version);

		TaskRef tr = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.xfer(userUid, shardIp);
		waitTaskEnd(tr);

		// current service should return nothing
		assertTrue(service.allUids().isEmpty());

		// new IAddressBook instance
		service = ServerSideServiceProvider.getProvider(context).instance(IAddressBook.class, containerUid);

		assertEquals(nbItems, service.allUids().size());
		assertEquals(version, service.getVersion());

		service.create("new-one", card);

		ContainerChangeset<String> changeset = service.changeset(version);
		assertEquals(1, changeset.created.size());
		assertEquals("new-one", changeset.created.get(0));
		assertTrue(changeset.updated.isEmpty());
		assertTrue(changeset.deleted.isEmpty());

	}

	private void waitTaskEnd(TaskRef taskRef) throws ServerFault {
		TaskStatus status = TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), taskRef);
		System.err.println("EndStatus: " + status);
		if (status.state == State.InError) {
			throw new ServerFault("xfer error");
		}
	}

	protected VCard defaultVCard() {
		VCard card = new VCard();

		card.identification = new VCard.Identification();
		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());

		card.related.spouse = "Clara Morgane";
		card.related.assistant = "Sylvain Garcia";
		card.related.manager = "David Phan";

		VCard.Organizational organizational = VCard.Organizational.create("Loser", "Boss", //
				VCard.Organizational.Org.create("Blue-mind", "tlse", "Dev"), //
				Arrays.<VCard.Organizational.Member>asList());

		card.organizational = organizational;

		return card;
	}

}
