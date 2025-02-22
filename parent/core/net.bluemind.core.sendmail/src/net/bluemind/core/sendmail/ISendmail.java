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

import java.io.InputStream;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;

import net.bluemind.core.api.fault.ServerFault;

public interface ISendmail {

	/**
	 * Send an email using specific SMTP authentication. This API is usable from
	 * outside core JVM.
	 * 
	 * @param creds      SMTP credentials
	 * @param fromEmail  Envelope from
	 * @param userDomain Used to locate a valid SMTP
	 * @param m
	 * @throws ServerFault
	 */
	public SendmailResponse send(SendmailCredentials creds, String fromEmail, String userDomain, Message m);

	/**
	 * @param m
	 * @throws ServerFault
	 */
	public SendmailResponse send(Mail m);

	/**
	 * @param from
	 * @param m
	 * @throws ServerFault
	 */
	public SendmailResponse send(Mailbox from, Message m);

	/**
	 * @param creds     SMTP credentials
	 * @param domainUid
	 * @param m
	 * @throws ServerFault
	 */
	public SendmailResponse send(SendmailCredentials creds, String domainUid, Message m);

	/**
	 * Send an email using specific SMTP authentication. This API is usable from
	 * outside core JVM.
	 * 
	 * @param creds      SMTP credentials
	 * @param fromEmail  Envelope from
	 * @param userDomain Used to locate a valid SMTP
	 * @param rcptTo     the real recipients
	 * @param m
	 * @throws ServerFault
	 */
	public SendmailResponse send(SendmailCredentials creds, String fromEmail, String userDomain, MailboxList rcptTo,
			Message m);

	public SendmailResponse send(SendmailCredentials creds, String fromEmail, String userDomain, MailboxList rcptTo,
			InputStream inStream);

	/**
	 * @param requestDSN if <code>true</code>, request the sending of a report to
	 *                   <code>fromEmail</code> upon successful delivery of the
	 *                   message (Delivery Status Notification, rfc3461)
	 */
	public SendmailResponse send(SendmailCredentials creds, String fromEmail, String userDomain, MailboxList rcptTo,
			InputStream inStream, boolean requestDSN);

}