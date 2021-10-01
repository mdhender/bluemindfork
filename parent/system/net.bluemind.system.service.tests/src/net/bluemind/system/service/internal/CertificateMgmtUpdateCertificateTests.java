/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.CertData;
import net.bluemind.system.api.ISecurityMgmt;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class CertificateMgmtUpdateCertificateTests {

	File caFile;
	File certFile;
	File privateKeyFile;

	ISecurityMgmt service;
	String domainUid;
	BmContext testContext;
	INodeClient nodeClient;

	private static final Path cacertFilePath = Paths.get("/var/lib/bm-ca/cacert.pem");
	private static final Path bmCertFilePath = Paths.get("/etc/bm/certs/bm_cert.pem");
	private static final Path sslCertFilePath = Paths.get("/etc/ssl/certs/bm_cert.pem");

	@Before
	public void setUp() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		Server nodeServer = new Server();
		nodeServer.ip = new BmConfIni().get(DockerContainer.NODE.getName());
		nodeServer.tags = Lists.newArrayList(TagDescriptor.bm_core.getTag(), TagDescriptor.mail_imap.getTag());
		assertNotNull(nodeServer);
		nodeClient = NodeActivator.get(nodeServer.ip);

		domainUid = "testdomain" + System.currentTimeMillis() + ".loc";
		PopulateHelper.initGlobalVirt(false, nodeServer);
		PopulateHelper.createDomain(domainUid);

		SecurityContext admin0 = new SecurityContext("admin0", "admin0", Collections.<String>emptyList(),
				Arrays.asList(SecurityContext.ROLE_SYSTEM), "global");
		Sessions.get().put(admin0.getSessionId(), admin0);

		testContext = new BmTestContext(admin0);
		service = testContext.provider().instance(ISecurityMgmt.class);

		caFile = new File("data/certs/cacert.pem");
		certFile = new File("data/certs/cert.pem");
		privateKeyFile = new File("data/certs/privatekey");

	}

	@After
	public void tearDown() throws Exception {

		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testUpdateCertificate_globalvirt() throws ServerFault, IOException {
		CertData certificateDate = CertData.create(
				Files.readAllLines(caFile.toPath()).stream().collect(Collectors.joining("\n")),
				Files.readAllLines(certFile.toPath()).stream().collect(Collectors.joining("\n")),
				Files.readAllLines(privateKeyFile.toPath()).stream().collect(Collectors.joining("\n")));
		service.updateCertificate(certificateDate);

		assertEquals(1, nodeClient.listFiles(cacertFilePath.toUri().getPath()).stream().count());
		assertEquals(1, nodeClient.listFiles(bmCertFilePath.toUri().getPath()).stream().count());
		assertEquals(1, nodeClient.listFiles(sslCertFilePath.toUri().getPath()).stream().count());

		nodeClient.deleteFile(cacertFilePath.toUri().getPath());
		nodeClient.deleteFile(bmCertFilePath.toUri().getPath());
		nodeClient.deleteFile(sslCertFilePath.toUri().getPath());
	}

	@Test
	public void testUpdateCertificate_domain() throws ServerFault, IOException {
		CertData certificateDate = CertData.create(
				Files.readAllLines(caFile.toPath()).stream().collect(Collectors.joining("\n")),
				Files.readAllLines(certFile.toPath()).stream().collect(Collectors.joining("\n")),
				Files.readAllLines(privateKeyFile.toPath()).stream().collect(Collectors.joining("\n")), domainUid);
		service.updateCertificate(certificateDate);

		assertEquals(0, nodeClient.listFiles(cacertFilePath.toUri().getPath()).stream().count());
		assertEquals(0, nodeClient.listFiles(bmCertFilePath.toUri().getPath()).stream().count());
		assertEquals(0, nodeClient.listFiles(sslCertFilePath.toUri().getPath()).stream().count());

		String bmCertFilePathForDomain = bmCertFilePath.toUri().getPath().replace("bm_cert.pem",
				"bm_cert-" + domainUid + ".pem");
		String sslCertFilePathForDomain = sslCertFilePath.toUri().getPath().replace("bm_cert.pem",
				"bm_cert-" + domainUid + ".pem");
		assertEquals(1, nodeClient.listFiles(bmCertFilePathForDomain).stream().count());
		assertEquals(1, nodeClient.listFiles(sslCertFilePathForDomain).stream().count());

		nodeClient.deleteFile(bmCertFilePathForDomain);
		nodeClient.deleteFile(sslCertFilePathForDomain);
	}

	@Test
	public void testUpdateCertificate_invalidDomain() throws ServerFault, IOException {
		String invalidDomainUid = "unknown.domain.loc";
		CertData certificateDate = CertData.create(
				Files.readAllLines(caFile.toPath()).stream().collect(Collectors.joining("\n")),
				Files.readAllLines(certFile.toPath()).stream().collect(Collectors.joining("\n")),
				Files.readAllLines(privateKeyFile.toPath()).stream().collect(Collectors.joining("\n")),
				invalidDomainUid);
		try {
			service.updateCertificate(certificateDate);
		} catch (ServerFault sfe) {
			assertEquals(ErrorCode.NOT_FOUND, sfe.getCode());
			assertTrue(sfe.getMessage().contains("Domain " + invalidDomainUid + " doesnt exists"));
		}
		assertEquals(0, nodeClient.listFiles(cacertFilePath.toUri().getPath()).stream().count());
		assertEquals(0, nodeClient.listFiles(bmCertFilePath.toUri().getPath()).stream().count());
		assertEquals(0, nodeClient.listFiles(sslCertFilePath.toUri().getPath()).stream().count());

		String bmCertFilePathForDomain = bmCertFilePath.toUri().getPath().replace("bm_cert.pem",
				"bm_cert-" + invalidDomainUid + ".pem");
		String sslCertFilePathForDomain = sslCertFilePath.toUri().getPath().replace("bm_cert.pem",
				"bm_cert-" + invalidDomainUid + ".pem");
		assertEquals(0, nodeClient.listFiles(bmCertFilePathForDomain).stream().count());
		assertEquals(0, nodeClient.listFiles(sslCertFilePathForDomain).stream().count());

	}

}
