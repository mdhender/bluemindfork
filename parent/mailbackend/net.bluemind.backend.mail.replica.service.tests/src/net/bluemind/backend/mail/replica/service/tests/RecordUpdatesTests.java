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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;

public class RecordUpdatesTests extends AbstractRollingReplicationTests {

	private ItemValue<MailboxFolder> inbox;

	public static final int MESSAGES = 300;
	public static final int MARK_STEPS = 5;

	public static final ItemFlagFilter unseenFilter = ItemFlagFilter.create().mustNot(ItemFlag.Deleted)
			.mustNot(ItemFlag.Seen);
	public static final ItemFlagFilter seenFilter = ItemFlagFilter.create().mustNot(ItemFlag.Deleted)
			.must(ItemFlag.Seen);

	private InputStream eml() {
		return EmlTemplates.withRandomMessageId("small_eml.ftl");
	}

	@Before
	@Override
	public void before() throws Exception {
		super.before();

		IMailboxFolders fapi = foldersApi();

		this.inbox = fapi.byName("INBOX");
		assertNotNull(inbox);

		FlagsList fl = new FlagsList();
		fl.add(Flag.SEEN);
		imapAsUser(sc -> {
			for (int i = 0; i < MESSAGES; i++) {
				int added = sc.append("INBOX", eml(), fl);
				assertTrue(added > 0);
				if (added % 100 == 0) {
					System.err.println("Mail with uid " + added + " added.");
				}
			}
			return null;
		});

		long count = 0;
		while (count < MESSAGES) {
			count = itemsApi().count(ItemFlagFilter.create().mustNot(ItemFlag.Deleted)).total;
			System.err.println("Count is at " + count);
		}

	}

	private IMailboxFolders foldersApi() {
		return provider().instance(IMailboxFolders.class, partition, mboxRoot);
	}

	private IMailboxItems itemsApi() {
		return provider().instance(IMailboxItems.class, inbox.uid);
	}

	@Test
	public void markAsReadThenUnread() throws InterruptedException {
		assertNotNull(inbox);
		IMailboxItems items = itemsApi();
		assertTrue(items.getVersion() > 0);

		updateItems(false, unseenFilter);
		Thread.sleep(500);
		updateItems(true, seenFilter);

		long time = System.currentTimeMillis();
		for (int i = 0; i < MARK_STEPS; i++) {
			System.err.println("Starting step " + (i + 1) + " / " + MARK_STEPS);
			updateItems(false, unseenFilter);
			updateItems(true, seenFilter);
		}
		time = System.currentTimeMillis() - time;
		System.err.println("Did " + MARK_STEPS + " steps in " + time + "ms.");

	}

	@Test
	public void markAsReadThenUnreadWithRandomRewrites() throws InterruptedException {
		assertNotNull(inbox);
		IMailboxItems items = itemsApi();
		assertTrue(items.getVersion() > 0);

		updateItems(false, unseenFilter);
		Thread.sleep(500);
		updateItems(true, seenFilter);

		long time = System.currentTimeMillis();
		for (int i = 0; i < MARK_STEPS; i++) {
			System.err.println("Starting step " + (i + 1) + " / " + MARK_STEPS);
			updateItems(false, unseenFilter);
			performRewrites();
			updateItems(true, seenFilter);
		}
		time = System.currentTimeMillis() - time;
		System.err.println("Did " + MARK_STEPS + " steps in " + time + "ms.");

	}

	private void performRewrites() {
		IMailboxItems api = itemsApi();
		long vers = api.getVersion();
		ListResult<Long> ids = itemsApi().allIds("-deleted", vers, MESSAGES, 0);
		List<Long> toList = ids.values;
		Collections.shuffle(toList, ThreadLocalRandom.current());
		List<Long> sublist = toList.subList(0, Math.min(5, toList.size()));
		System.err.println("Will rewrite " + sublist);

		String updPrefix = "[U " + System.nanoTime() + "] ";
		for (long toRewrite : sublist) {
			ItemValue<MailboxItem> orig = api.getCompleteById(toRewrite);
			System.err.println("Rewrite " + orig + "...");
			long time = System.currentTimeMillis();
			orig.value.body.subject = updPrefix + orig.value.body.subject;
			api.updateById(toRewrite, orig.value);
			time = System.currentTimeMillis() - time;
			System.err.println("Rewritten in " + time + "ms.");
		}
	}

	private void updateItems(boolean setSeen, ItemFlagFilter expectedMatch) throws InterruptedException {
		FlagsList fl = new FlagsList();
		fl.add(Flag.SEEN);
		boolean result = imapAsUser(sc -> {
			sc.select("INBOX");
			return sc.uidStore("1:*", fl, setSeen);
		});
		assertTrue(result);

		Awaitility.await().atMost(MESSAGES / 10, TimeUnit.SECONDS)
				.until(() -> itemsApi().count(expectedMatch).total >= MESSAGES);

		System.out.println(MESSAGES + " Marks replicated");
	}

}
