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

import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.service.tests.ReplicationEventsRecorder.Hierarchy;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;

public class RecordUpdatesTests extends AbstractRollingReplicationTests {

	private ItemValue<MailboxFolder> inbox;

	public static final int CNT = 500;

	public static final ItemFlagFilter unseenFilter = ItemFlagFilter.create().mustNot(ItemFlag.Deleted)
			.mustNot(ItemFlag.Seen);
	public static final ItemFlagFilter seenFilter = ItemFlagFilter.create().mustNot(ItemFlag.Deleted)
			.must(ItemFlag.Seen);

	private InputStream eml() {
		return EmlTemplates.withRandomMessageId("small_eml.ftl");
	}

	@Before
	public void before() throws Exception {
		super.before();

		Hierarchy hier = rec.hierarchy(domainUid, userUid);
		assertNotNull(hier);
		int attempts = 0;
		IMailboxFolders fapi = foldersApi();
		while (fapi.byName("INBOX") == null && attempts++ < 1000) {
			Thread.sleep(100);
		}

		this.inbox = fapi.byName("INBOX");

		FlagsList fl = new FlagsList();
		fl.add(Flag.SEEN);
		imapAsUser(sc -> {
			for (int i = 0; i < CNT; i++) {
				int added = sc.append("INBOX", eml(), fl);
				assertTrue(added > 0);
				if (added % 100 == 0) {
					System.err.println("Mail with uid " + added + " added.");
				}
			}
			return null;
		});

		int count = 0;
		while (count < CNT) {
			Thread.sleep(20);
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
	public void markAsReadThenUnread() throws IMAPException, InterruptedException {
		assertNotNull(inbox);
		IMailboxItems items = itemsApi();
		assertTrue(items.getVersion() > 0);

		updateItems(false, unseenFilter);
		Thread.sleep(500);
		updateItems(true, seenFilter);

		int steps = 50;
		long time = System.currentTimeMillis();
		for (int i = 0; i < steps; i++) {
			System.err.println("Starting step " + (i + 1) + " / " + steps);
			updateItems(false, unseenFilter);
			updateItems(true, seenFilter);
		}
		time = System.currentTimeMillis() - time;
		System.err.println("Did " + steps + " steps in " + time + "ms.");

	}

	private void updateItems(boolean setSeen, ItemFlagFilter expectedMatch) throws InterruptedException {
		FlagsList fl = new FlagsList();
		fl.add(Flag.SEEN);
		boolean result = imapAsUser(sc -> {
			sc.select("INBOX");
			return sc.uidStore("1:*", fl, setSeen);
		});
		assertTrue(result);
		int unseen = itemsApi().count(expectedMatch).total;
		System.err.println("match: " + unseen);
		int attempt = 0;
		while (unseen < CNT) {
			Thread.sleep(20L * ++attempt);
			unseen = itemsApi().count(expectedMatch).total;
			System.err.println("match: " + unseen);
		}
	}

}
