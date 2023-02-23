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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.List;

import org.junit.Test;

import com.google.common.io.Files;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.smime.cacerts.api.ISmimeCACert;
import net.bluemind.smime.cacerts.api.ISmimeRevocation;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.smime.cacerts.api.SmimeRevocation;
import net.bluemind.smime.cacerts.utils.CrlReader;
import net.bluemind.utils.CertificateUtils;

public class CrlReaderTests extends AbstractServiceTests {

	@Test
	public void test_readCrlFile() throws Exception {
		ItemValue<SmimeCacert> cacertItem = createCacert("uid1");

		X509Certificate caCert = (X509Certificate) CertificateUtils
				.generateX509Certificate(cacertItem.value.cert.getBytes());

		InputStream crlInputStream = new ByteArrayInputStream(Files.toByteArray(new File("data/trust-crl.crl")));
		CrlReader crlRead = new CrlReader(defaultContext, caCert, cacertItem.uid);
		crlRead.read(crlInputStream);
		List<SmimeRevocation> revocations = crlRead.createRevocations();
		assertNotNull(revocations);
		assertFalse(revocations.isEmpty());
		revocations.forEach(r -> {
			assertNotNull(r);
			assertNotNull(r.serialNumber);
			assertNotNull(r.revocationDate);
			assertNotNull(r.nextUpdate);
		});
	}

	@Test
	public void test_readCrlFile_invalid() throws Exception {
		ItemValue<SmimeCacert> cacertItem = createCacert("uid1", "data/cacert-test-invalid-sig.crt");

		X509Certificate caCert = (X509Certificate) CertificateUtils
				.generateX509Certificate(cacertItem.value.cert.getBytes());

		InputStream crlInputStream = new ByteArrayInputStream(
				Files.toByteArray(new File("data/crl-test-invalid-sig.crl")));
		CrlReader crlRead = new CrlReader(defaultContext, caCert, cacertItem.uid);
		crlRead.read(crlInputStream);
		List<SmimeRevocation> revocations = crlRead.createRevocations();
		assertNotNull(revocations);
		assertTrue(revocations.isEmpty());
		revocations.forEach(r -> {
			System.err.println(r.toString());
		});
	}

	@Test
	public void test_readEmptyCrlFile() throws Exception {
		ItemValue<SmimeCacert> cacertItem = createCacert("uid1");

		X509Certificate caCert = (X509Certificate) CertificateUtils
				.generateX509Certificate(cacertItem.value.cert.getBytes());

		InputStream crlInputStream = new ByteArrayInputStream(Files.toByteArray(new File("data/no-revocation.crl")));
		CrlReader crlRead = new CrlReader(defaultContext, caCert, cacertItem.uid);
		crlRead.read(crlInputStream);
		List<SmimeRevocation> revocations = crlRead.createRevocations();
		assertNotNull(revocations);
		assertTrue(revocations.isEmpty());
	}

	private ItemValue<SmimeCacert> createCacert(String uidpart) throws Exception {
		return createCacert(uidpart, "data/trust-ca.crt.cer");
	}

	private ItemValue<SmimeCacert> createCacert(String uidpart, String file) throws Exception {
		SmimeCacert cert = defaultSmimeCacert(file);
		String uid = uidpart + System.nanoTime();

		ISmimeCACert serviceCert = getServiceCacert(defaultSecurityContext, container.uid);
		serviceCert.create(uid, cert);

		ItemValue<SmimeCacert> smimeCert = serviceCert.getComplete(uid);
		assertNotNull(smimeCert);

		return smimeCert;
	}

	@Override
	protected ISmimeRevocation getServiceRevocation(SecurityContext context, String domainUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ISmimeRevocation.class, domainUid);
	}

	@Override
	protected ISmimeCACert getServiceCacert(SecurityContext context, String containerUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ISmimeCACert.class, containerUid);
	}

}
