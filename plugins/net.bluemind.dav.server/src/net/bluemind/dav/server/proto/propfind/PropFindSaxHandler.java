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
package net.bluemind.dav.server.proto.propfind;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.QN;

public class PropFindSaxHandler extends DefaultHandler {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(PropFindSaxHandler.class);

	private boolean recordProps = false;
	private Set<QName> queried;
	private boolean allProps;

	public PropFindSaxHandler() {
		queried = new LinkedHashSet<>();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (!allProps && "allprop".equals(localName) && NS.WEBDAV.equals(uri)) {
			allProps = true;
		}

		if (!allProps) {
			if (recordProps) {
				queried.add(QN.qn(uri, localName));
			} else if ("prop".equals(localName) && NS.WEBDAV.equals(uri)) {
				recordProps = true;
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (!allProps) {
			if ("prop".equals(localName) && NS.WEBDAV.equals(uri)) {
				recordProps = false;
			}
		}
	}

	public Set<QName> getQueried() {
		return queried;
	}

	public boolean isAllProps() {
		return allProps;
	}

}
