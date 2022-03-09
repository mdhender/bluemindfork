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
package net.bluemind.user.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.AddressBookBusAddresses;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.addressbook.domainbook.DomainAddressBook;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.core.utils.ValidationResult;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.group.api.Group;
import net.bluemind.group.persistence.GroupStore;
import net.bluemind.group.service.internal.ContainerGroupStoreService;
import net.bluemind.imap.QuotaInfo;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.role.api.DefaultRoles;
import net.bluemind.role.service.IInternalRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.ChangePassword;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;
import net.bluemind.user.persistence.UserStore;
import net.bluemind.user.persistence.security.HashAlgorithm;
import net.bluemind.user.persistence.security.HashFactory;
import net.bluemind.user.service.internal.ContainerUserStoreService;
import net.bluemind.user.service.internal.UserDefaultImage;
import net.bluemind.user.service.internal.UserService;

public class UserServiceTests {

	private UserStore userStore;
	private ItemStore userItemStore;

	protected String domainUid;
	private SecurityContext domainAdminSecurityContext;

	protected Container userContainer;

	private IServer serverService;

	private ItemValue<Server> dataLocation;
	private ContainerStore containerHome;
	private BmTestContext testContext;
	private SecurityContext userAdminSecurityContext;
	private SecurityContext userSecurityContext;
	private ContainerUserStoreService userStoreService;
	private ItemValue<Domain> domain;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		domainUid = "dom" + System.currentTimeMillis() + ".test";
		String sid = "sid" + System.currentTimeMillis();

		domainAdminSecurityContext = BmTestContext
				.contextWithSession(sid, "admin@" + domainUid, domainUid, SecurityContext.ROLE_ADMIN)
				.getSecurityContext();

		userAdminSecurityContext = BmTestContext.contextWithSession("sid2" + System.currentTimeMillis(),
				"useradmin@" + domainUid, domainUid, BasicRoles.ROLE_MANAGE_USER).getSecurityContext();

		userSecurityContext = BmTestContext
				.contextWithSession("sid3" + System.currentTimeMillis(), "user@" + domainUid, domainUid)
				.getSecurityContext();

		containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				domainAdminSecurityContext);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		Server server = new Server();
		server.ip = "prec";
		server.tags = Lists.newArrayList("blue/job", "ur/anus");

		String cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, server, imapServer);
		domain = PopulateHelper.createTestDomain(domainUid, esServer, imapServer);

		// create domain parititon on cyrus
		new CyrusService(cyrusIp).createPartition(domainUid);
		new CyrusService(cyrusIp).refreshPartitions(Arrays.asList(domainUid));
		new CyrusService(cyrusIp).reload();

		PopulateHelper.addUserWithRoles("useradmin", userAdminSecurityContext.getContainerUid(),
				BasicRoles.ROLE_MANAGE_USER);

		PopulateHelper.addUserWithRoles("user", userSecurityContext.getContainerUid());

		PopulateHelper.domainAdmin(domainUid, domainAdminSecurityContext.getSubject());

		PopulateHelper.addOrgUnit(domainUid, "fr", "France", null);
		PopulateHelper.addOrgUnit(domainUid, "tlse", "Toulouse", "fr");
		PopulateHelper.addOrgUnit(domainUid, "prs", "Paris", "fr");
		userContainer = containerHome.get(domainUid);
		assertNotNull(userContainer);

		serverService = ServerSideServiceProvider.getProvider(domainAdminSecurityContext).instance(IServer.class,
				InstallationId.getIdentifier());

		dataLocation = serverService.getComplete(cyrusIp);
		System.err.println("srv: " + dataLocation.value.fqdn + ", uid: " + dataLocation.uid);

		userStore = new UserStore(JdbcActivator.getInstance().getDataSource(), containerHome.get(domainUid));
		userItemStore = new ItemStore(JdbcActivator.getInstance().getDataSource(), containerHome.get(domainUid),
				domainAdminSecurityContext);

		testContext = new BmTestContext(SecurityContext.SYSTEM);

		userStoreService = new ContainerUserStoreService(testContext, containerHome.get(domainUid),
				testContext.provider().instance(IDomains.class).get(domainUid));
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IUser getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);
	}

	@Test
	public void testBMHiddenSysadmin() throws ServerFault {
		// FIXME test what ?!?
		List<String> uids = getService(domainAdminSecurityContext).allUids();
		assertTrue(uids.size() >= 1);
		assertTrue(uids.contains("bmhiddensysadmin"));
	}

	@Test
	public void testCreate() throws ServerFault, SQLException {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		String uid = login;
		getService(domainAdminSecurityContext).create(uid, user);

		Item item = userItemStore.get(uid);
		assertNotNull(item);
		User created = userStore.get(item);
		assertNotNull(created);

		ItemValue<User> full = userStoreService.get(uid);
		assertUserEquals(user, full.value);
		assertNotNull(full.value.password);
		assertNotNull(full.value.passwordLastChange);
		assertFalse(full.value.passwordMustChange);
		assertFalse(full.value.passwordNeverExpires);

		// user mailbox is created
		assertNotNull(testContext.provider().instance(IMailboxes.class, domainUid).getComplete(uid));

		// vcard is created too
		ItemValue<VCard> vcard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);
		assertNotNull(vcard);
	}

	@Test
	public void testRestoreCreate() throws ServerFault, SQLException, ParseException {
		String mailboxCopyGuid = UUID.randomUUID().toString();
		String login = "test." + System.nanoTime();
		String uid = login;
		User user = defaultUser(login);
		user.mailboxCopyGuid = mailboxCopyGuid;
		Item item = new Item();
		item.id = 73;
		item.uid = login;
		item.externalId = "externalId" + System.nanoTime();
		item.displayName = "test";
		item.created = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:44:21");
		item.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		item.version = 17;

		ItemValue<User> userItem = ItemValue.create(item, user);
		getService(domainAdminSecurityContext).restore(userItem, true);

		Item createdItem = userItemStore.get(uid);
		assertNotNull(createdItem);
		User createdUser = userStore.get(item);
		assertNotNull(createdUser);

		ItemValue<User> full = userStoreService.get(uid);
		assertUserEquals(user, full.value);
		assertEquals(mailboxCopyGuid, user.mailboxCopyGuid);
		assertItemEquals(userItem.item(), full.item());
		// after creation, user item is touched 2 times:
		// - UserService.createWithItem: setMailboxFilter
		// - CyrusIdentityHook.refreshCyrusSieve: setMailboxFilter
		assertEquals(item.version + 2, full.item().version);
		assertNotNull(full.value.password);
		assertNotNull(full.value.passwordLastChange);
		assertFalse(full.value.passwordMustChange);
		assertFalse(full.value.passwordNeverExpires);

		// user mailbox is created
		assertNotNull(testContext.provider().instance(IMailboxes.class, domainUid).getComplete(uid));

		// vcard is created too
		ItemValue<VCard> vcard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);
		assertNotNull(vcard);
	}

	@Test
	public void testCreateShouldapplyDefaultUserQuota() throws ServerFault {
		IDomainSettings settingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settings = settingsService.get();
		settings.put("mailbox_default_user_quota", "5000");
		settingsService.set(settings);

		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		String uid = login;
		getService(domainAdminSecurityContext).create(uid, user);

		ItemValue<User> full = userStoreService.get(uid);
		assertEquals(5000, full.value.quota.intValue());

		ItemValue<Mailbox> mbox = testContext.provider().instance(IMailboxes.class, domainUid).getComplete(uid);
		try (StoreClient sc = new StoreClient(new BmConfIni().get("imap-role"), 1143, "admin0", Token.admin0())) {
			sc.login();
			QuotaInfo quota = sc.quota("user/" + mbox.value.name + "@" + domainUid);
			assertEquals(quota.getLimit(), 5000);
		}
	}

	@Test
	public void testCreateWithExtId() throws ServerFault, SQLException {
		String uid = UUID.randomUUID().toString();
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		String externalId = "external-" + user.login;
		getService(domainAdminSecurityContext).createWithExtId(uid, externalId, user);

		Item item = userItemStore.get(uid);
		assertNotNull(item);
		assertEquals(externalId, item.externalId);
		User created = userStore.get(item);
		assertNotNull(created);

		ItemValue<User> full = userStoreService.get(uid);
		assertUserEquals(user, full.value);

		// user mailbox is created
		assertNotNull(testContext.provider().instance(IMailboxes.class, domainUid).getComplete(uid));

		// vcard is created too
		ItemValue<VCard> vcard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);
		assertNotNull(vcard);
	}

	@Test
	public void testVCard() throws ServerFault, InterruptedException, SQLException {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		String uid = login;

		user.emails = new ArrayList<>(user.emails);
		user.emails.add(Email.create("aaa@" + domainUid, false, true));
		getService(domainAdminSecurityContext).create(uid, user);

		// vcard is created too
		ItemValue<VCard> vcard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);
		assertNotNull(vcard);

		assertEquals(3, vcard.value.communications.emails.size());
		System.err.println("!!! " + vcard.value.communications.emails);
	}

	private void setDomainMaxUsers(String domainUid, int maxUsers) throws ServerFault {
		IDomainSettings settingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.domain_max_users.name(), "" + maxUsers);
		settingsService.set(settings);
	}

	private void setDomainMaxBasicUsers(String domainUid, int maxUsers) throws ServerFault {
		IDomainSettings settingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.domain_max_basic_account.name(), "" + maxUsers);
		settingsService.set(settings);
	}

	@Test
	public void testCreateMaxReached() throws ServerFault, InterruptedException, SQLException {
		IDirectory service = testContext.su().provider().instance(IDirectory.class, domainUid);
		DirEntryQuery query = DirEntryQuery.filterKind(Kind.USER);
		query.systemFilter = true;
		query.size = 0;
		ListResult<ItemValue<DirEntry>> users = service.search(query);

		int maxUsers = (int) users.total + 1;
		setDomainMaxUsers(domainUid, maxUsers);

		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		String uid = login;
		getService(domainAdminSecurityContext).create(uid, user);

		login = "test." + System.nanoTime();
		user = defaultUser(login);
		uid = login;

		try {
			getService(domainAdminSecurityContext).create(uid, user);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.FORBIDDEN, sf.getCode());
			assertEquals("Maximum FULL accounts allowed (" + maxUsers + ") for domain " + domainUid
					+ " reached. Unable to create new one.", sf.getMessage());
		}
	}

	@Test
	public void testFilterArchivedUsersFromSearch() throws Exception {
		// FIXME should be in net.bluemind.addressbook.domainbook tests
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		String uid = create(user);
		VertxEventChecker<JsonObject> eventChecker = new VertxEventChecker<>(
				AddressBookBusAddresses.getChangedEventAddress(DomainAddressBook.getIdentifier(domainUid)));
		VertxEventChecker<String> syncChecker = new VertxEventChecker<>("domainbook.sync." + domainUid);

		user = getService(domainAdminSecurityContext).getComplete(uid).value;

		eventChecker.shouldSuccess();
		System.err.println("Waiting for sync");
		syncChecker.shouldSuccess();
		System.err.println("Got sync");

		IAddressBook abService = ServerSideServiceProvider.getProvider(domainAdminSecurityContext)
				.instance(IAddressBook.class, "addressbook_" + domainUid);
		ItemValue<VCard> vcard = abService.getComplete(uid);
		assertNotNull(vcard);

		eventChecker = new VertxEventChecker<>(
				AddressBookBusAddresses.getChangedEventAddress(DomainAddressBook.getIdentifier(domainUid)));
		syncChecker = new VertxEventChecker<>("domainbook.sync." + domainUid);
		user.archived = true;
		getService(domainAdminSecurityContext).update(uid, user);

		eventChecker.shouldSuccess();
		syncChecker.shouldSuccess();

		vcard = abService.getComplete(uid);
		assertNull(vcard);
	}

	@Test
	public void testCreate_Roles() throws Exception {
		// test simple user
		try {
			getService(userSecurityContext).create("test." + System.nanoTime(),
					defaultUser("test." + System.nanoTime()));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		// test with managerUser role
		try {
			getService(userAdminSecurityContext).create("" + System.nanoTime(),
					defaultUser("test." + System.nanoTime()));
		} catch (ServerFault e) {
			fail(e.getMessage());
		}

	}

	@Test
	public void testRestoreUpdate() throws ServerFault, SQLException, ParseException {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);

		ItemValue<User> created = getService(domainAdminSecurityContext).getComplete(uid);
		created.version += 2;
		created.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:48:00");
		getService(domainAdminSecurityContext).restore(created, false);

		Item updatedItem = userItemStore.get(uid);
		assertNotNull(updatedItem);
		User updatedUser = userStore.get(updatedItem);
		assertNotNull(updatedUser);
		assertEquals(user.login, updatedUser.login);

		ItemValue<User> updated = userStoreService.get(uid);
		assertUserEquals(user, updated.value);
		assertItemEquals(created.item(), updated.item());
		// after creation, user item is touched 2 times:
		// - UserService.updateWithItem: setMailboxFilter
		assertEquals(created.version + 1, updated.version);

		// user mailbox is created
		assertNotNull(testContext.provider().instance(IMailboxes.class, domainUid).getComplete(uid));

		// vcard is created too
		ItemValue<VCard> vcard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);
		assertNotNull(vcard);
	}

	@Test
	public void testUpdate() throws ServerFault, SQLException {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);
		Date passwordUpdated = getPasswordLastChange(user);
		String mailboxCopyGuid = UUID.randomUUID().toString();

		// flush dir events..
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
		user.login = "update" + System.currentTimeMillis();
		user.emails = Arrays.asList(Email.create(user.login + "@" + domainUid, true));
		user.mailboxCopyGuid = mailboxCopyGuid;

		getService(domainAdminSecurityContext).update(uid, user);

		Item item = userItemStore.get(uid);
		assertNotNull(item);
		User updated = userStore.get(item);
		assertNotNull(updated);
		assertEquals(user.login, updated.login);

		// Check mailboxCopyGuid update
		assertEquals(mailboxCopyGuid, user.mailboxCopyGuid);

		// Check password from store
		assertNotNull(updated.password);
		assertEquals(passwordUpdated, updated.passwordLastChange);
		assertFalse(updated.passwordMustChange);
		assertFalse(updated.passwordNeverExpires);

		ItemValue<User> itemValue = getService(domainAdminSecurityContext).getComplete(uid);
		assertUserEquals(user, itemValue.value);
		assertEquals(1, itemValue.value.contactInfos.communications.emails.size());

		// Check password from service
		assertNull(itemValue.value.password);
		assertEquals(passwordUpdated, itemValue.value.passwordLastChange);
		assertFalse(itemValue.value.passwordMustChange);
		assertFalse(itemValue.value.passwordNeverExpires);

		// check direntry and vcard update

		// itemValue.value.contactInfos.communications.emails.add(VCard.Communications.Email.create("check@tot.com"));
		itemValue.value.emails = ImmutableList.<Email>builder().addAll(itemValue.value.emails)
				.add(Email.create("check@" + domainUid, false)).build();
		itemValue.value.contactInfos.identification.name = Name.create("Chan", "Jackie", null, null, null, null);

		getService(domainAdminSecurityContext).update(uid, itemValue.value);

		itemValue = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(2, itemValue.value.contactInfos.communications.emails.size());

		assertEquals("Chan", itemValue.value.contactInfos.identification.name.familyNames);
		assertEquals("Jackie", itemValue.value.contactInfos.identification.name.givenNames);

		// check two time to be sure that email DOES NOT ACCUMULATE
		itemValue = getService(domainAdminSecurityContext).getComplete(uid);

		// add bad email
		List<VCard.Communications.Email> emails = new ArrayList<>(itemValue.value.contactInfos.communications.emails);
		emails.add(VCard.Communications.Email.create(null));
		itemValue.value.contactInfos.communications.emails = emails;
		getService(domainAdminSecurityContext).update(uid, itemValue.value);
		itemValue = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(2, itemValue.value.contactInfos.communications.emails.size());
		assertNotNull(itemValue.value.contactInfos.communications.emails.get(0).getParameterValue("SYSTEM"));
		assertEquals("check@" + domainUid, itemValue.value.contactInfos.communications.emails.get(1).value);

		// Check password from store
		assertEquals(passwordUpdated, itemValue.value.passwordLastChange);
	}

	@Test
	public void testUpdate_Sec() throws Exception {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);

		// test with managerUser role
		try {
			getService(userAdminSecurityContext).update(uid, user);
		} catch (ServerFault e) {
			fail(e.getMessage());
		}

		// test simple user
		try {
			getService(userSecurityContext).update(uid, user);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

	}

	@Test
	public void testUpdating_UsingExternalRoutingWithInvalidSplitRelay_ShouldFail() throws Exception {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);

		user.routing = Routing.external;
		try {
			getService(userAdminSecurityContext).update(uid, user);
			fail();
		} catch (Exception e) {

		}
	}

	@Test
	public void testUpdateMaintainsPassword() throws ServerFault, SQLException {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		String uid = create(user);
		System.out.println("Password from bean: " + user.password);
		String prevPass = getPassword(user).split(":")[2];
		Date prevDate = getPasswordLastChange(user);
		user.password = null;

		getService(domainAdminSecurityContext).update(uid, user);
		String currentPass = getPassword(user).split(":")[2];
		assertEquals(currentPass, prevPass);

		Date currentDate = getPasswordLastChange(user);
		assertEquals(prevDate, currentDate);

		Item item = userItemStore.get(uid);
		assertNotNull(item);
		User updated = userStore.get(item);
		assertNotNull(updated);
		assertEquals(user.login, updated.login);

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS).update(uid, user);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testAddingANewUserShouldUsePBKDF2Algorithm() throws Exception {
		String password = "password";
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);

		String uid = create(user);
		user = userStore.get(userItemStore.get(uid));

		assertTrue(HashFactory.get(HashAlgorithm.PBKDF2).validate(password, user.password));
		assertFalse(HashFactory.get(HashAlgorithm.MD5).validate(password, user.password));
	}

	@Test
	public void testAddingANewUserWithKnownAlgorithmShouldKeepIt() throws Exception {
		String password = "{SSHA512}FDLTaAqeJLdNmsIjvqQchAKfkUFAESvyTYZBj9AM9AqGnPxK4oa2Imo5rnWNUmGJ55o0Q5wuCezY0PmwIth46805xHvxIpXs";
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		user.password = password;
		String uid = create(user);
		user = userStore.get(userItemStore.get(uid));
		assertEquals(HashFactory.algorithm(user.password), HashAlgorithm.SSHA512);
	}

	@Test
	public void testUpdatingPasswordShouldUsePBKDF2Algorithm() throws Exception {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);

		String uid = create(user);
		try (Connection con = JdbcTestHelper.getInstance().getDataSource().getConnection()) {
			try (Statement st = con.createStatement()) {
				st.executeUpdate(
						"UPDATE t_domain_user SET password = 'something', password_algorithm = 'MD5' WHERE login='"
								+ user.login + "'");
			}
		}
		getService(domainAdminSecurityContext).setPassword(uid, ChangePassword.create("newpw"));

		user = userStoreService.get(uid).value;

		assertEquals(HashFactory.DEFAULT, HashFactory.algorithm(user.password));
	}

	@Test
	public void testUpdatingPasswordWithKnownHashShouldKeepIt() throws Exception {
		String password = "{SSHA512}FDLTaAqeJLdNmsIjvqQchAKfkUFAESvyTYZBj9AM9AqGnPxK4oa2Imo5rnWNUmGJ55o0Q5wuCezY0PmwIth46805xHvxIpXs";
		User user = defaultUser("test.knownhash." + System.nanoTime());
		user.password = "scully";
		String uid = create(user);
		user = userStore.get(userItemStore.get(uid));
		assertEquals(HashFactory.algorithm(user.password), HashFactory.DEFAULT);
		getService(domainAdminSecurityContext).setPassword(uid, ChangePassword.create(password));
		user = userStoreService.get(uid).value;
		assertEquals(HashFactory.algorithm(user.password), HashAlgorithm.SSHA512);
	}

	@Test
	public void testASuccessfulCheckforThePasswordShouldUpdateHashIfNecessary() throws Exception {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);

		String pwHashMd5 = HashFactory.get(HashAlgorithm.MD5).create("pw");

		String uid = create(user);
		try (Connection con = JdbcTestHelper.getInstance().getDataSource().getConnection()) {
			try (Statement st = con.createStatement()) {
				st.executeUpdate("UPDATE t_domain_user SET password = '" + pwHashMd5
						+ "', password_algorithm = 'MD5' WHERE login='" + user.login + "'");
			}
		}
		UserService userService = (UserService) ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domainUid);
		userService.checkPassword(user.login, "pw");

		user = userStoreService.get(uid).value;

		assertEquals(HashFactory.DEFAULT, HashFactory.algorithm(user.password));
		assertTrue(HashFactory.getDefault().validate("pw", user.password));
	}

	@Test
	public void testValidateUsingExistingValuesShouldSucceed() throws ServerFault, SQLException {
		String login1 = "test." + System.nanoTime();
		User user1 = defaultUser(login1);
		String uid1 = create(user1);
		String login2 = "test." + System.nanoTime();
		User user2 = defaultUser(login2);
		String uid2 = create(user2);

		IInCoreUser userResolver = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInCoreUser.class, domainUid);
		ValidationResult validate = userResolver.validate(new String[] { uid1, uid2 });
		assertTrue(validate.valid);
	}

	@Test
	public void testValidateUsingAnNonExistingValueShouldFail() throws ServerFault, SQLException {
		String login1 = "test." + System.nanoTime();
		User user1 = defaultUser(login1);
		String uid1 = create(user1);
		String login2 = "test." + System.nanoTime();
		User user2 = defaultUser(login2);
		String uid2 = create(user2);

		IInCoreUser userResolver = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInCoreUser.class, domainUid);
		ValidationResult validate = userResolver.validate(new String[] { uid1, "i-dont-exist", uid2 });
		assertFalse(validate.valid);
		assertTrue(validate.validationResults.get(uid1));
		assertFalse(validate.validationResults.get("i-dont-exist"));
		assertTrue(validate.validationResults.get(uid2));
	}

	private String getPassword(User user) throws SQLException {
		String currentPass = null;
		try (Connection con = JdbcTestHelper.getInstance().getDataSource().getConnection()) {
			try (Statement st = con.createStatement()) {
				try (ResultSet rs = st
						.executeQuery("SELECT password FROM t_domain_user WHERE login='" + user.login + "'")) {
					if (rs.next()) {
						currentPass = rs.getString(1);
					}
				}
			}
		}
		return currentPass;
	}

	private Date getPasswordLastChange(User user) throws SQLException {
		Date passwordUpdate = null;
		try (Connection con = JdbcTestHelper.getInstance().getDataSource().getConnection()) {
			try (Statement st = con.createStatement()) {
				try (ResultSet rs = st.executeQuery(
						"SELECT password_lastchange FROM t_domain_user WHERE login='" + user.login + "'")) {
					if (rs.next()) {
						passwordUpdate = rs.getTimestamp(1) == null ? null : new Date(rs.getTimestamp(1).getTime());
					}
				}
			}
		}

		return passwordUpdate;
	}

	@Test
	public void testDelete() throws Exception {
		User user = defaultUser("admin" + System.nanoTime());

		String uid = UUID.randomUUID().toString();
		testContext.provider().instance(IUser.class, domainUid).create(uid, user);

		GroupStore groupStore = new GroupStore(JdbcActivator.getInstance().getDataSource(),
				containerHome.get(domainUid));

		ContainerGroupStoreService groupStoreService = new ContainerGroupStoreService(
				new BmTestContext(SecurityContext.SYSTEM), userContainer, domain);

		String g1Uid = UUID.randomUUID().toString();
		Group g1 = defaultGroup();
		groupStoreService.create(g1Uid, g1);

		String g2Uid = UUID.randomUUID().toString();
		Group g2 = defaultGroup();
		groupStoreService.create(g2Uid, g2);

		groupStore.addUsersMembers(groupStoreService.getItemStore().get(g1Uid),
				userItemStore.getMultiple(Arrays.asList(uid)));

		groupStore.addGroupsMembers(groupStoreService.getItemStore().get(g2Uid),
				userItemStore.getMultiple(Arrays.asList(g1Uid)));

		TaskRef tr = getService(domainAdminSecurityContext).delete(uid);
		waitEnd(tr);

		Item item = userItemStore.get(uid);
		assertNull(item);
	}

	@Test
	public void testDeletingAnUserDeletesAllSubscriptions() throws Exception {
		User user = defaultUser("admin" + System.nanoTime());

		String uid = UUID.randomUUID().toString();
		testContext.provider().instance(IUser.class, domainUid).create(uid, user);

		String cuid = UUID.randomUUID().toString();
		IContainers containerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainers.class);
		ContainerDescriptor desc = new ContainerDescriptor();
		desc.domainUid = domainUid;
		desc.defaultContainer = false;
		desc.name = uid;
		desc.owner = "system";
		desc.type = "type";
		containerService.create(cuid, desc);

		IUserSubscription sub = ServerSideServiceProvider.getProvider(domainAdminSecurityContext)
				.instance(IUserSubscription.class, domainUid);
		ContainerSubscription subscription = new ContainerSubscription();
		subscription.containerUid = cuid;
		sub.subscribe(uid, Arrays.asList(subscription));
		List<ContainerSubscriptionDescriptor> listSubscriptions = sub.listSubscriptions(uid, null);
		boolean found = false;
		for (ContainerSubscriptionDescriptor d : listSubscriptions) {
			if (d.containerUid.equals(cuid)) {
				found = true;
			}
		}
		assertTrue(found);

		TaskRef tr = getService(domainAdminSecurityContext).delete(uid);
		waitEnd(tr);

		listSubscriptions = sub.listSubscriptions(uid, null);
		assertTrue(listSubscriptions.isEmpty());
	}

	@Test
	public void testDelete_Sec() throws Exception {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);

		GroupStore groupStore = new GroupStore(JdbcActivator.getInstance().getDataSource(),
				containerHome.get(domainUid));

		ContainerGroupStoreService groupStoreService = new ContainerGroupStoreService(
				new BmTestContext(SecurityContext.SYSTEM), userContainer, domain);

		String g1Uid = UUID.randomUUID().toString();
		Group g1 = defaultGroup();
		groupStoreService.create(g1Uid, g1);

		groupStore.addUsersMembers(groupStoreService.getItemStore().get(g1Uid),
				userItemStore.getMultiple(Arrays.asList(uid)));

		// test simple user
		try {
			TaskRef tr = getService(userSecurityContext).delete(uid);
			waitEnd(tr);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		// test with managerUser role
		try {
			TaskRef tr = getService(userAdminSecurityContext).delete(uid);
			waitEnd(tr);
		} catch (ServerFault e) {
			fail(e.getMessage());
		}

	}

	@Test
	public void testGetComplete() throws ServerFault {
		User user = defaultUser("test" + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);

		ItemValue<User> userItem = getService(domainAdminSecurityContext).getComplete(uid);
		assertNotNull(userItem);
		assertEquals(uid, userItem.uid);
		assertUserEquals(user, userItem.value);

		assertNull(userItem.value.password);
		assertNotNull(userItem.value.passwordLastChange);
		assertFalse(userItem.value.passwordMustChange);

		userItem = getService(domainAdminSecurityContext).getComplete("nonExistant");
		assertNull(userItem);
	}

	@Test
	public void testGetComplete_Sec() throws Exception {
		User user = defaultUser("test" + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);

		// test simple user
		try {
			getService(userSecurityContext).getComplete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		// test with managerUser role
		try {
			getService(userAdminSecurityContext).getComplete(uid);
		} catch (ServerFault e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetComplete_Self() throws Exception {
		User user = defaultUser("test" + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);

		// test self retrieving
		try {
			getService(BmTestContext.contextWithSession("checkThat", uid, domainUid).getSecurityContext())
					.getComplete(uid);
		} catch (ServerFault e) {
			fail(e.getMessage());
		}
	}

	private String create(User user) {
		String uid = UUID.randomUUID().toString();
		try {
			getService(domainAdminSecurityContext).create(uid, user);
			return uid;
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		// FIXME should be in catch clause
		return null;
	}

	@Test
	public void testByEmail() throws ServerFault, SQLException {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);

		ItemValue<User> u = getService(domainAdminSecurityContext).byEmail(user.login + "@" + domainUid);
		assertNotNull(u);
		assertUserEquals(user, u.value);

		u = getService(domainAdminSecurityContext).byEmail("wtf@email.lan");
		assertNull(u);
	}

	@Test
	public void testByExtId() throws ServerFault, SQLException {
		User user = defaultUser("test." + System.nanoTime());

		String uid = UUID.randomUUID().toString();
		String externalId = "externalId";
		getService(domainAdminSecurityContext).createWithExtId(uid, externalId, user);

		ItemValue<User> u = getService(domainAdminSecurityContext).byExtId(externalId);
		assertNotNull(u);
		assertUserEquals(user, u.value);

		u = getService(domainAdminSecurityContext).byExtId("externalId doesn't exist");
		assertNull(u);
	}

	@Test
	public void testByInvalidExtId() throws ServerFault, SQLException {
		try {
			getService(domainAdminSecurityContext).byExtId(null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getService(domainAdminSecurityContext).byExtId("");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testByLogin() throws ServerFault, SQLException {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);

		ItemValue<User> u = getService(domainAdminSecurityContext).byLogin(user.login);
		assertNotNull(u);
		assertUserEquals(user, u.value);

		u = getService(domainAdminSecurityContext).byLogin("wtf");
		assertNull(u);
	}

	private User defaultUser(String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + domainUid;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.internal;
		user.contactInfos = defaultCard();
		user.dataLocation = dataLocation.uid;
		return user;
	}

	private VCard defaultCard() {
		VCard card = new VCard();
		card.identification.name = Name.create("Doe", "John", null, null, null, null);
		return card;
	}

	private Group defaultGroup() {
		Group group = new Group();
		group.name = "group-" + System.nanoTime();
		group.description = "Test group";

		Email e = new Email();
		e.address = group.name + "//@Test.foo";
		e.allAliases = true;
		e.isDefault = true;
		group.emails = Arrays.asList(e);

		return group;
	}

	@Test
	public void testMemberOf() throws ServerFault, SQLException {

		GroupStore groupStore = new GroupStore(JdbcActivator.getInstance().getDataSource(),
				containerHome.get(domainUid));

		ContainerGroupStoreService groupStoreService = new ContainerGroupStoreService(
				new BmTestContext(SecurityContext.SYSTEM), containerHome.get(domainUid), domain);

		String g1Uid = UUID.randomUUID().toString();
		Group g1 = defaultGroup();
		groupStoreService.create(g1Uid, g1);

		String g2Uid = UUID.randomUUID().toString();
		Group g2 = defaultGroup();
		groupStoreService.create(g2Uid, g2);

		String g3Uid = UUID.randomUUID().toString();
		Group g3 = defaultGroup();
		groupStoreService.create(g3Uid, g3);

		groupStore.addGroupsMembers(groupStoreService.getItemStore().get(g1Uid),
				groupStoreService.getItemStore().getMultiple(Arrays.asList(g2Uid)));

		groupStore.addGroupsMembers(groupStoreService.getItemStore().get(g1Uid),
				groupStoreService.getItemStore().getMultiple(Arrays.asList(g3Uid)));

		User user = defaultUser("test." + System.nanoTime());
		user.routing = Routing.none;
		String uid = create(user);

		groupStore.addUsersMembers(groupStoreService.getItemStore().get(g2Uid),
				userItemStore.getMultiple(Arrays.asList(uid)));

		List<ItemValue<Group>> memberOf = getService(domainAdminSecurityContext).memberOf(uid);
		assertEquals(2, memberOf.size());
		for (ItemValue<Group> g : memberOf) {
			assertFalse(g.uid.equals(g3Uid));
			assertTrue(g.uid.equals(g1Uid) || g.uid.equals(g2Uid));
		}
	}

	@Test
	public void testMemberOfInvalidUser() throws ServerFault {
		String invalidUserUid = UUID.randomUUID().toString();

		try {
			getService(domainAdminSecurityContext).memberOf(invalidUserUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertTrue(ErrorCode.NOT_FOUND.equals(sf.getCode()));
		}
	}

	@Test
	public void testSetRoles() throws ServerFault {

		User user = defaultUser("test." + System.nanoTime());
		user.orgUnitUid = "tlse";
		String uid = create(user);
		assertNotNull(uid);

		getService(domainAdminSecurityContext).setRoles(uid,
				new HashSet<>(Arrays.asList(BasicRoles.ROLE_MANAGE_GROUP, BasicRoles.ROLE_MANAGE_USER)));

		try {
			getService(SecurityContext.ANONYMOUS).setRoles(uid, new HashSet<String>());
			fail("only admin should be able to call setRoles");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			getService(domainAdminSecurityContext).setRoles("fakeUid", new HashSet<String>());
			fail("should failed because user doenst exist");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}

		try {
			getService(domainAdminSecurityContext).setRoles(uid,
					new HashSet<>(Arrays.asList(BasicRoles.ROLE_SYSTEM_MANAGER)));
			fail("you cannot delegate roles that you dont have");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		testContext.provider().instance(IUser.class, domainUid).setRoles(uid,
				new HashSet<>(Arrays.asList(BasicRoles.ROLE_SYSTEM_MANAGER)));
		try {
			getService(domainAdminSecurityContext).setRoles(uid,
					new HashSet<>(Arrays.asList(BasicRoles.ROLE_MANAGE_GROUP, BasicRoles.ROLE_SYSTEM_MANAGER)));
		} catch (ServerFault e) {
			fail("you can delegate roles that you dont have if already delegated");
		}

		try {
			getService(domainAdminSecurityContext).setRoles(uid,
					new HashSet<>(Arrays.asList(BasicRoles.ROLE_CALENDAR)));
		} catch (ServerFault e) {
			e.printStackTrace();
			fail("you should be able to delegate ROLE Calendar even if you doesnt have it");

		}

		SecurityContext userAdminOUTlseSecurityContext = BmTestContext
				.contextWithSession("sidtlse", "useradmin.tlse@" + domainUid, domainUid)
				.withRolesOnOrgUnit("tlse", BasicRoles.ROLE_MANAGE_USER).getSecurityContext();

		Sessions.get().put(userAdminOUTlseSecurityContext.getSessionId(), userAdminOUTlseSecurityContext);
		// check ou
		try {
			getService(userAdminOUTlseSecurityContext).setRoles(uid,
					new HashSet<>(Arrays.asList(BasicRoles.ROLE_CALENDAR)));
		} catch (ServerFault e) {
			e.printStackTrace();
			fail("you should be able to delegate ROLE Calendar even if you doesnt have it");

		}

		try {
			getService(userAdminOUTlseSecurityContext).setRoles(uid,
					new HashSet<>(Arrays.asList(BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES)));
		} catch (ServerFault e) {
			e.printStackTrace();
			fail("you should be able to delegate SELF ROLE ROLE_SELF_CHANGE_MAIL_IDENTITIES even if you doesnt have it (given by OU BasicRoles.ROLE_MANAGE_MAILBOX_IDENTITIES ");

		}

		try {
			getService(userAdminOUTlseSecurityContext).setRoles(uid,
					new HashSet<>(Arrays.asList(BasicRoles.ROLE_MANAGE_MAILBOX_IDENTITIES)));
			fail("you should  not be able to delegate roles you doesnt have");
		} catch (ServerFault e) {
		}

		// wrong ou
		SecurityContext userAdminOUParisSecurityContext = BmTestContext
				.contextWithSession("sidprs", "useradmin.prs@" + domainUid, domainUid)
				.withRolesOnOrgUnit("prs", BasicRoles.ROLE_MANAGE_USER).getSecurityContext();

		try {
			getService(userAdminOUParisSecurityContext).setRoles(uid,
					new HashSet<>(Arrays.asList(BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES)));
			fail("you should  not be able to delegate roles you doesnt have");
		} catch (ServerFault e) {
		}

	}

	@Test
	public void testCustomProperties() throws ServerFault {

		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		String uid = login;
		getService(domainAdminSecurityContext).create(uid, user);

		ItemValue<User> created = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(0, created.value.properties.size());

		Map<String, String> properties = new HashMap<String, String>();
		user.properties = properties;
		getService(domainAdminSecurityContext).update(uid, user);
		created = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(0, created.value.properties.size());

		properties.put("custom prop", "yeah");
		user.properties = properties;
		getService(domainAdminSecurityContext).update(uid, user);
		created = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(1, created.value.properties.size());
		assertEquals("yeah", created.value.properties.get("custom prop"));

		properties.put("another custom prop", "yeah yeah");
		user.properties = properties;
		getService(domainAdminSecurityContext).update(uid, user);
		created = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(2, created.value.properties.size());
		assertEquals("yeah", created.value.properties.get("custom prop"));
		assertEquals("yeah yeah", created.value.properties.get("another custom prop"));

	}

	public TaskStatus waitEnd(TaskRef ref) throws Exception {
		TaskStatus status = null;
		while (true) {
			ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, ref.id);
			status = task.status();
			if (status.state.ended) {
				break;
			}
		}

		return status;
	}

	@Test
	public void testDeleteAdmin0() throws Exception {
		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		IUser service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				"global.virt");
		ItemValue<User> u = service.byLogin("admin0");
		assertNotNull(u);

		TaskRef tr = service.delete(u.uid);
		waitEnd(tr);

		u = service.byLogin("admin0");
		assertNotNull(u);
	}

	@Test
	public void testSetPassword() throws Exception {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);

		ItemValue<User> userItemValue = getService(domainAdminSecurityContext).getComplete(uid);
		assertFalse(userItemValue.value.passwordMustChange);
		Date passwordTime = userItemValue.value.passwordLastChange;

		getService(domainAdminSecurityContext).setPassword(uid, ChangePassword.create("checkpass"));
		assertTrue(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreUser.class, domainUid)
				.checkPassword(user.login, "checkpass"));

		userItemValue = getService(domainAdminSecurityContext).getComplete(uid);
		assertTrue(passwordTime.before(userItemValue.value.passwordLastChange));

		getService(domainAdminSecurityContext).setPassword(uid, ChangePassword.create("checkpass", "np"));

		try {
			getService(domainAdminSecurityContext).setPassword(uid, ChangePassword.create("checkpass", "np"));
			fail("should fail because old pass is not good");
		} catch (ServerFault e) {
		}

		userItemValue.value.passwordMustChange = true;
		getService(domainAdminSecurityContext).update(userItemValue.uid, userItemValue.value);
		getService(domainAdminSecurityContext).setPassword(uid, ChangePassword.create("np", "checkpass"));

		userItemValue = getService(domainAdminSecurityContext).getComplete(uid);
		assertFalse(userItemValue.value.passwordMustChange);
	}

	@Test
	public void passwordUpdateNeeded() throws SQLException {
		String userLogin = "test." + System.nanoTime();
		User user = defaultUser(userLogin);
		String uid = create(user);
		assertNotNull(uid);

		user.passwordMustChange = true;
		getService(domainAdminSecurityContext).update(uid, user);

		assertTrue(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreUser.class, domainUid)
				.passwordUpdateNeeded(user.login));
		Set<String> roles = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInCoreUser.class, domainUid).getResolvedRoles(uid);
		assertEquals(1, roles.size());
		assertEquals(BasicRoles.ROLE_SELF_CHANGE_PASSWORD, roles.iterator().next());

		user.passwordMustChange = false;
		getService(domainAdminSecurityContext).update(uid, user);

		// Password expiration disabled
		assertFalse(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreUser.class, domainUid)
				.passwordUpdateNeeded(user.login));

		// Enable password expiration
		Map<String, String> domainSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid).get();
		domainSettings.put(DomainSettingsKeys.password_lifetime.name(), "10");
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domainUid)
				.set(domainSettings);

		assertFalse(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreUser.class, domainUid)
				.passwordUpdateNeeded(user.login));

		// Password lastchange to null
		Connection conn = null;
		PreparedStatement st = null;
		try {
			conn = JdbcTestHelper.getInstance().getDataSource().getConnection();
			st = conn.prepareStatement(
					"UPDATE t_domain_user SET password_lastchange=null WHERE login='" + userLogin + "'");
			st.executeUpdate();
		} finally {
			JdbcHelper.cleanup(conn, null, st);
		}

		assertTrue(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreUser.class, domainUid)
				.passwordUpdateNeeded(user.login));

		// Password lastchange 10 years ago
		try {
			conn = JdbcTestHelper.getInstance().getDataSource().getConnection();
			st = conn.prepareStatement(
					"UPDATE t_domain_user SET password_lastchange=now() - interval '10 year' WHERE login='" + userLogin
							+ "'");
			st.executeUpdate();
		} finally {
			JdbcHelper.cleanup(conn, null, st);
		}

		assertTrue(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreUser.class, domainUid)
				.passwordUpdateNeeded(user.login));

		// Password never expire
		user.passwordNeverExpires = true;
		getService(domainAdminSecurityContext).update(uid, user);

		assertFalse(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreUser.class, domainUid)
				.passwordUpdateNeeded(user.login));
	}

	@Test
	public void testCheckPassword() {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);

		getService(domainAdminSecurityContext).setPassword(uid, ChangePassword.create("checkpass"));

		assertTrue(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreUser.class, domainUid)
				.checkPassword(user.login, "checkpass"));
		assertFalse(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreUser.class, domainUid)
				.checkPassword(user.login, "invalid"));
	}

	@Test
	public void testCheckPassword_invalidLogin() {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);

		getService(domainAdminSecurityContext).setPassword(uid, ChangePassword.create("checkpass"));

		try {
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreUser.class, domainUid)
					.checkPassword(null, "invalid");
			fail("Test must thrown an exception!");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreUser.class, domainUid)
					.checkPassword("", "invalid");
			fail("Test must thrown an exception!");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testCheckPassword_invalidPassword() {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);

		getService(domainAdminSecurityContext).setPassword(uid, ChangePassword.create("checkpass"));

		try {
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreUser.class, domainUid)
					.checkPassword(user.login, null);
			fail("Test must thrown an exception!");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreUser.class, domainUid)
					.checkPassword(user.login, "");
			fail("Test must thrown an exception!");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testSetPassword_Sec() throws Exception {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);
		assertNotNull(uid);

		BmTestContext selfUser = BmTestContext.contextWithSession("setpass_sec", uid, domainUid,
				BasicRoles.ROLE_SELF_CHANGE_PASSWORD);

		BmTestContext notSelfUser = BmTestContext.contextWithSession("setpass_sec2", "notSec",
				BasicRoles.ROLE_SELF_CHANGE_PASSWORD);

		getService(domainAdminSecurityContext).setPassword(uid, ChangePassword.create("checkpass"));

		getService(selfUser.getSecurityContext()).setPassword(uid, ChangePassword.create("checkpass", "test"));

		try {
			getService(notSelfUser.getSecurityContext()).setPassword(uid, ChangePassword.create("test", "checkpass"));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			// SIMPLE user should not be able to change his password without
			// giving the old one ?
			getService(selfUser.getSecurityContext()).setPassword(uid, ChangePassword.create("checkpass"));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

	}

	public static void assertUserEquals(User expected, User value) {
		if (expected == null) {
			assertNull(value);
			return;
		}
		assertEquals(expected.archived, value.archived);
		assertEquals(expected.hidden, value.hidden);
		assertEquals(expected.system, value.system);
		assertEquals(expected.login, value.login);
		assertEquals(expected.dataLocation, value.dataLocation);
		assertEquals(expected.routing, value.routing);
		assertEquals(expected.emails, value.emails);
		assertEquals(expected.properties, value.properties);
		assertEquals(expected.quota, value.quota);

	}

	public static void assertItemEquals(Item expected, Item value) {
		if (expected == null) {
			assertNull(value);
			return;
		}
		assertEquals(expected.id, value.id);
		assertEquals(expected.uid, value.uid);
		assertEquals(expected.externalId, value.externalId);
		assertEquals(expected.created, value.created);
	}

	@Test
	public void testCreateInvalidPassword() throws ServerFault, InterruptedException, SQLException {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		String uid = login;

		user.password = "  ";
		try {
			getService(domainAdminSecurityContext).create(uid, user);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertTrue(sf.getCode() == ErrorCode.INVALID_PARAMETER);
			assertEquals("Password must not be empty", sf.getMessage());
		}

		user.password = "invalid-é";
		try {
			getService(domainAdminSecurityContext).create(uid, user);
			fail("Test must throw an exception");
		} catch (ServerFault sf) {
			assertTrue(sf.getCode() == ErrorCode.INVALID_PARAMETER);
			assertEquals("Invalid character in password (é)", sf.getMessage());
		}
	}

	@Test
	public void testCreateNullPassword() throws ServerFault, InterruptedException, SQLException {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		String uid = login;

		user.password = null;
		try {
			getService(domainAdminSecurityContext).create(uid, user);
		} catch (ServerFault sf) {
			fail();
		}

		assertNull(getPassword(user));
		assertNull(getPasswordLastChange(user));
	}

	@Test
	public void testDeleteMyself() throws Exception {
		User user = defaultUser(UUID.randomUUID().toString());
		String uid = create(user);
		TaskRef tr = getService(BmTestContext
				.contextWithSession(UUID.randomUUID().toString(), uid, domainUid, BasicRoles.ROLE_MANAGE_USER)
				.getSecurityContext()).delete(uid);
		TaskStatus status = waitEnd(tr);
		assertEquals(TaskStatus.State.InError, status.state);
	}

	@Test
	public void testSetPassword_ExternalUser_Forbidden() {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);
		getService(domainAdminSecurityContext).setExtId(uid, "this.is.ext.id");

		try {
			getService(domainAdminSecurityContext).setPassword(uid, ChangePassword.create("checkpass"));
			fail("should fail because setPassword is forbidden for external user");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.FORBIDDEN, e.getCode());
		}

	}

	@Test
	public void testSetPassword_newPasswordNotEqualToCurrentPassword() {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);

		try {
			getService(domainAdminSecurityContext).setPassword(uid, ChangePassword.create("checkpass", "checkpass"));
			fail("should fail because new password is equals to current password");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}
	}

	@Test
	public void testSetPassword_nullNewPassword() {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);

		try {
			getService(domainAdminSecurityContext).setPassword(uid, ChangePassword.create("checkpass", null));
			fail("should fail because new password is null");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}
	}

	@Test
	public void testDefaultAccountType() throws Exception {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		String uid = login;
		getService(domainAdminSecurityContext).create(uid, user);

		DirEntry de = testContext.provider().instance(IDirectory.class, domainUid).findByEntryUid(uid);
		assertEquals(AccountType.FULL, de.accountType);

		ItemValue<User> u = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(AccountType.FULL, u.value.accountType);
	}

	@Test
	public void testUpdateAccountType() throws Exception {
		setDomainMaxBasicUsers(domainUid, 1);

		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		user.accountType = AccountType.SIMPLE;
		String uid = login;
		getService(domainAdminSecurityContext).create(uid, user);
		ItemValue<User> u = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(AccountType.SIMPLE, u.value.accountType);

		getService(domainAdminSecurityContext).updateAccountType(uid, AccountType.FULL);

		DirEntry de = testContext.provider().instance(IDirectory.class, domainUid).findByEntryUid(uid);
		assertEquals(AccountType.FULL, de.accountType);

		u = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(AccountType.FULL, u.value.accountType);

	}

	@Test
	public void testUpdateAccountType_fullToFullvisio() throws Exception {
		setDomainMaxBasicUsers(domainUid, 1);

		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		user.accountType = AccountType.FULL;
		String uid = login;
		getService(domainAdminSecurityContext).create(uid, user);
		ItemValue<User> u = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(AccountType.FULL, u.value.accountType);
		Set<String> roles = getService(domainAdminSecurityContext).getResolvedRoles(u.uid);
		assertTrue(roles.stream().anyMatch(r -> r.equals("hasSimpleVideoconferencing")));
		assertTrue(roles.stream().noneMatch(r -> r.equals("hasFullVideoconferencing")));

		// SIMPLE
		getService(domainAdminSecurityContext).updateAccountType(uid, AccountType.SIMPLE);

		DirEntry de = testContext.provider().instance(IDirectory.class, domainUid).findByEntryUid(uid);
		assertEquals(AccountType.SIMPLE, de.accountType);

		u = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(AccountType.SIMPLE, u.value.accountType);

		roles = getService(domainAdminSecurityContext).getResolvedRoles(u.uid);
		assertTrue(roles.stream().noneMatch(r -> r.equals("hasFullVideoconferencing")));
		assertTrue(roles.stream().noneMatch(r -> r.equals("hasSimpleVideoconferencing")));

		// FULL_AND_VISIO
		getService(domainAdminSecurityContext).updateAccountType(uid, AccountType.FULL_AND_VISIO);

		de = testContext.provider().instance(IDirectory.class, domainUid).findByEntryUid(uid);
		assertEquals(AccountType.FULL_AND_VISIO, de.accountType);

		u = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(AccountType.FULL_AND_VISIO, u.value.accountType);

		roles = getService(domainAdminSecurityContext).getResolvedRoles(u.uid);
		assertTrue(roles.stream().anyMatch(r -> r.equals("hasFullVideoconferencing")));
		assertTrue(roles.stream().anyMatch(r -> r.equals("hasSimpleVideoconferencing")));
	}

	@Test
	public void testSimpleAccount_setRoles() {

		setDomainMaxBasicUsers(domainUid, 1);

		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		user.accountType = AccountType.SIMPLE;
		String uid = login;
		getService(domainAdminSecurityContext).create(uid, user);

		try {
			getService(domainAdminSecurityContext).setRoles(uid, Collections.EMPTY_SET);
			fail("should not be able to set role for a simple user");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.FORBIDDEN, sf.getCode());
		}
	}

	@Test
	public void testSimpleAccount_getRoles() {
		setDomainMaxBasicUsers(domainUid, 1);

		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		user.accountType = AccountType.SIMPLE;
		String uid = login;
		getService(domainAdminSecurityContext).create(uid, user);

		Set<String> roles = getService(domainAdminSecurityContext).getRoles(uid);
		assertTrue(roles.isEmpty());

		roles = getService(domainAdminSecurityContext).getResolvedRoles(uid);
		IInternalRoles roleService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInternalRoles.class);
		assertEquals(roles, roleService.resolve(DefaultRoles.SIMPLE_USER_DEFAULT_ROLES));
	}

	@Test
	public void testUpdateOnlyVCardShouldPreserveVCardSourceField() {
		IUser service = getService(domainAdminSecurityContext);

		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		service.create(login, user);
		String oldSource = service.getComplete(login).value.contactInfos.source;

		VCard newCard = defaultCard();
		service.updateVCard(login, newCard);
		ItemValue<User> updatedUser = service.getComplete(login);
		String newSource = updatedUser.value.contactInfos.source;

		assertNotNull(newSource);
		assertEquals(oldSource, newSource);
	}

	@Test
	public void deletePhoto_userWithoutPhoto() {
		IUser service = getService(domainAdminSecurityContext);

		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		service.create(login, user);

		IDirectory dirService = ServerSideServiceProvider.getProvider(domainAdminSecurityContext)
				.instance(IDirectory.class, domainUid);
		ContainerChangeset<String> before = dirService.changeset(0L);

		service.deletePhoto(login);
		ContainerChangeset<String> after = dirService.changeset(before.version);

		assertEquals(before.version, after.version);
	}

	@Test
	public void deletePhoto_userWithPhoto() throws IOException {
		byte[] userPhoto;

		try (InputStream in = UserDefaultImage.class.getResourceAsStream("/data/user.png")) {
			userPhoto = ByteStreams.toByteArray(in);
		}

		IUser service = getService(domainAdminSecurityContext);

		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		service.create(login, user);
		service.setPhoto(login, userPhoto);

		IDirectory dirService = ServerSideServiceProvider.getProvider(domainAdminSecurityContext)
				.instance(IDirectory.class, domainUid);
		ContainerChangeset<String> before = dirService.changeset(0L);

		service.deletePhoto(login);
		ContainerChangeset<String> after = dirService.changeset(before.version);

		assertTrue(before.version < after.version);
		before = after;

		service.deletePhoto(login);
		after = dirService.changeset(before.version);

		assertEquals(before.version, after.version);
	}

	@Test
	public void testUpdatePasswordMustChange() throws ServerFault, SQLException {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		String uid = create(user);

		Item item = userItemStore.get(uid);
		assertNotNull(item);
		User updated = userStore.get(item);
		assertNotNull(updated);
		assertFalse(updated.passwordMustChange);

		System.out.println("Set password must change");
		user.passwordMustChange = true;
		getService(domainAdminSecurityContext).update(uid, user);

		updated = userStore.get(item);
		assertNotNull(updated);
		assertTrue(user.passwordMustChange);

		System.out.println("Unset password must change");
		user.passwordMustChange = false;
		getService(domainAdminSecurityContext).update(uid, user);

		updated = userStore.get(item);
		assertNotNull(updated);
		assertFalse(user.passwordMustChange);
	}

	@Test
	public void testUpdatePasswordNeverExpire() throws ServerFault, SQLException {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		String uid = create(user);

		Item item = userItemStore.get(uid);
		assertNotNull(item);
		User updated = userStore.get(item);
		assertNotNull(updated);
		assertFalse(updated.passwordNeverExpires);

		System.out.println("Set password never expire");
		user.passwordNeverExpires = true;
		getService(domainAdminSecurityContext).update(uid, user);

		updated = userStore.get(item);
		assertNotNull(updated);
		assertTrue(user.passwordNeverExpires);

		System.out.println("Set password may expire");
		user.passwordNeverExpires = false;
		getService(domainAdminSecurityContext).update(uid, user);

		updated = userStore.get(item);
		assertNotNull(updated);
		assertFalse(user.passwordNeverExpires);
	}

	@Test
	public void testUserWithMailroutingNone_No_Email() {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		user.routing = Routing.none;
		user.emails = Collections.emptyList();
		String uid = login;
		getService(domainAdminSecurityContext).create(uid, user);

		ItemValue<User> loadedUser = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(Routing.internal, loadedUser.value.routing);
		assertEquals(1, loadedUser.value.emails.size());
		assertEquals(loadedUser.value.login + "@" + domainUid, loadedUser.value.emails.iterator().next().address);
	}

	@Test
	public void testUserWithMailroutingNull_No_Email() {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		user.routing = null;
		user.emails = Collections.emptyList();
		String uid = login;
		getService(domainAdminSecurityContext).create(uid, user);

		ItemValue<User> loadedUser = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(Routing.internal, loadedUser.value.routing);
		assertEquals(1, loadedUser.value.emails.size());
		assertEquals(loadedUser.value.login + "@" + domainUid, loadedUser.value.emails.iterator().next().address);
	}

	@Test
	public void testUserWithMailroutingNone_Internal_Email() {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		user.routing = Routing.none;
		Email createdEmail = Email.create("alt" + System.nanoTime() + "@" + domainUid, false, false);
		user.emails = Arrays.asList(createdEmail);
		String uid = login;
		getService(domainAdminSecurityContext).create(uid, user);

		ItemValue<User> loadedUser = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(Routing.internal, loadedUser.value.routing);
		assertEquals(2, loadedUser.value.emails.size());
		boolean foundInternal = false;
		boolean foundDomain = false;
		for (Iterator<Email> emailIter = loadedUser.value.emails.iterator(); emailIter.hasNext();) {
			Email email = emailIter.next();
			if (email.address.equals(createdEmail.address)) {
				foundDomain = true;
			}
			if (email.address.equals(user.login + "@" + domainUid)) {
				foundInternal = true;
			}
		}
		assertTrue(foundDomain);
		assertTrue(foundInternal);
	}

	@Test
	public void testUserWithMailroutingNone_External_Email() {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		user.routing = Routing.none;
		Email createdEmail = Email.create("alt" + System.nanoTime() + "@external.loc", false, false);
		user.emails = Arrays.asList(createdEmail);
		String uid = login;
		getService(domainAdminSecurityContext).create(uid, user);

		ItemValue<User> loadedUser = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(Routing.internal, loadedUser.value.routing);

		boolean emailFound = false;
		for (Iterator<Email> emailIter = loadedUser.value.emails.iterator(); emailIter.hasNext();) {
			Email email = emailIter.next();
			if (email.address.equals(user.login + "@" + domainUid)) {
				emailFound = true;
			}
		}
		assertTrue(emailFound);

		IMailboxes mb = ServerSideServiceProvider.getProvider(domainAdminSecurityContext).instance(IMailboxes.class,
				domain.uid);
		MailFilter mailboxFilter = mb.getMailboxFilter(uid);
		assertTrue(mailboxFilter.forwarding.enabled);
		assertEquals(createdEmail.address, mailboxFilter.forwarding.emails.iterator().next());
	}

	@Test
	public void testUserWithMailroutingNone_Internal_External_Emails() {
		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		user.routing = Routing.none;
		user.routing = Routing.none;
		Email createdEmail1 = Email.create("alt1" + System.nanoTime() + "@" + domainUid, false, false);
		Email createdEmail2 = Email.create("alt2" + System.nanoTime() + "@" + domainUid, false, false);
		Email createdEmail3 = Email.create("alt1" + System.nanoTime() + "@external.loc", false, false);
		Email createdEmail4 = Email.create("alt2" + System.nanoTime() + "@external.loc", false, false);
		user.emails = Arrays.asList(createdEmail1, createdEmail2, createdEmail3, createdEmail4);
		String uid = login;
		getService(domainAdminSecurityContext).create(uid, user);

		ItemValue<User> loadedUser = getService(domainAdminSecurityContext).getComplete(uid);
		assertEquals(Routing.internal, loadedUser.value.routing);

		boolean email1 = false;
		boolean email2 = false;
		for (Iterator<Email> emailIter = loadedUser.value.emails.iterator(); emailIter.hasNext();) {
			Email email = emailIter.next();
			if (email.address.equals(createdEmail1.address)) {
				email1 = true;
			}
			if (email.address.equals(createdEmail2.address)) {
				email2 = true;
			}
		}
		assertTrue(email1);
		assertTrue(email2);

		IMailboxes mb = ServerSideServiceProvider.getProvider(domainAdminSecurityContext).instance(IMailboxes.class,
				domain.uid);
		MailFilter mailboxFilter = mb.getMailboxFilter(uid);
		assertTrue(mailboxFilter.forwarding.enabled);

		assertTrue(mailboxFilter.forwarding.emails.contains(createdEmail3.address));
		assertTrue(mailboxFilter.forwarding.emails.contains(createdEmail4.address));
	}

}
