package net.bluemind.core.container.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSettingsKeys;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUserSubscription;

public class ContainerManagementTests {
	private String containerId;
	private Container container;
	private AclStore aclStore;

	private SecurityContext testSecurityContext;
	private ContainerStore containerStore;
	private String testGroup = "testGroup";
	private ItemStore itemStore;
	private String domainUid = "testbm.lan";

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		BmConfIni ini = new BmConfIni();

		Server pipo = new Server();
		pipo.tags = Collections.singletonList("mail/imap");
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		Server pg = new Server();
		pg.tags = Arrays.asList(TagDescriptor.bm_pgsql_data.getTag());
		pg.ip = ini.get("bluemind/postgres-tests");

		PopulateHelper.initGlobalVirt(pipo, pg);

		PopulateHelper.addDomain(domainUid);
		PopulateHelper.addUser("subject", domainUid);

		PopulateHelper.addUser("accesstest", domainUid);

		testSecurityContext = new SecurityContext("testSessionId", "test", Arrays.<String>asList(testGroup),
				Arrays.<String>asList(), domainUid);

		Sessions.get().put(testSecurityContext.getSessionId(), testSecurityContext);

		containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(), testSecurityContext);

		containerId = "test_" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "notMe", domainUid, true);
		container = containerStore.create(container);
		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, SecurityContext.SYSTEM);

		aclStore = new AclStore(new BmTestContext(SecurityContext.SYSTEM),
				JdbcTestHelper.getInstance().getDataSource());

		aclStore.store(container, Arrays.asList(AccessControlEntry.create(testSecurityContext.getSubject(), Verb.All),
				AccessControlEntry.create(testGroup, Verb.Write)));

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IContainerManagement service(SecurityContext securityContext, String containerUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(securityContext).instance(IContainerManagement.class,
				containerUid);
	}

	@Test
	public void testUpdate() throws ServerFault, SQLException {
		ContainerModifiableDescriptor d = new ContainerModifiableDescriptor();
		d.defaultContainer = true;
		d.name = "testUpdate";
		service(testSecurityContext, containerId).update(d);

		Container testContainer = containerStore.get(containerId);
		assertNotNull(testContainer);
		assertEquals("testUpdate", testContainer.name);
		// TODO : test security
	}

	@Test
	public void testGetACL() throws ServerFault, SQLException {
		ContainerModifiableDescriptor d = new ContainerModifiableDescriptor();
		d.defaultContainer = true;
		d.name = "testUpdate";
		List<AccessControlEntry> acl = service(testSecurityContext, containerId).getAccessControlList();
		assertNotNull(acl);
		assertTrue(acl.stream().anyMatch(ace -> ace.subject.equals(testGroup) && Verb.Write.equals(ace.verb)));
		assertTrue(acl.stream()
				.anyMatch(ace -> ace.subject.equals(testSecurityContext.getSubject()) && Verb.All.equals(ace.verb)));
		assertTrue(acl.stream().filter(ace -> ace.subject.equals(testGroup)).allMatch(ace -> Verb.Write.can(ace.verb)));
		// TODO : test security

	}

	@Test
	public void testSetACL() throws ServerFault, SQLException {

		List<AccessControlEntry> acl = Arrays.asList(
				AccessControlEntry.create(testSecurityContext.getSubject(), Verb.All), //
				AccessControlEntry.create(testGroup, Verb.Write), //
				AccessControlEntry.create("testSubject", Verb.Read));

		service(testSecurityContext, containerId).setAccessControlList(acl);

		List<AccessControlEntry> current = aclStore.get(container);
		assertEquals(3, current.size());

		assertEquals(ImmutableSet.copyOf(acl), ImmutableSet.copyOf(current));
		// TODO : test security

	}

	@Test
	public void testGetDescriptor() throws ServerFault, SQLException {

		IContainerManagement containerManagement = service(testSecurityContext, containerId);

		ContainerDescriptor desc = containerManagement.getDescriptor();
		assertNotNull(desc);
		assertEquals(containerId, desc.uid);
		assertEquals(container.name, desc.name);
		assertEquals(container.type, desc.type);
		// FIXME check default

		// check acl
		assertTrue(desc.verbs.stream().anyMatch(verb -> verb.can(Verb.Manage)));
		assertTrue(desc.writable);

		List<AccessControlEntry> acl = aclStore.get(container);
		acl.get(0).verb = Verb.Read;
		acl.get(1).verb = Verb.Read;
		aclStore.store(container, acl);

		desc = containerManagement.getDescriptor();
		// check acl
		assertTrue(desc.verbs.stream().noneMatch(verb -> verb.can(Verb.Manage)));
		assertFalse(desc.writable);

	}

	@Test
	public void testGetAllItems() throws SQLException, ServerFault {
		itemStore.create(Item.create("test1", "extTest1"));
		itemStore.create(Item.create("test2", "extTest2"));
		itemStore.create(Item.create("test3", "extTest3"));

		IContainerManagement containerManagement = service(testSecurityContext, containerId);
		List<ItemDescriptor> items = containerManagement.getAllItems();

		assertEquals(3, items.size());
		for (ItemDescriptor item : items) {
			switch (item.uid) {
			case "test1":
				assertEquals("extTest1", item.externalId);
				break;
			case "test2":
				assertEquals("extTest2", item.externalId);
				break;
			case "test3":
				assertEquals("extTest3", item.externalId);
				break;
			default:
				fail("Unknown item: " + item.uid);
			}
		}
	}

	@Test
	public void testGetItems() throws SQLException, ServerFault {
		itemStore.create(Item.create("test1", "extTest1"));
		itemStore.create(Item.create("test2", "extTest2"));
		itemStore.create(Item.create("test3", "extTest3"));

		IContainerManagement containerManagement = service(testSecurityContext, containerId);
		List<ItemDescriptor> items = containerManagement.getItems(Arrays.asList("test1", "test3", "fail"));

		assertEquals(2, items.size());
		for (ItemDescriptor item : items) {
			switch (item.uid) {
			case "test1":
				assertEquals("extTest1", item.externalId);
				break;
			case "test3":
				assertEquals("extTest3", item.externalId);
				break;
			default:
				fail("Unknown item: " + item.uid);
			}
		}
	}

	@Test
	public void testSetPersonalSettings() throws Exception {
		itemStore.create(Item.create("test1", "extTest1"));
		IContainerManagement service = service(testSecurityContext, containerId);

		ContainerDescriptor descriptor = service.getDescriptor();
		assertNotNull(descriptor.settings);
		assertTrue(descriptor.settings.isEmpty());

		HashMap<String, String> settings = new HashMap<>();
		settings.put("string", "blah");
		settings.put("color", "blue");

		service.setPersonalSettings(settings);

		descriptor = service.getDescriptor();
		assertNotNull(descriptor.settings);
		assertEquals(2, descriptor.settings.size());
		assertEquals("blah", descriptor.settings.get("string"));
		assertEquals("blue", descriptor.settings.get("color"));

	}

	@Test
	public void testSetSettings() throws Exception {
		itemStore.create(Item.create("test1", "extTest1"));
		IContainerManagement service = service(testSecurityContext, containerId);

		ContainerDescriptor descriptor = service.getDescriptor();
		assertNotNull(descriptor.settings);
		assertTrue(descriptor.settings.isEmpty());

		Map<String, String> settings = Map.of("string", "blah",
				ContainerSettingsKeys.mailbox_record_fast_sort_enabled.name(), "true");
		service.setSettings(settings);

		descriptor = service.getDescriptor();
		assertNotNull(descriptor.settings);
		assertEquals(2, descriptor.settings.size());
		assertEquals("blah", descriptor.settings.get("string"));
		assertEquals("true", descriptor.settings.get(ContainerSettingsKeys.mailbox_record_fast_sort_enabled.name()));

		service.setSetting(ContainerSettingsKeys.mailbox_record_fast_sort_enabled.name(), "false");
		descriptor = service.getDescriptor();
		assertEquals(descriptor.settings.get(ContainerSettingsKeys.mailbox_record_fast_sort_enabled.name()), "false");
	}

	@Test
	public void testMutateSetting() throws Exception {
		itemStore.create(Item.create("test1", "extTest1"));
		IContainerManagement service = service(testSecurityContext, containerId);

		ContainerDescriptor descriptor = service.getDescriptor();
		assertNotNull(descriptor.settings);
		assertTrue(descriptor.settings.isEmpty());

		Map<String, String> settings = Map.of("string", "blah",
				ContainerSettingsKeys.mailbox_record_fast_sort_enabled.name(), "true");
		service.setSettings(settings);

		descriptor = service.getDescriptor();
		assertNotNull(descriptor.settings);
		assertEquals(2, descriptor.settings.size());
		assertEquals("blah", descriptor.settings.get("string"));
		assertEquals("true", descriptor.settings.get(ContainerSettingsKeys.mailbox_record_fast_sort_enabled.name()));

		service.setSetting(ContainerSettingsKeys.mailbox_record_fast_sort_enabled.name(), "false");
		descriptor = service.getDescriptor();
		assertNotNull(descriptor.settings);
		assertEquals(2, descriptor.settings.size());
		assertEquals("blah", descriptor.settings.get("string"));
		assertEquals("false", descriptor.settings.get(ContainerSettingsKeys.mailbox_record_fast_sort_enabled.name()));
	}

	@Test
	public void testAllowSync() throws SQLException {
		itemStore.create(Item.create("test1", "extTest1"));
		IContainerManagement service = service(testSecurityContext, containerId);

		try {
			service.disallowOfflineSync("subject");
			fail("testSecurityContext does not have roles manageUserSubscriptions,self");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.PERMISSION_DENIED, sf.getCode());
		}

		try {
			service.allowOfflineSync("subject");
			fail("testSecurityContext does not have roles manageUserSubscriptions,self");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.PERMISSION_DENIED, sf.getCode());
		}

		SecurityContext domainAdminSecurityContext = new SecurityContext("testSessionId2", "admin",
				Arrays.<String>asList(), Arrays.<String>asList(SecurityContext.ROLE_SYSTEM), domainUid);
		Sessions.get().put(domainAdminSecurityContext.getSessionId(), domainAdminSecurityContext);

		service = service(domainAdminSecurityContext, containerId);
		try {
			service.allowOfflineSync("subject");
			fail("Cannot allow sync because not subscribed");
		} catch (ServerFault sf) {
			assertEquals("No subscription for container " + container.uid, sf.getMessage());
		}

		try {
			service.disallowOfflineSync("subject");
			fail("Cannot allow sync because not subscribed");
		} catch (ServerFault sf) {
			assertEquals("No subscription for container " + container.uid, sf.getMessage());
		}

		IUserSubscription userSubService = ServerSideServiceProvider.getProvider(domainAdminSecurityContext)
				.instance(IUserSubscription.class, domainUid);

		userSubService.subscribe("subject", Arrays.asList(ContainerSubscription.create(container.uid, false)));

		try {
			service.disallowOfflineSync("subject");
		} catch (ServerFault sf) {
			sf.printStackTrace();
			fail();
		}

		try {
			service.allowOfflineSync("subject");
		} catch (ServerFault sf) {
			sf.printStackTrace();
			fail();
		}
	}

	@Test
	public void testAccess() throws SQLException {
		SecurityContext ctx = new SecurityContext("accesstest", "accesstest", Collections.emptyList(),
				Arrays.<String>asList(), domainUid);
		Sessions.get().put(ctx.getSessionId(), ctx);

		IContainerManagement containerManagement = service(ctx, containerId);

		aclStore.store(container, Collections.emptyList());

		assertFalse(containerManagement.canAccess(Arrays.asList(Verb.Read.name())));
		assertFalse(containerManagement.canAccess(Arrays.asList(Verb.Write.name())));
		assertFalse(containerManagement.canAccess(Arrays.asList(Verb.Manage.name())));

		assertFalse(
				containerManagement.canAccess(Arrays.asList(Verb.Read.name(), Verb.Write.name(), Verb.Manage.name())));

		aclStore.store(container, Arrays.asList(AccessControlEntry.create(ctx.getSubject(), Verb.Read)));

		assertTrue(containerManagement.canAccess(Arrays.asList(Verb.Read.name())));
		assertFalse(containerManagement.canAccess(Arrays.asList(Verb.Write.name())));
		assertFalse(containerManagement.canAccess(Arrays.asList(Verb.Manage.name())));

		assertTrue(
				containerManagement.canAccess(Arrays.asList(Verb.Read.name(), Verb.Write.name(), Verb.Manage.name())));
		assertFalse(containerManagement.canAccess(Arrays.asList(Verb.Write.name(), Verb.Manage.name())));

		aclStore.store(container, Arrays.asList(AccessControlEntry.create(ctx.getSubject(), Verb.Write)));

		assertTrue(containerManagement.canAccess(Arrays.asList(Verb.Read.name())));
		assertTrue(containerManagement.canAccess(Arrays.asList(Verb.Write.name())));
		assertFalse(containerManagement.canAccess(Arrays.asList(Verb.Manage.name())));

		assertTrue(
				containerManagement.canAccess(Arrays.asList(Verb.Read.name(), Verb.Write.name(), Verb.Manage.name())));
		assertTrue(containerManagement.canAccess(Arrays.asList(Verb.Write.name(), Verb.Manage.name())));
	}

	@Test
	public void getItemCount() throws SQLException {
		IContainerManagement containerManagement = service(testSecurityContext, containerId);

		assertEquals(0, containerManagement.getItemCount().total);

		itemStore.create(Item.create("test1", "extTest1"));
		itemStore.create(Item.create("test2", "extTest2"));
		assertEquals(2, containerManagement.getItemCount().total);

	}
}
