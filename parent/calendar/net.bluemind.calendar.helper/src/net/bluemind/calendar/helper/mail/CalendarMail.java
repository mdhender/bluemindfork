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
import java.util.List;
import java.util.Optional;

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
	public final Mailbox from;
	public final Mailbox sender;
	public final MailboxList to;
	public final Method method;
	public final String subject;
	public final BodyPart html;
	public final Optional<MailboxList> cc;
	public final Optional<BodyPart> ics;
	public final Optional<List<EventAttachment>> attachments;

	private CalendarMail(Mailbox from, Mailbox sender, MailboxList to, Optional<MailboxList> cc, String subject,
			BodyPart html, Optional<BodyPart> ics, Optional<List<EventAttachment>> attachments, Method method) {
		this.from = from;
		this.sender = sender;
		this.to = to;
		this.cc = cc;
		this.subject = subject;
		this.html = html;
		this.ics = ics;
		this.attachments = attachments;
		this.method = method;
	}

	public Message getMessage() throws ServerFault {
		MessageBuilder builder = createBuilder();

		MessageImpl m = new MessageImpl();
		Header messageHeader = builder.newHeader();
		m.setDate(new Date());
		m.setSubject(subject);
		m.setSender(sender);
		m.setFrom(from);
		m.setTo(to);
		cc.ifPresent(c -> m.setCc(c));

		Header h = builder.newHeader();
		h = builder.newHeader();
		h.setField(Fields.contentType("text/html; charset=UTF-8;"));
		h.setField(Fields.contentTransferEncoding("quoted-printable"));
		html.setHeader(h);

		Multipart alternative = new MultipartImpl("alternative");
		alternative.addBodyPart(html);

		MessageImpl alternativeMessage = new MessageImpl();
		alternativeMessage.setMultipart(alternative);

		BodyPart alternativePart = new BodyPart();
		alternativePart.setMessage(alternativeMessage);

		Multipart mixed = new MultipartImpl("mixed");
		mixed.addBodyPart(alternativeMessage);

		BodyPart textCalendar = new BodyPart();
		BodyPart attachment = new BodyPart();
		ics.ifPresent(icsData -> {
			textCalendar.setBody(icsData.getBody());
			textCalendar.setFilename("event.ics");
			Header tcHeader = builder.newHeader();
			tcHeader.setField(Fields.contentType("text/calendar; charset=UTF-8; method=" + method.getValue()));
			tcHeader.setField(Fields.contentTransferEncoding("8bit"));
			textCalendar.setHeader(tcHeader);

			attachment.setBody(icsData.getBody());
			attachment.setFilename("event.ics");
			Header attHeader = builder.newHeader();
			attHeader.setField(Fields.contentType("application/ics; name=\"event.ics\""));
			attHeader.setField(Fields.contentDisposition("attachment; filename=\"event.ics\""));
			attHeader.setField(Fields.contentTransferEncoding("base64"));
			attachment.setHeader(attHeader);

			alternative.addBodyPart(textCalendar);
			mixed.addBodyPart(attachment);
		});

		attachments.ifPresent(atts -> {
			for (EventAttachment att : atts) {
				BodyPart attBody = new BodyPart();
				attBody.setBody(att.part.getBody());
				attBody.setFilename(att.name);
				Header header = builder.newHeader();
				header.setField(Fields.contentType(att.contentType + "; name=\"" + att.name + "\""));
				header.setField(Fields.contentDisposition("attachment; filename=\"" + att.name + "\""));
				header.setField(Fields.contentTransferEncoding("base64"));
				attBody.setHeader(header);
				mixed.addBodyPart(attBody);
			}
		});

		m.setMultipart(mixed);
		m.setHeader(messageHeader);

		return m;
	}

	private MessageBuilder createBuilder() {
		try {
			return MessageServiceFactory.newInstance().newMessageBuilder();
		} catch (MimeException e) {
			throw new ServerFault("Cannot create MessageBuilder", e);
		}
	}

	public static class CalendarMailBuilder {
		private Mailbox from;
		private Mailbox sender;
		private MailboxList to;
		private Method method;
		private String subject;
		private BodyPart html;
		private MailboxList cc;
		private BodyPart ics;
		private List<EventAttachment> attachments;

		public CalendarMail build() {
			check(from, "from");
			check(sender, "sender");
			check(to, "to");
			check(method, "method");
			check(subject, "subject");
			check(html, "html");

			return new CalendarMail(from, sender, to, Optional.ofNullable(cc), subject, html, Optional.ofNullable(ics),
					Optional.ofNullable(attachments), method);
		}

		private void check(Object obj, String field) {
			if (obj == null) {
				throw new ServerFault("Cannot create CalendarMail. " + field + " is null");
			}
		}

		public CalendarMailBuilder from(Mailbox from) {
			this.from = from;
			return this;
		}

		public CalendarMailBuilder sender(Mailbox sender) {
			this.sender = sender;
			return this;
		}

		public CalendarMailBuilder to(MailboxList to) {
			this.to = to;
			return this;
		}

		public CalendarMailBuilder method(Method method) {
			this.method = method;
			return this;
		}

		public CalendarMailBuilder subject(String subject) {
			this.subject = subject;
			return this;
		}

		public CalendarMailBuilder html(BodyPart html) {
			this.html = html;
			return this;
		}

		public CalendarMailBuilder cc(MailboxList cc) {
			this.cc = cc;
			return this;
		}

		public CalendarMailBuilder ics(BodyPart ics) {
			this.ics = ics;
			return this;
		}

		public CalendarMailBuilder attachments(List<EventAttachment> attachments) {
			this.attachments = attachments;
			return this;
		}
	}
}
