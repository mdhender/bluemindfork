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
package net.bluemind.pop3.endpoint.tests;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.core.container.model.ItemValue;

public class Pop3BigDeletionTests extends Pop3TestsBase {

	public static final int COUNT = 100;

	@Test
	public void testDeletionOfSeveralMails() throws Exception {
		ItemValue<MailboxFolder> inbox = provider().instance(IMailboxFolders.class, partition.name, mboxRoot)
				.byName("INBOX");
		Assert.assertNotNull(inbox);

		List<ItemValue<MailboxItem>> createdEmails = IntStream.range(0, COUNT).boxed().map(i -> {
			try {
				return createEmail(user1Login, "INBOX", testEml01());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());
		Thread.sleep(4000);

		try (Socket sock = new Socket()) {
			ConcurrentLinkedDeque<String> queue = rawSocket(sock);

			OutputStream out = sock.getOutputStream();
			out.write(("USER " + user1Login + "@" + domainUid + "\r\n").getBytes());
			out.write(("PASS " + user1Login + "\r\n").getBytes());
			out.flush();

			Awaitility.await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (testConditionForQueue(queue, 1, "^\\+OK POP3 ready$")));

			Awaitility.await().atMost(4, TimeUnit.SECONDS).until(() -> (testConditionForQueue(queue, 2, "^\\+OK$")));

			System.err.println("Yeah yeah pre del");
			queue.clear();
			for (int i = 1; i < createdEmails.size() + 1; i++) {
				try {
					final int cnt = i;
					System.err.println("del " + cnt);
					out.write(("DELE " + cnt + "\r\n").getBytes());
					out.flush();
					Awaitility.await().atMost(4, TimeUnit.SECONDS)
							.until(() -> (testConditionForQueue(queue, 1, "^\\+OK message " + cnt + " deleted$")));
					queue.clear();
				} catch (IOException e) {
					fail(e.getMessage());
				}
			}

			System.err.println("Send pop quit.");
			out.write(("QUIT\r\n").getBytes());
			out.flush();
			Awaitility.await().atMost(1, TimeUnit.MINUTES).until(() -> (testConditionForQueue(queue, 1, "^\\+OK$")));

			System.err.println("Yeah yeah post del");
		}

		try (Socket sock = new Socket()) {
			ConcurrentLinkedDeque<String> queue = rawSocket(sock);

			OutputStream out = sock.getOutputStream();
			Thread.sleep(500);

			out.write(("USER " + user1Login + "@" + domainUid + "\r\n").getBytes());
			out.write(("PASS " + user1Login + "\r\n").getBytes());
			out.flush();
			Thread.sleep(1_000);

			System.err.println("Calling stat...");
			out.write(("STAT\r\n").getBytes());
			out.flush();
			Awaitility.await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (testConditionForQueue(queue, 1, "^\\+OK 0 0$")));
		}
	}

	private Boolean testConditionForQueue(ConcurrentLinkedDeque<String> q, Integer condition, String regex) {
		Stream<String> list = Arrays.asList(q.stream().reduce((e, s) -> e + s).orElse("").split("\r\n")).stream()
				.filter(s -> s.matches(regex));
		return list.count() == condition;
	}

}
