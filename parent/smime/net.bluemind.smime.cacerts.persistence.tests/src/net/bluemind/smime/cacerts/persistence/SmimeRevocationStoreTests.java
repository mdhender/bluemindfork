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
package net.bluemind.smime.cacerts.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.smime.cacerts.api.SmimeRevocation;

public class SmimeRevocationStoreTests extends AbstractStoreTests {
	private static Logger logger = LoggerFactory.getLogger(SmimeRevocationStoreTests.class);
	private SmimeRevocationStore revocationStore;
	private ItemValue<SmimeCacert> cacertItem;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime();
		Container container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);

		assertNotNull(container);

		// create SmimeCacert item
		ItemStore itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

		SmimeCacertStore smimeStore = new SmimeCacertStore(JdbcTestHelper.getInstance().getDataSource(), container);
		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(uid);
		assertNotNull(item);

		SmimeCacert cacert = defaultSmimeCacert();
		smimeStore.create(item, cacert);
		SmimeCacert td = smimeStore.get(item);
		assertNotNull(td);
		cacertItem = ItemValue.create(item, td);

		revocationStore = new SmimeRevocationStore(JdbcTestHelper.getInstance().getDataSource(), container);
	}

	@Test
	public void testSchemaIsWellRegistered() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("smime-revocation-schema"));
	}

	@Test
	public void testStoreAndRetrieveAndDelete() throws SQLException {
		SmimeRevocation revocation = defaultSmimeRevocation(cacertItem, "serial_number_" + System.currentTimeMillis(),
				new Date());
		revocationStore.create(revocation, cacertItem);

		List<SmimeRevocation> fetchList = revocationStore.all();
		checkRevocation(fetchList, revocation);

		revocationStore.delete(cacertItem);
		fetchList = revocationStore.all();
		assertTrue(fetchList.isEmpty());
	}

	@Test
	public void testGetByCacert() throws SQLException {
		SmimeRevocation revocation = createRevocation("1", null);

		List<SmimeRevocation> fetchList = revocationStore.getByCacert(cacertItem);
		checkRevocation(fetchList, revocation);
	}

	@Test
	public void testGetBySn() throws SQLException {
		SmimeRevocation revocation = createRevocation("1", null);

		List<SmimeRevocation> fetchList = revocationStore.getBySn(revocation.serialNumber);
		checkRevocation(fetchList, revocation);
	}

	@Test
	public void getByCertClient() throws SQLException {
		final SmimeRevocation revocation = createRevocation("1", null);

		SmimeRevocation fetch = revocationStore.getByCertClient(revocation);
		checkRevocation(Arrays.asList(fetch), revocation);
	}

	@Test
	public void testGetBySnList() throws SQLException {
		SmimeRevocation r1 = createRevocation("A1", null);
		SmimeRevocation r2 = createRevocation("A2", null);

		List<SmimeRevocation> fetchList = revocationStore
				.getBySnList(Arrays.asList(r1.serialNumber, r2.serialNumber, "B1"));
		assertNotNull(fetchList);
		assertEquals(2, fetchList.size());
	}

	@Test
	public void testGetBySnAndCacert() throws SQLException {
		SmimeRevocation revocation = createRevocation("1", null);

		SmimeRevocation fetch = revocationStore.getBySn(revocation.serialNumber, cacertItem);
		checkRevocation(Arrays.asList(fetch), revocation);
	}

	@Test
	public void testGetByNextUpdateDate() throws SQLException {
		LocalDate localDate = LocalDate.now();
		Date date = Date.from(localDate.minusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant());
		SmimeRevocation revocation = createRevocation("1", date);

		List<SmimeRevocation> fetchList = revocationStore.getByNextUpdateDate(Timestamp.from(Instant.now()));
		assertFalse(fetchList.isEmpty());
	}

	private static void checkRevocation(List<SmimeRevocation> crlList, SmimeRevocation created) {
		assertNotNull(crlList);
		assertFalse(crlList.isEmpty());
		assertEquals(1, crlList.size());
		SmimeRevocation smimeRevocation = crlList.get(0);
		assertEquals(created.serialNumber, smimeRevocation.serialNumber);
		assertEquals(created.revocationDate, smimeRevocation.revocationDate);
		assertEquals(created.revocationReason, smimeRevocation.revocationReason);
		assertEquals(created.url, smimeRevocation.url);
		assertEquals(created.lastUpdate, smimeRevocation.lastUpdate);
		assertEquals(created.nextUpdate, smimeRevocation.nextUpdate);
		assertEquals(created.cacertItemUid, smimeRevocation.cacertItemUid);
	}

	private SmimeRevocation createRevocation(String snPrefix, Date date) {
		try {
			SmimeRevocation revocation = defaultSmimeRevocation(cacertItem,
					snPrefix + "serial_number_" + System.currentTimeMillis(), date == null ? new Date() : date);
			revocationStore.create(revocation, cacertItem);
			return revocation;
		} catch (SQLException e) {
			logger.error("error during S/MIME revocations persistence call", e);
			fail(e.getMessage());
			return null;
		}

	}

}
