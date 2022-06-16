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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.task.service.TaskUtils.ExtendedTaskStatus;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.CertData;
import net.bluemind.system.api.CertData.CertificateDomainEngine;
import net.bluemind.system.api.ISecurityMgmt;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.service.certificate.lets.encrypt.LetsEncryptCertificate;
import net.bluemind.system.service.certificate.lets.encrypt.LetsEncryptException;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class CertificateMgmtUpdateCertificateTests {

	ISecurityMgmt service;
	IDomains domainService;
	String domainUid;
	ItemValue<Domain> domain;

	BmContext testContext;
	INodeClient nodeClient;
	ItemValue<Domain> domainVirt;

	public static String bmCertFile = "/etc/bm/certs/bm_cert.pem";
	public static String sslCertFile = "/etc/ssl/certs/bm_cert.pem";
	public static String cacertFile = "/var/lib/bm-ca/cacert.pem";

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
				Arrays.asList(SecurityContext.ROLE_SYSTEM), domainUid);
		Sessions.get().put(admin0.getSessionId(), admin0);

		domainService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		domain = domainService.get(domainUid);
		assertNotNull(domain);
		domainVirt = domainService.get("global.virt");
		assertNotNull(domainVirt);

		// add domain settings
		IDomainSettings settingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settingsMap = new HashMap<String, String>();
		settingsMap.put(DomainSettingsKeys.ssl_certif_engine.name(), CertificateDomainEngine.FILE.name());
		settingsApi.set(settingsMap);

		assertTrue(settingsApi.get().containsKey(DomainSettingsKeys.ssl_certif_engine.name()));
		assertEquals(CertificateDomainEngine.FILE.name(),
				settingsApi.get().get(DomainSettingsKeys.ssl_certif_engine.name()));

		// add global settings
		ISystemConfiguration globalSettingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		settingsMap = new HashMap<String, String>();
		settingsMap.put(SysConfKeys.ssl_certif_engine.name(), CertificateDomainEngine.FILE.name());
		globalSettingsApi.updateMutableValues(settingsMap);

		assertTrue(globalSettingsApi.getValues().values.containsKey(SysConfKeys.ssl_certif_engine.name()));
		assertEquals(CertificateDomainEngine.FILE.name(),
				globalSettingsApi.getValues().values.get(SysConfKeys.ssl_certif_engine.name()));

		testContext = new BmTestContext(admin0);
		service = testContext.provider().instance(ISecurityMgmt.class);
	}

	@After
	public void tearDown() throws Exception {

		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testUpdateCertificate_globalvirt() throws ServerFault, IOException {
		CertData certificateDate = createCertData(CertificateDomainEngine.FILE, "global.virt", null);
		service.updateCertificate(certificateDate);

		checkFiles(1, true, null);
		deleteFiles(true, null);
	}

	@Test
	public void testUpdateCertificate_domain() throws ServerFault, IOException {
		CertData certificateDate = createCertData(CertificateDomainEngine.FILE, domainUid, null);
		service.updateCertificate(certificateDate);

		checkFiles(0, true, null);
		checkFiles(1, false, domainUid);

		deleteFiles(false, domainUid);
	}

	@Test
	public void testUpdateCertificate_invalidDomain() throws ServerFault, IOException {
		String invalidDomainUid = "unknown.domain.loc";
		CertData certificateDate = createCertData(CertificateDomainEngine.FILE, invalidDomainUid, null);
		try {
			service.updateCertificate(certificateDate);
		} catch (ServerFault sfe) {
			assertEquals(ErrorCode.NOT_FOUND, sfe.getCode());
			assertTrue(sfe.getMessage().contains("Domain " + invalidDomainUid + " doesnt exists"));
		}
		checkFiles(0, true, null);
		checkFiles(0, false, invalidDomainUid);
	}

	@Test
	public void testUpdateCertificate_letsencrypt() throws IOException {

		ISystemConfiguration sysconf = testContext.provider().instance(ISystemConfiguration.class);
		Map<String, String> settings = sysconf.getValues().values;
		settings.put(SysConfKeys.http_proxy_enabled.name(), "false");
		sysconf.updateMutableValues(settings);

		// add domain settings
		IDomainSettings settingsApi = testContext.provider().instance(IDomainSettings.class, domainUid);
		Map<String, String> settingsMap = new HashMap<String, String>();
		settingsMap.put(DomainSettingsKeys.external_url.name(), "test.bluemind.net");
		settingsMap.put(DomainSettingsKeys.ssl_certif_engine.name(), CertificateDomainEngine.LETS_ENCRYPT.name());
		settingsApi.set(settingsMap);
		assertEquals(CertificateDomainEngine.LETS_ENCRYPT.name(),
				settingsApi.get().get(DomainSettingsKeys.ssl_certif_engine.name()));

		CertData certificateData = CertData.createForLetsEncrypt(domainUid, "test@bluemind.net");

		try {
			TaskRef tr = service.generateLetsEncrypt(certificateData);
			TaskUtils.wait(testContext.provider(), tr);
		} catch (ServerFault e) {
			assertTrue(e.getMessage().contains("Let's Encrypt terms of service must been approved to continue"));
		}

		service.approveLetsEncryptTos(domainUid);

		TaskRef tr = service.generateLetsEncrypt(certificateData);
		ExtendedTaskStatus status = TaskUtils.wait(testContext.provider(), tr);
		assertEquals(TaskStatus.State.InError, status.state);

		domain.value.properties.clear();
		domainService.update(domainUid, domain.value);
	}

	@Test
	public void testUpdateCertificate_letsencrypt_invalid() throws IOException {

		ISystemConfiguration sysconf = testContext.provider().instance(ISystemConfiguration.class);
		Map<String, String> settings = sysconf.getValues().values;
		settings.put(SysConfKeys.http_proxy_enabled.name(), "false");
		sysconf.updateMutableValues(settings);

		// add domain settings
		IDomainSettings settingsApi = testContext.provider().instance(IDomainSettings.class, domainUid);
		Map<String, String> settingsMap = new HashMap<String, String>();
		settingsMap.put(DomainSettingsKeys.ssl_certif_engine.name(), CertificateDomainEngine.LETS_ENCRYPT.name());
		settingsApi.set(settingsMap);
		assertEquals(CertificateDomainEngine.LETS_ENCRYPT.name(),
				settingsApi.get().get(DomainSettingsKeys.ssl_certif_engine.name()));

		service.approveLetsEncryptTos(domainUid);

		CertData certificateData = CertData.createForLetsEncrypt(domainUid, null);

		try {
			TaskRef tr = service.generateLetsEncrypt(certificateData);
			TaskUtils.wait(testContext.provider(), tr);
		} catch (ServerFault e) {
			assertTrue(e.getMessage().contains("External URL missing for domain"));
		}

		settingsMap.put(DomainSettingsKeys.external_url.name(), "test.bluemind.net");
		settingsApi.set(settingsMap);

		try {
			TaskRef tr = service.generateLetsEncrypt(certificateData);
			TaskUtils.wait(testContext.provider(), tr);
		} catch (ServerFault e) {
			assertTrue(e.getMessage().contains("Unknown default domain for domain"));
		}

		certificateData.email = "test@bluemind.net";

		TaskRef tr = service.generateLetsEncrypt(certificateData);
		ExtendedTaskStatus status = TaskUtils.wait(testContext.provider(), tr);
		assertEquals(TaskStatus.State.InError, status.state);

		domain.value.properties.clear();
		domainService.update(domainUid, domain.value);
	}

	@Test
	public void testUpdateCertificate_disable() throws IOException {

		// add domain settings
		IDomainSettings settingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settingsMap = new HashMap<String, String>();
		settingsMap.put(DomainSettingsKeys.external_url.name(), "test.bluemind.net");
		settingsMap.put(DomainSettingsKeys.default_domain.name(), domainUid);
		settingsMap.put(DomainSettingsKeys.ssl_certif_engine.name(), CertificateDomainEngine.FILE.name());
		settingsApi.set(settingsMap);
		assertEquals(CertificateDomainEngine.FILE.name(),
				settingsApi.get().get(DomainSettingsKeys.ssl_certif_engine.name()));

		CertData certificateDate = CertificateMgmtUpdateCertificateTests.createCertData(CertificateDomainEngine.FILE,
				domainUid, null);
		service.updateCertificate(certificateDate);

		// check files created
		checkFiles(1, false, domainUid);

		// DISABLE
		try {
			certificateDate = CertificateMgmtUpdateCertificateTests.createCertData(CertificateDomainEngine.DISABLED,
					domainUid, null);
			service.updateCertificate(certificateDate);
		} catch (ServerFault e) {
			// catch the error occurred trying to restart nginx
			assertTrue(e.getMessage().contains("503"));
		}

		// check files removed
		checkFiles(0, false, domainUid);
	}

	@Test
	public void testUpdateCertificate_update_disable() throws IOException {

		// add domain settings
		IDomainSettings settingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settingsMap = new HashMap<String, String>();
		settingsMap.put(DomainSettingsKeys.external_url.name(), "test.bluemind.net");
		settingsMap.put(DomainSettingsKeys.default_domain.name(), domainUid);
		settingsMap.put(DomainSettingsKeys.ssl_certif_engine.name(), CertificateDomainEngine.FILE.name());
		settingsApi.set(settingsMap);
		assertEquals(CertificateDomainEngine.FILE.name(),
				settingsApi.get().get(DomainSettingsKeys.ssl_certif_engine.name()));

		CertData certificateDate = CertificateMgmtUpdateCertificateTests.createCertData(CertificateDomainEngine.FILE,
				domainUid, null);
		service.updateCertificate(certificateDate);

		// check files created
		checkFiles(1, false, domainUid);

		settingsMap.put(DomainSettingsKeys.ssl_certif_engine.name(), CertificateDomainEngine.LETS_ENCRYPT.name());
		settingsApi.set(settingsMap);
		assertEquals(CertificateDomainEngine.LETS_ENCRYPT.name(),
				settingsApi.get().get(DomainSettingsKeys.ssl_certif_engine.name()));

		service.approveLetsEncryptTos(domainUid);
		assertTrue(LetsEncryptCertificate.isTosApproved(domainService.get(domainUid).value));

		try {
			certificateDate = CertificateMgmtUpdateCertificateTests.createCertData(CertificateDomainEngine.DISABLED,
					domainUid, null);
			service.updateCertificate(certificateDate);
		} catch (ServerFault e) {
			// catch the error occurred trying to restart nginx
			assertTrue(e.getMessage().contains("503"));
		}

		// check files removed
		checkFiles(0, false, domainUid);

		assertFalse(LetsEncryptCertificate.isTosApproved(domainService.get(domainUid).value));
	}

	@Test
	public void testUpdateCertificate_disable_globalvirt() throws IOException {

		// add domain settings
		IDomainSettings settingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainVirt.uid);
		Map<String, String> settingsMap = new HashMap<String, String>();
		settingsMap.put(SysConfKeys.external_url.name(), "test.bluemind.net");
		settingsMap.put(SysConfKeys.default_domain.name(), domainVirt.uid);
		settingsMap.put(SysConfKeys.ssl_certif_engine.name(), CertificateDomainEngine.FILE.name());
		settingsApi.set(settingsMap);
		assertEquals(CertificateDomainEngine.FILE.name(), settingsApi.get().get(SysConfKeys.ssl_certif_engine.name()));

		CertData certificateDate = CertificateMgmtUpdateCertificateTests.createCertData(CertificateDomainEngine.FILE,
				domainVirt.uid, null);
		service.updateCertificate(certificateDate);

		// check files created
		checkFiles(1, false, null);

		service.approveLetsEncryptTos(domainVirt.uid);
		assertTrue(LetsEncryptCertificate.isTosApproved(domainService.get(domainVirt.uid).value));

		try {
			certificateDate = CertificateMgmtUpdateCertificateTests.createCertData(CertificateDomainEngine.DISABLED,
					domainVirt.uid, null);
			service.updateCertificate(certificateDate);
		} catch (ServerFault e) {
			assertTrue(e.getMessage().contains("Cannot disable 'global.virt' domain Certificate"));
		}

		// check files not removed
		checkFiles(1, false, null);

		assertTrue(LetsEncryptCertificate.isTosApproved(domainService.get(domainVirt.uid).value));
	}

	@Test
	public void testUpdateCertificate_updateExternalUrl_letsEncrypt() throws IOException {

		ISystemConfiguration sysconf = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		Map<String, String> settings = sysconf.getValues().values;
		settings.put(SysConfKeys.http_proxy_enabled.name(), "false");
		sysconf.updateMutableValues(settings);

		// add domain settings
		IDomainSettings settingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settingsMap = new HashMap<String, String>();
		settingsMap.put(DomainSettingsKeys.external_url.name(), "test.dev.bluemind.net");
		settingsMap.put(DomainSettingsKeys.ssl_certif_engine.name(), "LETS_ENCRYPT");
		settingsApi.set(settingsMap);

		domain.value.properties.put("TOS_APPROVAL", "true");
		domainService.update(domainUid, domain.value);

		settingsMap = new HashMap<String, String>();
		settingsMap.put(DomainSettingsKeys.external_url.name(), "test.bluemind.net");

		try {
			settingsApi.set(settingsMap);
		} catch (LetsEncryptException e) {
			assertTrue(e.getMessage().contains("Challenge failed"));
		}

		assertEquals("test.bluemind.net", settingsApi.get().get(DomainSettingsKeys.external_url.name()));

		domain.value.properties.clear();
		domainService.update(domainUid, domain.value);
	}

	@Test
	public void testUpdateCertificate_updateExternalUrl_file() throws IOException {

		ISystemConfiguration sysconf = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		Map<String, String> settings = sysconf.getValues().values;
		settings.put(SysConfKeys.http_proxy_enabled.name(), "false");
		sysconf.updateMutableValues(settings);

		// add domain settings
		IDomainSettings settingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settingsMap = new HashMap<String, String>();
		settingsMap.put(DomainSettingsKeys.external_url.name(), "test.dev.bluemind.net");
		settingsMap.put(DomainSettingsKeys.ssl_certif_engine.name(), "FILE");
		settingsApi.set(settingsMap);

		assertEquals("test.dev.bluemind.net", settingsApi.get().get(DomainSettingsKeys.external_url.name()));

		settingsMap = new HashMap<String, String>();
		settingsMap.put(DomainSettingsKeys.external_url.name(), "test.bluemind.net");

		try {
			settingsApi.set(settingsMap);
		} catch (LetsEncryptException e) {
			fail("Lets encrypt must not be called in that case");
		}

		assertEquals("test.bluemind.net", settingsApi.get().get(DomainSettingsKeys.external_url.name()));

		domain.value.properties.clear();
		domainService.update(domainUid, domain.value);
	}

	private void checkFiles(int nb, boolean withCacert, String domainUid) {
		if (withCacert) {
			assertEquals(nb, nodeClient.listFiles(cacertFile).stream().count());
		}

		String bmCertFilePath = bmCertFile;
		String sslCertFilePath = sslCertFile;
		if (domainUid != null) {
			bmCertFilePath = bmCertFile.replace("bm_cert.pem", "bm_cert-" + domainUid + ".pem");
			sslCertFilePath = sslCertFile.replace("bm_cert.pem", "bm_cert-" + domainUid + ".pem");
		}

		assertEquals(nb, nodeClient.listFiles(bmCertFilePath).stream().count());
		assertEquals(nb, nodeClient.listFiles(sslCertFilePath).stream().count());
	}

	private void deleteFiles(boolean withCacert, String domainUid) {
		if (withCacert) {
			nodeClient.deleteFile(cacertFile);
		}

		String bmCertFilePath = bmCertFile;
		String sslCertFilePath = sslCertFile;
		if (domainUid != null) {
			bmCertFilePath = bmCertFile.replace("bm_cert.pem", "bm_cert-" + domainUid + ".pem");
			sslCertFilePath = sslCertFile.replace("bm_cert.pem", "bm_cert-" + domainUid + ".pem");
		}

		nodeClient.deleteFile(bmCertFilePath);
		nodeClient.deleteFile(sslCertFilePath);
	}

	public static CertData createCertData(CertificateDomainEngine sslCertEngine, String domainUid, String email)
			throws IOException {

		File caFile = new File("data/certs/cacert.pem");
		File certFile = new File("data/certs/cert.pem");
		File privateKeyFile = new File("data/certs/privatekey");

		return CertData.create(sslCertEngine,
				Files.readAllLines(caFile.toPath()).stream().collect(Collectors.joining("\n")),
				Files.readAllLines(certFile.toPath()).stream().collect(Collectors.joining("\n")),
				Files.readAllLines(privateKeyFile.toPath()).stream().collect(Collectors.joining("\n")), domainUid,
				email);
	}
}
