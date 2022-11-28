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
package net.bluemind.maintenance.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.maintenance.IMaintenanceScript;
import net.bluemind.maintenance.MaintenanceScripts;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.pg.PostgreSQLService;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MaintenanceTests {
	public String mailboxIp;
	public String pgdataIp;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		// Wait for the core to be ready
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		BmConfIni ini = new BmConfIni();
		Server cyrus = new Server();
		mailboxIp = ini.get("imap-role");
		cyrus.ip = ini.get("imap-role");
		cyrus.tags = Arrays.asList(TagDescriptor.mail_imap.getTag());

		Server pg = new Server();
		pgdataIp = pg.ip = ini.get("bluemind/postgres-tests");
		pg.tags = Arrays.asList(TagDescriptor.bm_pgsql_data.getTag());
		PopulateHelper.initGlobalVirt(cyrus, pg);
	}

	private Server initDataServer() {
		Server srv = new Server();
		srv.fqdn = pgdataIp;
		srv.ip = pgdataIp;
		srv.name = pgdataIp;
		srv.tags = Arrays.asList(TagDescriptor.bm_pgsql_data.getTag());
		ItemValue<Server> server = ItemValue.create(pgdataIp, srv);
		PostgreSQLService service = new TestPostgreSQLService();
		// This name CANNOT be overriden (something else somewhere needs the name as is)
		String dbName = "bj-data";
		service.addDataServer(server, dbName);
		return srv;
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testMaintenanceAll() throws ServerFault {
		String[] expected = { "Analyze", "Repack" };
		List<IMaintenanceScript> scripts = MaintenanceScripts.getMaintenanceScripts();
		List<String> scriptNames = scripts.stream().map(s -> s.getClass().getSimpleName()).collect(Collectors.toList());

		scripts.stream().forEach(s -> System.err.println(s.getClass().getSimpleName()));
		assertTrue(scriptNames.containsAll(Arrays.asList(expected)));
	}

	@Test
	public void testMaintenanceAnalyze() throws ServerFault {
		List<IMaintenanceScript> scripts = MaintenanceScripts.getMaintenanceScripts();
		scripts.stream().forEach(s -> System.err.println(s.getClass().getSimpleName()));
		IMaintenanceScript analyze = scripts.stream().filter(s -> "Analyze".equals(s.getClass().getSimpleName()))
				.findFirst().get();
		TestMonitor monitor = new TestMonitor();
		analyze.run(monitor);
		int analyzeRuns = 0;
		for (String l : monitor.logs) {
			if (l == null)
				continue;
			System.err.println(l);
			if (l.contains("VACUUM ANALYZE took")) {
				analyzeRuns++;
			}
		}
		assertEquals(analyzeRuns, 3);
	}

	@Test
	public void testMaintenanceRepack() throws ServerFault {
		initDataServer();
		IServer service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext().provider()
				.instance(IServer.class, "default");
		service.allComplete().stream().forEach(s -> System.err.println(s));
		List<Server> servers = service.allComplete().stream()
				.filter(ivs -> ivs.value.tags.contains(TagDescriptor.bm_pgsql.getTag())
						|| ivs.value.tags.contains(TagDescriptor.bm_pgsql_data.getTag()))
				.map(ivs -> ivs.value).collect(Collectors.toList());
		for (Server s : servers) {
			System.err.println("server: " + s);
		}
		List<IMaintenanceScript> scripts = MaintenanceScripts.getMaintenanceScripts();
		IMaintenanceScript repack = scripts.stream().filter(s -> s.getClass().getSimpleName().equals("Repack"))
				.findFirst().orElseThrow(() -> new ServerFault("missing repack"));

		TestMonitor monitor = new TestMonitor();
		repack.run(monitor);
		monitor.logs.stream().filter(Objects::nonNull).forEach(l -> System.err.println(l));
		long createIndexes = monitor.logs.stream().filter(Objects::nonNull)
				.filter(l -> l.contains("CREATE UNIQUE INDEX CONCURRENTLY")).count();

		// Tests in docker are initialized with 8 partitions
		System.err.println("created indexes: " + createIndexes);
		monitor.logs.clear();

		System.err.println("relaunch effective this time ?");
		// Effective repack
		repack.run(monitor);
		monitor.logs.stream().filter(Objects::nonNull).forEach(l -> System.err.println(l));
		long repackedTables = monitor.logs.stream().filter(Objects::nonNull).filter(l -> l.contains("repacking table"))
				.count();
		assertTrue(repackedTables > 0);
	}

}
