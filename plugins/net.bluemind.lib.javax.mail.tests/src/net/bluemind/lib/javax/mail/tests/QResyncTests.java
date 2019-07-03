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

package net.bluemind.lib.javax.mail.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.event.MailEvent;
import javax.mail.event.MessageChangedEvent;
import javax.mail.event.MessageChangedListener;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.ResyncData;

import net.bluemind.pool.impl.BmConfIni;

public class QResyncTests {

	private static class TestListener implements MessageCountListener, MessageChangedListener {

		@Override
		public void messagesAdded(MessageCountEvent e) {
			System.out.println("Added " + e.getMessages().length + " message(s)");

		}

		@Override
		public void messagesRemoved(MessageCountEvent e) {
			System.out.println("Removed " + +e.getMessages().length + " message(s)");
		}

		@Override
		public void messageChanged(MessageChangedEvent e) {
			StringBuilder sb = new StringBuilder();
			IMAPMessage msg = (IMAPMessage) e.getMessage();
			sb.append("type: ")
					.append(e.getMessageChangeType() == MessageChangedEvent.ENVELOPE_CHANGED ? "enveloppe" : "flags");
			try {
				Flags flags = msg.getFlags();
				sb.append(", flags: (seen: " + flags.contains(Flag.SEEN) + ")");

				long ms = msg.getModSeq();
				sb.append(", modseq: " + ms);
			} catch (MessagingException e1) {
				e1.printStackTrace();
			}
			System.out.println("Changed " + sb.toString());
		}
	}

	@Test
	public void testQresyncInbox() throws MessagingException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props);
		assertNotNull(session);
		IMAPStore store = new IMAPStore(session, null);
		store.connect(new BmConfIni().get("bluemind/imap-role"), 1143, "test", "test");
		IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
		assertNotNull("Failed to get inbox", inbox);
		long validity = inbox.getUIDValidity();
		System.out.println("INBOX uid validity is '" + validity + "'");

		// Create a default MimeMessage object.
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress("test@bm.lan"));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress("test@bm.lan"));
		message.setSubject("This is the Subject Line!");
		message.setText("This is actual message");
		AppendUID[] appendResult = inbox.appendUIDMessages(new Message[] { message });
		assertTrue(appendResult.length == 1);
		AppendUID theUid = appendResult[0];
		System.out.println("the uid: " + theUid.uid);

		ResyncData rd = new ResyncData(validity, 1L);
		long time = System.currentTimeMillis();
		List<MailEvent> events = inbox.open(Folder.READ_ONLY, rd);
		time = System.currentTimeMillis() - time;
		System.out.println("Received " + events.size() + " events in " + time + "ms.");
		Iterator<MailEvent> it = events.iterator();
		TestListener listener = new TestListener();
		Set<Class<?>> seenKlass = new HashSet<>();
		for (int i = 0; i < events.size(); i++) {
			MailEvent ev = it.next();
			Class<?> evKlass = ev.getClass();
			if (!seenKlass.contains(evKlass)) {
				ev.dispatch(listener);
				seenKlass.add(evKlass);
			}
		}
	}

}
