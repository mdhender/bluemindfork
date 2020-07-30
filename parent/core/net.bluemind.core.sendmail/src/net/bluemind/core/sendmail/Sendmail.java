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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.smtp.SMTPMessage;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.network.topology.Topology;

public class Sendmail implements ISendmail {

	private static final Logger logger = LoggerFactory.getLogger(Sendmail.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.sendmail.ISendmail#send(java.lang.String,
	 * java.lang.String, java.lang.String, org.apache.james.mime4j.dom.Message)
	 */
	@Override
	public SendmailResponse send(SendmailCredentials creds, String domainUid, Message m) {
		return send(creds, m.getFrom().iterator().next().getAddress(), domainUid, allRecipients(m), m);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.sendmail.ISendmail#send(java.lang.String,
	 * java.lang.String, org.apache.james.mime4j.dom.Message)
	 */
	@Override
	public SendmailResponse send(SendmailCredentials creds, String fromEmail, String userDomain, Message m) {
		return send(creds, fromEmail, userDomain, allRecipients(m), m);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.sendmail.ISendmail#send(net.bluemind.core.sendmail.
	 * Mail)
	 */
	@Override
	public SendmailResponse send(Mail m) {
		return send(m.from, m.getMessage());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.sendmail.ISendmail#send(org.apache.james.mime4j.dom.
	 * address.Mailbox, org.apache.james.mime4j.dom.Message)
	 */
	@Override
	public SendmailResponse send(Mailbox from, Message m) {
		return send(SendmailCredentials.asAdmin0(), from.getAddress(), from.getDomain(), allRecipients(m), m);
	}

	private MailboxList allRecipients(Message m) {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.bluemind.core.sendmail.ISendmail#send(java.lang.String,java.lang.String,
	 * java.lang.String, java.lang.String,
	 * org.apache.james.mime4j.dom.address.MailboxList,
	 * org.apache.james.mime4j.dom.Message)
	 */
	@Override
	public SendmailResponse send(SendmailCredentials creds, String fromEmail, String userDomain, MailboxList rcptTo,
			Message m) {
		try (InputStream in = Mime4JHelper.asStream(m)) {
			return send(creds, fromEmail, userDomain, rcptTo, in);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			return SendmailResponse.fail(e.getMessage());
		}
	}

	@Override
	public SendmailResponse send(SendmailCredentials creds, String fromEmail, String userDomain, MailboxList rcptTo,
			InputStream inStream) {
		if (rcptTo == null) {
			throw new ServerFault("null To: field in message");
		}

		Properties prop = new Properties();
		String ip = Topology.get().any("mail/smtp").value.address();
		prop.put("mail.smtp.auth", true);
		prop.put("mail.smtp.starttls.enable", "true");
		prop.put("mail.smtp.host", ip);
		prop.put("mail.smtp.port", "587");
		prop.put("mail.smtp.ssl.trust", ip);

		try {
			Session session = Session.getInstance(prop, new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(creds.loginAtDomain, creds.authKey);
				}
			});
			long javaxMailReparse = System.currentTimeMillis();
			SMTPMessage msg = new SMTPMessage(session, new BufferedInputStream(inStream));
			javaxMailReparse = System.currentTimeMillis() - javaxMailReparse;
			long transSend = System.currentTimeMillis();
			List<Address> asList = new LinkedList<>();
			for (Mailbox to : rcptTo) {
				asList.add(new InternetAddress(to.getAddress(), false));
			}
			Address[] addresses = asList.toArray(new Address[0]);
			Transport.send(msg, addresses);
			transSend = System.currentTimeMillis() - transSend;
			SendmailResponse sendmailResponse = SendmailResponse.success();
			logger.info("JAVAX.MAIL Email sent {}, reparse {}ms, transport {}ms.",
					getLog(creds, fromEmail, rcptTo, sendmailResponse, Optional.empty()), javaxMailReparse, transSend);
			return sendmailResponse;

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return SendmailResponse.fail(e.getMessage());
		}

	}

	private String getLog(SendmailCredentials creds, String fromEmail, MailboxList rcptTo,
			SendmailResponse sendmailResponse, Optional<String> exceptionMessage) {
		return String.format("as: %s, from: %s, to %s, response: %s", creds.loginAtDomain, fromEmail,
				String.join(",", rcptTo.stream().map(rcpt -> rcpt.getAddress()).collect(Collectors.toList())),
				sendmailResponse != null ? sendmailResponse.toString() : exceptionMessage.orElse("Fail"));
	}
}
