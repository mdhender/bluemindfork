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
package net.bluemind.addressbook.domainbook.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.domainbook.DomainAddressBook;
import net.bluemind.addressbook.domainbook.IDomainAddressBook;
import net.bluemind.addressbook.domainbook.verticle.DomainBookVerticle;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.Member;
import net.bluemind.group.service.internal.ContainerGroupStoreService;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import net.bluemind.user.service.internal.ContainerUserStoreService;

public class DomainAddressBookTests {

	private SecurityContext domainAdmin;
	private String domainUid;
	private BmContext testContext;
	private Container dirContainer;
	private ItemValue<Domain> domain;

	@Before
	public void before() throws Exception {

		DomainBookVerticle.suspended = true;
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");
		PopulateHelper.initGlobalVirt(esServer);

		domainUid = "test" + System.currentTimeMillis() + ".lan";

		domainAdmin = BmTestContext.contextWithSession("testUser", "test", domainUid, SecurityContext.ROLE_ADMIN)
				.getSecurityContext();

		domain = PopulateHelper.createTestDomain(domainUid, esServer);
		dirContainer = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM)
				.get(domainUid);
		PopulateHelper.domainAdmin(domainUid, domainAdmin.getSubject());
		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		testContext = new BmTestContext(SecurityContext.SYSTEM);
	}

	@Test
	public void testCreateHiddenUser() throws ServerFault {
		IAddressBook domainBook = testContext.provider().instance(IAddressBook.class,
				DomainAddressBook.getIdentifier(domainUid));
		usersStoreService().create("test1", TestUser.of(domainUid).login("test1").hidden().build());
		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();

		ItemValue<VCard> vcard = domainBook.getComplete("test1");
		assertNull(vcard);

		usersStoreService().update("test1", TestUser.of(domainUid).login("test1").build());
		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();
		vcard = domainBook.getComplete("test1");
		assertNotNull(vcard);
	}

	@Test
	public void testCreateArchivedUser() throws ServerFault {
		IAddressBook domainBook = testContext.provider().instance(IAddressBook.class,
				DomainAddressBook.getIdentifier(domainUid));
		usersStoreService().create("test1", TestUser.of(domainUid).login("test1").archived().build());
		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();

		ItemValue<VCard> vcard = domainBook.getComplete("test1");
		assertNull(vcard);

		usersStoreService().update("test1", TestUser.of(domainUid).login("test1").build());
		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();
		vcard = domainBook.getComplete("test1");
		assertNotNull(vcard);

		usersStoreService().update("test1", TestUser.of(domainUid).login("test1").archived().build());
		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();

		vcard = domainBook.getComplete("test1");
		assertNull(vcard);

		usersStoreService().delete("test1");
		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();
		vcard = domainBook.getComplete("test1");
		assertNull(vcard);

	}

	@Test
	public void testSimple() throws ServerFault {
		IAddressBook domainBook = testContext.provider().instance(IAddressBook.class,
				DomainAddressBook.getIdentifier(domainUid));
		usersStoreService().create("test1", TestUser.of(domainUid).login("test1").build());
		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();

		ItemValue<VCard> vcard = domainBook.getComplete("test1");
		assertNotNull(vcard);

		usersStoreService().update("test1", TestUser.of(domainUid).login("test1").names("chocobo", "dor").build());
		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();
		vcard = domainBook.getComplete("test1");
		assertNotNull(vcard);
		assertEquals("dor", vcard.value.identification.name.familyNames);

		usersStoreService().delete("test1");
		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();
		vcard = domainBook.getComplete("test1");
		assertNull(vcard);
	}

	@Test
	public void testGroupVCardWithArchivedMember() throws ServerFault, InterruptedException, SQLException {
		IAddressBook domainBook = testContext.provider().instance(IAddressBook.class,
				DomainAddressBook.getIdentifier(domainUid));

		String uid = UUID.randomUUID().toString();
		groupsStoreService().create(uid, TestGroup.of(domainUid).build());

		usersStoreService().create("test1", TestUser.of(domainUid).login("test1").build());
		groupsStoreService().create("test2", TestGroup.of(domainUid).name("testG").build());
		usersStoreService().create("test3", TestUser.of(domainUid).login("test3").archived().build());

		groupsStoreService().addMembers(uid,
				Arrays.asList(Member.user("test1"), Member.group("test2"), Member.user("test3")));

		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();

		ItemValue<VCard> vcard = domainBook.getComplete(uid);
		assertNotNull(vcard);

		// 1 user and 1 groups ( and 1 archived user)
		assertEquals(2, vcard.value.organizational.member.size());

		// archived user
		ItemValue<User> user = testContext.provider().instance(IUser.class, domainUid).getComplete("test3");
		assertNotNull(user);
		user.value.archived = false;

		usersStoreService().update("test3", user.value);
		// emulate UserInGroupHook
		groupsStoreService().update(uid, groupsStoreService().get(uid).value);

		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();

		vcard = domainBook.getComplete(uid);

		// 2 user and 1 groups
		assertEquals(3, vcard.value.organizational.member.size());
	}

	@Test
	public void testGroupHidden() throws ServerFault, InterruptedException, SQLException {
		IAddressBook domainBook = testContext.provider().instance(IAddressBook.class,
				DomainAddressBook.getIdentifier(domainUid));

		String uid = UUID.randomUUID().toString();
		Group group = TestGroup.of(domainUid).build();
		groupsStoreService().create(uid, group);

		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();

		ItemValue<VCard> vcard = domainBook.getComplete(uid);
		assertNotNull(vcard);

		group.hidden = true;
		groupsStoreService().update(uid, group);

		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();

		vcard = domainBook.getComplete(uid);
		assertNull(vcard);

	}

	@Test
	public void testModifyVCardOnPublish() throws ServerFault {
		IAddressBook domainBook = testContext.provider().instance(IAddressBook.class,
				DomainAddressBook.getIdentifier(domainUid));
		usersStoreService().create("test1",
				TestUser.of(domainUid).login("test1").alias("test-private@" + domainUid).build());
		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();

		ItemValue<VCard> vcard = domainBook.getComplete("test1");
		assertNotNull(vcard);
		Optional<Email> privateEmail = vcard.value.communications.emails.stream()
				.filter(e -> e.value.equals("test-private@" + domainUid)).findFirst();
		assertFalse(privateEmail.isPresent());

		usersStoreService().update("test1",
				TestUser.of(domainUid).login("test1").alias("notprivate@" + domainUid).build());
		testContext.provider().instance(IDomainAddressBook.class, domainUid).sync();
		vcard = domainBook.getComplete("test1");
		assertNotNull(vcard);
		privateEmail = vcard.value.communications.emails.stream()
				.filter(e -> e.value.equals("test-private@" + domainUid)).findFirst();
		assertFalse(privateEmail.isPresent());
		privateEmail = vcard.value.communications.emails.stream().filter(e -> e.value.equals("notprivate@" + domainUid))
				.findFirst();
		assertTrue(privateEmail.isPresent());
	}

	private ContainerUserStoreService usersStoreService() {
		return new ContainerUserStoreService(testContext, dirContainer, domain);
	}

	private ContainerGroupStoreService groupsStoreService() {
		return new ContainerGroupStoreService(testContext, dirContainer, domain);
	}

}
