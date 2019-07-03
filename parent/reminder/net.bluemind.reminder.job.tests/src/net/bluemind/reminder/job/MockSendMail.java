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
package net.bluemind.reminder.job;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.sendmail.Sendmail;

public class MockSendMail extends Sendmail {
	public String subject;
	public String html;
	public String from;
	public String to;

	@Override
	public void send(Mailbox from, Message m) throws ServerFault {
		this.from = m.getFrom().get(0).getAddress();
		this.to = m.getTo().get(0).toString();
		this.subject = m.getSubject();
		Multipart body = (Multipart) m.getBody();
		MessageImpl mi = (MessageImpl) body.getBodyParts().get(0);
		MultipartImpl mip = (MultipartImpl) mi.getBody();
		BodyPart bp = (BodyPart) mip.getBodyParts().get(0);
		TextBody tb = (TextBody) bp.getBody();
		this.html = readBody(tb);
	}

	private String readBody(TextBody tb) throws ServerFault {
		try {
			return IOUtils.toString(tb.getInputStream(), "UTF-8");
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}
}
