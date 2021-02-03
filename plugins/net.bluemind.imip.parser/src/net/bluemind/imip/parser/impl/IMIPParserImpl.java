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
package net.bluemind.imip.parser.impl;

import java.util.LinkedList;
import java.util.List;

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.message.BodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imip.parser.IIMIPParser;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.ITIPMethod;

public class IMIPParserImpl implements IIMIPParser {

	public static final String TEXT_CALENDAR = "text/calendar";
	public static final String M_ALTERNATIVE = "multipart/alternative";

	private static final Logger logger = LoggerFactory.getLogger(IMIPParserImpl.class);

	@Override
	public IMIPInfos parse(Message m) {
		String mid = m.getMessageId();
		if (logger.isDebugEnabled()) {
			logger.debug("[" + mid + "] Checking if message is IMIP related... ");
		}

		if (!m.isMultipart()) {
			return null;
		}

		Body body = m.getBody();
		if (body == null) {
			logger.error("Message has no body.");
			return null;
		}

		Multipart mp = (Multipart) body;
		List<Entity> parts = mp.getBodyParts();
		parts = expandParts(parts);

		for (Entity e : parts) {
			String mime = e.getMimeType();
			if (TEXT_CALENDAR.equals(mime)) {
				logger.info("[" + mid + "] Found " + TEXT_CALENDAR + " part.");
				Header h = e.getHeader();
				ContentTypeField ctField = (ContentTypeField) h.getField("content-type");
				String mparam = ctField.getParameter("method");
				ITIPMethod method = null;
				try {
					method = ITIPMethod.valueOf(mparam.toUpperCase());
				} catch (Exception t) {
					logger.info("[" + mid + "] Missing or invalid iTIP method (" + mparam + "), skipping.");
					continue;
				}
				logger.info("[" + mid + "] method: " + method);
				IMIPInfos imip = new IMIPInfos();
				imip.method = method;
				imip.messageId = mid;
				IMIPInfos parseiTIP = parseiTIP(imip, e);
				return parseiTIP;
			}
		}

		return null;
	}

	private List<Entity> expandParts(List<Entity> parts) {
		List<Entity> ret = new LinkedList<Entity>();
		for (Entity e : parts) {
			if (!e.isMultipart()) {
				ret.add(e);
			} else {
				BodyPart bp = (BodyPart) e;
				Multipart mpart = (Multipart) bp.getBody();
				ret.addAll(expandParts(mpart.getBodyParts()));
			}
		}
		return ret;
	}

	private IMIPInfos parseiTIP(IMIPInfos imip, Entity e) {
		ITIPPartParser partParser = new ITIPPartParser(imip);
		try {
			return partParser.parse(e);
		} catch (Exception ioe) {
			logger.error("[" + imip.messageId + "] Parsing error", ioe);
			return null;
		}
	}
}
