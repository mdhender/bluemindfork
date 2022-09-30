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
package net.bluemind.mailbox.job.tests;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.job.QuotaGatheringJob;
import net.bluemind.scheduledjob.api.IJob;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class QuotaGatheringJobTests {

	private IJob serviceAdmin0;
	private String quotaGatheringJob = new QuotaGatheringJob().getJobId();
	private IMailboxes mailBoxesApi;
	private String domainUid = "test" + System.currentTimeMillis() + ".lab";
	private String loginUser1 = "user1";
	private String mailUser1;
	private String loginUser2 = "user2";
	private String mailUser2;

	@Before
	public void before() throws Exception {

		ElasticsearchTestHelper.getInstance().beforeTest(25);
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		SecurityContext admin = new SecurityContext("testUser", "test", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_ADMIN), domainUid);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		Assert.assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		Server impaServer = new Server();
		impaServer.tags = Collections.singletonList("mail/imap");
		impaServer.ip = PopulateHelper.FAKE_CYRUS_IP;

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt(esServer, impaServer);

		PopulateHelper.createTestDomain(domainUid);
		PopulateHelper.domainAdmin(domainUid, admin.getSubject());

		PopulateHelper.addUser(loginUser1, domainUid);
		PopulateHelper.addUser(loginUser2, domainUid);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		serviceAdmin0 = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IJob.class);

		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");

		mailBoxesApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class,
				domainUid);
		mailUser1 = loginUser1 + "@" + domainUid;
		mailUser2 = loginUser2 + "@" + domainUid;
	}

	@Test
	public void testQuotaGatheringJob() throws Exception {

		// Sets quota for user1 mailbox
		var mailboxUser1 = mailBoxesApi.getComplete(loginUser1);
		mailboxUser1.value.quota = 45 * 1024;
		mailBoxesApi.update(loginUser1, mailboxUser1.value);

		try (StoreClient sc = new StoreClient("127.0.0.1", 1144, mailUser1, loginUser1)) {
			assertTrue(sc.login());

			// Create 100 mails
			IntStream.range(1, 100).forEach(i -> {
				int added = sc.append("INBOX", eml("emls/test_mail.eml"), new FlagsList());
				assertTrue(added > 0);
			});

			Thread.sleep(1_000);

			JobExecutionQuery query = new JobExecutionQuery();
			serviceAdmin0.start(quotaGatheringJob, domainUid);
			query.jobId = quotaGatheringJob;
			await().atMost(10, TimeUnit.SECONDS).until(() -> !serviceAdmin0.searchExecution(query).values.isEmpty());

			JobExecution jobExecution = serviceAdmin0.searchExecution(query).values.get(0);
			var logs = serviceAdmin0.getLogs(jobExecution, 0);
			var isLogUsagePresent = logs.stream()
					.filter(l -> l.content.contains("usage for " + loginUser1 + " is: 11% in use (used: 5206 / 46080)"))
					.findAny();
			Assert.assertEquals(true, isLogUsagePresent.isPresent());
			Assert.assertEquals(1L, serviceAdmin0.searchExecution(query).total);
			Assert.assertEquals(JobExitStatus.SUCCESS, jobExecution.status);
		}
	}

	@Test
	public void testQuotaGatheringJobForTwoUsers() throws Exception {

		// Sets quota for user1 & user2 mailboxes
		var mailboxUser1 = mailBoxesApi.getComplete(loginUser1);
		mailboxUser1.value.quota = 45 * 1024;
		mailBoxesApi.update(loginUser1, mailboxUser1.value);

		var mailboxUser2 = mailBoxesApi.getComplete(loginUser2);
		mailboxUser2.value.quota = 1 * 1024;
		mailBoxesApi.update(loginUser2, mailboxUser2.value);

		try (StoreClient sc1 = new StoreClient("127.0.0.1", 1144, mailUser1, loginUser1);
				StoreClient sc2 = new StoreClient("127.0.0.1", 1144, mailUser2, loginUser2)) {
			assertTrue(sc1.login());
			assertTrue(sc2.login());

			// Create 100 mails for user1
			IntStream.range(1, 100).forEach(i -> {
				int added = sc1.append("INBOX", eml("emls/test_mail.eml"), new FlagsList());
				assertTrue(added > 0);
			});

			// Create 20 mails for user2
			IntStream.range(1, 20).forEach(i -> {
				int added = sc1.append("INBOX", eml("emls/test_mail.eml"), new FlagsList());
				assertTrue(added > 0);
			});

			Thread.sleep(1_000);

			JobExecutionQuery query = new JobExecutionQuery();
			serviceAdmin0.start(quotaGatheringJob, domainUid);
			query.jobId = quotaGatheringJob;
			await().atMost(20, TimeUnit.SECONDS).until(() -> !serviceAdmin0.searchExecution(query).values.isEmpty());

			JobExecution jobExecution = serviceAdmin0.searchExecution(query).values.get(0);
			var logs = serviceAdmin0.getLogs(jobExecution, 0);
			logs.forEach(l -> System.err.println(l));
			var isLogUsageUser1Present = logs.stream()
					.filter(l -> l.content.contains("usage for " + loginUser1 + " is: 13% in use (used: 6205 / 46080)"))
					.findAny();
			var isLogUsageUser2Present = logs.stream()
					.filter(l -> l.content.contains("usage for " + loginUser2 + " is: 0% in use (usable:")).findAny();
			Assert.assertEquals(true, isLogUsageUser1Present.isPresent());
			Assert.assertEquals(true, isLogUsageUser2Present.isPresent());
			Assert.assertEquals(1L, serviceAdmin0.searchExecution(query).total);
			Assert.assertEquals(JobExitStatus.SUCCESS, jobExecution.status);
		}
	}

	@Test
	public void testQuotaGatheringJobOverQuota() throws Exception {

		// Sets quota for user1 mailbox
		var mailboxUser1 = mailBoxesApi.getComplete(loginUser1);
		mailboxUser1.value.quota = 1 * 1024;
		mailBoxesApi.update(loginUser1, mailboxUser1.value);

		try (StoreClient sc = new StoreClient("127.0.0.1", 1144, mailUser1, loginUser1)) {
			assertTrue(sc.login());

			// Create 20 mails
			IntStream.range(1, 20).forEach(i -> {
				int added = sc.append("INBOX", eml("emls/test_mail.eml"), new FlagsList());
				assertTrue(added > 0);
			});

			Thread.sleep(1_000);

			JobExecutionQuery query = new JobExecutionQuery();
			serviceAdmin0.start(quotaGatheringJob, domainUid);
			query.jobId = quotaGatheringJob;
			await().atMost(10, TimeUnit.SECONDS).until(() -> !serviceAdmin0.searchExecution(query).values.isEmpty());

			JobExecution jobExecution = serviceAdmin0.searchExecution(query).values.get(0);
			var logs = serviceAdmin0.getLogs(jobExecution, 0);
			var isLogUsagePresent = logs.stream()
					.filter(l -> l.content.contains("usage for " + loginUser1 + " is above warning threshold (85%)"))
					.findAny();
			System.err.println(jobExecution.status);
			Assert.assertEquals(true, isLogUsagePresent.isPresent());
			Assert.assertEquals(1L, serviceAdmin0.searchExecution(query).total);
			Assert.assertEquals(JobExitStatus.COMPLETED_WITH_WARNINGS, jobExecution.status);
		}
	}

	@Test
	public void testQuotaGatheringJobNoUsageQuota() throws Exception {

		// Sets quota for user1 mailbox
		var mailboxUser1 = mailBoxesApi.getComplete(loginUser1);
		mailboxUser1.value.quota = 1 * 1024;
		mailBoxesApi.update(loginUser1, mailboxUser1.value);

		try (StoreClient sc = new StoreClient("127.0.0.1", 1144, mailUser1, loginUser1)) {
			assertTrue(sc.login());

			Thread.sleep(1_000);

			JobExecutionQuery query = new JobExecutionQuery();
			serviceAdmin0.start(quotaGatheringJob, domainUid);
			query.jobId = quotaGatheringJob;
			await().atMost(10, TimeUnit.SECONDS).until(() -> !serviceAdmin0.searchExecution(query).values.isEmpty());

			JobExecution jobExecution = serviceAdmin0.searchExecution(query).values.get(0);
			var logs = serviceAdmin0.getLogs(jobExecution, 0);
			var isLogUsagePresent = logs.stream()
					.filter(l -> l.content.contains("usage for " + loginUser1 + " is: 0% in use")).findAny();
			Assert.assertEquals(true, isLogUsagePresent.isPresent());
			Assert.assertEquals(1L, serviceAdmin0.searchExecution(query).total);
			Assert.assertEquals(JobExitStatus.SUCCESS, jobExecution.status);
		}
	}

	@Test
	public void testQuotaGatheringJobNoQuotaSet() throws Exception {

		try (StoreClient sc = new StoreClient("127.0.0.1", 1144, mailUser1, loginUser1)) {
			assertTrue(sc.login());

			// Create 20 mails
			IntStream.range(1, 20).forEach(i -> {
				int added = sc.append("INBOX", eml("emls/test_mail.eml"), new FlagsList());
				assertTrue(added > 0);
			});

			Thread.sleep(1_000);

			JobExecutionQuery query = new JobExecutionQuery();
			serviceAdmin0.start(quotaGatheringJob, domainUid);
			query.jobId = quotaGatheringJob;
			await().atMost(10, TimeUnit.SECONDS).until(() -> !serviceAdmin0.searchExecution(query).values.isEmpty());

			Assert.assertEquals(1L, serviceAdmin0.searchExecution(query).total);
			Assert.assertEquals(JobExitStatus.SUCCESS, serviceAdmin0.searchExecution(query).values.get(0).status);
		}
	}

	private InputStream eml(String resPath) {
		return QuotaGatheringJobTests.class.getClassLoader().getResourceAsStream(resPath);
	}
}
