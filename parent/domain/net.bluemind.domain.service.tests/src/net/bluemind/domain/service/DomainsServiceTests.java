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
package net.bluemind.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.service.internal.DomainStoreService;
import net.bluemind.domain.service.tests.FakeDomainHook;
import net.bluemind.group.api.IGroup;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;

public class DomainsServiceTests {

	private Container domainsContainer;
	private BmContext testContext;
	private DomainStoreService storeService;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		ElasticsearchTestHelper.getInstance().beforeTest();

		testContext = new BmTestContext(SecurityContext.SYSTEM);

		ContainerStore containerStore = new ContainerStore(testContext, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);
		domainsContainer = containerStore.get(DomainsContainerIdentifier.getIdentifier());
		assertNotNull(domainsContainer);

		storeService = new DomainStoreService(JdbcActivator.getInstance().getDataSource(), SecurityContext.SYSTEM,
				domainsContainer);
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		FakeDomainHook.initFlags();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreate() throws ServerFault {
		createDomain("test.lan");

		DomainStoreService domainStoreService = new DomainStoreService(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM, domainsContainer);
		assertNotNull(domainStoreService.get("test.lan", null));

		assertTrue(FakeDomainHook.created);

		// by default, we create 2 groups
		IGroup groups = testContext.provider().instance(IGroup.class, "test.lan");
		assertEquals(2, groups.allUids().size());
	}

	@Test
	public void testCreateAlreadyExists() throws ServerFault {
		createDomain("test.lan");
		try {
			createDomain("test.lan");
			fail();
		} catch (ServerFault e) {

		}
	}

	@Test
	public void testCreateRight() throws ServerFault {
		SecurityContext testSC = new SecurityContext("admSess", "admTest", Collections.<String>emptyList(),
				Arrays.asList(SecurityContext.ROLE_ADMIN), "test2.lan");
		Sessions.get().put("admSess", testSC);
		Domain d = domain("test2.lan");
		try {
			getService(testSC).create("test2.lan", d);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

	}

	@Test
	public void testUpdate() throws ServerFault {
		IDomains domains = getService();
		Domain testDomain = domain("test" + System.currentTimeMillis() + ".lan");
		domains.create(testDomain.name, testDomain);

		domains.update(testDomain.name, testDomain);

		assertTrue(FakeDomainHook.updated);

		SecurityContext testSC = new SecurityContext("admSess", "admTest", Collections.<String>emptyList(),
				Arrays.asList(BasicRoles.ROLE_MANAGE_DOMAIN), testDomain.name);
		Sessions.get().put("admSess", testSC);
		getService(testSC).update(testDomain.name, testDomain);
	}

	@Test
	public void testUpdateNotFound() throws ServerFault {
		IDomains domains = getService();
		Domain testDomain = domain("test" + System.currentTimeMillis() + ".lan");
		try {
			domains.update(testDomain.name, testDomain);
			fail("should fail because domain doesnt exists");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}
	}

	@Test
	public void testUpdateNameChange() throws ServerFault {
		IDomains domains = getService();
		Domain testDomain = domain("test" + System.currentTimeMillis() + ".lan");
		domains.create(testDomain.name, testDomain);

		try {
			domains.update(testDomain.name, domain("test" + System.currentTimeMillis() + ".lan"));
			fail("should fail because we cannot change domain name");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}
	}

	@Test
	public void testUpdateGlobalFlag() throws ServerFault {
		IDomains domains = getService();
		Domain testDomain = domain("test" + System.currentTimeMillis() + ".lan");
		domains.create(testDomain.name, testDomain);
		testDomain.global = !testDomain.global;
		try {
			domains.update(testDomain.name, testDomain);
			fail("should fail because we cannot change domain global flag");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}
	}

	@Test
	public void testUpdateAliasesChanged() throws ServerFault {
		IDomains domains = getService();
		Domain testDomain = domain("test" + System.currentTimeMillis() + ".lan");
		domains.create(testDomain.name, testDomain);
		testDomain.aliases = new HashSet<>(Arrays.asList("test.lan"));
		try {
			domains.update(testDomain.name, testDomain);
			fail("should fail because we cannot change domain global flag");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}
	}

	@Test(timeout = 45000)
	public void testDelete() throws Exception {
		String domainUid = "test" + System.currentTimeMillis() + ".lan";
		PopulateHelper.createTestDomain(domainUid);

		User user = PopulateHelper.getUser("test", domainUid, Routing.none);
		String userUid = PopulateHelper.addUser(domainUid, user);
		String orgUnitUid = createOrgUnit(domainUid, "organisation");
		String calendarUid = createCalendar(domainUid, "calendar", domainUid, orgUnitUid);
		try {
			getService().delete(domainUid);
			fail("should fail");
		} catch (ServerFault e) {
			// cant delete doamin because we need to delete dir entries
			// (addressbook)
		}

		// need to delete addressbook
		TaskRef taskRef = getService().deleteDomainItems(domainUid);

		TaskStatus status = TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), taskRef);

