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
package net.bluemind.dav.server.proto.sharing;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.bluemind.dav.server.proto.sharing.Sharing.SharingAction;
import net.bluemind.dav.server.proto.sharing.Sharing.SharingType;

public class SharingQuerySaxHandler extends DefaultHandler {

	private static final Logger logger = LoggerFactory.getLogger(SharingQuerySaxHandler.class);
	private List<Sharing> sharings;

	private StringBuilder sb = new StringBuilder();
	private boolean inHref;
	private Sharing sharing;

	public SharingQuerySaxHandler() {
		sharings = new ArrayList<Sharing>();
	}

	public List<Sharing> getSharings() {
		if (logger.isDebugEnabled()) {
			for (Sharing sharing : sharings) {
				logger.debug("GET SHARING: ACTION: {}, HREF: {}, TYPE: {}", sharing.action, sharing.href, sharing.type);
			}
		}
		return sharings;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if ("set".equals(localName)) {
			sharing = new Sharing();
			sharing.action = SharingAction.Set;
		} else if ("remove".equals(localName)) {
			sharing = new Sharing();
			sharing.action = SharingAction.Remove;
		} else if ("href".equals(localName)) {
			inHref = true;
			sb.setLength(0);
		} else if ("read-write".equals(localName)) {
			sharing.type = SharingType.ReatWrite;
		} else if ("read".equals(localName)) {
			sharing.type = SharingType.ReadOnly;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("href".equals(localName)) {
			inHref = false;
			sharing.href = sb.toString().replace("mailto:", "");
			sb.setLength(0);
		} else if ("set".equals(localName) || "remove".equals(localName)) {
			sharings.add(sharing);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (inHref) {
			sb.append(ch, start, length);
		}
	}

}
