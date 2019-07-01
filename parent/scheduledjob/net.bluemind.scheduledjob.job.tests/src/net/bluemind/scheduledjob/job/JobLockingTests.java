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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.scheduledjob.LockingJob1;
import net.bluemind.scheduledjob.LockingJob2;
import net.bluemind.scheduledjob.LockingJob3;
import net.bluemind.scheduledjob.api.IJob;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.impl.JobRegistry;
import net.bluemind.scheduledjob.scheduler.impl.JobRunner;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class JobLockingTests {

	private IJob serviceAdmin0;

	private String jobId1 = new LockingJob1().getJobId();
	private String jobId2 = new LockingJob2().getJobId();
	private String jobId3 = new LockingJob3().getJobId();

	@Before
	public void before() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();
		

		SecurityContext admin = new SecurityContext("testUser", "test", Arrays.<String> asList(),
				Arrays.<String> asList(SecurityContext.ROLE_ADMIN), "bm.lan");

		PopulateHelper.initGlobalVirt();

		PopulateHelper.createTestDomain("bm.lan");
		PopulateHelper.domainAdmin("bm.lan", admin.getSubject());

		final SettableFuture<Void> future = SettableFuture.<Void> create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		serviceAdmin0 = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IJob.class);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testJobsLockingSameResourcesShouldNotRunInParallel() throws ServerFault, InterruptedException {
		assertFalse(isActive(jobId1));
		assertFalse(isActive(jobId2));
		assertFalse(isActive(jobId3));

		// job1 locks resource1
		// job2 locks resource2
		// job3 locks resource1,resource2

		start(jobId1);
		Thread.sleep(500);
		assertTrue(isActive(jobId1));

		start(jobId3);
		Thread.sleep(500);
		// job3 cannot run, resource1 is locked by job1
		assertFalse(isActive(jobId3));

		start(jobId2);
		Thread.sleep(500);
		// job2 can run, resourc2 is not locked
		assertTrue(isActive(jobId2));
		// job3 cannot run, resource1 and resource2 are locked
		assertFalse(isActive(jobId3));

		Thread.sleep(3500);
		start(jobId3);
		// job3 can finally run after job1 and job2 are finished
		Thread.sleep(500);
		assertTrue(isActive(jobId3));
	}

	private void start(String id) {
		Collection<IScheduledJob> bluejobs = JobRegistry.getBluejobs();
		for (IScheduledJob iScheduledJob : bluejobs) {
			if (iScheduledJob.getJobId().equals(id)) {
				JobRunner runner = new JobRunner(iScheduledJob, false, "bm.lan");
				runner.run();
			}
		}

	}

	private boolean isActive(String jobId) throws ServerFault {
		JobExecutionQuery query = new JobExecutionQuery();
		query.active = true;
		ListResult<JobExecution> searchExecution = serviceAdmin0.searchExecution(query);
		for (JobExecution exec : searchExecution.values) {
			if (exec.jobId.equals(jobId)) {
				System.out.println(jobId + "  -->  " + exec.status);
				return true;
			}
		}
		return false;
	}
}
