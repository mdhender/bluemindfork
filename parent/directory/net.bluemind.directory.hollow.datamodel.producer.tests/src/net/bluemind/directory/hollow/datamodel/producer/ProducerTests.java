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
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord;
import net.bluemind.directory.hollow.datamodel.consumer.AnrToken;
import net.bluemind.directory.hollow.datamodel.consumer.BrowsableDirectorySearch;
import net.bluemind.directory.hollow.datamodel.consumer.DirectorySearchFactory;
import net.bluemind.directory.hollow.datamodel.consumer.SerializedDirectorySearch;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.externaluser.api.ExternalUser;
import net.bluemind.externaluser.api.IExternalUser;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class ProducerTests {

	private String domainUid;
	private ItemValue<Domain> domain;

	@Before
	public void setup() throws Exception {
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);

		JdbcTestHelper.getInstance().beforeTest();
		Serializers.clear();
		ElasticsearchTestHelper.getInstance().beforeTest();

		domainUid = "dom" + System.currentTimeMillis() + ".test";
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		Server pipo = new Server();
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		PopulateHelper.initGlobalVirt(esServer, pipo);
		domain = PopulateHelper.createTestDomain(domainUid, esServer, pipo);

		PopulateHelper.addUserWithRoles("useradmin", domainUid, BasicRoles.ROLE_MANAGE_USER);

		PopulateHelper.addUserWithRoles("user1", domainUid);
		PopulateHelper.addUserWithRoles("user2", domainUid);

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.uid);
		IMailshare mailshareService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailshare.class, domain.uid);
		IExternalUser externalUserService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IExternalUser.class, domain.uid);

		groupService.create("group1", defaultGroup("g1", PopulateHelper.FAKE_CYRUS_IP));
		groupService.create("group2", defaultGroup("g2", PopulateHelper.FAKE_CYRUS_IP));

		mailshareService.create("share1", defaultMailshare(PopulateHelper.FAKE_CYRUS_IP));

		externalUserService.create("external1", defaultExternalUser("external", "user1"));
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSimpleExport() throws Exception {
		DirectoryTestProducer producer = new DirectoryTestProducer(domainUid);
		producer.init();
		long snap = producer.produce();

		DirectoryTestConsumer consumer = new DirectoryTestConsumer(producer.file);
		DirectorySearchFactory.getDeserializers().remove(domainUid);
		DirectorySearchFactory.getDeserializers().put(domainUid, consumer);
		SerializedDirectorySearch search = DirectorySearchFactory.get(domainUid);

		consumer.refreshTo(snap);
		Collection<AddressBookRecord> dir = search.all();
		assertEquals(9, dir.size());

		assertTrue(producer.file.listFiles().length > 0);
		dir = search.all();
		assertEquals(9, dir.size());

		PopulateHelper.addUserWithRoles("user3", domainUid);
		PopulateHelper.addUserWithRoles("user7", domainUid);
		PopulateHelper.addUserWithRoles("user4", domainUid);
		consumer.refreshTo(producer.produce());

		assertTrue(producer.file.listFiles().length > 0);
		dir = search.all();
		assertEquals(12, dir.size());

		String uid5 = PopulateHelper.addUserWithRoles("user5", domainUid);
		PopulateHelper.addUserWithRoles("user6", domainUid);

		consumer.refreshTo(producer.produce());
		assertTrue(producer.file.listFiles().length > 0);
		dir = search.all();
		assertEquals(14, dir.size());

		IUser user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain.uid);
		TaskRef tr = user.delete(uid5);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), tr);

		consumer.refreshTo(producer.produce());
		assertTrue(producer.file.listFiles().length > 0);
		dir = search.all();
		assertEquals(13, dir.size());

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
		long snap = producer.produce();

		DirectoryTestConsumer consumer = new DirectoryTestConsumer(producer.file);
		DirectorySearchFactory.getDeserializers().remove(domainUid);
		DirectorySearchFactory.getDeserializers().put(domainUid, consumer);
		SerializedDirectorySearch search = DirectorySearchFactory.get(domainUid);
		consumer.refreshTo(snap);

		Collection<AddressBookRecord> all = search.all();
		all.forEach(ab -> {
			System.err.println("***************** " + ab.getUid() + " - " + ab.getMinimalid() + " -> "
					+ ab.getDistinguishedName());
		});

		String user7dn = "/o=mapi/ou=" + domainUid + "/cn=recipients/cn=user:" + "user7";

		Optional<AddressBookRecord> byUid = search.byUid("uSeR7");
		assertTrue(byUid.isPresent());
		assertEquals(user7dn, byUid.get().getDistinguishedName());
		assertEquals("uSeR7", byUid.get().getUid());

		byUid = search.byDistinguishedName(user7dn);
		assertTrue(byUid.isPresent());
		assertEquals(user7dn, byUid.get().getDistinguishedName());
		assertEquals("uSeR7", byUid.get().getUid());

		String external1dn = "/o=mapi/ou=" + domainUid + "/cn=recipients/cn=externaluser:" + "external1";
		byUid = search.byUid("external1");
		assertTrue(byUid.isPresent());
		assertEquals("external1", byUid.get().getUid());
		assertEquals(external1dn, byUid.get().getDistinguishedName());

		byUid = search.byDistinguishedName(external1dn);
		assertTrue(byUid.isPresent());
		assertEquals(external1dn, byUid.get().getDistinguishedName());
		assertEquals("external1", byUid.get().getUid());

	}

	@Test
	public void testSearchByNameOrEmail() throws Exception {
		DirectoryTestProducer producer = new DirectoryTestProducer(domainUid);
		producer.init();
		long snap = producer.produce();

		DirectoryTestConsumer consumer = new DirectoryTestConsumer(producer.file);
		DirectorySearchFactory.getDeserializers().remove(domainUid);
		DirectorySearchFactory.getDeserializers().put(domainUid, consumer);
		BrowsableDirectorySearch search = DirectorySearchFactory.browser(domainUid);

		consumer.refreshTo(snap);

		Thread.sleep(500);

		Collection<AddressBookRecord> ret = search.byNameOrEmailPrefix("user");
		System.err.println("size: " + ret.size());

		Set<Long> matched = new HashSet<>();
		for (AddressBookRecord rec : ret) {
			System.out.println("MATCH " + rec.getName() + " " + rec.getEmail());
			matched.add(rec.getMinimalidBoxed());
		}
		for (AddressBookRecord rec : search.all()) {
			if (!matched.contains(rec.getMinimalidBoxed())) {
				System.out.println("MISSMATCH " + rec.getOrdinal() + " '" + rec.getName() + "' '" + rec.getEmail()
						+ "' ANRs: " + rec.getAnr().stream().map(AnrToken::getToken).collect(Collectors.joining(", ")));
			}
		}
		assertEquals(4, ret.size());

		ret = search.byNameOrEmailPrefix("external");
		assertEquals(1, ret.size());
		assertEquals("external user1", ret.iterator().next().getName());

	}

	@Test
	public void testArchiveUser() throws Exception {
		DirectoryTestProducer producer = new DirectoryTestProducer(domainUid);
		producer.init();
		long snap = producer.produce();

		DirectoryTestConsumer consumer = new DirectoryTestConsumer(producer.file);
		DirectorySearchFactory.getDeserializers().remove(domainUid);
		DirectorySearchFactory.getDeserializers().put(domainUid, consumer);
		SerializedDirectorySearch search = DirectorySearchFactory.get(domainUid);

		consumer.refreshTo(snap);

		Optional<AddressBookRecord> byEmail = search.byEmail("user1@" + domainUid);
		assertTrue(byEmail.isPresent());

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domain.uid);
		ItemValue<User> user1 = userService.getComplete("user1");
		user1.value.archived = true;
		userService.update("user1", user1.value);

		consumer.refreshTo(producer.produce());

		byEmail = search.byEmail("user1@" + domainUid);
		assertTrue(byEmail.isPresent());
	}

	@Test
	public void testDeleteGroup() throws Exception {
		DirectoryTestProducer producer = new DirectoryTestProducer(domainUid);
		producer.init();
		long snap = producer.produce();

		DirectoryTestConsumer consumer = new DirectoryTestConsumer(producer.file);
		DirectorySearchFactory.getDeserializers().remove(domainUid);
		DirectorySearchFactory.getDeserializers().put(domainUid, consumer);
		SerializedDirectorySearch search = DirectorySearchFactory.get(domainUid);

		consumer.refreshTo(snap);

		Optional<AddressBookRecord> byEmail = search.byEmail("g1@" + domainUid);
		assertTrue(byEmail.isPresent());

		System.err.println("Deleting group...");
		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.uid);
		TaskRef taskRef = groupService.delete("group1");

		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), taskRef);

		consumer.refreshTo(producer.produce());

		byEmail = search.byEmail("g1@" + domainUid);
		assertFalse("g1@" + domainUid + " should not be in the index", byEmail.isPresent());
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
		long version = producer.produce();

		DirectoryTestConsumer consumer = new DirectoryTestConsumer(producer.file);
		DirectorySearchFactory.getDeserializers().remove(domainUid);
		DirectorySearchFactory.getDeserializers().put(domainUid, consumer);
		SerializedDirectorySearch search = DirectorySearchFactory.get(domainUid);

		consumer.refreshTo(version);

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
		byEmail = search.byEmail("external.user1@outside.org");
		assertTrue(byEmail.isPresent());
	}

	private Group defaultGroup(String name, String cyrusIp) {
		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				"default");

		String dataLocation = serverService.getComplete(cyrusIp).uid;

		Group group = new Group();

		group.name = name;
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

	private ExternalUser defaultExternalUser(String firstName, String name) {
		ExternalUser externalUser = new ExternalUser();
		String address = firstName + "." + name + "@outside.org";
		externalUser.dataLocation = null;
		externalUser.contactInfos = new VCard();
		externalUser.contactInfos.identification.formatedName = VCard.Identification.FormatedName
				.create(firstName + " " + name);
		externalUser.contactInfos.identification.name.familyNames = name;
		externalUser.contactInfos.identification.name.additionalNames = firstName;
		externalUser.contactInfos.communications.emails = Arrays.asList(VCard.Communications.Email.create(address));
		externalUser.emails = Arrays.asList(Email.create(address, true));
		externalUser.hidden = false;

		return externalUser;
	}

}
