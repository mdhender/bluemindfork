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
package net.bluemind.dav.server.proto.report.webdav;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.proto.report.ReportSaxDelegate;
import net.bluemind.dav.server.store.DavResource;

/**
 * What can you auto-complete on ?
 */
public class PrincipalSearchPropertySetDelegate extends ReportSaxDelegate {

	private static final Logger logger = LoggerFactory.getLogger(PrincipalSearchPropertySetDelegate.class);
	private static final QName root = new QName(NS.WEBDAV, "principal-search-property-set");

	@Override
	public QName getRoot() {
		return root;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		logger.debug("{} {}", uri, localName);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
	}

	@Override
	public ReportQuery endDocument(DavResource path) throws SAXException {
		return new PrincipalSearchPropertySetQuery(path, root);
	}

}
