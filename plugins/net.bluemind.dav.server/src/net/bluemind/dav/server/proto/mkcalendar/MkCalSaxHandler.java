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
package net.bluemind.dav.server.proto.mkcalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.bluemind.dav.server.proto.mkcalendar.MkCalQuery.Kind;

public class MkCalSaxHandler extends DefaultHandler {

	public Kind kind;
	public String displayName;
	private StringBuilder sb = new StringBuilder();
	private boolean inDn;
	private static final Logger logger = LoggerFactory.getLogger(MkCalSaxHandler.class);

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if ("displayname".equals(localName)) {
			inDn = true;
		} else if ("comp".equals(localName)) {
			kind = Kind.valueOf(attributes.getValue("name"));
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("displayname".equals(localName)) {
			inDn = false;
			displayName = sb.toString();
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (inDn) {
			sb.append(ch, start, length);
		}
	}

	@Override
	public void endDocument() throws SAXException {
		logger.info("Kind: {}, DN: {}", kind, displayName);
	}
}