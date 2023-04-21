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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.keycloak.service.tests;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.IKeycloakBluemindProviderAdmin;
import net.bluemind.keycloak.api.IKeycloakClientAdmin;
import net.bluemind.keycloak.api.IKeycloakFlowAdmin;
import net.bluemind.keycloak.api.IKeycloakKerberosAdmin;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class AbstractServiceTests {
	private static final Logger logger = LoggerFactory.getLogger(AbstractServiceTests.class);

	protected static SecurityContext securityContext;

	@BeforeClass
	public static void beforeClass() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		ArrayList<String> esTagsLst = new ArrayList<String>();
		esTagsLst.add("bm/es");
		esServer.tags = esTagsLst;

		Server kcServer = new Server();
		kcServer.ip = new BmConfIni().get("keycloak");
		ArrayList<String> kcTagsLst = new ArrayList<String>();
		kcTagsLst.add("bm/keycloak");
		kcServer.tags = kcTagsLst;

		PopulateHelper.initGlobalVirt(kcServer, esServer);
		securityContext = BmTestContext.contextWithSession("xxxxx", "yyyyy", "global.virt", SecurityContext.ROLE_SYSTEM)
				.getSecurityContext();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected static boolean createDomainWithUser(String domainName, String userName, String userPassword) {
		boolean wentOK = false;
		try {
			Server esServer = new Server();
			esServer.ip = new BmConfIni().get("es-host");
			esServer.tags = Lists.newArrayList("bm/es");
			PopulateHelper.createTestDomain(domainName, esServer);

			IDomainSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IDomainSettings.class, domainName);
			Map<String, String> domainSettings = settings.get();
			domainSettings.put(DomainSettingsKeys.mail_routing_relay.name(), "external@test.fr");
			domainSettings.put(DomainSettingsKeys.domain_max_basic_account.name(), "");
			domainSettings.put(DomainSettingsKeys.password_lifetime.name(), "10");
			settings.set(domainSettings);

			PopulateHelper.addUser(userName, userPassword, domainName, Routing.external);

			StateContext.setState("reset");
			StateContext.setState("core.started");

			wentOK = true;
		} catch (Throwable t) {
			logger.error(t.getClass().getName() + " : " + t.getMessage(), t);
		}

		return wentOK;
	}

	public static String getMyIpAddress() {
		String ret = "127.0.0.1";
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				if (iface.isLoopback() || !iface.isUp()) {
					continue;
				}
				List<InterfaceAddress> addresses = iface.getInterfaceAddresses();
				for (InterfaceAddress ia : addresses) {
					if (ia.getBroadcast() == null) {
						// ipv6
						continue;
					}
					String tmp = ia.getAddress().getHostAddress();
					if (!tmp.startsWith("127")) {
						return tmp;
					}
				}
			}
		} catch (SocketException e) {
			// yeah yeah
		}
		return ret;
	}

	protected abstract IKeycloakAdmin getKeycloakAdminService() throws ServerFault;

	protected abstract IKeycloakClientAdmin getKeycloakClientAdminService() throws ServerFault;

	protected abstract IKeycloakBluemindProviderAdmin getKeycloakBluemindProviderService() throws ServerFault;

	protected abstract IKeycloakKerberosAdmin getKeycloakKerberosService() throws ServerFault;

	protected abstract IKeycloakFlowAdmin getKeycloakFlowService() throws ServerFault;

	protected abstract IDomains getDomainService() throws ServerFault;

	protected abstract IDomainSettings getDomainSettingsService() throws ServerFault;
}
