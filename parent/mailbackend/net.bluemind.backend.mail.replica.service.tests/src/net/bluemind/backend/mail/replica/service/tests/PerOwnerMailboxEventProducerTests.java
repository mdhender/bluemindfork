/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;

public class PerOwnerMailboxEventProducerTests extends AbstractRollingReplicationTests {

	private ItemValue<MailboxFolder> inbox;

	@BeforeEach
	@Override
	public void before(TestInfo testInfo) throws Exception {
		super.before(testInfo);

		addMessageInUserInbox();
		inbox = waitForInbox();
		assertNotNull(inbox);
		waitForMessageInInbox(inbox);
	}

	private ItemValue<MailboxFolder> waitForInbox() throws Exception {
		IMailboxFolders userMboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		List<ItemValue<MailboxFolder>> mailboxFolders = retry(() -> userMboxesApi.all(), List::isEmpty, 30000);
		return mailboxFolders.stream().filter(itemValue -> itemValue.value.name.equals("INBOX")).findFirst()
				.orElse(null);
	}

	private void waitForMessageInInbox(ItemValue<MailboxFolder> inbox) throws Exception {
		IMailboxItems recordsApi = provider().instance(IMailboxItems.class, inbox.uid);
		retry(() -> recordsApi.changesetById(0L), result -> result.created.isEmpty(), 30000);
	}

	private <T> T retry(Supplier<T> operation, Predicate<T> retry, long duration) throws Exception {
		T result = operation.get();
		long startingAt = System.currentTimeMillis();
		while (retry.test(result)) {
			Thread.sleep(25);
			if (System.currentTimeMillis() - startingAt > duration) {
				throw new TimeoutException("Wait for record took more than 30sec");
			}
			result = operation.get();
		}
		return result;
	}

	@Test
	public void expectEventOnCreate() throws InterruptedException {
		CountDownLatch latch = expectOwnerEvent();
		addMessageInUserInbox();
		assertTrue(latch.await(10, TimeUnit.SECONDS), "Expected 1 specific update to occur on owner bus");
	}

	@Test
	public void expectEventOnUpdate() throws InterruptedException {
		CountDownLatch latch = expectOwnerEvent();
		flagMessageInUserInbox(Flag.SEEN);
		assertTrue(latch.await(10, TimeUnit.SECONDS), "Expected 1 specific update to occur on owner bus");
	}

	@Test
	public void expectEventOnDelete() throws InterruptedException {
		CountDownLatch latch = expectOwnerEvent();
		flagMessageInUserInbox(Flag.DELETED);
		assertTrue(latch.await(10, TimeUnit.SECONDS), "Expected 1 specific update to occur on owner bus");
	}

	private void addMessageInUserInbox() {
		imapAsUser(sc -> sc.append("INBOX", testEml(), new FlagsList()));
	}

	private void flagMessageInUserInbox(Flag... flags) {
		FlagsList fl = new FlagsList();
		fl.addAll(Arrays.asList(flags));
		imapAsUser(sc -> {
			sc.select("INBOX");
			return sc.uidStore("1:*", fl, true);
		});
	}

	private CountDownLatch expectOwnerEvent() {
		return expectMessages("mailreplica." + userUid + ".updated", 1,
				msg -> msg.getString("owner").equals(userUid) && msg.getString("mailbox").equals(inbox.uid)
						&& msg.getString("container").equals("mbox_records_" + inbox.uid)
						&& Objects.nonNull(msg.getLong("version")));
	}

}
