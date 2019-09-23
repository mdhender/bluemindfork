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
package net.bluemind.core.sendmail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.LinkedList;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.columba.ristretto.message.Address;
import org.columba.ristretto.smtp.SMTPProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.locator.client.LocatorClient;

public class Sendmail implements ISendmail {

	private static final Logger logger = LoggerFactory.getLogger(Sendmail.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.sendmail.ISendmail#send(org.apache.james.mime4j.dom.
	 * Message)
	 */
	@Override
	public void send(String domainUid, Message m) throws ServerFault {
		send(m.getFrom().iterator().next().getAddress(), domainUid, allRecipients(m), m);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.sendmail.ISendmail#send(java.lang.String,
	 * java.lang.String, org.apache.james.mime4j.dom.Message)
	 */
	@Override
	public void send(String fromEmail, String userDomain, Message m) throws ServerFault {
		send(fromEmail, userDomain, allRecipients(m), m);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.sendmail.ISendmail#send(net.bluemind.core.sendmail.
	 * Mail)
	 */
	@Override
	public void send(Mail m) throws ServerFault {
		send(m.from, m.getMessage());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.sendmail.ISendmail#send(org.apache.james.mime4j.dom.
	 * address.Mailbox, org.apache.james.mime4j.dom.Message)
	 */
	@Override
	public void send(Mailbox from, Message m) throws ServerFault {
		send(from.getAddress(), from.getDomain(), allRecipients(m), m);
	}

	private MailboxList allRecipients(Message m) throws ServerFault {
		LinkedList<Mailbox> rcpt = new LinkedList<>();
		AddressList tos = m.getTo();
		if (tos != null) {
			rcpt.addAll(tos.flatten());
		}
		AddressList ccs = m.getCc();
		if (ccs != null) {
			rcpt.addAll(ccs.flatten());
		}
		AddressList bccs = m.getBcc();
		if (bccs != null) {
			rcpt.addAll(bccs.flatten());
		}
		if (rcpt.isEmpty()) {
			throw new ServerFault("Empty recipients list.");
		}
		return new MailboxList(rcpt, true);
	}

	/**
	 * Send an email. This API is usable from outside core JVM.
	 * 
	 * @param fromEmail
	 *                       Envelope from
	 * @param userDomain
	 *                       Used to locate a valid SMTP
	 * @param rcptTo
	 *                       the real recipients
	 * @param m
	 * @throws ServerFault
	 */
	@Override
	public void send(String fromEmail, String userDomain, MailboxList rcptTo, Message m) throws ServerFault {
		if (rcptTo == null) {
			throw new ServerFault("null To: field in message");
		}

		InputStream in = null;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			MessageWriter writer = MessageServiceFactory.newInstance().newMessageWriter();
			writer.writeMessage(m, out);
			byte[] bytes = out.toByteArray();
			in = new ByteArrayInputStream(bytes);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault(e.getMessage());
		}

		LocatorClient lc = new LocatorClient();
		String ip = lc.locateHost("mail/smtp", "fake@" + userDomain);
		try {
			SMTPProtocol smtp = new SMTPProtocol(ip);

			smtp.openPort();
			smtp.ehlo(InetAddress.getLocalHost());
			smtp.mail(new Address(fromEmail));

			for (Mailbox to : rcptTo) {
				smtp.rcpt(new Address(to.getAddress()));
			}

			smtp.data(in);
			smtp.quit();

		} catch (Exception se) {
			logger.error(se.getMessage(), se);
			throw new ServerFault(se.getMessage());
		}
	}
}
