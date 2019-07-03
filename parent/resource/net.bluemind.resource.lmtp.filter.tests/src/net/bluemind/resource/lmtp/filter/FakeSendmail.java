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
package net.bluemind.resource.lmtp.filter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.Mail;

public class FakeSendmail implements ISendmail {
	public boolean mailSent = false;
	public String subject = "";
	public Set<String> from = new HashSet<>();
	public Set<String> to = new HashSet<>();

	@Override
	public void send(Mail m) throws ServerFault {
		mailSent = true;
	}

	@Override
	public void send(String fromEmail, String userDomain, Message m) throws ServerFault {
		mailSent = true;
		subject = m.getSubject();
	}

	@Override
	public void send(Mailbox sender, Message m) throws ServerFault {
		mailSent = true;
		subject = m.getSubject();
	}

	@Override
	public void send(String domainUid, Message m) throws ServerFault {
		mailSent = true;
		subject = m.getSubject();

		from.add(m.getFrom().get(0).getAddress());

		Iterator<Address> it = m.getTo().iterator();
		while (it.hasNext()) {
			to.add(((Mailbox) it.next()).getAddress());
		}
	}

	@Override
	public void send(String fromEmail, String userDomain, MailboxList rcptTo, Message m) throws ServerFault {
		// TODO Auto-generated method stub

	}
}
