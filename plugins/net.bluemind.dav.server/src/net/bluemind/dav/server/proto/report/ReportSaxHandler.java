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
package net.bluemind.dav.server.proto.report;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.bluemind.dav.server.proto.report.caldav.CalendarMultigetDelegate;
import net.bluemind.dav.server.proto.report.caldav.CalendarQueryDelegate;
import net.bluemind.dav.server.proto.report.calendarserver.CalendarServerPrincipalSearchDelegate;
import net.bluemind.dav.server.proto.report.webdav.ExpandPropertyDelegate;
import net.bluemind.dav.server.proto.report.webdav.PrincipalPropertySearchDelegate;
import net.bluemind.dav.server.proto.report.webdav.PrincipalSearchPropertySetDelegate;
import net.bluemind.dav.server.proto.report.webdav.SyncCollectionDelegate;
import net.bluemind.dav.server.store.DavResource;

public class ReportSaxHandler extends DefaultHandler {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ReportSaxHandler.class);

	private final Map<QName, ReportSaxDelegate> delegates;

	private final void reg(ReportSaxDelegate rsd) {
		delegates.put(rsd.getRoot(), rsd);
	}

	private ReportSaxDelegate delegate;
	private ReportQuery rq;
	private final DavResource res;

	public ReportSaxHandler(DavResource res) {
		this.res = res;
		delegates = new HashMap<>();
		reg(new PrincipalSearchPropertySetDelegate());
		reg(new PrincipalPropertySearchDelegate());
		reg(new ExpandPropertyDelegate());
		reg(new SyncCollectionDelegate());
		reg(new CalendarMultigetDelegate());
		reg(new CalendarQueryDelegate());
		reg(new CalendarServerPrincipalSearchDelegate());
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (delegate == null) {
			QName qn = new QName(uri, localName);
			if (delegates.containsKey(qn)) {
				delegate = delegates.get(qn);
			} else {
				throw new RuntimeException("Not implemented report: " + qn);
			}
		}
		delegate.startElement(uri, localName, qName, attributes);
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		delegate.endElement(uri, localName, qName);
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		delegate.characters(ch, start, length);
	}

	@Override
	public void endDocument() throws SAXException {
		rq = delegate.endDocument(res);
	}

	public ReportQuery getReportQuery() {
		return rq;
	}

}
