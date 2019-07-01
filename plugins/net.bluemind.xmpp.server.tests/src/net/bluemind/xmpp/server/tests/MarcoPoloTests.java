/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.xmpp.server.tests;

import java.util.concurrent.atomic.AtomicLong;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

public class MarcoPoloTests extends ServerTest {

	private static final long MAX = 1000;

	public void testMarcoPolo() throws XMPPException {
		XMPPConnection con1 = xc(p("jid1") + "@" + p("domain"), p("pass1"));
		assertNotNull(con1);
		XMPPConnection con2 = xc(p("jid2") + "@" + p("domain"), p("pass2"));
		assertNotNull(con2);

		ChatManager cm2 = con2.getChatManager();
		final AtomicLong rec2 = new AtomicLong(0);
		final MessageListener ml2 = new MessageListener() {

			@Override
			public void processMessage(Chat chat, Message message) {
				long value = rec2.incrementAndGet();
				if (value <= MAX) {
					try {
						chat.sendMessage("POLO");
					} catch (XMPPException e) {
						fail(e.getMessage());
					}
				}
			}
		};

		ChatManagerListener cml2 = new ChatManagerListener() {

			@Override
			public void chatCreated(Chat chat, boolean createdLocally) {
				System.out.println("Chat created: " + chat);
				chat.addMessageListener(ml2);
			}
		};
		cm2.addChatListener(cml2);

		ChatManager cm1 = con1.getChatManager();
		final AtomicLong rec1 = new AtomicLong(0);
		MessageListener ml1 = new MessageListener() {

			@Override
			public void processMessage(Chat chat, Message message) {
				long value = rec1.incrementAndGet();
				if (value <= MAX) {
					try {
						chat.sendMessage("MARCO");
					} catch (XMPPException e) {
						fail(e.getMessage());
					}
				}
			}
		};
		Chat chat1 = cm1.createChat(p("jid2") + "@" + p("domain"), ml1);
		chat1.sendMessage("MARCO");

		while (rec2.get() < MAX) {
			System.out.println("PROGRESS: " + rec2.get() + "/" + MAX);
			sleep(500);
		}

		con1.disconnect();
		con2.disconnect();
	}
}
