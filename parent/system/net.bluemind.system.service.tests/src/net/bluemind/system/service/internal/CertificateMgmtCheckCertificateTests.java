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

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.CertData;
import net.bluemind.system.api.CertData.CertificateDomainEngine;
import net.bluemind.system.service.certificate.engine.CertifEngineFactory;
import net.bluemind.system.service.certificate.engine.ICertifEngine;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class CertificateMgmtCheckCertificateTests {

	private byte[] caData;
	private byte[] certificateData;
	private byte[] privateKeyData;

	private byte[] certificateData2;
	private byte[] privateKeyData2;

	private String domainUid = "test.bm.lan";
	private BmContext testContext;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		PopulateHelper.initGlobalVirt();
		PopulateHelper.addDomain(domainUid);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		ItemValue<Domain> domainItem = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class).get(domainUid);
		assertNotNull(domainItem);

		SecurityContext admin0 = new SecurityContext("admin0", "admin0", Collections.<String>emptyList(),
				Arrays.asList(SecurityContext.ROLE_SYSTEM), domainUid);
		Sessions.get().put(admin0.getSessionId(), admin0);
		testContext = new BmTestContext(admin0);

		caData = Files.toByteArray(new File("data/certs/cacert.pem"));
		certificateData = Files.toByteArray(new File("data/certs/cert.pem"));
		certificateData2 = Files.toByteArray(new File("data/certs/cert2.pem"));
		privateKeyData = Files.toByteArray(new File("data/certs/privatekey"));
		privateKeyData2 = Files.toByteArray(new File("data/certs/privatekey2"));
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCheck() throws ServerFault {

		CertData certData = CertData.createWithDomainUid(CertificateDomainEngine.FILE, new String(caData),
				new String(certificateData), new String(privateKeyData), domainUid);

		ICertifEngine iCertifEngine = CertifEngineFactory.get(certData, testContext);
		iCertifEngine.checkCertificate();
	}

	@Test
	public void testCheckNotCA() {
		try {
			CertData certData = CertData.createWithDomainUid(CertificateDomainEngine.FILE, new String(certificateData),
					new String(certificateData), new String(privateKeyData), domainUid);

			ICertifEngine iCertifEngine = CertifEngineFactory.get(certData, testContext);
			iCertifEngine.checkCertificate();
			Assert.fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testCheckNotCert() {
		try {
			CertData certData = CertData.createWithDomainUid(CertificateDomainEngine.FILE, new String(caData),
					new String(caData), new String(privateKeyData), domainUid);

			ICertifEngine iCertifEngine = CertifEngineFactory.get(certData, testContext);
			iCertifEngine.checkCertificate();
			Assert.fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testCheck_Cert_dont_match_CA() {
		try {
			CertData certData = CertData.createWithDomainUid(CertificateDomainEngine.FILE, new String(caData),
					new String(certificateData2), new String(privateKeyData), domainUid);

			ICertifEngine iCertifEngine = CertifEngineFactory.get(certData, testContext);
			iCertifEngine.checkCertificate();
			Assert.fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testCheck_pk_not_match_cert() {
		try {
			CertData certData = CertData.createWithDomainUid(CertificateDomainEngine.FILE, new String(caData),
					new String(certificateData), new String(privateKeyData2), domainUid);

			ICertifEngine iCertifEngine = CertifEngineFactory.get(certData, testContext);
			iCertifEngine.checkCertificate();
			Assert.fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testCheck_invalidDatas() {
		try {
			CertData certData = CertData.createWithDomainUid(CertificateDomainEngine.FILE, new String("test"),
					new String(certificateData), new String(privateKeyData), domainUid);

			ICertifEngine iCertifEngine = CertifEngineFactory.get(certData, testContext);
			iCertifEngine.checkCertificate();
			Assert.fail();
		} catch (ServerFault e) {

		}

		try {
			CertData certData = CertData.createWithDomainUid(CertificateDomainEngine.FILE, new String(caData),
					new String("test"), new String(privateKeyData), domainUid);

			ICertifEngine iCertifEngine = CertifEngineFactory.get(certData, testContext);
			iCertifEngine.checkCertificate();
			Assert.fail();
		} catch (ServerFault e) {
		}

		try {
			CertData certData = CertData.createWithDomainUid(CertificateDomainEngine.FILE, new String(caData),
					new String(certificateData), new String("test"), domainUid);

			ICertifEngine iCertifEngine = CertifEngineFactory.get(certData, testContext);
			iCertifEngine.checkCertificate();
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

			CertData certData = CertData.createWithDomainUid(CertificateDomainEngine.FILE, new String(caChainData),
					new String(certChainData), new String(privateKey), domainUid);

			ICertifEngine iCertifEngine = CertifEngineFactory.get(certData, testContext);
			iCertifEngine.checkCertificate();
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

			CertData certData = CertData.createWithDomainUid(CertificateDomainEngine.FILE, new String(caChainData),
					new String(certChainData), new String(privateKey), domainUid);

			ICertifEngine iCertifEngine = CertifEngineFactory.get(certData, testContext);
			iCertifEngine.checkCertificate();
		} catch (IOException | ServerFault e) {
			e.printStackTrace();
			Assert.fail("Test thrown an exception");
		}
	}
}
