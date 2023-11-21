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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogStoreConfig;
import net.bluemind.core.container.model.ChangeLogEntry;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.smime.cacerts.api.ISmimeCACert;
import net.bluemind.smime.cacerts.api.ISmimeRevocation;
import net.bluemind.smime.cacerts.api.SmimeCacert;

public class SmimeCacertServiceTests extends AbstractServiceTests {

	private static final String CA_FILE_PATH = "data/trust-ca.crt.cer";

	private final String dataStreamName = AuditLogStoreConfig.resolveDataStreamName(domainUid);

	@Test
	public void testCreate() throws Exception {
		SmimeCacert cert = defaultSmimeCacert(CA_FILE_PATH);
		String uid = "test_" + System.nanoTime();

		// test anonymous
		try {
			getServiceCacert(SecurityContext.ANONYMOUS, container.uid).create(uid, cert);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ISmimeCACert serviceCert = getServiceCacert(defaultSecurityContext, container.uid);
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

		getServiceCacert(defaultSecurityContext, container.uid).restore(cacertItem, true);

		Item createdItem = itemStore.get(cacertItem.uid);
		assertNotNull(createdItem);
		assertEquals(cacertItem.internalId, createdItem.id);
		assertEquals(cacertItem.uid, createdItem.uid);
		assertEquals(cacertItem.externalId, createdItem.externalId);
		assertEquals(cacertItem.created, createdItem.created);

		SmimeCacert cert = getServiceCacert(defaultSecurityContext, container.uid).get(createdItem.uid);
		assertNotNull(cert);
		assertNotNull(cert.cert);

		// test anonymous
		try {
			getServiceCacert(SecurityContext.ANONYMOUS, container.uid).restore(cacertItem, true);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		// test data validation
		try {
			cacertItem = defaultSmimeItem(43);
			cacertItem.value.cert = "New Cert Content";
			getServiceCacert(defaultSecurityContext, container.uid).restore(cacertItem, false);
			fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testUpdate() throws Exception {
		SmimeCacert cert = defaultSmimeCacert(CA_FILE_PATH);
		String uid = "test_" + System.nanoTime();
		getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);

		// test anonymous
		try {
			getServiceCacert(SecurityContext.ANONYMOUS, container.uid).update(uid, cert);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getServiceCacert(defaultSecurityContext, container.uid).update(uid, cert);
	}

	@Test
	public void testDelete() throws Exception {

		SmimeCacert cert = defaultSmimeCacert(CA_FILE_PATH);

		String uid = "test_" + System.nanoTime();
		getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);

		// test anonymous
		try {
			getServiceCacert(SecurityContext.ANONYMOUS, container.uid).delete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getServiceCacert(defaultSecurityContext, container.uid).delete(uid);

		ItemValue<SmimeCacert> cacert = getServiceCacert(defaultSecurityContext, container.uid).getComplete(uid);
		assertNull(cacert);
	}

	@Test
	public void testReset() throws Exception {

		List<ItemValue<SmimeCacert>> certItems = getServiceCacert(defaultSecurityContext, container.uid).all();
		assertNotNull(certItems);
		assertEquals(0, certItems.size());

		SmimeCacert cert = defaultSmimeCacert(CA_FILE_PATH);
		String uid = "cert-one";
		getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);
		cert = defaultSmimeCacert(CA_FILE_PATH);
		uid = "cert-two";
		getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);
		cert = defaultSmimeCacert(CA_FILE_PATH);
		uid = "cert-three";
		getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);
		// test anonymous
		try {
			getServiceCacert(SecurityContext.ANONYMOUS, container.uid).reset();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getServiceCacert(defaultSecurityContext, container.uid).reset();

		certItems = getServiceCacert(defaultSecurityContext, container.uid).all();
		assertNotNull(certItems);
		assertEquals(0, certItems.size());
	}

	@Test
	public void testGetComplete() throws Exception {
		SmimeCacert cert = defaultSmimeCacert(CA_FILE_PATH);
		String uid = "test_" + System.nanoTime();
		getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);

		// test anonymous
		try {
			getServiceCacert(SecurityContext.ANONYMOUS, container.uid).getComplete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ItemValue<SmimeCacert> certItem = getServiceCacert(defaultSecurityContext, container.uid).getComplete(uid);
		assertNotNull(certItem);
		assertEquals(uid, certItem.uid);

		certItem = getServiceCacert(defaultSecurityContext, container.uid).getComplete("nonExistant");
		assertNull(certItem);
	}

	@Test
	public void testAll() throws Exception {

		List<ItemValue<SmimeCacert>> certItems = getServiceCacert(defaultSecurityContext, container.uid).all();
		assertNotNull(certItems);
		assertEquals(0, certItems.size());

		SmimeCacert cert = defaultSmimeCacert(CA_FILE_PATH);
		String uid = "cert-one";
		getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);
		cert = defaultSmimeCacert(CA_FILE_PATH);
		uid = "cert-two";
		getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);
		cert = defaultSmimeCacert(CA_FILE_PATH);
		uid = "cert-three";
		getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);
		// test anonymous
		try {
			getServiceCacert(SecurityContext.ANONYMOUS, container.uid).all();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		certItems = getServiceCacert(defaultSecurityContext, container.uid).all();
		assertNotNull(certItems);

		assertEquals(3, certItems.size());
	}