		assertTrue(status.state.succeed);

		// now we can delete domain
		getService().delete(domainUid);
	}

	@Test
	public void testDeleteNonExistant() throws ServerFault {
		try {
			getService().delete("fakeUid");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}

	}

	@Test
	public void testSetAliases() throws ServerFault {
		IDomains domains = getService();
		Domain testDomain = createDomain("test" + System.currentTimeMillis() + ".lan");

		TaskRef taskRef = domains.setAliases(testDomain.name, new HashSet<String>(Arrays.asList("test.lan")));

		ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class,
				"" + taskRef.id);

		while (!task.status().state.ended) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		assertTrue(task.status().state.succeed);

		assertTrue(FakeDomainHook.aliasesUpdated);
	}

	@Test
	public void testSetAliasesDomainNotFound() throws ServerFault {
		IDomains domains = getService();
		try {
			domains.setAliases("fakeDomainUid", new HashSet<String>());
			fail("should fail because domain doesnt exists");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}
	}

	@Test
	// FIXME: BM-14722
	public void testCanSetAliasesWhenConflict() throws Exception {
		String domainAlias = "newalias.tld";

		// create domain
		String domainUid = "test" + System.currentTimeMillis() + ".lan";
		PopulateHelper.createTestDomain(domainUid);

		// create an external user which use domain alias not yet created
		String leftPart = "whatever";
		PopulateHelper.addExternalUser(domainUid, leftPart + "@" + domainAlias, "externaluser");

		// create a user with all_alias
		User user = PopulateHelper.getUser(leftPart, domainUid, Routing.none);
		user.emails.forEach(email -> email.allAliases = true);
		PopulateHelper.addUser(domainUid, user);

		// try to add alias to domain
		getService().setAliases(domainUid, Sets.newHashSet(domainAlias));

//		try {
//			
//			fail("should fail because an external user using this alias exists");
//		} catch (ServerFault e) {
//			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
//		}
	}

	@Test
	public void testCustomProperties() throws ServerFault {
		IDomains domains = getService();
		Domain d = new Domain();
		d.name = "test.lan";
		d.label = "label";
		d.description = "desc";
		d.aliases = Collections.emptySet();
		domains.create("test.lan", d);

		ItemValue<Domain> created = domains.get("test.lan");
		assertEquals(0, created.value.properties.size());

		d.properties = new HashMap<String, String>();
		domains.update("test.lan", d);
		created = domains.get("test.lan");
		assertEquals(0, created.value.properties.size());

		d.properties.put("blue", "mind");
		domains.update("test.lan", d);
		created = domains.get("test.lan");
		assertEquals(1, created.value.properties.size());
		assertEquals("mind", created.value.properties.get("blue"));
	}

	private Domain domain(String name) {
		return Domain.create(name, "label", "desc", Collections.emptySet());
	}

	private Domain domain(String name, Set<String> aliases) {
		return Domain.create(name, "label", "desc", aliases);
	}

	private Domain createDomain(String name) throws ServerFault {
		return createDomain(name, Collections.emptySet());
	}

	private Domain createDomain(String name, Set<String> aliases) throws ServerFault {
		Domain d = domain(name, aliases);
		getService().create(d.name, d);
		return d;
	}

	private String createOrgUnit(String domainUid, String name) {
		IOrgUnits orgUnitService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IOrgUnits.class, domainUid);
		OrgUnit orgUnit = OrgUnit.create(name, null);
		String uid = "test-created:" + name;
		orgUnitService.create(uid, orgUnit);
		return uid;
	}

	private String createCalendar(String domainUid, String name, String ownerUid, String orgUnitUid) {
		ICalendarsMgmt calendarsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ICalendarsMgmt.class, domainUid);
		CalendarDescriptor calendarDescriptor = CalendarDescriptor.create(name, ownerUid, domainUid);
		calendarDescriptor.orgUnitUid = orgUnitUid;
		String uid = "test-created:" + name;
		calendarsService.create(uid, calendarDescriptor);
		return uid;
	}

	private IDomains getService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
	}

	private IDomains getService(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(IDomains.class);
	}

	@Test
	public void testCreateDomainNameIsAliasOnAnotherDomain() throws ServerFault {
		IDomains domains = getService();
		Domain d = domain("test.lan", new HashSet<>(Arrays.asList("test2.lan")));
		domains.create("test.lan", d);

		Domain d2 = domain("test2.lan");
		try {
			domains.create("test2.lan", d2);
			fail();
		} catch (ServerFault e) {
			System.out.println(e.getMessage());
		}
	}

	@Test
	public void testCreateDomainAliasIsNameOnAnotherDomain() throws ServerFault {
		IDomains domains = getService();
		createDomain("test.lan");
		Domain d2 = domain("test2.lan", new HashSet<>(Arrays.asList("test.lan")));

		try {
			domains.create("test2.lan", d2);
			fail();
		} catch (ServerFault e) {
			System.out.println(e.getMessage());
		}
	}

	@Test
	public void testCreateDomainAliasIsAliasOnAnotherDomain() throws ServerFault {
		IDomains domains = getService();
		createDomain("test.lan", new HashSet<String>(Arrays.asList("test2.lan")));
		Domain d2 = domain("test3.lan", new HashSet<String>(Arrays.asList("test2.lan")));

		try {
			domains.create("test3.lan", d2);
			fail();
		} catch (ServerFault e) {
			System.out.println(e.getMessage());
		}
	}

	@Test
	public void testUpdateDomainAliasIsNameOnAnotherDomain() throws ServerFault {
		IDomains domains = getService();
		createDomain("test.lan");
		Domain d2 = createDomain("test2.lan");
		d2.aliases = new HashSet<String>(Arrays.asList("test.lan"));
		try {
			domains.update(d2.name, d2);
			fail();
		} catch (ServerFault e) {
			System.out.println(e.getMessage());
		}
	}

	@Test
	public void testUpdateDomainAliasIsAliasOnAnotherDomain() throws ServerFault {
		IDomains domains = getService();
		createDomain("test.lan", new HashSet<String>(Arrays.asList("test3.lan")));
		Domain d2 = createDomain("test2.lan");
		d2.aliases = new HashSet<String>(Arrays.asList("test3.lan"));
		try {
			domains.update(d2.name, d2);
			fail();
		} catch (ServerFault e) {
			System.out.println(e.getMessage());
		}
	}

	@Test
	public void testAddAliasIsNameOnAnotherDomain() throws ServerFault {
		IDomains domains = getService();
		createDomain("test.lan");
		Domain d2 = createDomain("test2.lan");
		try {
			domains.setAliases(d2.name, new HashSet<>(Arrays.asList("test.lan")));
			fail();
		} catch (ServerFault e) {
			System.out.println(e.getMessage());
		}
	}

	@Test
	public void testAddAliasIsAliasOnAnotherDomain() throws ServerFault {
		IDomains domains = getService();
		createDomain("test.lan", new HashSet<>(Arrays.asList("test3.lan")));
		Domain d2 = createDomain("test2.lan");

		try {
			domains.setAliases(d2.name, new HashSet<>(Arrays.asList("test3.lan")));
			fail();
		} catch (ServerFault e) {
			System.out.println(e.getMessage());
		}
	}

	@Test
	public void testGetDomain() throws ServerFault {
		Domain d = domain("bm.lan", new HashSet<>(Arrays.asList("test3.lan")));
		storeService.create("bm.lan", "bm.lan", d);

		assertNotNull(getService().get("bm.lan"));
		assertNull(getService().get("fake.lan"));
		// get do not search in aliases
		assertNull(getService().get("test3.lan"));

		SecurityContext notSameDomainSC = BmTestContext
				.contextWithSession("notSameDomain" + System.currentTimeMillis(), "nosamedom@fake.lan", "fake.lan")
				.getSecurityContext();

		SecurityContext sameDomainSC = BmTestContext
				.contextWithSession("sameDomain" + System.currentTimeMillis(), "samedom@bm.lan", "bm.lan")
				.getSecurityContext();

		try {
			getService(notSameDomainSC).get("bm.lan");
			fail("should not be able to retrieve this domain");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			getService(sameDomainSC).get("bm.lan");
			CacheRegistry reg = ServerSideServiceProvider.getProvider(sameDomainSC).instance(CacheRegistry.class);
			assertNotNull(reg);
			Cache<String, Object> cache = reg.get("ContainerUidCache");
			assertNotNull(cache);
			cache.asMap().forEach((k, v) -> System.err.println(k + " => " + v));
			assertTrue(cache.asMap().containsKey("domains_bluemind-noid"));
		} catch (ServerFault e) {
			fail("should be able to retrieve this domain");
		}
	}

	@Test
	public void testFindByNameOrAliases() throws ServerFault {
		Domain d = domain("bm.lan", new HashSet<>(Arrays.asList("test3.lan")));
		storeService.create("bm.lan", "bm.lan", d);

		assertNotNull(getService().findByNameOrAliases("bm.lan"));
		assertNotNull(getService().findByNameOrAliases("test3.lan"));
		assertNull(getService().findByNameOrAliases("fake.lan"));

		SecurityContext notSameDomainSC = BmTestContext
				.contextWithSession("notSameDomain" + System.currentTimeMillis(), "nosamedom@fake.lan", "fake.lan")
				.getSecurityContext();

		SecurityContext sameDomainSC = BmTestContext
				.contextWithSession("sameDomain" + System.currentTimeMillis(), "samedom@bm.lan", "bm.lan")
				.getSecurityContext();

		try {
			getService(SecurityContext.ANONYMOUS).findByNameOrAliases("test3.lan");
			fail("should not be able to retrieve this domain");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			getService(notSameDomainSC).findByNameOrAliases("test3.lan");
			fail("should not be able to retrieve this domain");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			getService(sameDomainSC).findByNameOrAliases("test3.lan");
		} catch (ServerFault e) {
			fail("should be able to retrieve this domain");
		}
	}

	@Test
	public void testDefaultAliasDefaultValue() throws ServerFault {
		IDomains domains = getService();
		Domain d = createDomain("test.lan", new HashSet<>(Arrays.asList("test1.lan", "test2.lan")));
		ItemValue<Domain> created = domains.get("test.lan");
		assertEquals(created.value.defaultAlias, d.name);

		d = Domain.create("test.lan", "label", "desc", new HashSet<>(Arrays.asList("test1.lan", "test2.lan")));
		assertEquals(d.name, d.defaultAlias);

		d = Domain.create("test.lan", "label", "desc", new HashSet<>(Arrays.asList("test1.lan", "test2.lan")),
				"test2.lan");
		assertEquals("test2.lan", d.defaultAlias);
	}

	@Test
	public void testDefaultAliasInvalidValue() throws ServerFault {
		IDomains domains = getService();
		Domain d = Domain.create("test.lan", "label", "desc", new HashSet<>(Arrays.asList("test1.lan", "test2.lan")));

		d.defaultAlias = "not.existent";
		try {
			domains.create("test.lan", d);
			fail("domain creation with default alias not equal to domain name or present in aliases should not be permitted");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testUpdateDefaultAliasForbidden() throws ServerFault {
		IDomains domains = getService();
		Domain domain = createDomain("test.lan", new HashSet<>(Arrays.asList("moto.becane")));
		ItemValue<Domain> created = domains.get("test.lan");
		assertNotNull(created);

		domain.defaultAlias = "moto.becane";
		try {
			domains.update(domain.name, domain);
			fail("domain update of defaultAlias should not be permitted");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testSetDefaultAlias() throws ServerFault {
		IDomains domains = getService();
		Domain domain = createDomain("test.lan", new HashSet<>(Arrays.asList("moto.guzzy")));
		domains.setDefaultAlias(domain.name, "moto.guzzy");
		ItemValue<Domain> created = domains.get("test.lan");
		assertEquals(created.value.defaultAlias, "moto.guzzy");
	}

	@Test
	public void testSetDefaultAliasInvalidAlias() throws ServerFault {
		IDomains domains = getService();
		Domain domain = createDomain("test.lan");
		try {
			domains.setDefaultAlias(domain.name, "not.valid");
			fail("domainAlias must be part of aliases only");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

}
