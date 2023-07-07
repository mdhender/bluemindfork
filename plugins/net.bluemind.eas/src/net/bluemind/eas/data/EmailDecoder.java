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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.field.address.LenientAddressBuilder;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.w3c.dom.Element;

import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.BufferByteSource;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.MSEmail;
import net.bluemind.eas.data.email.Type;
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
		Element body = DOMUtils.getUniqueElement(syncData, "Body");
		if (body == null) {
			return;
		}
		Type type = Type.fromInt(Integer.parseInt(DOMUtils.getUniqueElement(body, "Type").getTextContent()));
		if (type == Type.MIME) {
			mail.setContent(BufferByteSource.of(DOMUtils.getUniqueElement(body, "Data").getTextContent().getBytes()));
		} else {
			try (Message m = MessageServiceFactory.newInstance().newMessageBuilder().newMessage()) {
				Element to = DOMUtils.getUniqueElement(syncData, "To");

				if (to != null) {
					List<Mailbox> recipients = new ArrayList<>();
					Splitter.on(";").split(to.getTextContent()).forEach(
							recipient -> recipients.add(LenientAddressBuilder.DEFAULT.parseMailbox(recipient)));
					m.setTo(recipients);
				}

				Element subject = DOMUtils.getUniqueElement(syncData, "Subject");
				if (subject != null) {
					m.setSubject(subject.getTextContent());
				}

				String data = DOMUtils.getUniqueElement(body, "Data").getTextContent();
				if (type == Type.PLAIN_TEXT) {
					m.getHeader().setField(Fields.contentType(Mime4JHelper.TEXT_PLAIN));
				} else if (type == Type.HTML) {
					m.getHeader().setField(Fields.contentType(Mime4JHelper.TEXT_HTML));
				}
				m.setBody(new BasicBodyFactory().textBody(data));

				// TOO Attachments

				InputStream in = Mime4JHelper.asStream(m);
				mail.setContent(BufferByteSource.of(ByteStreams.toByteArray(in)));
			} catch (Exception e) {
				logger.error("Failed to decode body", e);
			}
		}
	}
}