	@Test
	public void testMultipleGet() throws Exception {
		SmimeCacert cert = defaultSmimeCacert(CA_FILE_PATH);
		String uid = UUID.randomUUID().toString();
		getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);

		cert = defaultSmimeCacert(CA_FILE_PATH);
		String uid2 = UUID.randomUUID().toString();
		getServiceCacert(defaultSecurityContext, container.uid).create(uid2, cert);

		List<ItemValue<SmimeCacert>> items = getServiceCacert(defaultSecurityContext, container.uid)
				.multipleGet(Arrays.asList(uid, uid2));
		assertNotNull(items);
		assertEquals(2, items.size());

		items = getServiceCacert(defaultSecurityContext, container.uid).multipleGet(Arrays.asList("nonExistant"));

		assertNotNull(items);
		assertEquals(0, items.size());

		try {
			getServiceCacert(SecurityContext.ANONYMOUS, container.uid).multipleGet(Arrays.asList(uid, uid2));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testMultipleGetById() throws Exception {
		SmimeCacert cert = defaultSmimeCacert(CA_FILE_PATH);
		String uid = UUID.randomUUID().toString();
		getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);

		cert = defaultSmimeCacert(CA_FILE_PATH);
		String uid2 = UUID.randomUUID().toString();
		getServiceCacert(defaultSecurityContext, container.uid).create(uid2, cert);

		List<ItemValue<SmimeCacert>> items = getServiceCacert(defaultSecurityContext, container.uid)
				.multipleGet(Arrays.asList(uid, uid2));
		assertNotNull(items);
		assertEquals(2, items.size());

		try {
			getServiceCacert(SecurityContext.ANONYMOUS, container.uid)
					.multipleGetById(Arrays.asList(items.get(0).internalId, items.get(1).internalId));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		items = getServiceCacert(defaultSecurityContext, container.uid)
				.multipleGetById(Arrays.asList(items.get(0).internalId, items.get(1).internalId));
		assertNotNull(items);
		assertEquals(2, items.size());

		items = getServiceCacert(defaultSecurityContext, container.uid)
				.multipleGetById(Arrays.asList(9876543L, 34567L));
		assertNotNull(items);
		assertEquals(0, items.size());

	}

	@Test
	public void testAllUids() throws Exception {

		List<ItemValue<SmimeCacert>> certItems = getServiceCacert(defaultSecurityContext, container.uid).all();
		assertNotNull(certItems);
		assertEquals(0, certItems.size());

		SmimeCacert cert = defaultSmimeCacert(CA_FILE_PATH);
		String uid = "cert-one";
		getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);
		cert = defaultSmimeCacert(CA_FILE_PATH);
		uid = "cert-two";
		getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);
		cert = defaultSmimeCacert(CA_FILE_PATH);
		uid = "cert-three";
		getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);
		// test anonymous
		try {
			getServiceCacert(SecurityContext.ANONYMOUS, container.uid).allUids();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		List<String> uids = getServiceCacert(defaultSecurityContext, container.uid).allUids();
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
			getServiceCacert(defaultSecurityContext, container.uid).create(uid, cert);
			fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testChangeset() throws Exception {

		getServiceCacert(defaultSecurityContext, container.uid).create("test1", defaultSmimeCacert(CA_FILE_PATH));
		getServiceCacert(defaultSecurityContext, container.uid).create("test2", defaultSmimeCacert(CA_FILE_PATH));
		getServiceCacert(defaultSecurityContext, container.uid).delete("test1");
		getServiceCacert(defaultSecurityContext, container.uid).update("test2", defaultSmimeCacert(CA_FILE_PATH));

		// begin tests
		ContainerChangeset<String> changeset = getServiceCacert(defaultSecurityContext, container.uid).changeset(null);

		assertEquals(1, changeset.created.size());
		assertEquals("test2", changeset.created.get(0));

		assertEquals(0, changeset.deleted.size());

		getServiceCacert(defaultSecurityContext, container.uid).delete("test2");
		changeset = getServiceCacert(defaultSecurityContext, container.uid).changeset(changeset.version);

		assertEquals(0, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(1, changeset.deleted.size());
		assertEquals("test2", changeset.deleted.get(0));
	}

	@Test
	public void testChangesetById() throws Exception {

		getServiceCacert(defaultSecurityContext, container.uid).create("test1", defaultSmimeCacert(CA_FILE_PATH));
		getServiceCacert(defaultSecurityContext, container.uid).create("test2", defaultSmimeCacert(CA_FILE_PATH));
		getServiceCacert(defaultSecurityContext, container.uid).delete("test1");
		getServiceCacert(defaultSecurityContext, container.uid).update("test2", defaultSmimeCacert(CA_FILE_PATH));

		// begin tests
		ContainerChangeset<Long> changeset = getServiceCacert(defaultSecurityContext, container.uid)
				.changesetById(null);
		assertEquals(1, changeset.created.size());
		Long id = changeset.created.get(0);
		assertEquals(0, changeset.deleted.size());

		getServiceCacert(defaultSecurityContext, container.uid).delete("test2");
		changeset = getServiceCacert(defaultSecurityContext, container.uid).changesetById(changeset.version);

		assertEquals(0, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(1, changeset.deleted.size());
		assertEquals(id, changeset.deleted.get(0));
	}

	@Test
	public void testItemChangelog() throws Exception {

		getServiceCacert(defaultSecurityContext, container.uid).create("test1", defaultSmimeCacert(CA_FILE_PATH));
		getServiceCacert(defaultSecurityContext, container.uid).update("test1", defaultSmimeCacert(CA_FILE_PATH));
		getServiceCacert(defaultSecurityContext, container.uid).create("test2", defaultSmimeCacert(CA_FILE_PATH));
		getServiceCacert(defaultSecurityContext, container.uid).delete("test1");
		getServiceCacert(defaultSecurityContext, container.uid).update("test2", defaultSmimeCacert(CA_FILE_PATH));
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			ItemChangelog itemChangeLog = getServiceCacert(defaultSecurityContext, container.uid).itemChangelog("test1",
					0L);
			return 3 == itemChangeLog.entries.size();
		});
		ItemChangelog itemChangeLog = getServiceCacert(defaultSecurityContext, container.uid).itemChangelog("test1",
				0L);
		assertEquals(3, itemChangeLog.entries.size());
		assertEquals(ChangeLogEntry.Type.Created, itemChangeLog.entries.get(0).type);
		assertEquals(ChangeLogEntry.Type.Updated, itemChangeLog.entries.get(1).type);
		assertEquals(ChangeLogEntry.Type.Deleted, itemChangeLog.entries.get(2).type);

		itemChangeLog = getServiceCacert(defaultSecurityContext, container.uid).itemChangelog("test2", 0L);
		assertEquals(2, itemChangeLog.entries.size());
		assertEquals(ChangeLogEntry.Type.Created, itemChangeLog.entries.get(0).type);
		assertEquals(ChangeLogEntry.Type.Updated, itemChangeLog.entries.get(1).type);

	}

	@Test
	public void testDeleteUnknownEvent() throws ServerFault {
		try {
			getServiceCacert(defaultSecurityContext, container.uid).delete(UUID.randomUUID().toString());
			fail();
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Test
	public void testUpdateUnknownEvent() throws Exception {
		try {
			getServiceCacert(defaultSecurityContext, container.uid).update(UUID.randomUUID().toString(),
					defaultSmimeCacert(CA_FILE_PATH));
			fail();
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Override
	protected ISmimeCACert getServiceCacert(SecurityContext context, String containerUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ISmimeCACert.class, containerUid);
	}

	private ItemValue<SmimeCacert> defaultSmimeItem(long id) throws Exception {
		Item item = new Item();
		item.id = id;
		item.uid = "test_" + System.nanoTime();
		item.externalId = "externalId" + System.nanoTime();
		item.displayName = "test";
		item.created = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:44:21");
		item.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		item.version = 17;
		return ItemValue.create(item, defaultSmimeCacert("data/trust-ca.crt.cer"));
	}

	@Override
	protected ISmimeRevocation getServiceRevocation(SecurityContext context, String containerUid) throws ServerFault {
		return null;
	}
}
