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
package net.bluemind.imip.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.HeaderImpl;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.apache.james.mime4j.stream.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import net.bluemind.imip.parser.impl.IMIPParserHelper;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;

/**
 * Adds an html part when email body is only an ICS
 * 
 * @author tom
 * 
 */
public class PureICSRewriter {

	private static final Logger logger = LoggerFactory.getLogger(PureICSRewriter.class);

	private Set<String> dontCopy = new HashSet<String>();

	public PureICSRewriter() {
		dontCopy.add(FieldName.CONTENT_TYPE);
		dontCopy.add(FieldName.CONTENT_TRANSFER_ENCODING);
	}

	public Message rewrite(Message m) {
		if (!isRewritable(m)) {
			return m;
		}
		try {
			return rewriteImpl(m);
		} catch (Exception e) {
			logger.error("Failed at rewritting pure ics: " + e.getMessage(), e);
			return m;
		}
	}

	private Message rewriteImpl(Message m) throws IOException, ParserException {
		MessageImpl nm = new MessageImpl();
		Header nh = new HeaderImpl();
		Header old = m.getHeader();
		// copy all headers except the ones we will replace
		for (Field f : old.getFields()) {
			String n = f.getName();
			if (!dontCopy.contains(n)) {
				nh.addField(f);
			}
		}
		nm.setHeader(nh);

		BasicBodyFactory bbf = new BasicBodyFactory();
		MultipartImpl alter = new MultipartImpl("alternative");

		TextBody calB = (TextBody) m.getBody();
		InputStream in = calB.getInputStream();
		InputStreamReader reader = new InputStreamReader(in);
		List<CalendarComponent> edv = IMIPParserHelper.fromICS(reader);

		BodyPart htmlPart = new BodyPart();
		Header htmlh = new HeaderImpl();
		ContentTypeField ct = Fields.contentType("text/html", ImmutableMap.of("charset", "utf-8"));
		htmlh.addField(ct);
		htmlPart.setHeader(htmlh);
		Property altDesc = null;
		for (CalendarComponent calElement : edv) {
			altDesc = altDesc != null ? altDesc : calElement.getProperty("X-ALT-DESC");
		}

		if (altDesc != null) {
			String htmlValue = altDesc.getValue();
			logger.debug("htmlValue:\n{}", htmlValue);
			htmlPart.setBody(bbf.textBody(htmlValue));
		} else {
			htmlPart.setBody(bbf.textBody("<html><body></body></html>"));
		}

		alter.addBodyPart(htmlPart);

		BodyPart calPart = new BodyPart();
		Header calh = new HeaderImpl();
		calh.addField(old.getField(FieldName.CONTENT_TYPE));
		calh.addField(old.getField(FieldName.CONTENT_TRANSFER_ENCODING));
		calPart.setHeader(calh);
		calPart.setBody(calB);

		alter.addBodyPart(calPart);

		nm.setMultipart(alter);

		return nm;
	}

	private boolean isRewritable(Message m) {
		boolean ret = false;
		try {
			Header h = m.getHeader();
			if (h != null) {
				ContentTypeField ctype = (ContentTypeField) h.getField(FieldName.CONTENT_TYPE);
				if (ctype != null) {
					ret = "text/calendar".equals(ctype.getMimeType());
				}
			}
		} catch (Exception t) {
			logger.error(t.getMessage(), t);
		}
		return ret;

	}

}
