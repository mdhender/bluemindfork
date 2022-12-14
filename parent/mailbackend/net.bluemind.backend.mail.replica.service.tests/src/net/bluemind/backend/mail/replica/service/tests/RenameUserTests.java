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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import net.bluemind.backend.mail.api.IOutbox;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.imap.FlagsList;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class RenameUserTests extends AbstractRollingReplicationTests {

	private IServiceProvider suProvider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	@Test
	public void renameUserAndCheckSubtree() throws Exception {
		IUser userApi = suProvider().instance(IUser.class, domainUid);
		ItemValue<User> theUser = userApi.getComplete(userUid);
		assertNotNull(theUser);
		theUser.value.login = "renamed." + theUser.value.login;
		System.err.println("update starts...");
		userApi.update(theUser.uid, theUser.value);
		IMailboxes mboxApi = suProvider().instance(IMailboxes.class, domainUid);
		ItemValue<Mailbox> mbox = mboxApi.getComplete(theUser.uid);
		String subtree = IMailReplicaUids.subtreeUid(domainUid, mbox);
		System.err.println("subtree: " + subtree);
		IContainers contApi = suProvider().instance(IContainers.class);
		ContainerDescriptor fetched = contApi.getIfPresent(subtree);
		System.err.println("fetched " + fetched.name);
		assertTrue("subtree name should include 'renamed.' but got " + fetched.name, fetched.name.contains("renamed."));

		String eml = "From: john.doe@gmail.com\r\nTo: " + theUser.value.defaultEmailAddress()
				+ "\r\nX-Bm-Draft-Refresh-Date: 1632837985361\r\n\r\nYeah\r\n";

		imapAsUser(sc -> {
			int added = sc.append("Outbox", new ByteArrayInputStream(eml.getBytes(StandardCharsets.US_ASCII)),
					new FlagsList());
			assertTrue(added > 0);
			return added;
		});

		IDbByContainerReplicatedMailboxes foldersApi = provider().instance(IDbByContainerReplicatedMailboxes.class,
				subtree);
		ItemValue<MailboxFolder> outbox = foldersApi.byName("Outbox");
		assertNotNull(outbox);
		IDbMailboxRecords itemsApi = provider().instance(IDbMailboxRecords.class, outbox.uid);
		List<ItemValue<MailboxRecord>> outboxItems = itemsApi.all();
		int retry = 0;
		while (outboxItems.isEmpty() && retry++ < 100) {
			Thread.sleep(10);
			outboxItems = itemsApi.all();
		}
		assertFalse(outboxItems.isEmpty());

		IOutbox outboxApi = provider().instance(IOutbox.class, domainUid, mbox.uid);
		TaskRef taskRef = outboxApi.flush();
		String result = TaskUtils.logStreamWait(provider(), taskRef);
		assertTrue(result.contains("1 mails to send"));
		assertTrue(result.contains("OUTBOX finished successfully"));
	}

}
