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
package net.bluemind.dav.server.proto.report.calendarserver;

import java.util.ArrayList;
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

public class CalendarServerPrincipalSearchDelegate extends ReportSaxDelegate {

	private static final Logger logger = LoggerFactory.getLogger(CalendarServerPrincipalSearchDelegate.class);
	private static final QName root = CSReports.CALENDARSERVER_PRINCIPAL_SEARCH;

	private StringBuilder mv;
	private String searchToken;
	private int nResults;
	private boolean inProp;
	private boolean inSearchToken;
	private boolean inLimit;
	private PrincipalSearchContext context;

	private List<QName> results;

	public CalendarServerPrincipalSearchDelegate() {
		this.mv = new StringBuilder(64);
		results = new ArrayList<>(8);
		nResults = 10;
	}

	@Override
	public QName getRoot() {
		return root;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (inProp) {
			results.add(QN.qn(uri, localName));
		}

		if (NS.WEBDAV.equals(uri)) {
			if ("prop".equals(localName)) {
				inProp = true;
			}
		} else if (NS.CSRV_ORG.equals(uri)) {
			if ("search-token".equals(localName)) {
				inSearchToken = true;
			} else if ("nresults".equals(localName)) {
				inLimit = true;
			} else if ("calendarserver-principal-search".equals(localName)) {
				context = PrincipalSearchContext.valueOf(attributes.getValue("context"));
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (NS.WEBDAV.equals(uri)) {
			if ("prop".equals(localName)) {
				inProp = false;
			}
		} else if (NS.CSRV_ORG.equals(uri)) {
			if ("search-token".equals(localName)) {
				inSearchToken = false;
				searchToken = mv.toString();
				mv.setLength(0);
			} else if ("nresults".equals(localName)) {
				inLimit = false;
				nResults = Integer.parseInt(mv.toString());
				mv.setLength(0);
			}
		}
	}

	@Override
	public ReportQuery endDocument(DavResource path) throws SAXException {
		CalendarServerPrincipalSearchQuery ppsq = new CalendarServerPrincipalSearchQuery(path, root);
		ppsq.setExpectedResults(results);
		ppsq.setSearchToken(searchToken);
		ppsq.setContext(context);
		ppsq.setLimit(nResults);
		logger.info("match '{}' pattern, return {} max with {} props.", searchToken, nResults, results.size());
		return ppsq;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (inSearchToken || inLimit) {
			mv.append(ch, start, length);
		}
	}

}
