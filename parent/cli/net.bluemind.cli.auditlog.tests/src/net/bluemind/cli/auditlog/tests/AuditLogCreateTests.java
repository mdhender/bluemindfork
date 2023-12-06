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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

package net.bluemind.cli.auditlog.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import net.bluemind.cli.auditlog.tests.utils.AbstractCliAuditLogServiceTests;
import net.bluemind.cli.auditlog.tests.utils.CliTestHelper;
import net.bluemind.cli.launcher.CLIManager;
import net.bluemind.core.auditlogs.client.es.datastreams.DataStreamActivator;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogConfig;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class AuditLogCreateTests extends AbstractCliAuditLogServiceTests {
	private static final Logger logger = LoggerFactory.getLogger(AuditLogCreateTests.class);
	private static final String AUDIT_LOG_INDEX = AuditLogConfig.resolveDataStreamName(domainUid);
	private static CliTestHelper testHelper;
	private static CLIManager cli;
	private ObjectMapper mapper = new ObjectMapper();

	@BeforeClass
	public static void beforeClass() throws Exception {
		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
		testHelper = new CliTestHelper();
		cli = new CLIManager(Activator.context.getBundle().getVersion());
	}

	@Before
	public void before() throws Exception {
		super.before();
		testHelper.beforeTest();
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		ElasticsearchTestHelper.getInstance().beforeTest();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);
		PopulateHelper.addDomain(domainUid);
	}

	@After
	public void after() throws Exception {
		super.after();
		testHelper.afterTest();
		JdbcTestHelper.getInstance().beforeTest();
		ElasticsearchTestHelper.getInstance().beforeTest();

	}

	@AfterClass
	public static void afterClass() throws Exception {
		testHelper.afterClassTest();
	}

	@Test
	public void testMustCreateDataStreamForASpecificDomain() throws IOException {
		String dataStreamFullName = "toto_domain01";
		DataStreamActivator dataStreamActivator = new DataStreamActivator();

		cli.processArgs("auditlog", "create", "--name", dataStreamFullName);
		String output = testHelper.outputAndReset();
		assertTrue(output.contains("Datastream '" + dataStreamFullName + "' successfully created"));

		boolean isDataStream = dataStreamActivator.hasDataStream(dataStreamFullName);
		assertTrue(isDataStream);
	}

	@Test
	public void testMustCreateSeveralDataStreamsUsingPattern() throws ElasticsearchException, IOException {
		String dataStreamNameDomain = "toto_" + domainUid;
		String dataStreamNameGlobal = "toto_" + "global.virt";
		DataStreamActivator dataStreamActivator = new DataStreamActivator();
		cli.processArgs("auditlog", "create", "--pattern", "toto_%s");
		String output = testHelper.outputAndReset();
		assertNotNull(output);
		assertTrue(dataStreamActivator.hasDataStream(dataStreamNameDomain));
		assertTrue(dataStreamActivator.hasDataStream(dataStreamNameGlobal));
	}

	@Test
	public void testMustFailBecauseOfIncorrectPatternFormat01() {
		String pattern = "toto_%d";

		cli.processArgs("auditlog", "create", "--pattern", pattern);
		String output = testHelper.outputAndReset();
		assertTrue(output.contains("Pattern '" + pattern + "' must have format 'my_pattern_%s'"));
	}

	@Test
	public void testMustFailBecauseOfIncorrectPatternFormat02() {
		String pattern = "toto";

		cli.processArgs("auditlog", "create", "--pattern", "toto");
		String output = testHelper.outputAndReset();
		assertTrue(output.contains("Pattern '" + pattern + "' must have format 'my_pattern_%s'"));
	}

}
