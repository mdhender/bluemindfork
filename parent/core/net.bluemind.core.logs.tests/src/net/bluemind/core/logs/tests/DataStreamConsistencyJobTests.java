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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.logs.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.IAuditLogMgmt;
import net.bluemind.core.auditlogs.client.es.job.DataStreamConsistencyJob;
import net.bluemind.core.auditlogs.client.loader.AuditLogLoader;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.scheduledjob.api.IJob;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DataStreamConsistencyJobTests {

	private String domainUid = "bm.lan";
	private String domainUid1 = "bm1.lan";
	private static final String AUDITLOG_PREFIX = "audit_log";

	private IJob serviceAdmin0;

	private String dataStreamConsistencyJob = new DataStreamConsistencyJob().getJobId();
	IAuditLogMgmt auditLogManager;

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

		serviceAdmin0 = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IJob.class);

		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
	}

	@Test
	public void testDataStreamConsistencyJob() throws Exception {

		String fullDataStreamNameForDomainUid1 = AUDITLOG_PREFIX + "_" + domainUid1;
		// Remove datastream for domainUid1
		auditLogManager.removeDatastreamForPrefixAndDomain(AUDITLOG_PREFIX, domainUid1);

		// Asserts datastream has been removed
		assertFalse(auditLogManager.isDataStream(fullDataStreamNameForDomainUid1));

		JobExecutionQuery query = new JobExecutionQuery();
		serviceAdmin0.start(dataStreamConsistencyJob, null);

		// Wait for datastream consistency check job to be completed.
		waitFor(dataStreamConsistencyJob);

		query.jobId = dataStreamConsistencyJob;
		Assert.assertEquals(1L, serviceAdmin0.searchExecution(query).total);
		Assert.assertEquals(JobExitStatus.SUCCESS, serviceAdmin0.searchExecution(query).values.get(0).status);

		assertTrue(auditLogManager.isDataStream(fullDataStreamNameForDomainUid1));
	}

	private void waitFor(String jobId) throws ServerFault {
		JobExecutionQuery query = new JobExecutionQuery();
		query.active = true;
		query.jobId = jobId;
		do {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		} while (!serviceAdmin0.searchExecution(query).values.isEmpty());
	}
}
