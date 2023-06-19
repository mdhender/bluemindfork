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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.smime.cacerts.api.ISmimeCACert;
import net.bluemind.smime.cacerts.api.ISmimeRevocation;
import net.bluemind.smime.cacerts.api.RevocationResult;
import net.bluemind.smime.cacerts.api.RevocationResult.RevocationStatus;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.smime.cacerts.api.SmimeCacertInfos;
import net.bluemind.smime.cacerts.api.SmimeCertClient;
import net.bluemind.smime.cacerts.api.SmimeRevocation;

public class SmimeRevocationServiceTests extends AbstractServiceTests {

	private Map<String, ItemValue<SmimeCacert>> cacertsList;
	private Map<String, SmimeRevocation> revocationsList;
	private List<SmimeCertClient> clientsList;

	@Test
	public void test_isNotRevoked() {
		List<SmimeCertClient> clients = Arrays
				.asList(SmimeCertClient.create("serial_not_revoked", "issuer_not_revoked"));
		// test anonymous
		try {
			getServiceRevocation(SecurityContext.ANONYMOUS, domainUid).areRevoked(clients);
			fail();
		} catch (ServerFault e) {
			assertTrue(ErrorCode.UNKNOWN.equals(e.getCode()) || ErrorCode.PERMISSION_DENIED.equals(e.getCode()));
		}

		try {
			Set<RevocationResult> revoked = getServiceRevocation(defaultSecurityContext, domainUid).areRevoked(clients);
			assertNotNull(revoked);
			assertEquals(1, revoked.size());
			RevocationResult revokedResult = revoked.iterator().next();
			assertEquals(RevocationStatus.NOT_REVOKED.name(), revokedResult.status.name());
			assertNotNull(revokedResult.revocation);
			assertNull(revokedResult.revocation.revocationDate);
			assertNull(revokedResult.revocation.revocationReason);
			assertNotNull(revokedResult.revocation.issuer);
			assertNotNull(revokedResult.revocation.serialNumber);
			assertEquals(clients.get(0).serialNumber, revokedResult.revocation.serialNumber);
			assertEquals(clients.get(0).issuer, revokedResult.revocation.issuer);
		} catch (ServerFault e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void test_isRevoked() throws Exception {
		insertTestData(null, null, null);

		List<SmimeCertClient> clients = new ArrayList<>();
		clients.addAll(clientsList);
		clients.add(SmimeCertClient.create("C1", "issuerC1"));

		Set<RevocationResult> revokedResult = null;
		try {
			revokedResult = getServiceRevocation(defaultSecurityContext, domainUid).areRevoked(clients);
		} catch (ServerFault e) {
			fail(e.getMessage());
		}

		assertNotNull(revokedResult);
		assertEquals(4, revokedResult.size());
		assertTrue(revokedResult.stream().anyMatch(r -> "A1".equals(r.revocation.serialNumber)));
		assertTrue(revokedResult.stream().anyMatch(r -> "A2".equals(r.revocation.serialNumber)));
		assertTrue(revokedResult.stream().anyMatch(r -> "B1".equals(r.revocation.serialNumber)));
		assertTrue(revokedResult.stream().anyMatch(r -> "C1".equals(r.revocation.serialNumber)));

		assertEquals(3, revokedResult.stream().filter(r -> RevocationStatus.REVOKED.equals(r.status)).count());
		assertTrue(revokedResult.stream().filter(r -> RevocationStatus.NOT_REVOKED.equals(r.status))
				.anyMatch(r -> "C1".equals(r.revocation.serialNumber)));

		clearTestData();

	}

	@Test
	public void test_refresh_uid() throws Exception {
		ItemValue<SmimeCacert> cacert = createCacert("uid1");
		// revocations fetched on SmimeCacert creation
		SmimeCacertInfos fetchedRevocations = getServiceRevocation(defaultSecurityContext, domainUid).fetch(cacert);
		List<SmimeCertClient> clients = fetchedRevocations.revocations.stream()
				.map(r -> SmimeCertClient.create(r.serialNumber, r.issuer)).toList();

		Set<RevocationResult> revokedResult = getServiceRevocation(defaultSecurityContext, domainUid)
				.areRevoked(clients);
		assertFalse(revokedResult.isEmpty());
		revokedResult.forEach(r -> {
			assertEquals(RevocationStatus.REVOKED, r.status);
		});

		getServiceRevocation(defaultSecurityContext, domainUid).refreshRevocations(cacert.uid);

		revokedResult = getServiceRevocation(defaultSecurityContext, domainUid).areRevoked(clients);
		assertFalse(revokedResult.isEmpty());
		assertEquals(fetchedRevocations.revocations.size(), revokedResult.size());
		revokedResult.forEach(r -> {
			assertEquals(RevocationStatus.REVOKED, r.status);
			assertTrue(fetchedRevocations.revocations.stream().map(rv -> rv.serialNumber).toList()
					.contains(r.revocation.serialNumber));
		});
	}

	@Test
	public void test_refresh_all() throws Exception {
		ItemValue<SmimeCacert> cacert = createCacert("uid1");
		// revocations fetched on SmimeCacert creation
		SmimeCacertInfos fetchedRevocations = getServiceRevocation(defaultSecurityContext, domainUid).fetch(cacert);
		List<SmimeCertClient> clients = fetchedRevocations.revocations.stream()
				.map(r -> SmimeCertClient.create(r.serialNumber, r.issuer)).toList();

		Set<RevocationResult> revoked = getServiceRevocation(defaultSecurityContext, domainUid).areRevoked(clients);
		assertFalse(revoked.isEmpty());
		revoked.forEach(r -> {
			assertEquals(RevocationStatus.REVOKED, r.status);
		});

		getServiceRevocation(defaultSecurityContext, domainUid).refreshDomainRevocations();

		revoked = getServiceRevocation(defaultSecurityContext, domainUid).areRevoked(clients);
		assertFalse(revoked.isEmpty());
		assertEquals(fetchedRevocations.revocations.size(), revoked.size());
		revoked.forEach(r -> {
			assertEquals(RevocationStatus.REVOKED, r.status);
			assertTrue(fetchedRevocations.revocations.stream().map(rv -> rv.serialNumber).toList()
					.contains(r.revocation.serialNumber));
		});
	}

	@Test
	public void test_fetch() throws Exception {
		ItemValue<SmimeCacert> cacert = createCacert("uid1");
		// revocations fetched on SmimeCacert creation
		SmimeCacertInfos fetchedRevocations = getServiceRevocation(defaultSecurityContext, domainUid).fetch(cacert);
		List<SmimeCertClient> clients = fetchedRevocations.revocations.stream()
				.map(r -> SmimeCertClient.create(r.serialNumber, r.issuer)).toList();

		Set<RevocationResult> revoked = getServiceRevocation(defaultSecurityContext, domainUid).areRevoked(clients);
		assertFalse(revoked.isEmpty());
		revoked.forEach(r -> {
			assertEquals(RevocationStatus.REVOKED, r.status);
		});
	}

	@Test
	public void test_getByNextUpdateDate() throws Exception {
		LocalDate localDate = LocalDate.now();
		Date now = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
		Date revokedDate = Date.from(localDate.minusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant());
		Date lastUpdate = Date.from(localDate.minusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant());
		Date nextUpdate = Date.from(localDate.minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant());
		insertTestData(revokedDate, lastUpdate, nextUpdate);

		Set<RevocationResult> revokedResult = getServiceRevocation(defaultSecurityContext, domainUid)
				.areRevoked(clientsList);
		assertFalse(revokedResult.isEmpty());
		revokedResult.forEach(r -> {
			assertEquals(RevocationStatus.REVOKED, r.status);
		});

		List<ItemValue<SmimeCacert>> byNextUpdateDate = getIncoreService(defaultSecurityContext, domainUid)
				.getByNextUpdateDate(now);
		assertFalse(byNextUpdateDate.isEmpty());
		clearTestData();
	}

	private void insertTestData(Date revokedDate, Date lastUpdate, Date nextUpdate) throws Exception {
		cacertsList = new HashMap<>();
		ItemValue<SmimeCacert> cacert = createCacert("uid1");
		assertNotNull(cacert);
		cacertsList.put("uid1", cacert);
		ItemValue<SmimeCacert> cacert2 = createCacert("uid2");
		assertNotNull(cacert2);
		cacertsList.put("uid2", cacert2);

		revocationsList = new HashMap<>();
		clientsList = new ArrayList<>();
		SmimeRevocation r1 = createRevocation("A1", revokedDate, "you're revoked", "crls_location", lastUpdate,
				nextUpdate, cacert);
		assertNotNull(r1);
		revocationsList.put("A1", r1);
		clientsList.add(SmimeCertClient.create(r1.serialNumber, r1.issuer));
		SmimeRevocation r2 = createRevocation("A2", revokedDate, "you're revoked", "crls_location2", lastUpdate,
				nextUpdate, cacert);
		assertNotNull(r2);
		revocationsList.put("A2", r2);
		clientsList.add(SmimeCertClient.create(r2.serialNumber, r2.issuer));
		SmimeRevocation r3 = createRevocation("B1", revokedDate, "", "", lastUpdate, nextUpdate, cacert2);
		assertNotNull(r3);
		revocationsList.put("B1", r3);
		clientsList.add(SmimeCertClient.create(r3.serialNumber, r3.issuer));
	}

	private void clearTestData() {
		cacertsList.clear();
		revocationsList.clear();
	}

	private ItemValue<SmimeCacert> createCacert(String uidpart) throws Exception {
		SmimeCacert cert = defaultSmimeCacert("data/trust-ca.crt.cer");
		String uid = uidpart + System.nanoTime();

		ISmimeCACert serviceCert = getServiceCacert(defaultSecurityContext, container.uid);
		serviceCert.create(uid, cert);

		ItemValue<SmimeCacert> smimeCert = serviceCert.getComplete(uid);
		assertNotNull(smimeCert);
		assertNotNull(smimeCert.item());

		return smimeCert;
	}

	private SmimeRevocation createRevocation(String serialNumber, Date revocationDate, String revocationReason,
			String url, Date lastUpdate, Date nextUpdate, ItemValue<SmimeCacert> cacert) {
		SmimeRevocation revocation = SmimeRevocation.create(serialNumber,
				revocationDate != null ? revocationDate : new Date(), revocationReason, url,
				lastUpdate != null ? lastUpdate : new Date(), nextUpdate != null ? nextUpdate : new Date(),
				"issuer" + cacert.uid, cacert.uid);
		IInCoreSmimeRevocation incoreService = getIncoreService(defaultSecurityContext, domainUid);
		incoreService.create(revocation, cacert);
		return incoreService.get(serialNumber, cacert);
	}

	@Override
	protected ISmimeRevocation getServiceRevocation(SecurityContext context, String domainUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ISmimeRevocation.class, domainUid);
	}

	@Override
	protected ISmimeCACert getServiceCacert(SecurityContext context, String containerUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ISmimeCACert.class, containerUid);
	}

	private IInCoreSmimeRevocation getIncoreService(SecurityContext context, String domainUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IInCoreSmimeRevocation.class, domainUid);
	}
}
