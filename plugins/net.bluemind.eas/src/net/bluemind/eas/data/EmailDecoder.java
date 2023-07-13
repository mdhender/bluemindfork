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
package net.bluemind.eas.data;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.field.address.LenientAddressBuilder;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.base.Splitter;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.BufferByteSource;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.MSEmail;
import net.bluemind.eas.data.email.Type;
import net.bluemind.eas.dto.base.AirSyncBaseResponse.Attachment;
import net.bluemind.eas.dto.base.AirSyncBaseResponse.Attachment.Method;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.mime4j.common.Mime4JHelper;

public class EmailDecoder extends Decoder implements IDataDecoder {

	@Override
	public IApplicationData decode(BackendSession bs, Element syncData) {
		MSEmail mail = new MSEmail();

		Element read = DOMUtils.getUniqueElement(syncData, "Read");
		if (read != null) {
			mail.setRead(parseDOMInt2Boolean(read));
		} else {
			mail.setRead(null);
		}

		Element flag = DOMUtils.getUniqueElement(syncData, "Flag");
		if (flag != null) {
			Element fs = DOMUtils.getUniqueElement(flag, "Status");
			if (fs != null) {
				mail.setStarred(parseDOMInt(fs) == 1);
			} else {
				mail.setStarred(false);
			}
		} else {
			mail.setStarred(null);
		}

		decodeBody(syncData, mail);

		return mail;
	}

	private void decodeBody(Element syncData, MSEmail mail) {

		List<Attachment> attachments = decodeAttachments(syncData);

		Element body = DOMUtils.getUniqueElement(syncData, "Body");
		if (body == null && attachments.isEmpty()) {
			return;
		}

		if (body != null && Type
				.fromInt(Integer.parseInt(DOMUtils.getUniqueElement(body, "Type").getTextContent())) == Type.MIME) {
			mail.setContent(BufferByteSource.of(DOMUtils.getUniqueElement(body, "Data").getTextContent().getBytes()));
			return;
		}

		try (MessageImpl m = new MessageImpl()) {
			MessageBuilder builder = MessageServiceFactory.newInstance().newMessageBuilder();
			Header header = builder.newHeader();
			MultipartImpl multipart = new MultipartImpl("mixed");

			Element to = DOMUtils.getUniqueElement(syncData, "To");
			if (to != null) {
				List<Mailbox> recipients = new ArrayList<>();
				Splitter.on(";").split(to.getTextContent())
						.forEach(recipient -> recipients.add(LenientAddressBuilder.DEFAULT.parseMailbox(recipient)));
				m.setTo(recipients);
			}

			Element subject = DOMUtils.getUniqueElement(syncData, "Subject");
			if (subject != null) {
				m.setSubject(subject.getTextContent());
			}

			if (body != null) {
				Type type = Type.fromInt(Integer.parseInt(DOMUtils.getUniqueElement(body, "Type").getTextContent()));
				if (type == Type.PLAIN_TEXT) {
					header.setField(
							Fields.contentType(Mime4JHelper.TEXT_PLAIN + "; charset=" + StandardCharsets.UTF_8.name()));
				} else if (type == Type.HTML) {
					header.setField(
							Fields.contentType(Mime4JHelper.TEXT_HTML + "; charset=" + StandardCharsets.UTF_8.name()));
				}
				header.setField(Fields.contentTransferEncoding("quoted-printable"));

				String data = DOMUtils.getUniqueElement(body, "Data").getTextContent();
				TextBody textBody = new BasicBodyFactory().textBody(data, StandardCharsets.UTF_8);

				BodyPart bodyPart = new BodyPart();
				bodyPart.setBody(textBody);
				bodyPart.setHeader(header);
				multipart.addBodyPart(bodyPart);
			}

			attachments.forEach(attachment -> {
				BodyPart attachmentBodyPart = new BodyPart();
				try {
					attachmentBodyPart
							.setBody(new BasicBodyFactory().binaryBody(attachment.content.openBufferedStream()));
					attachmentBodyPart.setFilename(attachment.displayName);
					Header attachmentHeader = builder.newHeader();
					attachmentHeader.setField(
							Fields.contentType(attachment.contentType + "; name=\"" + attachment.displayName + "\""));
					attachmentHeader.setField(
							Fields.contentDisposition("attachment" + "; filename=\"" + attachment.displayName + "\""));
					attachmentHeader.setField(Fields.contentTransferEncoding("base64"));
					attachmentBodyPart.setHeader(attachmentHeader);
					multipart.addBodyPart(attachmentBodyPart);

				} catch (IOException e) {
					logger.error("Failed to add attachment", e);
				}

			});
			m.setMultipart(multipart);

			mail.setContent(BufferByteSource.of(Mime4JHelper.mmapedEML(m).nettyBuffer()));
		} catch (Exception e) {
			logger.error("Failed to decode body", e);
		}
	}

	private List<Attachment> decodeAttachments(Element syncData) {
		Element attachments = DOMUtils.getUniqueElement(syncData, "Attachments");
		if (attachments == null) {
			return Collections.emptyList();
		}

		List<Attachment> ret = new ArrayList<>();
		NodeList children = attachments.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Element node = (Element) attachments.getChildNodes().item(i);
			if ("Add".equals(node.getTagName())) {
				Attachment attachment = new Attachment();
				attachment.clientId = parseDOMString(DOMUtils.getUniqueElement(node, "ClientId"));
				attachment.method = Method.of(parseDOMString(DOMUtils.getUniqueElement(node, "AttMethod")));
				attachment.content = BufferByteSource
						.of(Base64.getDecoder().decode(parseDOMString(DOMUtils.getUniqueElement(node, "Content"))));
				attachment.displayName = parseDOMString(DOMUtils.getUniqueElement(node, "DisplayName"));
				attachment.contentType = URLConnection.guessContentTypeFromName(attachment.displayName);
				ret.add(attachment);
			} else {
				logger.warn("Unsupported method {}", node.getTagName());
			}
		}
		return ret;
	}

}
