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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.logs.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.auditlogs.IAuditLogMgmt;
import net.bluemind.core.auditlogs.client.loader.AuditLogLoader;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.maintenance.IMaintenanceScript;
import net.bluemind.maintenance.MaintenanceScripts;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DataStreamConsistencyMaintenanceTests {
	private String domainUid = "bm.lan";
	private String domainUid1 = "bm1.lan";
	private IAuditLogMgmt auditLogManager;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		ElasticsearchTestHelper.getInstance().beforeTest();
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		SecurityContext admin = new SecurityContext("testUser", "test", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_ADMIN), "bm.lan");

		PopulateHelper.initGlobalVirt(esServer);

		PopulateHelper.createTestDomain(domainUid);
		PopulateHelper.createTestDomain(domainUid1);
		PopulateHelper.domainAdmin(domainUid, admin.getSubject());
		PopulateHelper.domainAdmin(domainUid1, admin.getSubject());
		AuditLogLoader auditLogProvider = new AuditLogLoader();
		auditLogManager = auditLogProvider.getManager();

		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testMaintenanceDataStreamConsistency() throws Exception {

		// Remove datastream for domainUid1
		auditLogManager.removeAuditLogBackingStore(domainUid1);

		// Asserts datastream has been removed
		assertFalse(auditLogManager.hasAuditLogBackingStore(domainUid1));

		List<IMaintenanceScript> scripts = MaintenanceScripts.getMaintenanceScripts();
		scripts.stream().forEach(s -> System.err.println(s.getClass().getSimpleName()));
		IMaintenanceScript dataStreamConsistency = scripts.stream()
				.filter(s -> "DataStreamConsistency".equals(s.name())).findFirst().get();
		TestMonitor monitor = new TestMonitor();
		dataStreamConsistency.run(monitor);
		int analyzeRuns = 0;
		for (String l : monitor.logs) {
			if (l == null)
				continue;
			if (l.contains("DataStreamConsistency")) {
				analyzeRuns++;
			}
		}
		assertEquals(analyzeRuns, 1);

		assertTrue(auditLogManager.hasAuditLogBackingStore(domainUid1));
	}

}
