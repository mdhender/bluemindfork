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
package net.bluemind.dav.server.proto.report.caldav;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.QN;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.proto.report.ReportSaxDelegate;
import net.bluemind.dav.server.store.DavResource;

public class CalendarMultigetDelegate extends ReportSaxDelegate {

	private static final Logger logger = LoggerFactory.getLogger(CalendarMultigetDelegate.class);
	private static final QName root = CDReports.CALENDAR_MULTIGET;

	private List<QName> props;
	private List<String> hrefs;
	private boolean onHref;
	private boolean onProps;
	private StringBuilder sb;

	public CalendarMultigetDelegate() {
		props = new ArrayList<>(2);
		hrefs = new LinkedList<>();
		this.sb = new StringBuilder(256);
	}

	@Override
	public QName getRoot() {
		return root;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if ("href".equals(localName) && NS.WEBDAV.equals(uri)) {
			onHref = true;
			sb.setLength(0);
		} else if ("prop".equals(localName) && NS.WEBDAV.equals(uri)) {
			onProps = true;
		} else if (onProps) {
			props.add(QN.qn(uri, localName));
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("href".equals(localName) && NS.WEBDAV.equals(uri)) {
			onHref = false;
			hrefs.add(sb.toString());
		} else if ("prop".equals(localName) && NS.WEBDAV.equals(uri)) {
			onProps = true;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (onHref) {
			sb.append(ch, start, length);
		}
	}

	@Override
	public ReportQuery endDocument(DavResource path) throws SAXException {
		logger.info("Multiget of {} props on {} calendar items.", props.size(), hrefs.size());
		return new CalendarMultigetQuery(path, root, props, hrefs);
	}

}
