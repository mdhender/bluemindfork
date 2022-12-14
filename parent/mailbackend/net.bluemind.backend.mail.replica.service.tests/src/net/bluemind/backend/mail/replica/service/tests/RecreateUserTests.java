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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class RecreateUserTests extends AbstractRollingReplicationTests {

	@Before
	@Override
	public void before() throws Exception {
		super.before();
	}

	private IServiceProvider suProvider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	@Test
	public void deleteThenRecreateUser() throws Exception {
		IUser userApi = suProvider().instance(IUser.class, domainUid);
		ItemValue<User> theUser = userApi.getComplete(userUid);
		assertNotNull(theUser);

		IMailboxFoldersByContainer foldersApi = userProvider.instance(IMailboxFoldersByContainer.class,
				IMailReplicaUids.subtreeUid(domUid, Mailbox.Type.user, userUid));
		Set<Long> currentFolders = foldersApi.all().stream().map(iv -> iv.internalId).collect(Collectors.toSet());

		long brokenCircuits = CircuitBreaksCounter.breaks();

		TaskRef tr = userApi.delete(userUid);
		TaskStatus status = TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), tr);
		assertEquals(TaskStatus.State.Success, status.state);

		System.err.println("Start RE-populate user " + userUid);
		PopulateHelper.addUser(userUid, domainUid, Routing.internal);
		ItemValue<User> newUser = userApi.getComplete(userUid);
		assertNotNull(newUser);
		assertNotEquals(theUser.internalId, newUser.internalId);

		Thread.sleep(2000);

		foldersApi = userProvider.instance(IMailboxFoldersByContainer.class,
				IMailReplicaUids.subtreeUid(domUid, Mailbox.Type.user, userUid));
		Set<Long> freshFolders = foldersApi.all().stream().map(iv -> iv.internalId).collect(Collectors.toSet());

		// +1 for the root (aka INBOX)
		int expected = DefaultFolder.USER_FOLDERS.size() + 1;
		assertEquals("we should have at least " + expected, expected, freshFolders.size());

		boolean intersect = currentFolders.stream().anyMatch(freshFolders::contains);
		assertFalse("fresh folder internalId must all be different", intersect);

		long brokenInTest = CircuitBreaksCounter.breaks() - brokenCircuits;
		assertEquals("circuit breaker should not trigger", 0, brokenInTest);

	}

}
