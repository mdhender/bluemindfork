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
package net.bluemind.resource.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bluemind.addressbook.domainbook.verticle.DomainBookVerticle;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.type.IResourceTypeUids;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property.Type;
import net.bluemind.resource.persistence.ResourceTypeStore;
import net.bluemind.resource.service.internal.ResourceContainerStoreService;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ResourcesServiceTests {
	protected String testDomainUid;
	private ResourceContainerStoreService store;

	private SecurityContext domainAdminSC;
	private SecurityContext badDomainAdminSC;
	private SecurityContext userSC;

	private BmContext domainAdminCtx;

	private ContainerStore containerHome;
	private static byte[] image;

	@BeforeClass
	public static void init() throws IOException {
		BufferedImage buff = ImageIO
				.read(ResourcesServiceTests.class.getClassLoader().getResourceAsStream("download.png"));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(buff, "png", baos);
		image = baos.toByteArray();
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		DomainBookVerticle.suspended = true;
		testDomainUid = "test.lan";
		BmContext systemCtx = new BmTestContext(SecurityContext.SYSTEM);
		containerHome = new ContainerStore(systemCtx, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		PopulateHelper.initGlobalVirt();
		ItemValue<Domain> domain = PopulateHelper.createTestDomain(testDomainUid);

		String containerId = testDomainUid;
		String containerTypeId = IResourceTypeUids.getIdentifier(testDomainUid);
		// Container is already created by domain hook
		// Container.create(containerId,
		// ResourcesContainerType.TYPE, "test", "me", true);
		// container = containerHome.create(container);
		Container container = containerHome.get(containerId);
		assertNotNull(container);

		ResourceTypeStore typeStore = new ResourceTypeStore(JdbcActivator.getInstance().getDataSource(),
				containerHome.get(containerTypeId));

		ResourceTypeDescriptor typeDescriptor = ResourceTypeDescriptor.create("testType", //
				ResourceTypeDescriptor.Property.create("test1", Type.String, ""), //
				ResourceTypeDescriptor.Property.create("test2", Type.Boolean, ""), //
				ResourceTypeDescriptor.Property.create("test3", Type.Number, ""), //
				ResourceTypeDescriptor.Property.create("test4", Type.String, ""));

		typeStore.create("testType", typeDescriptor);
		typeStore.create("testType2", typeDescriptor);
		typeStore.create("the-type", typeDescriptor);

		store = new ResourceContainerStoreService(new BmTestContext(SecurityContext.SYSTEM), domain, container);

		domainAdminCtx = BmTestContext.contextWithSession("d1", "admin", testDomainUid, SecurityContext.ROLE_ADMIN);
		domainAdminSC = domainAdminCtx.getSecurityContext();

		userSC = BmTestContext.contextWithSession("u1", "u1", testDomainUid).getSecurityContext();

		badDomainAdminSC = BmTestContext.contextWithSession("d2", "admin2", "fakeDomain", SecurityContext.ROLE_ADMIN)
				.getSecurityContext();

		new AclStore(systemCtx, JdbcActivator.getInstance().getDataSource()).store(container, //
				Arrays.asList( //
						AccessControlEntry.create(userSC.getSubject(), Verb.Read), //
						AccessControlEntry.create(domainAdminSC.getSubject(), Verb.All), //
						AccessControlEntry.create(badDomainAdminSC.getSubject(), Verb.All)));
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IResources service(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IResources.class, testDomainUid);

	}

	protected IMailboxes mailboxesService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IMailboxes.class, testDomainUid);

	}

	@Test
	public void testGet() throws Exception {
		String rtId = UUID.randomUUID().toString();
		store.create(rtId, defaultDescriptor());
		ResourceDescriptor ret = service(userSC).get(rtId);
		assertNotNull(ret);

		assertNull(service(userSC).get("fakeId"));
	}

	@Test
	public void getByEmail() throws ServerFault, SQLException {
		String rtId = UUID.randomUUID().toString();
		ResourceDescriptor r = defaultDescriptor();
		service(domainAdminSC).create(rtId, r);

		ItemValue<ResourceDescriptor> ret = service(userSC).byEmail(r.emails.iterator().next().address);
		assertNotNull(ret);
		assertEquals(rtId, ret.uid);
		assertNotNull(ret.value);
		assertEquals(1, ret.value.emails.size());

		assertNull(service(userSC).get("fakeId"));
	}

	@Test
	public void testUpdate() throws Exception {
		String rtId = UUID.randomUUID().toString();
		try {
			service(SecurityContext.SYSTEM).create(rtId, defaultDescriptor());

		} catch (Exception e) {
			fail("update initialization failed " + e.getMessage());
		}

		ResourceDescriptor rd = defaultDescriptor();
		rd.label = "updated";
		rd.emails = Arrays.asList(Email.create("test1@" + testDomainUid, true),
				Email.create("test2@" + testDomainUid, false));
		service(domainAdminSC).update(rtId, rd);
		ItemValue<DirEntryAndValue<ResourceDescriptor>> res = store.get(rtId, null);
		assertNotNull(res.value.vcard);
		assertEquals(2, res.value.vcard.communications.emails.size());
		Iterator<Email> it = res.value.mailbox.emails.iterator();
		assertEquals("test1@" + testDomainUid, it.next().address);
		assertEquals("test2@" + testDomainUid, it.next().address);

		ContainerStore cs = new ContainerStore(domainAdminCtx,
				DataSourceRouter.get(new BmTestContext(domainAdminSC), ICalendarUids.TYPE + ":" + rtId),
				SecurityContext.SYSTEM);
		Container calendarContainer = cs.get(ICalendarUids.TYPE + ":" + rtId);
		assertNotNull(calendarContainer);
		assertEquals(ICalendarUids.TYPE, calendarContainer.type);
		assertEquals(rd.label, calendarContainer.name);

		ItemValue<Mailbox> m = mailboxesService(domainAdminSC).getComplete(rtId);
		assertNotNull(m);
		assertEquals(rtId, m.uid);
		assertNotNull(m.value);
		assertEquals(Mailbox.Type.resource, m.value.type);
		assertTrue(m.value.system);
		assertTrue(m.value.hidden);

		MailFilter f = mailboxesService(domainAdminSC).getMailboxFilter(m.uid);
		assertNotNull(f);
		assertEquals(1, f.rules.size());
		assertEquals(true, f.rules.get(0).active);
		assertTrue(f.rules.get(0).conditions.isEmpty());
		assertNotNull(f.rules.get(0).discard().orElse(null));

		try {
			service(domainAdminSC).update("fakeId", rd);
			fail();
		} catch (ServerFault e) {
			// normal
		}

		// cant change type
		rd.typeIdentifier = "testType2";
		try {
			service(domainAdminSC).update(rtId, rd);
			fail();
		} catch (ServerFault e) {
			// normal
		}

		try {
			service(badDomainAdminSC).update(rtId, defaultDescriptor());
			fail();
		} catch (ServerFault e) {
			// normal
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testRestore() throws Exception {
		String rtId = UUID.randomUUID().toString();
		ResourceDescriptor rd = defaultDescriptor();
		ItemValue<ResourceDescriptor> rdItem = ItemValue.create(rtId, rd);
		rdItem.internalId = 73;
		rdItem.externalId = "external-" + System.currentTimeMillis();
		rdItem.displayName = "test";
		rdItem.created = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:44:21");
		rdItem.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		rdItem.version = 17;
		service(domainAdminSC).restore(rdItem, true);
		rdItem = store.get(rtId);

		rdItem.value.label = "updated";
		rdItem.value.emails = Arrays.asList(Email.create("test1@" + testDomainUid, true),
				Email.create("test2@" + testDomainUid, false));
		rdItem.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		rdItem.version = 23;

		service(domainAdminSC).restore(rdItem, false);

		ItemValue<DirEntryAndValue<ResourceDescriptor>> updatedItem = store.get(rtId, null);
		assertNotNull(updatedItem.value.vcard);
		assertEquals(2, updatedItem.value.vcard.communications.emails.size());
		Iterator<Email> it = updatedItem.value.mailbox.emails.iterator();
		assertEquals("test1@" + testDomainUid, it.next().address);
		assertEquals("test2@" + testDomainUid, it.next().address);

		assertEquals(rdItem.internalId, updatedItem.internalId);
		assertEquals(rdItem.uid, updatedItem.uid);
		assertEquals(rdItem.externalId, updatedItem.externalId);
		assertEquals(rdItem.created, updatedItem.created);
		// Call to mailboxes.setMailboxFilter change updated date and increment version
//		assertEquals(rdItem.updated, updatedItem.updated);
		assertEquals(rdItem.version + 1, updatedItem.version);
	}

	@Test
	public void testUpdateAlreadyUsedMail() throws Exception {
		String rtId = UUID.randomUUID().toString();
		store.create(rtId, defaultDescriptor());

		ResourceDescriptor desc = defaultDescriptor();
		desc.emails = Arrays.asList(Email.create("test2@" + testDomainUid, true),
				Email.create("testalreadyexists@" + testDomainUid, false));
		store.create("rt2", desc);

		try {
			desc = defaultDescriptor();
			desc.emails = Arrays.asList(Email.create("testalreadyexists@" + testDomainUid, true));
			service(domainAdminSC).update(rtId, desc);
			fail();
		} catch (ServerFault e) {
			// normal
		}

		ItemValue<DirEntryAndValue<ResourceDescriptor>> res = store.get(rtId, null);
		assertEquals(1, res.value.mailbox.emails.size());
		assertEquals(defaultDescriptor().emails.iterator().next().address,
				res.value.mailbox.emails.iterator().next().address);

	}

	@Test
	public void testDelete() throws Exception {
		String rtId = UUID.randomUUID().toString();
		service(domainAdminSC).create(rtId, defaultDescriptor());
		String datalocation = PopulateHelper.FAKE_CYRUS_IP;
		ContainerStore cs = new ContainerStore(domainAdminCtx, domainAdminCtx.getMailboxDataSource(datalocation),
				SecurityContext.SYSTEM);
		ContainerStore systemcs = new ContainerStore(domainAdminCtx, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		try {
			TaskRef tr = service(badDomainAdminSC).delete(rtId);
			TaskUtils.wait(ServerSideServiceProvider.getProvider(badDomainAdminSC), tr);
			fail();
		} catch (ServerFault e) {
			// normal
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		assertNotNull(systemcs.get("freebusy:" + rtId));

		TaskRef tr = service(domainAdminSC).delete(rtId);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(badDomainAdminSC), tr);
		assertNull(store.get(rtId, null));
		assertNull(cs.get(rtId));
		assertNull(systemcs.get("freebusy:" + rtId));

		tr = service(domainAdminSC).delete("fakeId");
		TaskStatus status = TaskUtils.wait(ServerSideServiceProvider.getProvider(badDomainAdminSC), tr);
		assertEquals(TaskStatus.State.InError, status.state);
	}

	@Test
	public void testCreate() throws Exception {
		String rtId = UUID.randomUUID().toString();

		try {
			service(badDomainAdminSC).create(rtId, defaultDescriptor());
			fail();
		} catch (ServerFault e) {
			// normal
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		service(domainAdminSC).create(rtId, defaultDescriptor());
		ItemValue<DirEntryAndValue<ResourceDescriptor>> res = store.get(rtId, null);
		assertNotNull(res);
		assertNotNull(res.value.vcard);
		assertEquals(1, res.value.vcard.communications.emails.size());
		assertEquals(defaultDescriptor().emails.iterator().next().address,
				res.value.mailbox.emails.iterator().next().address);

		ContainerStore cs = new ContainerStore(domainAdminCtx,
				DataSourceRouter.get(domainAdminCtx, ICalendarUids.TYPE + ":" + rtId), SecurityContext.SYSTEM);
		Container calendarContainer = cs.get(ICalendarUids.TYPE + ":" + rtId);
		assertNotNull(calendarContainer);
		assertEquals(ICalendarUids.TYPE, calendarContainer.type);

		ItemValue<Mailbox> m = mailboxesService(domainAdminSC).getComplete(rtId);
		assertNotNull(m);
		assertEquals(rtId, m.uid);
		assertNotNull(m.value);
		assertEquals(Mailbox.Type.resource, m.value.type);
		assertTrue(m.value.system);
		assertTrue(m.value.hidden);

		MailFilter f = mailboxesService(domainAdminSC).getMailboxFilter(m.uid);
		assertNotNull(f);
		assertEquals(1, f.rules.size());
		assertEquals(true, f.rules.get(0).active);
		assertTrue(f.rules.get(0).conditions.isEmpty());
		assertNotNull(f.rules.get(0).discard().orElse(null));

		ResourceDescriptor rd = defaultDescriptor();
		rd.typeIdentifier = "nonExistant";

		rd.typeIdentifier = "testType2";
		try {
			service(domainAdminSC).update(rtId, rd);
			fail();
		} catch (ServerFault e) {
			// normal
		}
	}

	@Test
	public void testRestoreCreate() throws Exception {
		String rtId = UUID.randomUUID().toString();
		ResourceDescriptor rd = defaultDescriptor();
		ItemValue<ResourceDescriptor> rdItem = ItemValue.create(rtId, rd);
		rdItem.internalId = 73;
		rdItem.externalId = "external-" + System.currentTimeMillis();
		rdItem.displayName = "test";
		rdItem.created = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:44:21");
		rdItem.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		rdItem.version = 17;

		service(domainAdminSC).restore(rdItem, true);
		ItemValue<ResourceDescriptor> createdItem = store.get(rtId);

		assertNotNull(createdItem);

		assertEquals(rdItem.internalId, createdItem.internalId);
		assertEquals(rdItem.uid, createdItem.uid);
		assertEquals(rdItem.externalId, createdItem.externalId);
		assertEquals(rdItem.created, createdItem.created);
		// Call to mailboxes.setMailboxFilter change updated date and increment version
//		assertEquals(rdItem.updated, createdItem.updated);
		assertEquals(rdItem.version + 1, createdItem.version);
	}

	@Test
	public void testCreateAlreadyUsedMail() throws Exception {
		String rtId = UUID.randomUUID().toString();
		store.create(rtId, defaultDescriptor());

		ResourceDescriptor desc = defaultDescriptor();
		desc.emails = Arrays.asList(Email.create("test2@" + testDomainUid, true),
				Email.create("testalreadyexists@" + testDomainUid, false));
		store.create("rt2", desc);

		try {
			desc = defaultDescriptor();
			desc.emails = Arrays.asList(Email.create("testalreadyexists@" + testDomainUid, true));
			service(domainAdminSC).create("shouldFaild", desc);
			fail();
		} catch (ServerFault e) {
			// normal
		}

		assertNull(store.get("shouldFaild", null));
	}

	@Test
	public void testSetIcon() throws Exception {
		String rtId = UUID.randomUUID().toString();
		service(domainAdminSC).create(rtId, defaultDescriptor());

		service(domainAdminSC).setIcon(rtId, image);

		// test bad image
		try {
			service(domainAdminSC).setIcon(rtId, "toto".getBytes());
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		// test only admin can set icon
		try {
			service(badDomainAdminSC).setIcon(rtId, image);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		// test set icon to inexistent resource type
		try {
			service(domainAdminSC).setIcon("fakeId", image);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}

	}

	@Test
	public void testGetIcon() throws Exception {
		String rtId = UUID.randomUUID().toString();
		service(domainAdminSC).create(rtId, defaultDescriptor());

		assertNotNull(service(domainAdminSC).getIcon(rtId));
		service(domainAdminSC).setIcon(rtId, image);

		assertNotNull(service(domainAdminSC).getIcon(rtId));
		assertTrue(Arrays.equals(image, service(domainAdminSC).getIcon(rtId)));

		// test everyone can read icon
		try {
			service(userSC).getIcon(rtId);

		} catch (ServerFault e) {
			fail("should not fail");
		}

		// test set icon to inexistent resource type
		assertNull(service(domainAdminSC).getIcon("fakeId"));
	}

	@Test
	public void testByType() {
		service(domainAdminSC).create(UUID.randomUUID().toString(), defaultDescriptor());

		ResourceDescriptor res = defaultDescriptor();
		res.typeIdentifier = "the-type";
		res.emails = Arrays
				.asList(Email.create(UUID.randomUUID().toString().toLowerCase() + "@" + testDomainUid, true));
		service(domainAdminSC).create("the-type", res);

		List<String> byType = service(domainAdminSC).byType("the-type");
		assertEquals(1, byType.size());
		assertEquals("the-type", byType.get(0));

	}

	private ResourceDescriptor defaultDescriptor() {
		ResourceDescriptor rd = new ResourceDescriptor();
		rd.label = "test 1";
		rd.description = "hi !";
		rd.typeIdentifier = "testType";
		rd.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		rd.emails = Arrays.asList(Email.create("test1@" + testDomainUid, true));
		rd.properties = Arrays.asList(ResourceDescriptor.PropertyValue.create("test1", "value1"),
				ResourceDescriptor.PropertyValue.create("test2", "true"),
				ResourceDescriptor.PropertyValue.create("test3", "1"),
				ResourceDescriptor.PropertyValue.create("test4", null));
		return rd;
	}

}
