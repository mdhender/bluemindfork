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
package net.bluemind.imap.cyrus.swap.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.elasticsearch.common.Strings;
import org.junit.Test;

import com.github.javafaker.Faker;
import com.github.javafaker.Lorem;
import com.google.common.base.Stopwatch;
import com.google.common.io.ByteStreams;

import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPByteSource;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.StoreClient;
import net.bluemind.network.utils.NetworkHelper;

public class WriteToCyrusReadWithEndpointTests extends AbstractRollingReplicationTests {

	private Lorem lorem;

	@Override
	public void before() throws Exception {
		try {
			super.before();
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
		new NetworkHelper("127.0.0.1").waitForListeningPort(1144, 10, TimeUnit.SECONDS);
		this.lorem = Faker.instance().lorem();
	}

	private InputStream eml(int id) {
		StringBuilder sb = new StringBuilder();
		sb.append("From: john.grubber@die-hard.net\r\n");
		sb.append("To: simon-petter.gruber@die-hard.net\r\n");
		sb.append("Subject: McLane has " + id + " machine gun(s)\r\n\r\n");
		sb.append("Message-ID: <john." + Strings.padStart(id + "", 8, '0') + "@junit." + domainUid + ">\r\n");
		sb.append("Oh oh oh " + lorem.sentence(128, 64 + id % 32) + " !\r\n");
		return new ByteArrayInputStream(sb.toString().getBytes());
	}

	@Test
	public void testPopulateThenReadThroughEndpoint() throws IMAPException, IOException {
		int cnt = 50;
		System.err.println("Fill mailbox with imap through cyrus...");
		doAsCyrusUser(sc -> {
			FlagsList fl = new FlagsList();
			fl.add(Flag.SEEN);
			for (int i = 0; i < cnt; i++) {
				int added = sc.append("INBOX", eml(i), fl);
				assertTrue(added > 0);
			}
		});

		IDbMailboxRecords inboxRecApi = provider().instance(IDbMailboxRecords.class, userInbox.uid);

		System.err.println("waiting for records...");
		Stopwatch chrono = Stopwatch.createStarted();
		Awaitility.await().atMost(10, TimeUnit.SECONDS)
				.until(() -> inboxRecApi.count(ItemFlagFilter.all()).total >= cnt);
		System.err.println("Replicated " + cnt + " in " + chrono.elapsed(TimeUnit.MILLISECONDS) + "ms.");

		chrono = Stopwatch.createStarted();
		long fullSize = 0;
		try (StoreClient sc = new StoreClient("127.0.0.1", 1144, userUid + "@" + domainUid, apiKey)) {
			assertTrue(sc.login());
			System.err.println("Logged to our endpoint !");
			sc.select("INBOX");
			Collection<Integer> existing = sc.uidSearch(new SearchQuery());
			assertEquals(cnt, existing.size());
			for (int uid : existing) {
				try (IMAPByteSource ibs = sc.uidFetchMessage(uid)) {
					long total = ByteStreams.copy(ibs.source().openBufferedStream(), ByteStreams.nullOutputStream());
					System.err.println("uid " + uid + ": " + total + " byte(s)");
					fullSize += total;
				}
			}
		}
		System.err.println(
				"refetched " + fullSize + " byte(s) from our endpoint in " + chrono.elapsed(TimeUnit.MILLISECONDS));
	}

}
