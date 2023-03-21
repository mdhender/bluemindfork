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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.columba.ristretto.message.Address;
import org.columba.ristretto.smtp.SMTPException;
import org.columba.ristretto.smtp.SMTPProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.network.topology.Topology;

public class Sendmail implements ISendmail {

	private static final Logger logger = LoggerFactory.getLogger(Sendmail.class);
	public static final String SMTP_SUBMIT_PORT_PROP = "bm.smtp.submit.port";
	public static String SMTP_STARTTLS_PROP = "bm.smtp.submit.tls";

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
		return send(creds, fromEmail, userDomain, rcptTo, inStream, false);
	}

	@Override
	public SendmailResponse send(SendmailCredentials creds, String fromEmail, String userDomain, MailboxList rcptTo,
			InputStream inStream, boolean requestDSN) {
		if (rcptTo == null) {
			throw new ServerFault("null To: field in message");
		}
		SendmailResponse sendmailResponse = null;
		List<FailedRecipient> failedRecipients = new ArrayList<>();

		String ip = Topology.get().any("mail/smtp").value.address();
		try (SMTPProtocol smtp = new SMTPProtocol(ip,
				Integer.parseInt(System.getProperty(SMTP_SUBMIT_PORT_PROP, "587")))) {
			boolean useTls = Boolean.parseBoolean(System.getProperty(SMTP_STARTTLS_PROP, "true"));
			smtp.openPort();
			if (useTls) {
				smtp.startTLS();
			} else {
				logger.warn("TLS disabled by system property {}", SMTP_STARTTLS_PROP);
			}
			smtp.ehlo(InetAddress.getLocalHost());
			smtp.auth("PLAIN", creds.loginAtDomain, creds.authKey.toCharArray());
			smtp.mail(new Address(fromEmail));

			int requestedDSNs = 0;
			for (Mailbox to : rcptTo) {
				try {
					if (requestDSN) {
						smtp.rcptWithDeliveryReport(new Address(to.getAddress()));
						requestedDSNs++;
					} else {
						smtp.rcpt(new Address(to.getAddress()));
					}
				} catch (SMTPException e) {
					failedRecipients.add(new FailedRecipient(to.getAddress(), e.getMessage()));
				}
			}
			sendmailResponse = new SendmailResponse(smtp.data(inStream), failedRecipients, requestedDSNs);
			smtp.quit();

			logger.info("Email sent {}", getLog(creds, fromEmail, rcptTo, sendmailResponse, Optional.empty()));
			return sendmailResponse;
		} catch (Exception se) {
			logger.error("Email not sent {}",
					getLog(creds, fromEmail, rcptTo, sendmailResponse, Optional.of(se.getMessage())));
			logger.error(se.getMessage(), se);

			return SendmailResponse.fail(se.getMessage(), failedRecipients);
		}
	}

	private String getLog(SendmailCredentials creds, String fromEmail, MailboxList rcptTo,
			SendmailResponse sendmailResponse, Optional<String> exceptionMessage) {
		return String.format("as: %s, from: %s, to %s, response: %s", creds.loginAtDomain, fromEmail,
				String.join(",", rcptTo.stream().map(rcpt -> rcpt.getAddress()).collect(Collectors.toList())),
				sendmailResponse != null ? sendmailResponse.toString() : exceptionMessage.orElse("Fail"));
	}

}
