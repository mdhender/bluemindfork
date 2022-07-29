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
package net.bluemind.calendar.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.addressbook.domainbook.IDomainAddressBook;
import net.bluemind.backend.cyrus.CyrusAdmins;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.calendar.api.CalendarLookupResponse;
import net.bluemind.calendar.api.ICalendarAutocomplete;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.service.internal.CalendarAutocompleteService;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import net.bluemind.user.persistence.UserSubscriptionStore;

public class CalendarAutocompleteServiceTests {
	private static final String DOMAIN = "test.lan";
	private BmContext testContext;
	protected SecurityContext defaultSecurityContext;
	private ContainerStore containerStore;
	private Server imapServer;
	private SecurityContext adminSecurityContext;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		ElasticsearchTestHelper.getInstance().beforeTest();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		this.imapServer = new Server();
		imapServer.ip = new BmConfIni().get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, imapServer);
		PopulateHelper.createTestDomain(DOMAIN, esServer, imapServer);

		this.createCyrusPartition(imapServer, DOMAIN);

		PopulateHelper.addUser("test", DOMAIN);
		defaultSecurityContext = new SecurityContext("testUser", "test", Arrays.<String>asList(),
				Arrays.<String>asList("hasSimpleVideoconferencing"), DOMAIN);

		Sessions.get().put(defaultSecurityContext.getSessionId(), defaultSecurityContext);

		adminSecurityContext = new SecurityContext("testAdmin", "testAdmin", Arrays.<String>asList(),
				Arrays.<String>asList(BasicRoles.ROLE_ADMIN, "hasSimpleVideoconferencing"), DOMAIN);

		Sessions.get().put(adminSecurityContext.getSessionId(), adminSecurityContext);
		testContext = new BmTestContext(SecurityContext.SYSTEM);
		containerStore = new ContainerStore(testContext, JdbcTestHelper.getInstance().getMailboxDataDataSource(),
				defaultSecurityContext);
		ContainerStore dirContainerStore = new ContainerStore(testContext, JdbcTestHelper.getInstance().getDataSource(),
				defaultSecurityContext);

		Container container = Container.create("addressbook_testUser", "addressbook", "Contacts",
				defaultSecurityContext.getSubject(), DOMAIN, true);
		container = containerStore.create(container);
		assertNotNull(container);

		AclStore aclStore = new AclStore(testContext, DataSourceRouter.get(testContext, container.uid));
		aclStore.store(container,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));

		UserSubscriptionStore userSubscriptionStore = new UserSubscriptionStore(SecurityContext.SYSTEM,
				JdbcTestHelper.getInstance().getDataSource(), dirContainerStore.get(DOMAIN));

		userSubscriptionStore.subscribe(defaultSecurityContext.getSubject(), container);
	}

	private void createCyrusPartition(final Server imapServer, final String domainUid) {
		final CyrusService cyrusService = new CyrusService(imapServer.ip);
		cyrusService.createPartition(domainUid);
		cyrusService.refreshPartitions(Arrays.asList(domainUid));
		new CyrusAdmins(
				ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class, "default"),
				imapServer.ip).write();
		cyrusService.reload();
	}

	protected Container createTestContainer(String uid, String name, Verb verb, boolean isDefault) throws Exception {

		Container container = Container.create(uid, ICalendarUids.TYPE, name, defaultSecurityContext.getSubject(),
				DOMAIN, isDefault);
		container = containerStore.create(container);
		assertNotNull(container);

		AclStore aclStore = new AclStore(new BmTestContext(SecurityContext.SYSTEM),
				DataSourceRouter.get(new BmTestContext(SecurityContext.SYSTEM), container.uid));
		aclStore.store(container, Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), verb)));

		return container;
	}

	public static void main(String[] args) {
		String s = "Clâir Tùtù";
		System.out.println(java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", ""));
	}

	protected void createTestUser(String uid, String firstname, String lastname, Verb verb)
			throws SQLException, ServerFault {
		IUser users = testContext.provider().instance(IUser.class, DOMAIN);

		String f = java.text.Normalizer.normalize(firstname, java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
		String l = java.text.Normalizer.normalize(lastname, java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
		User user = defaultUser((l + f).toLowerCase(), lastname, firstname);
		users.create(uid, user);

		ContainerStore cs = new ContainerStore(new BmTestContext(adminSecurityContext),
				DataSourceRouter.get(new BmTestContext(adminSecurityContext), ICalendarUids.defaultUserCalendar(uid)),
				adminSecurityContext);
		Container cal = cs.get(ICalendarUids.defaultUserCalendar(uid));
		assertNotNull("user default calendar not found ", cal);

		AclStore aclStore = new AclStore(new BmTestContext(SecurityContext.SYSTEM),
				DataSourceRouter.get(new BmTestContext(SecurityContext.SYSTEM), cal.uid));
		aclStore.store(cal, Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), verb)));
	}

	protected String createTestExternalUser(String name, String email) {
		return PopulateHelper.addExternalUser(DOMAIN, email, name);
	}

	protected void createTestGroup(String uid, String name, String... membersUid) throws SQLException, ServerFault {
		Member[] members = Arrays.stream(membersUid).map(m -> Member.user(m)).toArray(Member[]::new);
		createTestGroup(uid, name, members);
	}

	protected void createTestGroup(String uid, String name, Member... members) throws SQLException, ServerFault {
		createTestGroup(uid, name, false, false, members);
	}

	protected void createTestGroup(String uid, String name, boolean hidden, boolean hiddenMembers, Member... members)
			throws SQLException, ServerFault {
		IGroup groups = testContext.provider().instance(IGroup.class, DOMAIN);
		Group g = new Group();
		g.name = name;
		// server imap uid
		g.dataLocation = this.imapServer.ip;
		g.hidden = hidden;
		g.hiddenMembers = hiddenMembers;
		groups.create(uid, g);

		groups.add(uid, Arrays.asList(members));
	}

	private User defaultUser(String login, String lastname, String firstname) {
		net.bluemind.user.api.User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + DOMAIN;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.none;
		VCard card = new VCard();
		card.identification.name = Name.create(lastname, firstname, null, null, null, null);
		card.identification.formatedName = VCard.Identification.FormatedName.create(firstname + " " + lastname,
				Arrays.<VCard.Parameter>asList());
		user.contactInfos = card;
		return user;
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
	}

	public ICalendarAutocomplete getService(SecurityContext context) throws ServerFault {
		return new CalendarAutocompleteService(new BmTestContext(context));
	}

	@Test
	public void calendarLookupAsAdmin() throws Exception {
		ICalendarAutocomplete service = getService(adminSecurityContext);
		String uid1 = UUID.randomUUID().toString();
		createTestUser(uid1, "David", "Phan", Verb.Read);
		List<CalendarLookupResponse> res = service.calendarLookup("david", Verb.Read);
		assertEquals(0, res.size());
	}

	@Test
	public void calendarLookup() throws Exception {
		ICalendarAutocomplete service = getService(defaultSecurityContext);
		List<CalendarLookupResponse> res = service.calendarLookup("david", Verb.Read);
		assertEquals(0, res.size());

		String uid1 = UUID.randomUUID().toString();
		createTestUser(uid1, "David", "Phan", Verb.Read);
		res = service.calendarLookup("david", Verb.Read);
		assertEquals(1, res.size());
		assertEquals(ICalendarUids.defaultUserCalendar(uid1), res.get(0).uid);

		res = service.calendarLookup("david", Verb.Write);
		assertEquals(0, res.size());

		// stupid case? two default calendars
		String uid2 = UUID.randomUUID().toString();
		createTestContainer(uid2, "John Bang", Verb.Read, false);
		res = service.calendarLookup("john", Verb.Read);
		assertEquals(1, res.size());

		// test sort
		createTestUser(UUID.randomUUID().toString(), "david", "beckham", Verb.Read);
		createTestUser(UUID.randomUUID().toString(), "david", "hasselhoff", Verb.Read);

		res = service.calendarLookup("david", Verb.Read);
		assertEquals(3, res.size());

		assertEquals("david beckham", res.get(0).name);
		assertEquals("david hasselhoff", res.get(1).name);
		assertEquals("David Phan", res.get(2).name);

		// test %name%
		createTestUser(UUID.randomUUID().toString(), "jean-louis", "david", Verb.Read);
		res = service.calendarLookup("david", Verb.Read);
		assertEquals(4, res.size());
		assertEquals("david beckham", res.get(0).name);
		assertEquals("david hasselhoff", res.get(1).name);
		assertEquals("David Phan", res.get(2).name);
		assertEquals("jean-louis david", res.get(3).name);

		createTestUser(UUID.randomUUID().toString(), "Claîre", "Bùté", Verb.Read);
		res = service.calendarLookup("but", Verb.Read);
		assertEquals(1, res.size());

		// test limit
		for (int i = 0; i < 11; i++) {
			createTestUser(UUID.randomUUID().toString(), "david", "" + i, Verb.Read);
		}
		res = service.calendarLookup("david", Verb.Read);
		assertEquals(10, res.size());
	}

	@Test
	public void userCreatedCalendarLookup() throws Exception {
		ICalendarAutocomplete service = getService(defaultSecurityContext);

		// user created calendar
		String uid = UUID.randomUUID().toString();
		createTestContainer(uid, "perso", Verb.Read, false);

		List<CalendarLookupResponse> res = service.calendarLookup("perso", Verb.Read);
		assertEquals(1, res.size());
		assertEquals(uid, res.get(0).uid);
	}

	@Test
	public void calendarGroupLookup() throws Exception {
		ICalendarAutocomplete service = getService(defaultSecurityContext);
		List<CalendarLookupResponse> res = service.calendarLookup("david", Verb.Read);
		assertEquals(0, res.size());

		createTestUser("u1", "david", "phan", Verb.Read);

		createTestUser("u2", "John", "Bang", Verb.Read);
		createTestUser("u3", "daivd", "Gilmour", Verb.Invitation);

		createTestUser("u4", "Ma", "Cache", Verb.Read);

		createTestGroup("g2", "David hidden band", true, false, Member.user("u4"));

		createTestUser("u5", "Ma", "coumba", Verb.Read);

		createTestGroup("g3", "David Kiss Group", false, true, Member.user("u5"));

		createTestUser("u6", "Ma", "rrouchka", Verb.Read);

		createTestGroup("g4", "David sub goup", "u6");

		createTestGroup("g1", "David Gilmour all band", Member.user("u1"), Member.user("u2"), Member.user("u3"),
				Member.group("g2"), Member.group("g3"), Member.group("g4"));
		testContext.provider().instance(IDomainAddressBook.class, DOMAIN).sync();

		res = service.calendarLookup("david", Verb.Read);
		assertEquals(3, res.size());

		CalendarLookupResponse att1 = res.get(0);
		CalendarLookupResponse att2 = res.get(1);
		assertEquals("g1", att1.uid);
		assertEquals(ICalendarUids.defaultUserCalendar("u1"), att2.uid);

		// u1 + u2 + g4.u6 (u3 is not Readable, g2 and g3 are hidden)
		assertEquals(3, service.calendarGroupLookup(att1.uid).size());
	}

	@Test
	public void verb() throws Exception {
		ICalendarAutocomplete service = getService(defaultSecurityContext);
		List<CalendarLookupResponse> res = service.calendarLookup("david", Verb.Read);
		assertEquals(0, res.size());

		String uid1 = UUID.randomUUID().toString();
		createTestUser(uid1, "David", "Phan", Verb.Write);
		res = service.calendarLookup("david", Verb.Read);
		assertEquals(1, res.size());
		assertEquals(ICalendarUids.defaultUserCalendar(uid1), res.get(0).uid);

		String uid2 = UUID.randomUUID().toString();
		createTestUser(uid2, "John", "Bang", Verb.Write);
		res = service.calendarLookup("john", Verb.Invitation);
		assertEquals(1, res.size());
		assertEquals(ICalendarUids.defaultUserCalendar(uid2), res.get(0).uid);
	}

	@Test
	public void externalUsersNotCountedWhenLookingForGroup() throws Exception {
		ICalendarAutocomplete service = getService(defaultSecurityContext);

		createTestUser("user1", "John", "Bang", Verb.Read);
		createTestUser("user2", "Johnny", "B", Verb.Read);
		String extUserUid = createTestExternalUser("ExtUserName", "ext@user.com");
		String groupUid = "myGroup";
		createTestGroup(groupUid, "zorg", false, false, Member.user("user1"), Member.user("user2"),
				Member.externalUser(extUserUid));

		List<CalendarLookupResponse> res = service.calendarLookup("zorg", Verb.Invitation);
		assertEquals(2, res.get(0).memberCount);
	}

}
