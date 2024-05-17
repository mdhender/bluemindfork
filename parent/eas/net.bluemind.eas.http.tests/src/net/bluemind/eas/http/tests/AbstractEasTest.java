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
import org.junit.BeforeClass;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.domainbook.verticle.DomainBookVerticle;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
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
	protected SecurityContext domainUserSecurityContext;

	protected String login2;
	protected String password2;
	protected SecurityContext domainUser2SecurityContext;

	protected String login3;
	protected String password3;
	protected SecurityContext domainUser3SecurityContext;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP + "," + PopulateHelper.FAKE_CYRUS_IP_2);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP + "," + PopulateHelper.FAKE_CYRUS_IP_2);
		System.setProperty("ahcnode.fail.https.ok", "true");
		System.setProperty("mapi.notification.fresh", "true");
	}

	@Before
	public void setUp() throws Exception {
		System.setProperty("auditlog.config.path", "resources/auditlog-test.conf");

		DomainBookVerticle.suspended = true;
		GlobalConfig.DISABLE_POLICIES = true;
		StateContext.setInternalState(new RunningState());
		CoreStateListener.state = SystemState.CORE_STATE_RUNNING;
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		String domainUid = "dom" + System.currentTimeMillis() + ".test";

		Server imap = new Server();
		imap.ip = PopulateHelper.FAKE_CYRUS_IP;
		imap.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		PopulateHelper.initGlobalVirt(esServer);
		domain = PopulateHelper.createTestDomain(domainUid, esServer, imap);

		PopulateHelper.addUserWithRoles("user", domainUid, "hasEAS");
		login = "user@" + domainUid;
		password = "user";

		domainUserSecurityContext = BmTestContext.contextWithSession("u1", "user", domainUid).getSecurityContext();

		PopulateHelper.addUserWithRoles("user2", domain.uid, "hasEAS");
		login2 = "user2@" + domain.uid;
		password2 = "user2";

		domainUser2SecurityContext = BmTestContext.contextWithSession("u2", "user2", domain.uid).getSecurityContext();

		PopulateHelper.addUserWithRoles("user3", domain.uid, "hasEAS");
		login3 = "user3@" + domain.uid;
		password3 = "user3";

		domainUser3SecurityContext = BmTestContext.contextWithSession("u3", "user3", domain.uid).getSecurityContext();

		try {
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ISystemConfiguration.class)
					.updateMutableValues(Map.of("eas_sync_unknown", "true"));
		} catch (ServerFault e) {

		}
	}

	@After
	public void teardown() throws InterruptedException {
		Thread.sleep(1000); // await push watch termination
	}

}
