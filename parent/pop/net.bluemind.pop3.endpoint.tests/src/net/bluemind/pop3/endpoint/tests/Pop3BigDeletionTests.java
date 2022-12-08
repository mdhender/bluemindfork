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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.pop3.endpoint.Pop3Config;

public class Pop3BigDeletionTests extends Pop3TestsBase {

	private static final Logger logger = LoggerFactory.getLogger(Pop3BigDeletionTests.class);

	@Test
	public void testDeletionOfSeveralMails() throws Exception {
		ItemValue<MailboxFolder> inbox = provider().instance(IMailboxFolders.class, partition.name, mboxRoot)
				.byName("INBOX");
		Assert.assertNotNull(inbox);

		List<ItemValue<MailboxItem>> createdEmails = IntStream.range(0, 1_000).boxed().map(i -> {
			try {
				return createEmail(user1Login, "INBOX", testEml01());
			} catch (IOException e) {
				e.printStackTrace();
				return null;
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

			IntStream.range(1, createdEmails.size() + 1).boxed().forEach(i -> {
				try {
					out.write(("DELE " + i + "\r\n").getBytes());
					out.flush();
					Thread.sleep(20);
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			});

			out.write(("QUIT\r\n").getBytes());
			out.flush();
		}

		try (Socket sock = new Socket()) {
			ConcurrentLinkedDeque<String> queue = rawSocket(sock);

			OutputStream out = sock.getOutputStream();
			Thread.sleep(500);

			out.write(("USER " + user1Login + "@" + domainUid + "\r\n").getBytes());
			out.write(("PASS " + user1Login + "\r\n").getBytes());
			out.flush();
			Thread.sleep(1_000);

			out.write(("STAT\r\n").getBytes());
			out.flush();
			Awaitility.await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (testConditionForQueue(queue, 1, "^\\+OK 0 0$")));
		}
	}

	private Boolean testConditionForQueue(ConcurrentLinkedDeque<String> q, Integer condition, String regex) {
		Stream<String> list = Arrays.asList(q.stream().reduce((e, s) -> e + s).get().split("\r\n")).stream()
				.filter(s -> s.matches(regex));
		return list.count() == condition;
	}

	private ConcurrentLinkedDeque<String> rawSocket(Socket sock) throws IOException {

		Config conf = Pop3Config.get();
		int port = conf.getInt("pop3.port");
		sock.connect(new InetSocketAddress("127.0.0.1", port));
		ConcurrentLinkedDeque<String> queue = new ConcurrentLinkedDeque<>();
		Thread t = new Thread(() -> {
			try {
				InputStream in = sock.getInputStream();
				byte[] buf = new byte[1024];
				while (true) {
					int read = in.read(buf, 0, 1024);
					if (read == -1) {
						break;
					}
					String resp = new String(buf, 0, read);
					logger.debug("QUEUE OFFER: " + resp);
					queue.offer(resp);
				}
			} catch (Exception e) {
			}
		});
		t.start();
		return queue;
	}
}
