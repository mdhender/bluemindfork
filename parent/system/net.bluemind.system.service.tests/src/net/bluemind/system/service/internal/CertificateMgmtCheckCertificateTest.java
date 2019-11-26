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
package net.bluemind.system.service.internal;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.service.certificate.SecurityMgmt;

public class CertificateMgmtCheckCertificateTest {

	private byte[] caData;
	private byte[] certData;
	private byte[] privateKeyData;

	private byte[] certData2;
	private byte[] privateKeyData2;

	@Before
	public void setUp() throws IOException {
		caData = Files.toByteArray(new File("data/certs/cacert.pem"));
		certData = Files.toByteArray(new File("data/certs/cert.pem"));
		certData2 = Files.toByteArray(new File("data/certs/cert2.pem"));
		privateKeyData = Files.toByteArray(new File("data/certs/privatekey"));
		privateKeyData2 = Files.toByteArray(new File("data/certs/privatekey2"));
	}

	@Test
	public void testCheck() throws ServerFault {
		SecurityMgmt.checkCertificate(caData, certData, privateKeyData);
	}

	@Test
	public void testCheckNotCA() {
		try {
			SecurityMgmt.checkCertificate(certData, certData, privateKeyData);
			Assert.fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testCheckNotCert() {
		try {
			SecurityMgmt.checkCertificate(caData, caData, privateKeyData);
			Assert.fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testCheck_Cert_dont_match_CA() {
		try {
			SecurityMgmt.checkCertificate(caData, certData2, privateKeyData);
			Assert.fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testCheck_pk_not_match_cert() {
		try {
			SecurityMgmt.checkCertificate(caData, certData, privateKeyData2);
			Assert.fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testCheck_invalidDatas() {
		try {
			SecurityMgmt.checkCertificate("test".getBytes(), certData, privateKeyData);
			Assert.fail();
		} catch (ServerFault e) {

		}

		try {
			SecurityMgmt.checkCertificate(caData, "test".getBytes(), privateKeyData);
			Assert.fail();
		} catch (ServerFault e) {
		}

		try {
			SecurityMgmt.checkCertificate(caData, certData, "test".getBytes());
			Assert.fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testCaChainUnorderedCheck() {
		try {
			byte[] caChainData = Files.toByteArray(new File("data/cert-BM-3891/ca-all-unordered.crt"));
			byte[] certChainData = Files.toByteArray(new File("data/cert-BM-3891/bm-tu.crt"));
			byte[] privateKey = Files.toByteArray(new File("data/cert-BM-3891/bm-tu.key"));

			SecurityMgmt.checkCertificate(caChainData, certChainData, privateKey);
		} catch (IOException | ServerFault e) {
			e.printStackTrace();
			Assert.fail("Test thrown an exception");
		}
	}

	@Test
	public void testCaChainCheck() {
		try {
			byte[] caChainData = Files.toByteArray(new File("data/cert-BM-3891/ca-all.crt"));
			byte[] certChainData = Files.toByteArray(new File("data/cert-BM-3891/bm-tu.crt"));
			byte[] privateKey = Files.toByteArray(new File("data/cert-BM-3891/bm-tu.key"));

			SecurityMgmt.checkCertificate(caChainData, certChainData, privateKey);
		} catch (IOException | ServerFault e) {
			e.printStackTrace();
			Assert.fail("Test thrown an exception");
		}
	}
}
