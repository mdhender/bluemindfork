/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.cyrus.replication.client.SyncClient;
import net.bluemind.backend.mail.replica.service.tests.ReplicationEventsRecorder.Hierarchy;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class RecreateUserTests extends AbstractRollingReplicationTests {

	private String apiKey;

	@Before
	public void before() throws Exception {
		super.before();

		this.apiKey = "sid";
		SecurityContext secCtx = new SecurityContext("sid", userUid, Collections.emptyList(), Collections.emptyList(),
				domainUid);
		Sessions.get().put(apiKey, secCtx);

		long delay = System.currentTimeMillis();
		Hierarchy hierarchy = null;
		do {
			Thread.sleep(200);
			hierarchy = rec.hierarchy(domainUid, userUid);
			System.out.println("Hierarchy version is " + hierarchy.exactVersion);
			if (System.currentTimeMillis() - delay > 10000) {
				throw new TimeoutException("Hierarchy init took more than 10sec");
			}
		} while (hierarchy.exactVersion < 6);
		System.out.println("Hierarchy is now at version " + hierarchy.exactVersion);
		System.err.println("before is complete, starting test.");
	}

	@After
	public void after() throws Exception {
		System.err.println("Test is over, after starts...");
		super.after();
	}

	private IServiceProvider suProvider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	@Test
	public void deleteThenRecreateUser() throws Exception {
		IUser userApi = suProvider().instance(IUser.class, domainUid);
		ItemValue<User> theUser = userApi.getComplete(userUid);
		assertNotNull(theUser);

		TaskRef tr = userApi.delete(userUid);
		TaskStatus status = TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), tr);
		assertEquals(TaskStatus.State.Success, status.state);

		System.err.println("Start RE-populate user " + userUid);
		PopulateHelper.addUser(userUid, domainUid, Routing.internal);
		ItemValue<User> newUser = userApi.getComplete(userUid);
		assertNotNull(newUser);
		assertNotEquals(theUser.internalId, newUser.internalId);

		Thread.sleep(400);
		System.err.println("Connecting with syncClient");
		SyncClient sc = new SyncClient("127.0.0.1", 2501);
		sc.connect().thenCompose(c -> {
			return sc.getUser(userUid + "@" + domainUid);
		}).thenCompose(userResp -> {
			// we should assert that the box name is well composed regarding '.'
			// and '^'
			return sc.disconnect();
		}).get(5, TimeUnit.SECONDS);

	}

}
