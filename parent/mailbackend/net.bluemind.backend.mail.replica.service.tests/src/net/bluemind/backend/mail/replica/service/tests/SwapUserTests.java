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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.cyrus.replication.client.SyncClient;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.service.tests.ReplicationEventsRecorder.Hierarchy;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class SwapUserTests extends AbstractRollingReplicationTests {

	private String apiKey;
	private ItemValue<Mailbox> replMbox;

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

		// populate another user to do the mailbox swap
		String willReplace = PopulateHelper.addUser(userUid + ".replacement", domainUid);
		SecurityContext replSc = new SecurityContext("repsid", willReplace, Collections.emptyList(),
				Collections.emptyList(), domainUid);
		Sessions.get().put(replSc.getSessionId(), replSc);
		ServerSideServiceProvider replProv = ServerSideServiceProvider.getProvider(replSc);
		this.replMbox = suProvider().instance(IMailboxes.class, domainUid).getComplete(willReplace);
		assertNotNull(replMbox);
		String subtree = IMailReplicaUids.subtreeUid(domainUid, replMbox);
		System.err.println("Fresh subtree is " + subtree);
		IDbReplicatedMailboxes replFolders = replProv.instance(IDbByContainerReplicatedMailboxes.class, subtree);
		ItemValue<MailboxFolder> replInbox = replFolders.byName("INBOX");
		long wait = System.currentTimeMillis();
		while (replInbox == null && (System.currentTimeMillis() - wait) < 30000) {
			Thread.sleep(50);
			replInbox = replFolders.byName("INBOX");
		}
		assertNotNull("inbox for second user not replicated in time", replInbox);
		System.err.println("Second user populated in " + (System.currentTimeMillis() - wait) + "ms.");

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
	public void deleteUserThenRenameMailbox() throws Exception {
		IUser userApi = suProvider().instance(IUser.class, domainUid);
		ItemValue<User> theUser = userApi.getComplete(userUid);
		assertNotNull(theUser);

		System.err.println("Start delete of " + userUid);
		TaskRef tr = userApi.delete(userUid);
		TaskStatus status = TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), tr);
		assertEquals(TaskStatus.State.Success, status.state);

		Thread.sleep(400);

		System.err.println("Connecting with syncClient");
		SyncClient sc = new SyncClient("127.0.0.1", 2501);
		sc.connect().thenCompose(c -> {
			return sc.getUser(userUid + "@" + domainUid);
		}).thenCompose(userResp -> {
			assertTrue("Cyrus returns 'OK success' for unknown or deleted mailboxes but we did not: " + userResp,
					userResp.isOk());
			return sc.disconnect();
		}).get(5, TimeUnit.SECONDS);

		ItemValue<User> toRename = userApi.getComplete(replMbox.uid);
		toRename.value.login = theUser.value.login;
		toRename.value.emails.add(Email.create(theUser.value.login + "@" + domainUid, false));
		System.err.println("Start rename process....");
		userApi.update(toRename.uid, toRename.value);
		System.err.println("Sleeping 3sec after update returned.");
		Thread.sleep(3000);

		System.err.println("Second check with syncClient");
		SyncClient sc2 = new SyncClient("127.0.0.1", 2501);
		sc2.connect().thenCompose(c -> {
			return sc2.getUser(userUid + "@" + domainUid);
		}).thenCompose(userResp -> {
			assertTrue("'OK success' expected after rename but we did not: " + userResp, userResp.isOk());
			System.err.println(userResp);
			assertFalse("GET USER must not return an empty response after the rename", userResp.dataLines.isEmpty());
			return sc2.disconnect();
		}).get(5, TimeUnit.SECONDS);

	}

}
