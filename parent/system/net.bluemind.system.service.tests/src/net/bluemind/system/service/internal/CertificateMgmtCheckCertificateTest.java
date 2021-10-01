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
import net.bluemind.system.api.CertData;
import net.bluemind.system.service.certificate.SecurityMgmt;

public class CertificateMgmtCheckCertificateTest {

	private byte[] caData;
	private byte[] certificateData;
	private byte[] privateKeyData;

	private byte[] certificateData2;
	private byte[] privateKeyData2;

	@Before
	public void setUp() throws IOException {
		caData = Files.toByteArray(new File("data/certs/cacert.pem"));
		certificateData = Files.toByteArray(new File("data/certs/cert.pem"));
		certificateData2 = Files.toByteArray(new File("data/certs/cert2.pem"));
		privateKeyData = Files.toByteArray(new File("data/certs/privatekey"));
		privateKeyData2 = Files.toByteArray(new File("data/certs/privatekey2"));
	}

	@Test
	public void testCheck() throws ServerFault {

		CertData certData = new CertData();
		certData.certificateAuthority = new String(caData);
		certData.certificate = new String(certificateData);
		certData.privateKey = new String(privateKeyData);

		SecurityMgmt.checkCertificate(certData);
	}

	@Test
	public void testCheckNotCA() {
		try {
			CertData certData = new CertData();
			certData.certificateAuthority = new String(certificateData);
			certData.certificate = new String(certificateData);
			certData.privateKey = new String(privateKeyData);

			SecurityMgmt.checkCertificate(certData);
			Assert.fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testCheckNotCert() {
		try {
			CertData certData = new CertData();
			certData.certificateAuthority = new String(caData);
			certData.certificate = new String(caData);
			certData.privateKey = new String(privateKeyData);

			SecurityMgmt.checkCertificate(certData);
			Assert.fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testCheck_Cert_dont_match_CA() {
		try {
			CertData certData = new CertData();
			certData.certificateAuthority = new String(caData);
			certData.certificate = new String(certificateData2);
			certData.privateKey = new String(privateKeyData);

			SecurityMgmt.checkCertificate(certData);
			Assert.fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testCheck_pk_not_match_cert() {
		try {
			CertData certData = new CertData();
			certData.certificateAuthority = new String(caData);
			certData.certificate = new String(certificateData);
			certData.privateKey = new String(privateKeyData2);

			SecurityMgmt.checkCertificate(certData);
			Assert.fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testCheck_invalidDatas() {
		try {
			CertData certData = new CertData();
			certData.certificateAuthority = "test";
			certData.certificate = new String(certificateData);
			certData.privateKey = new String(privateKeyData);

			SecurityMgmt.checkCertificate(certData);
			Assert.fail();
		} catch (ServerFault e) {

		}

		try {
			CertData certData = new CertData();
			certData.certificateAuthority = new String(caData);
			certData.certificate = "test";
			certData.privateKey = new String(privateKeyData);

			SecurityMgmt.checkCertificate(certData);
			Assert.fail();
		} catch (ServerFault e) {
		}

		try {
			CertData certData = new CertData();
			certData.certificateAuthority = new String(caData);
			certData.certificate = new String(certificateData);
			certData.privateKey = "test";

			SecurityMgmt.checkCertificate(certData);
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

			CertData certData = new CertData();
			certData.certificateAuthority = new String(caChainData);
			certData.certificate = new String(certChainData);
			certData.privateKey = new String(privateKey);

			SecurityMgmt.checkCertificate(certData);
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

			CertData certData = new CertData();
			certData.certificateAuthority = new String(caChainData);
			certData.certificate = new String(certChainData);
			certData.privateKey = new String(privateKey);

			SecurityMgmt.checkCertificate(certData);
		} catch (IOException | ServerFault e) {
			e.printStackTrace();
			Assert.fail("Test thrown an exception");
		}
	}
}
