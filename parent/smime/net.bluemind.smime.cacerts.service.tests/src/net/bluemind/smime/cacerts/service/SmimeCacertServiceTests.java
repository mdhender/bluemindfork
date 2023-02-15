/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.smime.cacerts.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ChangeLogEntry;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.smime.cacerts.api.ISmimeCACert;
import net.bluemind.smime.cacerts.api.ISmimeCRL;
import net.bluemind.smime.cacerts.api.SmimeCacert;

public class SmimeCacertServiceTests extends AbstractServiceTests {

	@Test
	public void testCreate() throws ServerFault, SQLException {
		SmimeCacert cert = defaultSmimeCacert();
		String uid = "test_" + System.nanoTime();

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS, container.uid).create(uid, cert);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ISmimeCACert serviceCert = getService(defaultSecurityContext, container.uid);
		try {
			serviceCert.create(uid, cert);
		} catch (ServerFault e) {
			fail(e.getMessage());
		}

		Item item = itemStore.get(uid);
		assertNotNull(item);
		ItemValue<SmimeCacert> smimeCert = serviceCert.getComplete(item.uid);
		assertNotNull(smimeCert);
		assertNotNull(smimeCert.value);

		// invalid cert
		cert.cert = "coucou";
		try {
			serviceCert.create(uid, cert);
			fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testRestoreCreate() throws Exception {

		ItemValue<SmimeCacert> cacertItem = defaultSmimeItem(42);

		getService(defaultSecurityContext, container.uid).restore(cacertItem, true);

		Item createdItem = itemStore.get(cacertItem.uid);
		assertNotNull(createdItem);
		assertEquals(cacertItem.internalId, createdItem.id);
		assertEquals(cacertItem.uid, createdItem.uid);
		assertEquals(cacertItem.externalId, createdItem.externalId);
		assertEquals(cacertItem.created, createdItem.created);

		SmimeCacert cert = getService(defaultSecurityContext, container.uid).get(createdItem.uid);
		assertNotNull(cert);
		assertNotNull(cert.cert);

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS, container.uid).restore(cacertItem, true);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		// test data validation
		try {
			cacertItem = defaultSmimeItem(43);
			cacertItem.value.cert = "New Cert Content";
			getService(defaultSecurityContext, container.uid).restore(cacertItem, false);
			fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testUpdate() throws ServerFault {
		SmimeCacert cert = defaultSmimeCacert();
		String uid = "test_" + System.nanoTime();
		getService(defaultSecurityContext, container.uid).create(uid, cert);

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS, container.uid).update(uid, cert);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService(defaultSecurityContext, container.uid).update(uid, cert);
	}

	@Test
	public void testDelete() throws ServerFault {

		SmimeCacert cert = defaultSmimeCacert();

		String uid = "test_" + System.nanoTime();
		getService(defaultSecurityContext, container.uid).create(uid, cert);

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS, container.uid).delete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService(defaultSecurityContext, container.uid).delete(uid);

		ItemValue<SmimeCacert> certItem = getService(defaultSecurityContext, container.uid).getComplete(uid);
		assertNull(certItem);

	}

	@Test
	public void testReset() throws ServerFault {

		List<ItemValue<SmimeCacert>> certItems = getService(defaultSecurityContext, container.uid).all();
		assertNotNull(certItems);
		assertEquals(0, certItems.size());

		SmimeCacert cert = defaultSmimeCacert();
		String uid = "cert-one";
		getService(defaultSecurityContext, container.uid).create(uid, cert);
		cert = defaultSmimeCacert();
		uid = "cert-two";
		getService(defaultSecurityContext, container.uid).create(uid, cert);
		cert = defaultSmimeCacert();
		uid = "cert-three";
		getService(defaultSecurityContext, container.uid).create(uid, cert);
		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS, container.uid).reset();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService(defaultSecurityContext, container.uid).reset();

