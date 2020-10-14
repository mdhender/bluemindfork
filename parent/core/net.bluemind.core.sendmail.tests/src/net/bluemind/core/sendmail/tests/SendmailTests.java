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
package net.bluemind.core.sendmail.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.subethamail.smtp.auth.LoginFailedException;
import org.subethamail.smtp.auth.PlainAuthenticationHandlerFactory;
import org.subethamail.smtp.auth.UsernamePasswordValidator;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import com.google.common.io.ByteStreams;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailCredentials;
import net.bluemind.core.sendmail.SendmailResponse;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;

public class SendmailTests {

	private SMTPServer server;
	private TestMessageListener testListener;

	@Before
	public void before() {
		this.testListener = new TestMessageListener();
		this.server = new SMTPServer(new SimpleMessageListenerAdapter(testListener));
		System.setProperty(Sendmail.SMTP_SUBMIT_PORT_PROP, "1587");
		System.setProperty(Sendmail.SMTP_STARTTLS_PROP, "false");

		server.setPort(1587);
		UsernamePasswordValidator validator = (s, s1) -> {
			if (!"username".equalsIgnoreCase(s) || !"password".equalsIgnoreCase(s1)) {
				throw new LoginFailedException();
			}
		};
		server.setDisableReceivedHeaders(true);
		server.setAuthenticationHandlerFactory(new PlainAuthenticationHandlerFactory(validator));
		server.start();

		Server smtp = new Server();
		smtp.tags = Arrays.asList("mail/smtp");
		smtp.ip = "127.0.0.1";
		Topology.update(Arrays.asList(ItemValue.create("bm-master", smtp)));
	}

	@After
	public void after() {
		server.stop();
	}

	@Test
	public void send7MB() throws IOException {
		Sendmail s = new Sendmail();
		try (InputStream eml = SendmailTests.class.getClassLoader().getResourceAsStream("data/mail_de_7Mo.eml")) {
			assertNotNull(eml);
			SendmailCredentials creds = SendmailCredentials.as("username", "password");
			MailboxList recips = new MailboxList(Arrays.asList(new Mailbox("recipient", "bluemind.net")), true);
			long time = System.currentTimeMillis();
			long before = testListener.total();
			SendmailResponse resp = s.send(creds, "username@bluemind.net", "bluemind.net", recips, eml);
			assertEquals(250, resp.code());
			long after = testListener.total();
			time = System.currentTimeMillis() - time;
			System.err.println("Sent " + (after - before) + " in " + time + "ms.");
			assertEquals(9564250, after - before);
		}
	}

	@Test
	public void massSend7MB() throws IOException {
		Sendmail s = new Sendmail();
		ByteBuf target = Unpooled.buffer();
		try (InputStream eml = SendmailTests.class.getClassLoader().getResourceAsStream("data/mail_de_7Mo.eml")) {
			assertNotNull(eml);
			ByteStreams.copy(eml, new ByteBufOutputStream(target));
		}
		int cnt = 100;
		MailboxList recips = new MailboxList(Arrays.asList(new Mailbox("recipient", "bluemind.net")), true);
		long totalTime = 0;
		long totalSize = testListener.total();
		for (int i = 0; i < cnt; i++) {
			try (InputStream eml = new ByteBufInputStream(target.duplicate())) {
				SendmailCredentials creds = SendmailCredentials.as("username", "password");
				long time = System.currentTimeMillis();
				SendmailResponse resp = s.send(creds, "username@bluemind.net", "bluemind.net", recips, eml);
				assertEquals(250, resp.code());
				time = System.currentTimeMillis() - time;
				totalTime += time;
				System.err.println((i + 1) + "/" + cnt + " Sent in " + time + "ms.");
			}
		}
		long afterSize = testListener.total();
		assertEquals(9564250, (afterSize - totalSize) / cnt);
		long average = totalTime / cnt;
		System.err.println("Average Send time is " + average + "ms.");
	}

}
