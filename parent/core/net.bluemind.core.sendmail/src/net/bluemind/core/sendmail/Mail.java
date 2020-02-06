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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.UnstructuredField;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.field.UnstructuredFieldImpl;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.HeaderImpl;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.stream.RawField;

public class Mail {
	public Mailbox from;
	public Mailbox sender;
	public Mailbox to;

	public String subject;
	public String html;

	public List<RawField> headers = new ArrayList<RawField>();

	public Message getMessage() {
		MessageImpl m = new MessageImpl();
		Header h = new HeaderImpl();
		h.addField(Fields.date(new Date()));
		h.addField(Fields.subject(subject));
		h.addField(Fields.sender(sender));
		h.addField(Fields.from(from));
		h.addField(Fields.to(to));
		h.setField(Fields.contentType("text/html; charset=UTF-8;"));
		h.setField(Fields.contentTransferEncoding("quoted-printable"));
		for (RawField rf : headers) {
			UnstructuredField field = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
			h.setField(field);
		}
		m.setHeader(h);

		BodyPart body = createTextPart(html);
		m.setBody(body.getBody());

		return m;
	}

	private static BodyPart createTextPart(String text) {
		BasicBodyFactory bodyFactory = new BasicBodyFactory();
		TextBody body;
		try {
			body = bodyFactory.textBody(text, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("unsupported encoding");
		}
		BodyPart bodyPart = new BodyPart();
		bodyPart.setText(body);
		return bodyPart;
	}
}
