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
package net.bluemind.directory.hollow.datamodel.producer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord;
import net.bluemind.directory.hollow.datamodel.consumer.DirectorySearchFactory;
import net.bluemind.directory.hollow.datamodel.consumer.SerializedDirectorySearch;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class ProducerTests {

	private String domainUid;
	private ItemValue<Domain> domain;

	@BeforeClass
	public static void oneShotBefore() {
		System.setProperty("es.mailspool.count", "1");
	}

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();

		domainUid = "dom" + System.currentTimeMillis() + ".test";
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		String cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, imapServer);
		domain = PopulateHelper.createTestDomain(domainUid, esServer, imapServer);

		// create domain parititon on cyrus
		new CyrusService(cyrusIp).createPartition(domainUid);
		new CyrusService(cyrusIp).refreshPartitions(Arrays.asList(domainUid));
		new CyrusService(cyrusIp).reload();

		PopulateHelper.addUserWithRoles("useradmin", domainUid, BasicRoles.ROLE_MANAGE_USER);

		PopulateHelper.addUserWithRoles("user1", domainUid);
		PopulateHelper.addUserWithRoles("user2", domainUid);

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.uid);
		IMailshare mailshareService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailshare.class, domain.uid);

		groupService.create(UUID.randomUUID().toString(), defaultGroup("test1", cyrusIp));
		groupService.create(UUID.randomUUID().toString(), defaultGroup("test2", cyrusIp));

		mailshareService.create(UUID.randomUUID().toString(), defaultMailshare(cyrusIp));

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSimpleExport() throws Exception {
		DirectoryTestProducer producer = new DirectoryTestProducer(domainUid);
		producer.init();
		producer.produce();
		Thread.sleep(3000);

		DirectoryTestConsumer consumer = new DirectoryTestConsumer(producer.file);
		DirectorySearchFactory.getDeserializers().remove(domainUid);
		DirectorySearchFactory.getDeserializers().put(domainUid, consumer);
		SerializedDirectorySearch search = DirectorySearchFactory.get(domainUid);

		Collection<AddressBookRecord> dir = search.all();
		assertEquals(8, dir.size());

		producer.produce();
		Thread.sleep(3000);
		assertTrue(producer.file.listFiles().length > 0);
		dir = search.all();
		assertEquals(8, dir.size());

		PopulateHelper.addUserWithRoles("user3", domainUid);
		PopulateHelper.addUserWithRoles("user7", domainUid);
		PopulateHelper.addUserWithRoles("user4", domainUid);
		producer.produce();
		Thread.sleep(3000);
		assertTrue(producer.file.listFiles().length > 0);
		dir = search.all();
		assertEquals(11, dir.size());

		String uid5 = PopulateHelper.addUserWithRoles("user5", domainUid);
		PopulateHelper.addUserWithRoles("user6", domainUid);

		producer.produce();
		Thread.sleep(3000);
		assertTrue(producer.file.listFiles().length > 0);
		dir = search.all();
		assertEquals(13, dir.size());

		IUser user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain.uid);
		TaskRef tr = user.delete(uid5);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), tr);

		producer.produce();
		Thread.sleep(3000);
		assertTrue(producer.file.listFiles().length > 0);
		dir = search.all();
		assertEquals(12, dir.size());

	}

	@Test
	public void testSearchByUid() throws Exception {
		User user = PopulateHelper.getUser("user7", domainUid, Mailbox.Routing.none);
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domainUid);
		String uid = "uSeR7";
		userService.create(uid, user);

		DirectoryTestProducer producer = new DirectoryTestProducer(domainUid);
		producer.init();
		producer.produce();
		Thread.sleep(3000);

		DirectoryTestConsumer consumer = new DirectoryTestConsumer(producer.file);
		DirectorySearchFactory.getDeserializers().remove(domainUid);
		DirectorySearchFactory.getDeserializers().put(domainUid, consumer);
		SerializedDirectorySearch search = DirectorySearchFactory.get(domainUid);

		Collection<AddressBookRecord> all = search.all();
		all.forEach(ab -> {
			System.err.println("***************** " + ab.getUid().getValue() + " - " + ab.getMinimalid() + " -> "
					+ ab.getDistinguishedName().getValue());
		});

		String user7dn = "/o=mapi/ou=" + domainUid + "/cn=recipients/cn=user:" + "user7";

		Optional<AddressBookRecord> byUid = search.byUid("uSeR7");
		assertTrue(byUid.isPresent());
		assertEquals(user7dn, byUid.get().getDistinguishedName().getValue());
		assertEquals("uSeR7", byUid.get().getUid().getValue());

		byUid = search.byDistinguishedName(user7dn);
		assertTrue(byUid.isPresent());
		assertEquals(user7dn, byUid.get().getDistinguishedName().getValue());
		assertEquals("uSeR7", byUid.get().getUid().getValue());
	}

	@Test
	public void testSearchByNameOrEmail() throws Exception {
		DirectoryTestProducer producer = new DirectoryTestProducer(domainUid);
		producer.init();
		producer.produce();
		Thread.sleep(3000);

		DirectoryTestConsumer consumer = new DirectoryTestConsumer(producer.file);
		DirectorySearchFactory.getDeserializers().remove(domainUid);
		DirectorySearchFactory.getDeserializers().put(domainUid, consumer);
		SerializedDirectorySearch search = DirectorySearchFactory.get(domainUid);

		Wait w = new Wait(consumer);
		producer.produce();
		w.waitFor();

		Collection<AddressBookRecord> ret = search.byNameOrEmailPrefix("user");
		assertEquals(4, ret.size());
	}

	@Test
	public void testArchiveUser() throws Exception {
		DirectoryTestProducer producer = new DirectoryTestProducer(domainUid);
		producer.init();
		producer.produce();
		Thread.sleep(3000);

		DirectoryTestConsumer consumer = new DirectoryTestConsumer(producer.file);
		DirectorySearchFactory.getDeserializers().remove(domainUid);
		DirectorySearchFactory.getDeserializers().put(domainUid, consumer);
		SerializedDirectorySearch search = DirectorySearchFactory.get(domainUid);

		Wait w = new Wait(consumer);
		producer.produce();
		w.waitFor();

		Optional<AddressBookRecord> byEmail = search.byEmail("user1@" + domainUid);
		assertTrue(byEmail.isPresent());

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domain.uid);
		ItemValue<User> user1 = userService.getComplete("user1");
		user1.value.archived = true;
		userService.update("user1", user1.value);

		producer.produce();
		Thread.sleep(3000);

		w = new Wait(consumer);
		producer.produce();
		w.waitFor();

		byEmail = search.byEmail("user1@" + domainUid);
		assertFalse(byEmail.isPresent());
	}

	@Test
	public void testSearchByEmailAlias() throws Exception {
		IDomains domainApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		String d2 = domainUid.replace(".test", ".loc");
		domainApi.setAliases(domainUid, new HashSet<>(Arrays.asList(d2)));
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domain.uid);
		ItemValue<User> user = userService.getComplete("user2");
		user.value.emails.add(Email.create("imuser2@" + d2, false, true));
		userService.update("user2", user.value);
		ItemValue<User> user1 = userService.getComplete("user1");
		Email user1Email = user1.value.emails.iterator().next();
		user1Email.allAliases = false;
		userService.update("user1", user1.value);

		DirectoryTestProducer producer = new DirectoryTestProducer(domainUid);
		producer.init();
		producer.produce();
		Thread.sleep(3000);

		DirectoryTestConsumer consumer = new DirectoryTestConsumer(producer.file);
		DirectorySearchFactory.getDeserializers().remove(domainUid);
		DirectorySearchFactory.getDeserializers().put(domainUid, consumer);
		SerializedDirectorySearch search = DirectorySearchFactory.get(domainUid);

		Wait w = new Wait(consumer);
		producer.produce();
		w.waitFor();

		// user1 : user1@dom1544606454540.test, allAliases: false : default:
		// true
		Optional<AddressBookRecord> byEmail = search.byEmail("user1@" + domainUid);
		assertTrue(byEmail.isPresent());
		byEmail = search.byEmail("user1@" + d2);
		assertFalse(byEmail.isPresent());

		// user2 : user2@dom1544606454540.test, allAliases: false : default:
		// true
		// user2 : user2@dom1544606454540.loc, allAliases: true : default: false
		byEmail = search.byEmail("user2@" + domainUid);
		assertTrue(byEmail.isPresent());
		byEmail = search.byEmail("user2@" + d2);
		assertFalse(byEmail.isPresent());
		byEmail = search.byEmail("imuser2@" + domainUid);
		assertTrue(byEmail.isPresent());
		byEmail = search.byEmail("imuser2@" + d2);
		assertTrue(byEmail.isPresent());
	}

	public static class Wait {

		private DirectoryTestConsumer consumer;
		private long version;

		public Wait(DirectoryTestConsumer consumer) {
			this.consumer = consumer;
			this.version = consumer.getVersion();
			System.err.println("init consumer with version " + version);
		}

		public void waitFor() {
			int countdown = 20;
			while (version == consumer.getVersion() && countdown > 0) {
				System.err.println("version " + version + " --> " + consumer.getVersion());
				try {
					Thread.sleep(150);
				} catch (InterruptedException e) {
				}
				countdown--;
			}
			version = consumer.getVersion();
		}

	}

	private Group defaultGroup(String prefix, String cyrusIp) {
		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				"default");

		String dataLocation = serverService.getComplete(cyrusIp).uid;

		Group group = new Group();

		if (prefix == null || prefix.isEmpty()) {
			prefix = "group";
		}
		group.name = prefix + "-" + System.nanoTime();
		group.description = "Test group";

		Email e = new Email();
		e.address = group.name + "@" + domainUid;
		e.allAliases = true;
		e.isDefault = true;
		group.emails = new ArrayList<Email>(1);
		group.emails.add(e);
		group.mailArchived = false;
		group.dataLocation = dataLocation;

		return group;
	}

	private Mailshare defaultMailshare(String cyrusIp) {
		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				"default");

		String dataLocation = serverService.getComplete(cyrusIp).uid;
		Mailshare ms = new Mailshare();
		ms.name = "test";
		ms.dataLocation = dataLocation;
		ms.emails = Arrays.asList(Email.create("test@bm.lan", true, true));
		ms.routing = Routing.internal;
		return ms;
	}

}
