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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.QN;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.proto.report.ReportSaxDelegate;
import net.bluemind.dav.server.proto.report.caldav.CalendarQueryQuery.Filter;
import net.bluemind.dav.server.proto.report.caldav.CalendarQueryQuery.TimeRangeFilter;
import net.bluemind.dav.server.store.DavResource;

public class CalendarQueryDelegate extends ReportSaxDelegate {

	private static final Logger logger = LoggerFactory.getLogger(CalendarQueryDelegate.class);
	private static final QName root = CDReports.CALENDAR_QUERY;

	private List<QName> props;
	private boolean onProps;
	private List<Filter> filters;

	public CalendarQueryDelegate() {
		props = new ArrayList<>(2);
		filters = new ArrayList<>(3);
	}

	@Override
	public QName getRoot() {
		return root;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if ("prop".equals(localName) && NS.WEBDAV.equals(uri)) {
			onProps = true;
		} else if ("time-range".equals(localName) && NS.CALDAV.equals(uri)) {
			TimeRangeFilter trf = new TimeRangeFilter();
			trf.start = BmDateTimeWrapper.create(attributes.getValue("start"));
			String end = attributes.getValue("end");
			if (end != null) {
				trf.end = BmDateTimeWrapper.create(attributes.getValue("end"));
			} else {
				Calendar cal = GregorianCalendar.getInstance();
				cal.add(Calendar.MONTH, 3);
				trf.end = BmDateTimeWrapper.fromTimestamp(cal.getTimeInMillis());
			}
			filters.add(trf);
		} else if (onProps) {
			props.add(QN.qn(uri, localName));
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("prop".equals(localName) && NS.WEBDAV.equals(uri)) {
			onProps = false;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {

	}

	@Override
	public ReportQuery endDocument(DavResource path) throws SAXException {
		logger.info("Query of {} props on filter {}", props.size(), filters.size());
		return new CalendarQueryQuery(path, root, props, filters);
	}

}
