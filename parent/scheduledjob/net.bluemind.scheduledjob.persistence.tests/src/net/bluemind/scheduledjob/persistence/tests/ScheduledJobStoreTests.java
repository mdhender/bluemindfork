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
package net.bluemind.scheduledjob.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.persistence.DomainStore;
import net.bluemind.scheduledjob.api.Job;
import net.bluemind.scheduledjob.api.JobDomainStatus;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobPlanification;
import net.bluemind.scheduledjob.api.JobQuery;
import net.bluemind.scheduledjob.api.JobRec;
import net.bluemind.scheduledjob.api.LogEntry;
import net.bluemind.scheduledjob.api.LogLevel;
import net.bluemind.scheduledjob.api.PlanKind;
import net.bluemind.scheduledjob.persistence.ScheduledJobStore;

public class ScheduledJobStoreTests {

	private ItemStore domainItemStore;
	private DomainStore domainStore;
	private ScheduledJobStore store;

	@Before
	public void before() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		String installationId = InstallationId.getIdentifier();
		Container domains = Container.create(installationId + "_domains", "domains", "domains container", "system",
				true);
		domains = containerStore.create(domains);
		assertNotNull(domains);

		domainItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), domains, securityContext);

		domainStore = new DomainStore(JdbcTestHelper.getInstance().getDataSource());

		store = new ScheduledJobStore(JdbcTestHelper.getInstance().getDataSource());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSearchingExecutionWithStatusUnknownShouldNotFail() throws SQLException, ServerFault {
		Domain domain = createDomain();

		Date startDate = new Date();
		JobExecution r = new JobExecution();
		r.execGroup = "group";
		r.domainName = domain.name;
		r.jobId = "jobId";
		r.startDate = startDate;
		r.endDate = startDate;
		r.status = JobExitStatus.FAILURE;

		store.ensureDefaultPlan(domain.name, r.jobId);

		JobExecution je = store.createExecution(r);
		assertEquals(1, je.id);

		JobExecutionQuery jeq = new JobExecutionQuery();
		jeq.id = je.id;
		jeq.statuses = new HashSet<>(EnumSet.allOf(JobExitStatus.class));

		try {
			store.findExecutions(jeq);
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void testSearchExecutionResultsAreOrderedByStartExecutionDate() throws InterruptedException, SQLException {
		Domain domain = createDomain();

		// create 2 executions
		long nowInMillis = Calendar.getInstance().getTimeInMillis();
		JobExecution jobExec1 = defaultJobExecution(domain);
		jobExec1.startDate = new Date(nowInMillis + 5 * 60000); // begins in 5mins
		jobExec1.jobId = "jobId1";
		JobExecution jobExec2 = defaultJobExecution(domain);
		jobExec2.startDate = new Date(nowInMillis);
		jobExec2.jobId = "jobId2";

		store.createExecution(jobExec1);
		store.createExecution(jobExec2);

		// check results are well ordered by start execution date
		JobExecutionQuery query = new JobExecutionQuery();
		query.from = 0;
		query.size = 1;

		ListResult<JobExecution> results = store.findExecutions(query);
		assertNotNull(results);
		assertEquals(1, results.values.size());
		assertEquals("jobId1", results.values.get(0).jobId);
	}

	@Test
	public void testExecution() throws SQLException, ServerFault {
		Domain domain = createDomain();

		Date startDate = new Date();
		JobExecution r = new JobExecution();
		r.execGroup = "group";
		r.domainName = domain.name;
		r.jobId = "jobId";
		r.startDate = startDate;
		r.endDate = startDate;
		r.status = JobExitStatus.FAILURE;

		store.ensureDefaultPlan(domain.name, r.jobId);

		JobExecution je = store.createExecution(r);
		assertEquals(1, je.id);

		JobExecutionQuery jeq = new JobExecutionQuery();
		jeq.id = je.id;

		ListResult<JobExecution> executions = store.findExecutions(jeq);
		assertEquals(1, executions.total);

		JobExecution execution = executions.values.get(0);
		assertEquals(je, execution);

		store.delete(Arrays.asList(execution.id));

		executions = store.findExecutions(jeq);
		assertEquals(0, executions.total);
	}

	@Test
	public void testExecutionWithNullStatuses() throws SQLException, ServerFault {
		Domain domain = createDomain();

		Date startDate = new Date();
		JobExecution r = new JobExecution();
		r.execGroup = "group";
		r.domainName = domain.name;
		r.jobId = "jobId";
		r.startDate = startDate;
		r.endDate = startDate;
		r.status = JobExitStatus.FAILURE;

		store.ensureDefaultPlan(domain.name, r.jobId);

		JobExecution je = store.createExecution(r);
		assertEquals(1, je.id);

		JobExecutionQuery jeq = new JobExecutionQuery();
		jeq.id = je.id;
		jeq.statuses = null;

		ListResult<JobExecution> executions = store.findExecutions(jeq);
		assertEquals(1, executions.total);

		JobExecution execution = executions.values.get(0);
		assertEquals(je, execution);

		store.delete(Arrays.asList(execution.id));

		executions = store.findExecutions(jeq);
		assertEquals(0, executions.total);
	}

	@Test
	public void testGetJob() throws SQLException, ServerFault {
		Domain domain = createDomain();

		Date startDate = new Date();
		JobExecution r = new JobExecution();
		r.execGroup = "group";
		r.domainName = domain.name;
		r.jobId = "jobId";
		r.startDate = startDate;
		r.endDate = startDate;
		r.status = JobExitStatus.FAILURE;

		store.ensureDefaultPlan(domain.name, r.jobId);

		JobExecution je = store.createExecution(r);
		assertEquals(1, je.id);

		Job job = store.getJobFromId(r.jobId);
		assertNotNull(job);

		assertNull(store.getJobFromId("xxx"));
	}

	@Test
	public void testLogEntries() throws SQLException, ServerFault {
		Domain domain = createDomain();

		Date startDate = new Date();
		JobExecution r = new JobExecution();
		r.execGroup = "group";
		r.domainName = domain.name;
		r.jobId = "jobId";
		r.startDate = startDate;
		r.endDate = startDate;
		r.status = JobExitStatus.FAILURE;

		store.ensureDefaultPlan(domain.name, r.jobId);

		JobExecution je = store.createExecution(r);
		assertEquals(1, je.id);

		Set<LogEntry> entries = new HashSet<LogEntry>();

		LogEntry e1 = new LogEntry();
		e1.timestamp = System.currentTimeMillis();
		e1.severity = LogLevel.INFO;
		e1.locale = "en";
		e1.content = "looks good";
		e1.offset = 0;
		entries.add(e1);

		LogEntry e2 = new LogEntry();
		e2.timestamp = e1.timestamp + 1000;
		e2.severity = LogLevel.INFO;
		e2.locale = "en";
		e2.content = "looks good too";
		e2.offset = 0;
		entries.add(e2);

		LogEntry e3 = new LogEntry();
		e3.timestamp = e2.timestamp + 1000;
		e3.severity = LogLevel.WARNING;
		e3.locale = "en";
		e3.content = "oops";
		e3.offset = 0;
		entries.add(e3);

		LogEntry e4 = new LogEntry();
		e4.timestamp = e3.timestamp + 1000;
		e4.severity = LogLevel.ERROR;
		e4.locale = "en";
		e4.content = "Bloody hell!";
		e4.offset = 0;
		entries.add(e4);

		store.storeLogEntries(je.id, entries);

		Set<LogEntry> fetchedEntries = store.fetchLogEntries(SecurityContext.SYSTEM, je.id);

		assertEquals(4, fetchedEntries.size());

		int found = 0;
		for (LogEntry le : fetchedEntries) {
			System.err.println(le);
			if (le.timestamp == e1.timestamp) {
				assertEquals(le, e1);
				found++;
			}
			if (le.timestamp == e2.timestamp) {
				assertEquals(le, e2);
				found++;
			}
			if (le.timestamp == e3.timestamp) {
				assertEquals(le, e3);
				found++;
			}
			if (le.timestamp == e4.timestamp) {
				assertEquals(le, e4);
				found++;
			}
		}

		assertEquals(4, found);
	}

	@Test
	public void testJobPlan() throws SQLException, ServerFault {
		Domain domain = createDomain();

		Date startDate = new Date();
		JobExecution r = new JobExecution();
		r.execGroup = "group";
		r.domainName = domain.name;
		r.jobId = "jobId";
		r.startDate = startDate;
		r.endDate = startDate;
		r.status = JobExitStatus.FAILURE;

		store.ensureDefaultPlan(domain.name, r.jobId);

		JobExecution je = store.createExecution(r);
		assertEquals(1, je.id);

		Job job = store.getJobFromId(r.jobId);
		assertNotNull(job);
		assertFalse(job.sendReport);
		assertNull(job.recipients);
		assertNull(job.kind);
		assertEquals(0, job.domainPlanification.size());

		job.sendReport = true;
		job.recipients = "david@bm.lan";

		JobPlanification jp = new JobPlanification();
		jp.domain = domain.name;
		jp.kind = PlanKind.SCHEDULED;
		jp.rec = new JobRec();
		jp.rec.cronString = "cron string here";

		job.domainPlanification.add(jp);

		store.updateJob(job);

		job = store.getJobFromId(r.jobId);
		assertEquals("david@bm.lan", job.recipients);
		assertEquals(0, job.domainPlanification.size());

		JobQuery jq = new JobQuery();
		store.loadStatusesAndPlans(SecurityContext.SYSTEM, jq, Arrays.asList(job));

		assertEquals(1, job.domainPlanification.size());
		JobPlanification found = job.domainPlanification.get(0);
		assertNotNull(found);
		assertEquals(domain.name, found.domain);
		assertEquals(jp.kind, found.kind);
		assertEquals(jp.rec.cronString, found.rec.cronString);

		List<JobDomainStatus> domainStatus = job.domainStatus;
		assertEquals(1, domainStatus.size());
	}

	@Test
	public void testDeleteExecutions() throws SQLException, ServerFault {
		Domain domain = createDomain();

		createExecution(domain);
		createExecution(domain);

		JobExecutionQuery jeq = new JobExecutionQuery();
		ListResult<JobExecution> executions = store.findExecutions(jeq);
		assertEquals(2, executions.total);

		store.delete(executions.values.stream().map(e -> e.id).collect(Collectors.toList()));

		executions = store.findExecutions(jeq);
		assertEquals(0, executions.total);
	}

	private void createExecution(Domain domain) {

		JobExecution je = defaultJobExecution(domain);
		store.ensureDefaultPlan(domain.name, je.jobId);
		store.createExecution(je);
	}

	private JobExecution defaultJobExecution(Domain domain) {
		String s = UUID.randomUUID().toString();

		JobExecution je = new JobExecution();
		je.execGroup = "group";
		je.domainName = domain.name;
		je.jobId = "jobId" + s;
		je.startDate = new Date();
		je.endDate = new Date(Calendar.getInstance().getTimeInMillis() + 5 * 60000); // end 5mins later
		je.status = JobExitStatus.FAILURE;
		return je;
	}

	/**
	 * @return
	 * @throws SQLException
	 */
	private Domain createDomain() throws SQLException {
		domainItemStore.create(Item.create("osef", null));
		Item item = domainItemStore.get("osef");

		Domain domain = new Domain();
		domain.name = "bm.lan";
		domain.label = "BlueMind";

		domainStore.create(item, domain);
		assertNotNull(domainStore.get(item));
		return domain;
	}

}
