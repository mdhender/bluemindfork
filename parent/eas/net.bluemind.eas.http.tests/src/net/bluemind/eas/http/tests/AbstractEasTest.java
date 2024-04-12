/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.eas.http.tests;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import com.google.common.collect.Lists;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.eas.config.global.GlobalConfig;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.vertx.common.CoreStateListener;

public class AbstractEasTest {

	protected ItemValue<Domain> domain;
	protected String login;
	protected String password;
	protected String latd;

	@Before
	public void setUp() throws Exception {
		GlobalConfig.DISABLE_POLICIES = true;
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		StateContext.setInternalState(new RunningState());
		CoreStateListener.state = SystemState.CORE_STATE_RUNNING;
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		String domainUid = "dom" + System.currentTimeMillis() + ".test";

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		Server server = new Server();
		server.ip = "prec";
		server.tags = Lists.newArrayList("blue/job", "ur/anus");

		Server pipo = new Server();
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		PopulateHelper.initGlobalVirt(esServer, server, pipo);
		domain = PopulateHelper.createTestDomain(domainUid, esServer, pipo);

		PopulateHelper.addUserWithRoles("user", domainUid, "hasEAS");
		latd = "user@" + domainUid;
		password = "user";
		login = latd;

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ISystemConfiguration.class)
				.updateMutableValues(Map.of("eas_sync_unknown", "true"));
	}

	@After
	public void teardown() throws InterruptedException {
		Thread.sleep(1000); // await push watch termination
	}

}
