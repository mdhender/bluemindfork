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
package net.bluemind.scheduledjob.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.scheduledjob.api.IInCoreJob;
import net.bluemind.scheduledjob.api.Job;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.api.JobPlanification;
import net.bluemind.scheduledjob.api.JobQuery;
import net.bluemind.scheduledjob.api.JobRec;
import net.bluemind.scheduledjob.api.LogEntry;
import net.bluemind.scheduledjob.api.LogLevel;
import net.bluemind.scheduledjob.api.PlanKind;
import net.bluemind.scheduledjob.scheduler.impl.JobRegistry;
import net.bluemind.scheduledjob.scheduler.impl.Scheduler;
import net.bluemind.scheduledjob.service.jobs.WaitingJob;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ScheduledJobServiceTests {

	private final String DOMAIN_JOB = "DomainJob";
	private final String GLOBAL_JOB = "GlobalJob";
	private final String WAITING_JOB = "WaitingJob";

	private IInCoreJob serviceAdmin0;
	private IInCoreJob serviceAdmin;

	@Before
	public void before() throws Exception {

		File f = new File(System.getProperty("user.home") + "/no.core.jobs");
		if (!f.exists()) {
			Files.write("".getBytes(), f);
		}

		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext admin = new SecurityContext("testUser", "test", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_ADMIN), "bm.lan");
		SecurityContext admin1 = new SecurityContext("testUser1", "test1", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_ADMIN), "bm1.lan");
		SecurityContext admin2 = new SecurityContext("testUser2", "test2", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_ADMIN), "bm2.lan");

		PopulateHelper.initGlobalVirt();

		PopulateHelper.createTestDomain("bm.lan");
		PopulateHelper.domainAdmin("bm.lan", admin.getSubject());
		PopulateHelper.createTestDomain("bm1.lan");
		PopulateHelper.domainAdmin("bm1.lan", admin1.getSubject());
		PopulateHelper.createTestDomain("bm2.lan");
		PopulateHelper.domainAdmin("bm2.lan", admin2.getSubject());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		// ?????
		new JobRegistry();

		serviceAdmin0 = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreJob.class);

		serviceAdmin = ServerSideServiceProvider.getProvider(admin).instance(IInCoreJob.class);

		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSetup() throws ServerFault {
		assertNotNull(serviceAdmin0);

		JobQuery query = new JobQuery();
		ListResult<Job> jobs = serviceAdmin0.searchJob(query);
		assertNotNull(jobs);

		serviceAdmin0.start(DOMAIN_JOB, null);

		waitFor(DOMAIN_JOB);
	}

	@Test
	public void testJobStartPermissionBJR67() throws ServerFault {
		try {
			serviceAdmin0.start(GLOBAL_JOB, null);
		} catch (Exception e) {
			fail("Admin0 should be able to start GlobalJob without problem");
		}

		try {
			serviceAdmin.start(GLOBAL_JOB, null);
			fail("Non-global admin should receive an exception when trying to start GlobalJob");
		} catch (ServerFault e) {
			// this is fine
		} catch (Exception e) {
			fail("Should have received a serverfault, but got " + e.getMessage() + " instead.");
		}

		waitFor(GLOBAL_JOB);
	}

	@Test
	public void testStartLoop() throws ServerFault {
		for (int i = 0; i < 250; i++) {
			try {
				serviceAdmin0.start(DOMAIN_JOB, null);
			} catch (ServerFault e) {
				System.out.print("failed with code: " + e.getCode());
				try {
					Thread.sleep(80);
				} catch (InterruptedException e1) {
				}
			} catch (Throwable t) {
				t.printStackTrace();
				fail("not an expected serverfault " + t.getMessage());
			}
		}

		waitFor(DOMAIN_JOB);
	}

	@Test
	public void testStartAndFindExecutionAd0() throws ServerFault {
		JobExecutionQuery query = new JobExecutionQuery();
		query.jobId = GLOBAL_JOB;
		ListResult<JobExecution> executions = serviceAdmin0.searchExecution(query);
		assertEquals(0L, executions.total);

		serviceAdmin0.start(GLOBAL_JOB, null);
		waitFor(GLOBAL_JOB);

		executions = serviceAdmin0.searchExecution(query);

		assertEquals(1L, executions.total);
	}

	@Test
	public void testStartAndFindExecution() throws ServerFault {
		JobExecutionQuery query = new JobExecutionQuery();
		query.jobId = DOMAIN_JOB;
		ListResult<JobExecution> executions = serviceAdmin.searchExecution(query);

		assertEquals(0L, executions.total);

		serviceAdmin.start(DOMAIN_JOB, null);
		waitFor(DOMAIN_JOB);

		// wait for insert
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}

		executions = serviceAdmin.searchExecution(query);
		assertEquals(1L, executions.total);

		JobExecution latest = executions.values.get(0);
		assertNotNull(latest.startDate);
		assertNotNull(latest.endDate);
		assertNotNull(latest.domainUid);
		System.out.println("latest exec end: " + latest.endDate);

		Job job = serviceAdmin.getJobFromId(DOMAIN_JOB);
		assertEquals(1, job.domainPlanification.size());

		JobPlanification planif = job.domainPlanification.get(0);
		System.out.println("last run in planification " + planif.lastRun);
		assertEquals(latest.startDate, planif.lastRun);

		Set<LogEntry> logs = serviceAdmin.getLogs(latest, 0);
		assertNotNull(logs);

		assertFalse(logs.isEmpty());
		long timestamp = 0;

		for (LogEntry log : logs) {

			long ts = log.timestamp;
			assertTrue("timestamp must be positive", ts > 0);
			System.out.println("ts: " + ts + " oldTs: " + timestamp + " " + log.severity + " (" + log.content + ")");
			assertTrue("entries must be sorted by timestamp, from oldest to newest", ts >= timestamp);
			timestamp = ts;
			assertNotNull("entry content must not be null", log.content);
			assertNotNull("log entry severity must not be null", log.severity);

			if (log.content.startsWith("#progress ")) {
				assertEquals(LogLevel.PROGRESS, log.severity);
			}

		}
	}

	@Test
	public void testCancelJob() throws Exception {
		JobExecutionQuery query = new JobExecutionQuery();
		query.jobId = WAITING_JOB;

		// we need to sleep some time, job starting is async

		serviceAdmin0.start(WAITING_JOB, null);
		Thread.sleep(500);
		assertEquals(1, Scheduler.get().getActiveSlots().size());
		// this job will get ignored, job is already running
		serviceAdmin0.start(WAITING_JOB, null);
		Thread.sleep(500);
		assertEquals(1, Scheduler.get().getActiveSlots().size());
		serviceAdmin0.cancel(WAITING_JOB, null);
		Thread.sleep(500);
		assertEquals(0, Scheduler.get().getActiveSlots().size());
		serviceAdmin0.start(WAITING_JOB, null);
		Thread.sleep(500);
		assertEquals(1, Scheduler.get().getActiveSlots().size());
		serviceAdmin0.cancel(WAITING_JOB, null);
		Thread.sleep(500);
		assertEquals(0, Scheduler.get().getActiveSlots().size());

		ListResult<JobExecution> executions = serviceAdmin0.searchExecution(query);
		assertEquals(2, executions.total);

		executions.values.forEach(ret -> {
			assertEquals(JobExitStatus.FAILURE, ret.status);
		});

		assertEquals(2, WaitingJob.cancelled);
	}

	@Test
	public void testUpdateExecution() throws Exception {
		JobExecution je = new JobExecution();
		je.domainUid = "bm.lan";
		je.jobId = DOMAIN_JOB;
		je.startDate = new Date();
		je.execGroup = DOMAIN_JOB;
		je.status = JobExitStatus.IN_PROGRESS;
		serviceAdmin.createExecution(je);

		JobExecutionQuery query = new JobExecutionQuery();
		query.jobId = DOMAIN_JOB;

		ListResult<JobExecution> executions = serviceAdmin.searchExecution(query);
		executions = serviceAdmin.searchExecution(query);
		assertEquals(1, executions.total);

		JobExecution latest = executions.values.get(0);
		assertEquals(latest.status, JobExitStatus.IN_PROGRESS);

		serviceAdmin.updateExecution(latest);
		executions = serviceAdmin.searchExecution(query);
		assertEquals(1, executions.total);
		JobExecution updated = executions.values.get(0);
		assertEquals(updated.status, JobExitStatus.IN_PROGRESS);
	}

	@Test
	public void testUpdatePlan() throws ServerFault {
		Job job = serviceAdmin.getJobFromId(DOMAIN_JOB);
		assertNotNull(job);

		List<JobPlanification> plans = job.domainPlanification;
		job.sendReport = true;
		String recip = System.currentTimeMillis() + "@bluemind.lan";
		job.recipients = recip;

		JobPlanification jp = new JobPlanification();
		jp.kind = PlanKind.SCHEDULED;
		jp.domain = "bm.lan";
		JobRec rec = new JobRec();
		rec.cronString = JobRec.AT_MIDNIGHT;
		jp.rec = rec;
		plans.add(jp);

		serviceAdmin.update(job);

		Job fetched = serviceAdmin.getJobFromId(DOMAIN_JOB);

		plans = fetched.domainPlanification;

		assertEquals(1, plans.size());
		boolean contains = false;
		for (JobPlanification plan : plans) {
			if (plan.domain.equals("bm.lan")) {
				contains = true;
			}
		}
		assertTrue(contains);
		assertTrue(fetched.sendReport);
		assertEquals(recip, fetched.recipients);

		// reset to every minute scheduling
		jp.kind = PlanKind.SCHEDULED;
		rec.cronString = JobRec.EVERY_MINUTE;
		serviceAdmin.update(job);
	}

	@Test
	public void testDeleteExec() throws ServerFault {
		// ensure one execution
		serviceAdmin0.start(GLOBAL_JOB, null);
		waitFor(GLOBAL_JOB);

		JobExecutionQuery query = new JobExecutionQuery();
		query.jobId = GLOBAL_JOB;
		ListResult<JobExecution> execs = serviceAdmin0.searchExecution(query);

		assertTrue("We expected one execution of event alert here", execs.total > 0);

		long total = execs.total;
		System.out.println("execs before delete: " + total);

		// remove the oldest one
		JobExecution toDelete = execs.values.get((int) total - 1);
		assertTrue(toDelete.id > 0);
		serviceAdmin0.deleteExecution(toDelete.id);

		execs = serviceAdmin0.searchExecution(query);
		long newTotal = execs.total;

		// ok, this test does not check that we deleted the correct execution
		assertEquals(total - 1, newTotal);

		Set<LogEntry> logs = serviceAdmin0.getLogs(toDelete, 0);
		assertTrue(logs.isEmpty());
	}

	@Test
	public void testExecPagination() throws ServerFault {

		// ensure one execution
		serviceAdmin0.start(GLOBAL_JOB, null);
		waitFor(GLOBAL_JOB);

		// ensure another execution
		serviceAdmin0.start(GLOBAL_JOB, null);
		waitFor(GLOBAL_JOB);

		JobExecutionQuery query = new JobExecutionQuery();
		query.from = 0;
		query.size = 1;
		query.jobId = GLOBAL_JOB;
		ListResult<JobExecution> execs = serviceAdmin0.searchExecution(query);

		System.out.println("execs.size: " + execs.values.size() + " total found: " + execs.total);

		assertEquals("execution pagination is not working", 1, execs.values.size());

		assertTrue(execs.total > execs.values.size());
		waitFor(GLOBAL_JOB);
	}

	@Test
	public void testGetGlobalJobFromId() throws ServerFault {
		Job job = serviceAdmin0.getJobFromId(GLOBAL_JOB);
		assertNotNull(job);
		assertNotNull(job.kind);
	}

	@Test
	public void testGetDomainJobFromId() throws ServerFault {
		Job job = serviceAdmin.getJobFromId(DOMAIN_JOB);
		assertNotNull(job);
		assertNotNull(job.kind);
	}

	@Test
	public void testGetJobFromIdPerms() throws ServerFault {
		Job job = serviceAdmin.getJobFromId(DOMAIN_JOB);
		assertNotNull(job);

		try {
			serviceAdmin.getJobFromId(GLOBAL_JOB);
			fail("Should have received a server fault about job visibility");
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void testFindJobs() throws ServerFault {
		// ensure one execution
		serviceAdmin.start(DOMAIN_JOB, null);
		waitFor(DOMAIN_JOB);

		ListResult<Job> ret = serviceAdmin.searchJob(new JobQuery());
		assertNotNull(ret);

		// at least DOMAIN_JOB
		assertTrue(ret.total >= 1);
		assertFalse(ret.values.isEmpty());

		for (Job j : ret.values) {
			assertNotNull("job id must not be null", j.id);
			assertNotNull("job kind must not be null", j.kind);
			assertEquals(JobKind.MULTIDOMAIN, j.kind);
			System.out.println("Found job " + j.id + " desc: " + j.description);
		}
	}

	@Test
	public void testSearchJobs_withDomainAndId() throws ServerFault {
		serviceAdmin0.start(DOMAIN_JOB, "bm.lan");
		waitFor(DOMAIN_JOB, "bm.lan");
		serviceAdmin0.start(DOMAIN_JOB, "bm1.lan");
		waitFor(DOMAIN_JOB, "bm1.lan");
		serviceAdmin0.start(DOMAIN_JOB, "bm2.lan");
		waitFor(DOMAIN_JOB, "bm2.lan");

		JobQuery jq = JobQuery.withIdAndDomainUid(DOMAIN_JOB, "bm.lan");
		ListResult<Job> ret = serviceAdmin0.searchJob(jq);
		assertNotNull(ret);
		assertEquals(1, ret.total);
		Job j = ret.values.get(0);
		j.kind = JobKind.MULTIDOMAIN;
		j.sendReport = true;
		j.recipients = "r0@bm.lan";
		serviceAdmin0.update(j);

		jq = JobQuery.withIdAndDomainUid(DOMAIN_JOB, "bm1.lan");
		ret = serviceAdmin0.searchJob(jq);
		assertNotNull(ret);
		assertEquals(1, ret.total);
		j = ret.values.get(0);
		j.kind = JobKind.MULTIDOMAIN;
		j.sendReport = true;
		j.recipients = "r1@bm.lan";
		serviceAdmin0.update(j);

		jq = JobQuery.withIdAndDomainUid(DOMAIN_JOB, "bm2.lan");
		ret = serviceAdmin0.searchJob(jq);
		assertNotNull(ret);
		assertEquals(1, ret.total);
		j = ret.values.get(0);
		j.kind = JobKind.MULTIDOMAIN;
		j.sendReport = true;
		j.recipients = "r2@bm.lan";
		serviceAdmin0.update(j);

		// domain bm.lan
		jq = JobQuery.withIdAndDomainUid(DOMAIN_JOB, "bm.lan");
		ret = serviceAdmin0.searchJob(jq);
		assertNotNull(ret);
		assertEquals(1, ret.total);
		j = ret.values.get(0);
		assertNotNull("job id must not be null", j.id);
		assertNotNull("job kind must not be null", j.kind);
		assertTrue("job send report must not be true", j.sendReport);
		assertEquals(JobKind.MULTIDOMAIN, j.kind);
		assertEquals("r0@bm.lan", j.recipients);
		System.out.println("Found job " + j.id + " desc: " + j.description + " recipients: " + j.recipients);

		// domain bm1.lan
		jq = JobQuery.withIdAndDomainUid(DOMAIN_JOB, "bm1.lan");
		ret = serviceAdmin0.searchJob(jq);
		assertNotNull(ret);
		assertEquals(1, ret.total);
		j = ret.values.get(0);
		assertNotNull("job id must not be null", j.id);
		assertNotNull("job kind must not be null", j.kind);
		assertTrue("job send report must not be true", j.sendReport);
		assertEquals(JobKind.MULTIDOMAIN, j.kind);
		assertEquals("r1@bm.lan", j.recipients);
		System.out.println("Found job " + j.id + " desc: " + j.description + " recipients: " + j.recipients);

		// domain bm2.lan
		jq = JobQuery.withIdAndDomainUid(DOMAIN_JOB, "bm2.lan");
		ret = serviceAdmin0.searchJob(jq);
		assertNotNull(ret);
		assertEquals(1, ret.total);
		j = ret.values.get(0);
		assertNotNull("job id must not be null", j.id);
		assertNotNull("job kind must not be null", j.kind);
		assertTrue("job send report must not be true", j.sendReport);
		assertEquals(JobKind.MULTIDOMAIN, j.kind);
		assertEquals("r2@bm.lan", j.recipients);
		System.out.println("Found job " + j.id + " desc: " + j.description + " recipients: " + j.recipients);
	}

	@Test
	public void testFindJobsAdmin0() throws ServerFault {
		serviceAdmin0.start(DOMAIN_JOB, "bm.lan");
		waitFor(DOMAIN_JOB, "bm.lan");
		serviceAdmin0.start(DOMAIN_JOB, "bm1.lan");
		waitFor(DOMAIN_JOB, "bm1.lan");
		serviceAdmin0.start(DOMAIN_JOB, "bm2.lan");
		waitFor(DOMAIN_JOB, "bm2.lan");

		JobQuery jq = JobQuery.withIdAndDomainUid(DOMAIN_JOB, "bm.lan");
		ListResult<Job> ret = serviceAdmin0.searchJob(jq);
		assertNotNull(ret);
		assertEquals(1, ret.total);
		Job j = ret.values.get(0);
		j.kind = JobKind.MULTIDOMAIN;
		j.sendReport = true;
		j.recipients = "r0@bm.lan";
		serviceAdmin0.update(j);

		jq = JobQuery.withIdAndDomainUid(DOMAIN_JOB, "bm1.lan");
		ret = serviceAdmin0.searchJob(jq);
		assertNotNull(ret);
		assertEquals(1, ret.total);
		j = ret.values.get(0);
		j.kind = JobKind.MULTIDOMAIN;
		j.sendReport = true;
		j.recipients = "r1@bm.lan";
		serviceAdmin0.update(j);

		jq = JobQuery.withIdAndDomainUid(DOMAIN_JOB, "bm2.lan");
		ret = serviceAdmin0.searchJob(jq);
		assertNotNull(ret);
		assertEquals(1, ret.total);
		j = ret.values.get(0);
		j.kind = JobKind.MULTIDOMAIN;
		j.sendReport = true;
		j.recipients = "r2@bm.lan";
		serviceAdmin0.update(j);

		// domain bm.lan
		jq = JobQuery.withIdAndDomainUid(DOMAIN_JOB, "bm.lan");
		ret = serviceAdmin0.searchJob(jq);
		j = ret.values.get(0);
		assertNotNull("job id must not be null", j.id);
		assertNotNull("job kind must not be null", j.kind);
		assertTrue(j.sendReport);
		assertEquals(JobKind.MULTIDOMAIN, j.kind);
		assertEquals("r0@bm.lan", j.recipients);
		System.out.println("Found job " + j.id + " desc: " + j.description + " recipients: " + j.recipients);

		// domain bm1.lan
		jq = JobQuery.withIdAndDomainUid(DOMAIN_JOB, "bm1.lan");
		ret = serviceAdmin0.searchJob(jq);
		j = ret.values.get(0);
		assertNotNull("job id must not be null", j.id);
		assertNotNull("job kind must not be null", j.kind);
		assertTrue(j.sendReport);
		assertEquals(JobKind.MULTIDOMAIN, j.kind);
		assertEquals("r1@bm.lan", j.recipients);
		System.out.println("Found job " + j.id + " desc: " + j.description + " recipients: " + j.recipients);

		// domain bm2.lan
		jq = JobQuery.withIdAndDomainUid(DOMAIN_JOB, "bm2.lan");
		ret = serviceAdmin0.searchJob(jq);
		j = ret.values.get(0);
		assertNotNull("job id must not be null", j.id);
		assertNotNull("job kind must not be null", j.kind);
		assertTrue(j.sendReport);
		assertEquals(JobKind.MULTIDOMAIN, j.kind);
		assertEquals("r2@bm.lan", j.recipients);
		System.out.println("Found job " + j.id + " desc: " + j.description + " recipients: " + j.recipients);
	}

	@Test
	public void testActiveJobs() throws ServerFault {
		JobExecutionQuery query = new JobExecutionQuery();
		query.jobId = DOMAIN_JOB;

		ListResult<JobExecution> executions = serviceAdmin.searchExecution(query);
		assertNotNull(executions);
		long total = executions.total;
		System.out.println("Before force-starting QUOTA: " + total + " executions.");

		serviceAdmin.start(DOMAIN_JOB, null);

		// the job might take some time to start & request its slot
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		JobExecutionQuery execQuery = new JobExecutionQuery();
		execQuery.active = true;
		ListResult<JobExecution> active = serviceAdmin.searchExecution(execQuery);

		assertFalse("We should at least have an active domainJob", active.values.isEmpty());

		for (JobExecution je : active.values) {
			assertEquals("Not all active executions I can fetch are from my domain", "bm.lan", je.domainUid);
		}

		// wait until one execution is recorded
		do {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			executions = serviceAdmin.searchExecution(query);
		} while (executions.total <= total);
		System.out.println("Executions total is now " + executions.total);

		waitFor(DOMAIN_JOB);
	}

	// 1ère version
	@Test
	public void testSearchExecutionResultsAreOrderedByStartExecutionDate() throws InterruptedException {
		JobExecutionQuery query = new JobExecutionQuery();
		query.from = 0;
		query.size = 2;
		query.jobId = DOMAIN_JOB;

		// start 2 executions
		serviceAdmin0.start(DOMAIN_JOB, "bm.lan");
		waitFor(DOMAIN_JOB);
		serviceAdmin0.start(DOMAIN_JOB, "bm.lan");
		waitFor(DOMAIN_JOB);

		// check we got the last one
		ListResult<JobExecution> results = serviceAdmin0.searchExecution(query);

		assertNotNull(results);
		assertEquals(2, results.values.size());
		assertTrue(results.values.get(0).startDate.after(results.values.get(1).startDate));
		Date lastDate = results.values.get(0).startDate;

		query.size = 1;
		results = serviceAdmin0.searchExecution(query);
		assertEquals(1, results.values.size());
		assertEquals(lastDate, results.values.get(0).startDate);
	}

	// 2ème version
	@Test
	public void test2SearchExecutionResultsAreOrderedByStartExecutionDate() throws InterruptedException {
		JobExecutionQuery query = new JobExecutionQuery();
		query.from = 0;
		query.size = 1;

		// start 2 executions
		serviceAdmin0.start(DOMAIN_JOB, "bm.lan");
		waitFor(DOMAIN_JOB);
		serviceAdmin0.start(GLOBAL_JOB, "bm.lan");
		waitFor(GLOBAL_JOB);

		// check we got the last one
		ListResult<JobExecution> results = serviceAdmin0.searchExecution(query);

		assertNotNull(results);
		assertEquals(1, results.values.size());
		assertEquals(GLOBAL_JOB, results.values.get(0).jobId);
	}

	private void waitFor(String jobId) throws ServerFault {
		int waitCount = 0;
		JobExecutionQuery query = new JobExecutionQuery();
		query.active = true;
		query.jobId = jobId;
		do {
			try {
				Thread.sleep(1000);
				waitCount++;
			} catch (InterruptedException e) {
			}
		} while (!serviceAdmin0.searchExecution(query).values.isEmpty() && waitCount < 70);
		if (waitCount >= 70) {
			throw new ServerFault("too long to execute");
		}
	}

	private void waitFor(String jobId, String domain) throws ServerFault {
		int waitCount = 0;
		JobExecutionQuery query = new JobExecutionQuery();
		query.active = true;
		query.jobId = jobId;
		query.domain = domain;
		do {
			try {
				Thread.sleep(1000);
				waitCount++;
			} catch (InterruptedException e) {
			}
		} while (!serviceAdmin0.searchExecution(query).values.isEmpty() && waitCount < 70);
		if (waitCount >= 70) {
			throw new ServerFault("too long to execute");
		}
	}

}
