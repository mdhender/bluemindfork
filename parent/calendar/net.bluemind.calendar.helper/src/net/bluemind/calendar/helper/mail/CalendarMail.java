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
package net.bluemind.calendar.helper.mail;

import java.util.Date;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;

import net.bluemind.core.api.fault.ServerFault;
import net.fortuna.ical4j.model.property.Method;

public class CalendarMail {
	public Mailbox from;
	public Mailbox sender;
	public MailboxList to;
	public MailboxList cc;
	public String subject;
	public BodyPart html;
	public BodyPart ics;
	public Method method;

	public Message getMessage() throws ServerFault {
		MessageBuilder builder = null;
		try {
			builder = MessageServiceFactory.newInstance().newMessageBuilder();
		} catch (MimeException e) {
			throw new ServerFault("Cannot create MessageBuilder", e);
		}

		MessageImpl m = new MessageImpl();
		m.setDate(new Date());
		m.setSubject(subject);
		m.setSender(sender);
		m.setFrom(from);
		m.setTo(to);
		m.setCc(cc);

		Header h = builder.newHeader();
		h = builder.newHeader();
		h.setField(Fields.contentType("text/html; charset=UTF-8;"));
		h.setField(Fields.contentTransferEncoding("quoted-printable"));
		html.setHeader(h);

		BodyPart textCalendar = null;
		BodyPart attachment = null;
		if (ics != null) {
			textCalendar = new BodyPart();
			textCalendar.setBody(ics.getBody());
			textCalendar.setFilename("event.ics");
			h = builder.newHeader();
			h.setField(Fields.contentType("text/calendar; charset=UTF-8; method=" + method.getValue()));
			h.setField(Fields.contentTransferEncoding("8bit"));
			textCalendar.setHeader(h);

			attachment = new BodyPart();
			attachment.setBody(ics.getBody());
			attachment.setFilename("event.ics");
			h = builder.newHeader();
			h.setField(Fields.contentType("application/ics; name=\"event.ics\""));
			h.setField(Fields.contentDisposition("attachment; filename=\"event.ics\""));
			h.setField(Fields.contentTransferEncoding("base64"));
			attachment.setHeader(h);
		}

		Multipart alternative = new MultipartImpl("alternative");
		alternative.addBodyPart(html);
		if (textCalendar != null) {
			alternative.addBodyPart(textCalendar);
		}

		MessageImpl alternativeMessage = new MessageImpl();
		alternativeMessage.setMultipart(alternative);

		BodyPart alternativePart = new BodyPart();
		alternativePart.setMessage(alternativeMessage);

		Multipart mixed = new MultipartImpl("mixed");
		mixed.addBodyPart(alternativeMessage);
		if (attachment != null) {
			mixed.addBodyPart(attachment);
		}

		m.setMultipart(mixed);

		return m;
	}
}
