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
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.IMAPException;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ManyMailboxesTests extends AbstractRollingReplicationTests {

	private List<String> mailboxes;
	private SyncClient syncClient;

	/**
	 * each user produces 6 folders
	 */
	public static final int TOTAL = 175;

	@Before
	@Override
	public void before() throws Exception {
		super.before();
		int CNT = TOTAL;
		this.mailboxes = new ArrayList<>(6 * CNT);
		for (int i = 1; i <= CNT; i++) {
			String uid = null;
			if (i % 20 == 0) {
				uid = "shared.junit" + Strings.padStart(Integer.toString(i), 5, '0');
				ServerSideServiceProvider apis = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
				IMailshare sharesApi = apis.instance(IMailshare.class, domainUid);
				Mailshare ms = new Mailshare();
				ms.routing = Routing.internal;
				ms.name = uid;
				sharesApi.create(ms.name, ms);
				String inName = ms.name.replace('.', '^');
				mailboxes.add(domainUid + "!" + inName);
				mailboxes.add(domainUid + "!" + inName + ".Sent");
			} else {
				uid = "junit" + Strings.padStart(Integer.toString(i), 5, '0');

				PopulateHelper.addUser(uid, domainUid, Routing.internal);

				mailboxes.add(domainUid + "!user." + uid);
				mailboxes.add(domainUid + "!user." + uid + ".Sent");
				mailboxes.add(domainUid + "!user." + uid + ".Trash");
				mailboxes.add(domainUid + "!user." + uid + ".Drafts");
				mailboxes.add(domainUid + "!user." + uid + ".Outbox");
				mailboxes.add(domainUid + "!user." + uid + ".Junk");
			}
			Thread.sleep(20);
			System.err.println("After " + uid);
		}
		System.err.println("Registered " + mailboxes.size() + " mailboxes.");
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
	public void testGetDottedMailshare()
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException {
		String[] mboxes = mailboxes.stream().filter(v -> v.contains("shared")).toArray(String[]::new);
		System.err.println("Starting on slice with " + mboxes.length + " item(s)");
		long time = System.currentTimeMillis();
		UnparsedResponse response = syncClient.getMailboxes(mboxes).get(30, TimeUnit.SECONDS);
		assertNotNull(response);
		time = System.currentTimeMillis() - time;
	}

	@Test
	public void testGet1000Mailboxes()
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException {
		int loops = 10;
		for (int i = 0; i < loops; i++) {
			System.err.println("**** " + (i + 1) + " / " + loops + " ****");
			for (List<String> slice : Lists.partition(mailboxes, 1000)) {
				String[] mboxes = slice.toArray(new String[0]);
				System.err.println("Starting on slice with " + mboxes.length + " item(s)");
				long time = System.currentTimeMillis();
				UnparsedResponse response = syncClient.getMailboxes(mboxes).get(30, TimeUnit.SECONDS);
				assertNotNull(response);
				time = System.currentTimeMillis() - time;
				System.err.println((i + 1) + "/ " + loops + ": Response for " + slice.size() + " in " + time + "ms.");
			}
		}
	}

}
