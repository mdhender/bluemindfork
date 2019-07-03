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
package net.bluemind.mime4j.common.rewriters.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.message.BodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.HeaderImpl;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.apache.james.mime4j.stream.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForwardAsAttachmentHandler extends DontTouchHandler {

	private static final Logger logger = LoggerFactory.getLogger(ForwardAsAttachmentHandler.class);

	private InputStream toForward;

	public ForwardAsAttachmentHandler(Message entity, BodyFactory bf, Mailbox defaultFrom, InputStream toForward) {
		super(entity, bf, defaultFrom);
		this.toForward = toForward;
	}

	@Override
	protected Message firstRewrite(Message parsed) {
		logger.info("Rewrite message: " + parsed.getClass().getCanonicalName());

		Message ret = parsed;
		if (parsed.isMultipart() && parsed.getMimeType().endsWith("/mixed")) {
			logger.info("Device part of the message is multipart. Adding a part with original message");
			Body body = parsed.getBody();
			logger.info("multi: " + body.getClass().getCanonicalName());
			Multipart mp = (Multipart) body;
			addOriginalParts(mp);
		} else {
			logger.warn("Device part of the message is NOT multipart, we must create a new message");
			Body body = parsed.getBody();
			logger.info("Body: " + body.getClass().getCanonicalName());
			MessageImpl msg = new MessageImpl();
			msg.setFrom(ret.getFrom());
			msg.setTo(ret.getTo());
			msg.setCc(ret.getCc());
			msg.setBcc(ret.getBcc());
			msg.setSubject(ret.getSubject());

			MultipartImpl mi = new MultipartImpl("mixed");
			BodyPart bp = new BodyPart();
			HeaderImpl header = new HeaderImpl();
			Header fwh = parsed.getHeader();
			copyHeaderField(header, fwh, FieldName.CONTENT_TYPE);
			copyHeaderField(header, fwh, FieldName.CONTENT_TRANSFER_ENCODING);
			copyHeaderField(header, fwh, FieldName.DATE);
			logger.info("Included device body: " + body.getClass().getCanonicalName());
			bp.setBody(body);
			bp.setHeader(header);
			mi.addBodyPart(bp);

			addOriginalParts(mi);

			msg.setMultipart(mi);
			ret = msg;
		}

		return ret;
	}

	private void addOriginalParts(Multipart mi) {
		try {
			BodyPart bpa = new BodyPart();
			BinaryBody bb = getBodyFactory().binaryBody(toForward);
			bpa.setBody(bb, "message/rfc822");
			bpa.setContentTransferEncoding("base64");
			bpa.setFilename("forward.eml");
			mi.addBodyPart(bpa);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

	}

	private void copyHeaderField(HeaderImpl newHeader, Header source, String field) {
		Field f = source.getField(field);
		if (f != null) {
			newHeader.addField(f);
		}
	}
}
