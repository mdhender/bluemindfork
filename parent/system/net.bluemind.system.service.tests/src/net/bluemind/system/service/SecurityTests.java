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
package net.bluemind.system.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
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
import net.bluemind.system.service.certificate.IInCoreSecurityMgmt;
import net.bluemind.system.service.certificate.lets.encrypt.LetsEncryptCertificate;
import net.bluemind.system.service.internal.CertificateMgmtUpdateCertificateTests;
import net.bluemind.system.service.internal.ValidatorHook;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class SecurityTests {

	String domainUid = "test.bluemind.net";
	ItemValue<Domain> domain;
	IDomains domainService;
	BmTestContext context;
	INodeClient nodeClient;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		context = new BmTestContext(SecurityContext.SYSTEM);

		Server nodeServer = new Server();
		nodeServer.ip = new BmConfIni().get(DockerContainer.NODE.getName());
		nodeServer.tags = Lists.newArrayList(TagDescriptor.bm_core.getTag(), TagDescriptor.mail_imap.getTag(),
				TagDescriptor.bm_nginx.getTag());
		assertNotNull(nodeServer);
		nodeClient = NodeActivator.get(nodeServer.ip);

		// create domain test.bluemind.net
		PopulateHelper.initGlobalVirt(false, nodeServer);
		PopulateHelper.createDomain(domainUid);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		domainService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		domain = domainService.get(domainUid);
		assertNotNull(domain);

		ValidatorHook.throwException = false;
	}

	@Test
	public void updateFirewallRulesAsAnonymous() {
		try {
			ServerSideServiceProvider.getProvider(SecurityContext.ANONYMOUS).instance(ISecurityMgmt.class)
					.updateFirewallRules();
			fail("Only global domain users can update firewall");
		} catch (ServerFault e) {
			System.out.println(e.getMessage());
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void updateCertificateAsAnonymous() throws IOException {

		// add domain settings
		IDomainSettings settingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settingsMap = new HashMap<String, String>();
		settingsMap.put(DomainSettingsKeys.external_url.name(), "test.bluemind.net");
		settingsMap.put(DomainSettingsKeys.default_domain.name(), domainUid);
		settingsMap.put(DomainSettingsKeys.ssl_certif_engine.name(), CertificateDomainEngine.FILE.name());
		settingsApi.set(settingsMap);

		VertxPlatform.spawnBlocking(10, TimeUnit.SECONDS);

		try {
			CertData certData = CertificateMgmtUpdateCertificateTests.createCertData(CertificateDomainEngine.FILE,
					domainUid, "test@bluemind.net");
			ServerSideServiceProvider.getProvider(SecurityContext.ANONYMOUS).instance(ISecurityMgmt.class)
					.updateCertificate(certData);
		} catch (ServerFault e) {
			System.out.println(e.getMessage());
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	

	@Test
	public void getAndApproveLetsEncryptTerms() {

		ISecurityMgmt service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISecurityMgmt.class);
		String letsEncryptTos = service.getLetsEncryptTos();
		assertNotNull(letsEncryptTos);
		assertTrue(letsEncryptTos.startsWith("https://letsencrypt.org"));
		assertTrue(letsEncryptTos.endsWith(".pdf"));
		service.approveLetsEncryptTos(domainUid);
		assertTrue(LetsEncryptCertificate.isTosApproved(domainService.get(domainUid).value));
	}

	@Test
	public void generateLetsEncryptAsAnonymous() throws IOException {

		// add domain settings
		IDomainSettings settingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settingsMap = new HashMap<String, String>();
		settingsMap.put(DomainSettingsKeys.external_url.name(), "test.bluemind.net");
		settingsMap.put(DomainSettingsKeys.default_domain.name(), domainUid);
		settingsMap.put(DomainSettingsKeys.ssl_certif_engine.name(), CertificateDomainEngine.FILE.name());
		settingsApi.set(settingsMap);

		VertxPlatform.spawnBlocking(10, TimeUnit.SECONDS);

		try {
			CertData certData = CertificateMgmtUpdateCertificateTests
					.createCertData(CertificateDomainEngine.LETS_ENCRYPT, domainUid, "test@bluemind.net");
			ServerSideServiceProvider.getProvider(SecurityContext.ANONYMOUS).instance(ISecurityMgmt.class)
					.generateLetsEncrypt(certData);
		} catch (ServerFault e) {
			System.out.println(e.getMessage());
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void getDomainExternalUrl() {

		// add domain external url
		IDomainSettings settingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settingsMap = new HashMap<>();
		settingsMap.put(DomainSettingsKeys.external_url.name(), "test.bluemind.net");
		settingsMap.put(DomainSettingsKeys.default_domain.name(), domainUid);
		settingsMap.put(DomainSettingsKeys.ssl_certif_engine.name(), CertificateDomainEngine.FILE.name());
		settingsApi.set(settingsMap);

		// add global external url
		ISystemConfiguration sysconf = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		Map<String, String> sysSettings = sysconf.getValues().values;
		sysSettings.put(SysConfKeys.external_url.name(), "ext.bluemind.net");
		sysSettings.put(SysConfKeys.ssl_certif_engine.name(), CertificateDomainEngine.FILE.name());
		sysconf.updateMutableValues(sysSettings);

		VertxPlatform.spawnBlocking(10, TimeUnit.SECONDS);

		Map<String, ItemValue<Domain>> domainExternalUrls = ServerSideServiceProvider
				.getProvider(SecurityContext.SYSTEM).instance(IInCoreSecurityMgmt.class).getDomainExternalUrls();

		assertEquals(2, domainExternalUrls.size());
		assertTrue(domainExternalUrls.keySet().stream().anyMatch(k -> k.equals("test.bluemind.net")));
		assertTrue(domainExternalUrls.keySet().stream().anyMatch(k -> k.equals("ext.bluemind.net")));
	}

	@Test
	public void getDomainExternalUrl_noglobal() {

		// add domain external url
		IDomainSettings settingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settingsMap = new HashMap<>();
		settingsMap.put(DomainSettingsKeys.external_url.name(), "test.bluemind.net");
		settingsMap.put(DomainSettingsKeys.default_domain.name(), domainUid);
		settingsMap.put(DomainSettingsKeys.ssl_certif_engine.name(), CertificateDomainEngine.FILE.name());
		settingsApi.set(settingsMap);

		VertxPlatform.spawnBlocking(10, TimeUnit.SECONDS);

		Map<String, ItemValue<Domain>> domainExternalUrls = ServerSideServiceProvider
				.getProvider(SecurityContext.SYSTEM).instance(IInCoreSecurityMgmt.class).getDomainExternalUrls();

		assertEquals(1, domainExternalUrls.size());
		assertEquals("test.bluemind.net", domainExternalUrls.keySet().iterator().next());

	}

	@Test
	public void getDomainExternalUrl_nodomain() {

		// add global external url
		ISystemConfiguration sysconf = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		Map<String, String> sysSettings = sysconf.getValues().values;
		sysSettings.put(SysConfKeys.external_url.name(), "ext.bluemind.net");
		sysSettings.put(SysConfKeys.ssl_certif_engine.name(), CertificateDomainEngine.FILE.name());
		sysconf.updateMutableValues(sysSettings);

		VertxPlatform.spawnBlocking(10, TimeUnit.SECONDS);

		Map<String, ItemValue<Domain>> domainExternalUrls = ServerSideServiceProvider
				.getProvider(SecurityContext.SYSTEM).instance(IInCoreSecurityMgmt.class).getDomainExternalUrls();

		assertEquals(1, domainExternalUrls.size());
		assertEquals("ext.bluemind.net", domainExternalUrls.keySet().iterator().next());

	}
}
