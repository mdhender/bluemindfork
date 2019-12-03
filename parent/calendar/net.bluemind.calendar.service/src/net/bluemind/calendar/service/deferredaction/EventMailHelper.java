/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.calendar.service.deferredaction;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.AbstractEntity;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;

import freemarker.template.TemplateException;
import net.bluemind.calendar.helper.mail.CalendarMailHelper;
import net.bluemind.calendar.helper.mail.Messages;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailHelper;

public class EventMailHelper {
	private static final String BODY_TEMPLATE = "EventAlert.ftl";
	private static final String SUBJECT_TEMPLATE = "EventSubjectAlert.ftl";
	private ISendmail mailer;

	public EventMailHelper(ISendmail mailer) {
		this.mailer = mailer == null ? new Sendmail() : mailer;
	}

	public EventMailHelper() {
		this(new Sendmail());
	}

	private Message createMessage(Locale locale, Map<String, Object> data, Mailbox mailbox)
			throws MimeException, IOException, TemplateException {
		MessagesResolver resolver = new MessagesResolver(Messages.getEventDetailMessages(locale),
				Messages.getEventAlertMessages(locale));

		BodyPart body = new CalendarMailHelper().buildBody(BODY_TEMPLATE, locale.getLanguage(), resolver, data);
		String subject = new CalendarMailHelper().buildSubject(SUBJECT_TEMPLATE, locale.getLanguage(), resolver, data);
		return createMessageImpl(mailbox, mailbox, mailbox, subject, body);

	}

	private MessageImpl createMessageImpl(Mailbox sender, Mailbox from, Mailbox to, String subject, BodyPart body)
			throws MimeException {
		MessageImpl message = new MessageImpl();
		message.setDate(new Date());
		message.setSender(sender);
		message.setFrom(from);
		message.setTo(to);
		message.setSubject(subject);

		MessageBuilder builder = MessageServiceFactory.newInstance().newMessageBuilder();
		Header header = builder.newHeader();
		header.setField(Fields.contentType("text/html; charset=UTF-8;"));
		header.setField(Fields.contentTransferEncoding("quoted-printable"));
		body.setHeader(header);

		message.setMultipart(createMixedBody(body));
		return message;
	}

	private Multipart createMixedBody(BodyPart body) {
		MessageImpl alternativeMessage = new MessageImpl();
		Multipart createAlternativePart = createMultipart(body, "alternative");
		alternativeMessage.setMultipart(createAlternativePart);
		MessageImpl createAlternativeMessage = alternativeMessage;
		return createMultipart(createAlternativeMessage, "mixed");
	}

	private Multipart createMultipart(AbstractEntity bodyPart, String subType) {
		Multipart multipart = new MultipartImpl(subType);
		multipart.addBodyPart(bodyPart);
		return multipart;
	}

	public void send(Locale locale, Map<String, Object> data, ItemValue<net.bluemind.mailbox.api.Mailbox> userMailbox)
			throws MimeException, IOException, TemplateException {
		Mailbox mailbox = getMailbox(userMailbox);
		Message message = createMessage(locale, data, mailbox);
		mailer.send(mailbox, message);
	}

	private org.apache.james.mime4j.dom.address.Mailbox getMailbox(
			ItemValue<net.bluemind.mailbox.api.Mailbox> mailbox) {
		return SendmailHelper.formatAddress(mailbox.displayName, mailbox.value.defaultEmail().address);
	}
}