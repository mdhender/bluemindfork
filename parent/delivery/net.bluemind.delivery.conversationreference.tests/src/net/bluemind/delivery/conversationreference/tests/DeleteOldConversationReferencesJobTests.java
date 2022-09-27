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
package net.bluemind.delivery.conversationreference.tests;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.delivery.conversationreference.persistence.ConversationReference;
import net.bluemind.delivery.conversationreference.service.DeleteOldConversationReferencesJob;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.scheduledjob.api.IJob;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DeleteOldConversationReferencesJobTests {

	private static final Logger logger = LoggerFactory.getLogger(DeleteOldConversationReferencesJobTests.class);

	private IJob serviceAdmin0;

	private String deleteOldConversationJob = new DeleteOldConversationReferencesJob().getJobId();

	private ConversationReferenceStoreWithExpiresDates store;

	@Before
	public void before() throws Exception {
		Long mailboxId1 = 10L;
		Long mailboxId2 = 20L;

		String message1Id = "<6a2b976c0f1f14876917aea6ebfb457f@f8de2c4a.internal>";
		String message2Id = "<09f8c8e65442062c0d1d23022a5be532@f8de2c4a.internal>";
		String message3Id = "<09f8c8e65552062c0d1d23033a5be533@f8de2c4a.internal>";

		long conversation1Id = -1060821470570927639L;
		Long conversation2Id = 8745557296093279025L;

		HashFunction hf = Hashing.sipHash24();
		long hashMessage1Id = hf.hashBytes(message1Id.getBytes()).asLong();
		long hashMessage2Id = hf.hashBytes(message2Id.getBytes()).asLong();
		long hashMessage3Id = hf.hashBytes(message3Id.getBytes()).asLong();

		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext admin = new SecurityContext("testUser", "test", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_ADMIN), "bm.lan");

		PopulateHelper.initGlobalVirt();

		PopulateHelper.createTestDomain("bm.lan");
		PopulateHelper.domainAdmin("bm.lan", admin.getSubject());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		serviceAdmin0 = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IJob.class);

		store = new ConversationReferenceStoreWithExpiresDates(JdbcTestHelper.getInstance().getMailboxDataDataSource()); // bj-data

		store.create(ConversationReference.of(hashMessage1Id, conversation1Id, mailboxId1));
		store.create(ConversationReference.of(hashMessage2Id, conversation1Id, mailboxId1));
		store.create(ConversationReference.of(hashMessage1Id, conversation2Id, mailboxId2));
		store.create(ConversationReference.of(hashMessage2Id, conversation2Id, mailboxId2));
		store.insertWithExpires(mailboxId2, hashMessage3Id, conversation2Id);
		store.insertWithExpires(mailboxId1, hashMessage3Id, conversation1Id);

		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testDeleteOfConversationReferenceJob() throws Exception {
		long numberOfRecords = store.getNumberOfEntries();
		logger.info("Number of records in t_conversationreference before DeleteOldConversationReferencesJob : {}",
				numberOfRecords);
		Assert.assertEquals(6L, numberOfRecords);
		JobExecutionQuery query = new JobExecutionQuery();
		serviceAdmin0.start(deleteOldConversationJob, null);
		waitFor(deleteOldConversationJob);

		query.jobId = deleteOldConversationJob;
		Assert.assertEquals(1L, serviceAdmin0.searchExecution(query).total);
		Assert.assertEquals(JobExitStatus.SUCCESS, serviceAdmin0.searchExecution(query).values.get(0).status);

		numberOfRecords = store.getNumberOfEntries();
		logger.info("Number of records in t_conversationreference after DeleteOldConversationReferencesJob : {}",
				numberOfRecords);
		Assert.assertEquals(4L, numberOfRecords);
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
