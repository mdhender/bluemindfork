/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.backend.mail.replica.service.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.sendmail.FailedRecipient;

public class NonDeliveryReportMessage {
	private static final Logger logger = LoggerFactory.getLogger(NonDeliveryReportMessage.class);

	private final List<FailedRecipient> failedRecipients;
	private final Message relatedMsg;

	public NonDeliveryReportMessage(List<FailedRecipient> failedRecipients, Message relatedMsg) {
		this.failedRecipients = failedRecipients;
		this.relatedMsg = relatedMsg;
	}

	public MessageImpl createNDRMessage(String content) {
		return createNDRMessageImpl(content, "Undelivered Mail Returned to Sender");
	}

	private MessageImpl createNDRMessageImpl(String content, String subject) {
		MessageImpl message = new MessageImpl();
		message.setSubject(subject);

		Multipart mp = new MultipartImpl("report");
		mp.addBodyPart(msgBodyPart(content));
		mp.addBodyPart(msgDeliveryStatusPart());
		try {
			mp.addBodyPart(msgAsAttachment());
		} catch (MimeException | IOException e) {
			logger.error("Cannot add related message as attachment.", e);
		}

		message.setMultipart(mp, Map.of("report-type", "delivery-status"));
		return message;
	}

	private BodyPart msgBodyPart(String content) {
		BodyPart bodyPart = new BodyPart();
		TextBody textBody = new BasicBodyFactory().textBody(content, StandardCharsets.UTF_8);
		bodyPart.setText(textBody);
		return bodyPart;
	}

	private BodyPart msgAsAttachment() throws MimeException, IOException {
		BodyPart rfc822 = new BodyPart();
		Header header = new DefaultMessageBuilder().newHeader();
		header.addField(new RawField("Content-Description", "Undelivered Message"));
		header.addField(Fields.contentType("message/rfc822"));
		header.addField(Fields.contentTransferEncoding("7bit"));
		rfc822.setHeader(header);
		rfc822.setFilename(relatedMsg.getSubject() + ".eml");
		MessageWriter writer = MessageServiceFactory.newInstance().newMessageWriter();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writer.writeMessage(relatedMsg, out);
		rfc822.setBody(new BasicBodyFactory().binaryBody(out.toByteArray()));
		return rfc822;
	}

	private BodyPart msgDeliveryStatusPart() {
		BodyPart mdnPart = new BodyPart();
		Header mdnHeader = new DefaultMessageBuilder().newHeader();
		RawField contentDescHeader = new RawField("Content-Description", "Delivery report");
		mdnHeader.addField(contentDescHeader);
		mdnHeader.addField(Fields.contentType("message/delivery-status"));
		mdnPart.setHeader(mdnHeader);

		StringBuilder mdnContent = new StringBuilder();
		String clientDomain = relatedMsg.getFrom().get(0).getDomain();
		mdnContent.append(new RawField("Reporting-MTA", String.format("dns; %s", clientDomain))).append("\r\n");
		mdnContent.append(new RawField("X-Original-Message-ID", String.format("%s", relatedMsg.getMessageId())))
				.append("\r\n");

		for (FailedRecipient rcpt : failedRecipients) {
			mdnContent.append("\r\n");
			mdnContent.append(new RawField("Final-Recipient", String.format("rfc822; %s", rcpt.recipient)))
					.append("\r\n");
			mdnContent.append(new RawField("Action", "failed")).append("\r\n");
			mdnContent.append(new RawField("Status", rcpt.smtpStatus)).append("\r\n");
			String serverDomain = rcpt.recipient.split("@")[1];
			mdnContent.append(new RawField("Remote-MTA", String.format("dns; %s", serverDomain))).append("\r\n");
			mdnContent.append(new RawField("Diagnostic-Code",
					String.format("smtp; %d %s", rcpt.code, "\"" + rcpt.message.replace("<", "[").replace(">", "]"))))
					.append("\"\r\n");
		}

		mdnPart.setBody(new BasicBodyFactory().textBody(mdnContent.toString(), StandardCharsets.UTF_8));
		return mdnPart;
	}
}
