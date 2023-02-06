/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.todolist.hook.internal;

import java.util.Date;
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

public class TodoMail {

	private final Mailbox from;
	private final MailboxList to;
	private final String subject;
	private final BodyPart html;
	private final Optional<BodyPart> ics;
	private final Method method;

	public TodoMail(Mailbox from, MailboxList to, String subject, BodyPart html, Optional<BodyPart> ics,
			Method method) {
		this.from = from;
		this.to = to;
		this.subject = subject;
		this.html = html;
		this.ics = ics;
		this.method = method;
	}

	public Message getMessage() {
		MessageBuilder builder = createBuilder();

		MessageImpl m = new MessageImpl();
		m.setDate(new Date());
		m.setSubject(subject);
		m.setSender(from);
		m.setFrom(from);
		m.setTo(to);

		Header h = builder.newHeader();
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
			textCalendar.setFilename("todo.ics");
			Header tcHeader = builder.newHeader();
			tcHeader.setField(Fields.contentType("text/calendar; charset=UTF-8; method=" + method.getValue()));
			tcHeader.setField(Fields.contentTransferEncoding("8bit"));
			textCalendar.setHeader(tcHeader);

			attachment.setBody(icsData.getBody());
			attachment.setFilename("todo.ics");
			Header attHeader = builder.newHeader();
			attHeader.setField(Fields.contentType("application/ics; name=\"todo.ics\""));
			attHeader.setField(Fields.contentDisposition("attachment; filename=\"todo.ics\""));
			attHeader.setField(Fields.contentTransferEncoding("base64"));
			attachment.setHeader(attHeader);

			alternative.addBodyPart(textCalendar);
			mixed.addBodyPart(attachment);
		});

		m.setMultipart(mixed);

		return m;
	}

	public static class TodoMailBuilder {
		private Mailbox from;
		private MailboxList to;
		private Method method;
		private String subject;
		private BodyPart html;
		private Optional<BodyPart> ics;

		public TodoMailBuilder from(Mailbox from) {
			this.from = from;
			return this;
		}

		public TodoMailBuilder to(MailboxList to) {
			this.to = to;
			return this;
		}

		public TodoMailBuilder method(Method method) {
			this.method = method;
			return this;
		}

		public TodoMailBuilder html(BodyPart html) {
			this.html = html;
			return this;
		}

		public TodoMailBuilder subject(String subject) {
			this.subject = subject;
			return this;
		}

		public TodoMailBuilder ics(Optional<BodyPart> ics) {
			this.ics = ics;
			return this;
		}

		public TodoMail build() {
			check(from, "from");
			check(to, "to");
			check(method, "method");
			check(subject, "subject");
			check(html, "html");

			return new TodoMail(from, to, subject, html, ics, method);
		}

		private void check(Object obj, String field) {
			if (obj == null) {
				throw new ServerFault("Cannot create CalendarMail. " + field + " is null");
			}
		}

	}

	private MessageBuilder createBuilder() {
		try {
			return MessageServiceFactory.newInstance().newMessageBuilder();
		} catch (MimeException e) {
			throw new ServerFault("Cannot create MessageBuilder", e);
		}
	}

}
