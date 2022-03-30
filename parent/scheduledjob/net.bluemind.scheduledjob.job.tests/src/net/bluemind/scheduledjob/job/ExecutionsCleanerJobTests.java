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
package net.bluemind.scheduledjob.job;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.scheduledjob.DummyJob1;
import net.bluemind.scheduledjob.DummyJob2;
import net.bluemind.scheduledjob.api.IJob;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ExecutionsCleanerJobTests {

	private IJob serviceAdmin0;

	private String purgeJob = new ExecutionsCleanerJob().getJobId();
	private String jobId1 = new DummyJob1().getJobId();
	private String jobId2 = new DummyJob2().getJobId();

	@Before
	public void before() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext admin = new SecurityContext("testUser", "test", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_ADMIN), "bm.lan");

		PopulateHelper.initGlobalVirt();

		PopulateHelper.createTestDomain("bm.lan");
		PopulateHelper.domainAdmin("bm.lan", admin.getSubject());

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		serviceAdmin0 = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IJob.class);

		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testPurgingJobs() throws ServerFault {
		// limit the limit to prevent running the test for too long
		ExecutionsCleanerJob.setLimit(10);

		for (int i = 0; i < 15; i++) {
			serviceAdmin0.start(jobId1, null);
			waitFor(jobId1);
		}
		for (int i = 0; i < 5; i++) {
			serviceAdmin0.start(jobId2, null);
			waitFor(jobId2);
		}

		serviceAdmin0.start(purgeJob, null);
		waitFor(purgeJob);

		JobExecutionQuery query = new JobExecutionQuery();
		query.jobId = jobId1;
		Assert.assertEquals(10, serviceAdmin0.searchExecution(query).total);

		query = new JobExecutionQuery();
		query.jobId = jobId2;
		ListResult<JobExecution> result = serviceAdmin0.searchExecution(query);
		Assert.assertEquals(5, result.total);
	}

	@Test
	public void testJobsGetPurgedInCorrectOrder() throws ServerFault {
		// limit the limit to prevent running the test for too long
		ExecutionsCleanerJob.setLimit(10);

		for (int i = 0; i < 15; i++) {
			serviceAdmin0.start(jobId1, null);
			waitFor(jobId1);
		}

		JobExecutionQuery query = new JobExecutionQuery();
		query.active = false;
		query.jobId = jobId1;

		ListResult<JobExecution> searchExecution = serviceAdmin0.searchExecution(query);
		Collections.sort(searchExecution.values, (c1, c2) -> {
			return c1.id - c2.id;
		});
		List<JobExecution> remainingJobExecutions = searchExecution.values.subList(5, 15);

		serviceAdmin0.start(purgeJob, null);
		waitFor(purgeJob);

		query = new JobExecutionQuery();
		query.jobId = jobId1;
		ListResult<JobExecution> foundExecutions = serviceAdmin0.searchExecution(query);
		Assert.assertEquals(10, foundExecutions.total);

		for (JobExecution jobExecution : foundExecutions.values) {
			boolean match = false;
			for (JobExecution remaining : remainingJobExecutions) {
				if (remaining.id == jobExecution.id) {
					match = true;
				}
			}
			if (!match) {
				Assert.fail("somehow we purged the wrong jobs");
			}
		}
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
