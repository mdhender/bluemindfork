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

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.IKeycloakBluemindProviderAdmin;
import net.bluemind.keycloak.api.IKeycloakClientAdmin;
import net.bluemind.keycloak.api.IKeycloakFlowAdmin;
import net.bluemind.keycloak.api.IKeycloakKerberosAdmin;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class AbstractServiceTests {

	protected static SecurityContext securityContext;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		Server kcServer = new Server();
		kcServer.ip = new BmConfIni().get("keycloak");
		ArrayList<String> kcTagsLst = new ArrayList<String>();
		kcTagsLst.add("bm/keycloak");
		kcServer.tags = kcTagsLst;

		PopulateHelper.initGlobalVirt(kcServer);

		securityContext = BmTestContext.contextWithSession("sid", "admin", "global.virt", SecurityContext.ROLE_SYSTEM)
				.getSecurityContext();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		getKeycloakAdminService().allRealms().forEach(r -> {
			getKeycloakAdminService().deleteRealm(r.id);
		});
	}

	protected IServiceProvider getProvider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	protected IKeycloakAdmin getKeycloakAdminService() throws ServerFault {
		return getProvider().instance(IKeycloakAdmin.class);
	}

	protected IKeycloakClientAdmin getKeycloakClientAdminService(String domainUid) throws ServerFault {
		return getProvider().instance(IKeycloakClientAdmin.class, domainUid);
	}

	protected IKeycloakFlowAdmin getKeycloakFlowService(String domainUid) throws ServerFault {
		return getProvider().instance(IKeycloakFlowAdmin.class, domainUid);
	}

	protected IKeycloakBluemindProviderAdmin getKeycloakBluemindProviderService(String domainUid) throws ServerFault {
		return getProvider().instance(IKeycloakBluemindProviderAdmin.class, domainUid);
	}

	protected IKeycloakKerberosAdmin getKeycloakKerberosService(String domainUid) throws ServerFault {
		return getProvider().instance(IKeycloakKerberosAdmin.class, domainUid);
	}

	protected IDomains getDomainService() throws ServerFault {
		return getProvider().instance(IDomains.class);
	}

	protected IDomainSettings getDomainSettingsService(String domainUid) throws ServerFault {
		return getProvider().instance(IDomainSettings.class, domainUid);
	}
}
