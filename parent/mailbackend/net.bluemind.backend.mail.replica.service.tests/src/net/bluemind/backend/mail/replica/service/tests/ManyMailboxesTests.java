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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.replication.client.SyncClient;
import net.bluemind.backend.cyrus.replication.client.UnparsedResponse;
import net.bluemind.imap.IMAPException;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ManyMailboxesTests extends AbstractRollingReplicationTests {

	private List<String> mailboxes;
	private SyncClient syncClient;

	/**
	 * each user produces 6 folders
	 */
	public static final int TOTAL = 250;
	public static final int SAMPLE = 1000;

	@Before
	@Override
	public void before() throws Exception {
		super.before();
		int CNT = TOTAL;
		this.mailboxes = new ArrayList<>(5 * CNT);
		for (int i = 1; i <= CNT; i++) {
			String userN = "junit" + Strings.padStart(Integer.toString(i), 5, '0');
			PopulateHelper.addUser(userN, domainUid, Routing.internal);
			mailboxes.add(domainUid + "!user." + userN);
			mailboxes.add(domainUid + "!user." + userN + ".Sent");
			mailboxes.add(domainUid + "!user." + userN + ".Trash");
			mailboxes.add(domainUid + "!user." + userN + ".Drafts");
			mailboxes.add(domainUid + "!user." + userN + ".Outbox");
			mailboxes.add(domainUid + "!user." + userN + ".Junk");
			Thread.sleep(20);
			System.err.println("After " + userN);
		}
		System.err.println("Connecting in 2sec...");
		Thread.sleep(2000);
		this.syncClient = new SyncClient("127.0.0.1", 2501);
		syncClient.connect().get(10, TimeUnit.SECONDS);
	}

	@Override
	public void after() throws Exception {
		syncClient.disconnect().get(10, TimeUnit.SECONDS);
		super.after();
	}

	@Test
	public void testGet1000Mailboxes()
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException {
		for (List<String> slice : Lists.partition(mailboxes, 1000)) {
			String[] mboxes = slice.toArray(new String[0]);
			System.err.println("Starting on slice with " + mboxes.length + " item(s)");
			long time = System.currentTimeMillis();
			UnparsedResponse response = syncClient.getMailboxes(mboxes).get(10, TimeUnit.SECONDS);
			assertNotNull(response);
			time = System.currentTimeMillis() - time;
			System.err.println("Got response in " + time + "ms.");
		}
	}

}