		certItems = getService(defaultSecurityContext, container.uid).all();
		assertNotNull(certItems);
		assertEquals(0, certItems.size());
	}

	@Test
	public void testGetComplete() throws ServerFault {
		SmimeCacert cert = defaultSmimeCacert();
		String uid = "test_" + System.nanoTime();
		getService(defaultSecurityContext, container.uid).create(uid, cert);

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS, container.uid).getComplete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ItemValue<SmimeCacert> certItem = getService(defaultSecurityContext, container.uid).getComplete(uid);
		assertNotNull(certItem);

		assertEquals(uid, certItem.uid);
		certItem = getService(defaultSecurityContext, container.uid).getComplete("nonExistant");
		assertNull(certItem);
	}

	@Test
	public void testAll() throws ServerFault {

		List<ItemValue<SmimeCacert>> certItems = getService(defaultSecurityContext, container.uid).all();
		assertNotNull(certItems);
		assertEquals(0, certItems.size());

		SmimeCacert cert = defaultSmimeCacert();
		String uid = "cert-one";
		getService(defaultSecurityContext, container.uid).create(uid, cert);
		cert = defaultSmimeCacert();
		uid = "cert-two";
		getService(defaultSecurityContext, container.uid).create(uid, cert);
		cert = defaultSmimeCacert();
		uid = "cert-three";
		getService(defaultSecurityContext, container.uid).create(uid, cert);
		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS, container.uid).all();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		certItems = getService(defaultSecurityContext, container.uid).all();
		assertNotNull(certItems);

		assertEquals(3, certItems.size());
	}

	@Test
	public void testMultipleGet() throws ServerFault {
		SmimeCacert cert = defaultSmimeCacert();
		String uid = UUID.randomUUID().toString();
		getService(defaultSecurityContext, container.uid).create(uid, cert);

		cert = defaultSmimeCacert();
		String uid2 = UUID.randomUUID().toString();
		getService(defaultSecurityContext, container.uid).create(uid2, cert);

		List<ItemValue<SmimeCacert>> items = getService(defaultSecurityContext, container.uid)
				.multipleGet(Arrays.asList(uid, uid2));
		assertNotNull(items);
		assertEquals(2, items.size());

		items = getService(defaultSecurityContext, container.uid).multipleGet(Arrays.asList("nonExistant"));

		assertNotNull(items);
		assertEquals(0, items.size());

		try {
			getService(SecurityContext.ANONYMOUS, container.uid).multipleGet(Arrays.asList(uid, uid2));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testMultipleGetById() throws ServerFault {
		SmimeCacert cert = defaultSmimeCacert();
		String uid = UUID.randomUUID().toString();
		getService(defaultSecurityContext, container.uid).create(uid, cert);

		cert = defaultSmimeCacert();
		String uid2 = UUID.randomUUID().toString();
		getService(defaultSecurityContext, container.uid).create(uid2, cert);

		List<ItemValue<SmimeCacert>> items = getService(defaultSecurityContext, container.uid)
				.multipleGet(Arrays.asList(uid, uid2));
		assertNotNull(items);
		assertEquals(2, items.size());

		try {
			getService(SecurityContext.ANONYMOUS, container.uid)
					.multipleGetById(Arrays.asList(items.get(0).internalId, items.get(1).internalId));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		items = getService(defaultSecurityContext, container.uid)
				.multipleGetById(Arrays.asList(items.get(0).internalId, items.get(1).internalId));
		assertNotNull(items);
		assertEquals(2, items.size());

		items = getService(defaultSecurityContext, container.uid).multipleGetById(Arrays.asList(9876543L, 34567L));
		assertNotNull(items);
		assertEquals(0, items.size());

	}

	@Test
	public void testAllUids() throws ServerFault {

		List<ItemValue<SmimeCacert>> certItems = getService(defaultSecurityContext, container.uid).all();
		assertNotNull(certItems);
		assertEquals(0, certItems.size());

		SmimeCacert cert = defaultSmimeCacert();
		String uid = "cert-one";
		getService(defaultSecurityContext, container.uid).create(uid, cert);
		cert = defaultSmimeCacert();
		uid = "cert-two";
		getService(defaultSecurityContext, container.uid).create(uid, cert);
		cert = defaultSmimeCacert();
		uid = "cert-three";
		getService(defaultSecurityContext, container.uid).create(uid, cert);
		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS, container.uid).allUids();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		List<String> uids = getService(defaultSecurityContext, container.uid).allUids();
		assertNotNull(uids);
		assertEquals(3, uids.size());
		assertTrue(uids.contains("cert-one"));
		assertTrue(uids.contains("cert-two"));
		assertTrue(uids.contains("cert-three"));
	}

	@Test
	public void testCreateImproperSmimeCert() throws ServerFault {
		SmimeCacert cert = null;
		String uid = "test_" + System.nanoTime();

		try {
			getService(defaultSecurityContext, container.uid).create(uid, cert);
			fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testChangelog() throws ServerFault {

		getService(defaultSecurityContext, container.uid).create("test1", defaultSmimeCacert());
		getService(defaultSecurityContext, container.uid).create("test2", defaultSmimeCacert());
		getService(defaultSecurityContext, container.uid).delete("test1");
		getService(defaultSecurityContext, container.uid).update("test2", defaultSmimeCacert());

		// begin tests
		ContainerChangelog log = getService(defaultSecurityContext, container.uid).containerChangelog(null);

		assertEquals(4, log.entries.size());

		for (ChangeLogEntry entry : log.entries) {
			System.out.println(entry.version);
		}
		log = getService(defaultSecurityContext, container.uid).containerChangelog(log.entries.get(0).version);
		assertEquals(3, log.entries.size());
	}

	@Test
	public void testChangeset() throws ServerFault {

		getService(defaultSecurityContext, container.uid).create("test1", defaultSmimeCacert());
		getService(defaultSecurityContext, container.uid).create("test2", defaultSmimeCacert());
		getService(defaultSecurityContext, container.uid).delete("test1");
		getService(defaultSecurityContext, container.uid).update("test2", defaultSmimeCacert());

		// begin tests
		ContainerChangeset<String> changeset = getService(defaultSecurityContext, container.uid).changeset(null);

		assertEquals(1, changeset.created.size());
		assertEquals("test2", changeset.created.get(0));

		assertEquals(0, changeset.deleted.size());

		getService(defaultSecurityContext, container.uid).delete("test2");
		changeset = getService(defaultSecurityContext, container.uid).changeset(changeset.version);

		assertEquals(0, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(1, changeset.deleted.size());
		assertEquals("test2", changeset.deleted.get(0));
	}

	@Test
	public void testChangesetById() throws ServerFault {

		getService(defaultSecurityContext, container.uid).create("test1", defaultSmimeCacert());
		getService(defaultSecurityContext, container.uid).create("test2", defaultSmimeCacert());
		getService(defaultSecurityContext, container.uid).delete("test1");
		getService(defaultSecurityContext, container.uid).update("test2", defaultSmimeCacert());

		// begin tests
		ContainerChangeset<Long> changeset = getService(defaultSecurityContext, container.uid).changesetById(null);
		assertEquals(1, changeset.created.size());
		Long id = changeset.created.get(0);
		assertEquals(0, changeset.deleted.size());

		getService(defaultSecurityContext, container.uid).delete("test2");
		changeset = getService(defaultSecurityContext, container.uid).changesetById(changeset.version);

		assertEquals(0, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(1, changeset.deleted.size());
		assertEquals(id, changeset.deleted.get(0));
	}

	@Test
	public void testItemChangelog() throws ServerFault {

		getService(defaultSecurityContext, container.uid).create("test1", defaultSmimeCacert());
		getService(defaultSecurityContext, container.uid).update("test1", defaultSmimeCacert());
		getService(defaultSecurityContext, container.uid).create("test2", defaultSmimeCacert());
		getService(defaultSecurityContext, container.uid).delete("test1");
		getService(defaultSecurityContext, container.uid).update("test2", defaultSmimeCacert());

		ItemChangelog itemChangeLog = getService(defaultSecurityContext, container.uid).itemChangelog("test1", 0L);
		assertEquals(3, itemChangeLog.entries.size());
		assertEquals(ChangeLogEntry.Type.Created, itemChangeLog.entries.get(0).type);
		assertEquals(ChangeLogEntry.Type.Updated, itemChangeLog.entries.get(1).type);
		assertEquals(ChangeLogEntry.Type.Deleted, itemChangeLog.entries.get(2).type);

		itemChangeLog = getService(defaultSecurityContext, container.uid).itemChangelog("test2", 0L);
		assertEquals(2, itemChangeLog.entries.size());
		assertEquals(ChangeLogEntry.Type.Created, itemChangeLog.entries.get(0).type);
		assertEquals(ChangeLogEntry.Type.Updated, itemChangeLog.entries.get(1).type);

	}

	@Test
	public void testDeleteUnknownEvent() throws ServerFault {
		try {
			getService(defaultSecurityContext, container.uid).delete(UUID.randomUUID().toString());
			fail();
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Test
	public void testUpdateUnknownEvent() throws ServerFault {
		try {
			getService(defaultSecurityContext, container.uid).update(UUID.randomUUID().toString(),
					defaultSmimeCacert());
			fail();
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Override
	protected ISmimeCACert getService(SecurityContext context, String containerUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ISmimeCACert.class, containerUid);
	}

	private ItemValue<SmimeCacert> defaultSmimeItem(long id) throws ParseException {
		Item item = new Item();
		item.id = id;
		item.uid = "test_" + System.nanoTime();
		item.externalId = "externalId" + System.nanoTime();
		item.displayName = "test";
		item.created = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:44:21");
		item.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		item.version = 17;
		return ItemValue.create(item, defaultSmimeCacert());
	}

	@Override
	protected ISmimeCRL getServiceCrl(SecurityContext context, String containerUid) throws ServerFault {
		return null;
	}
}
