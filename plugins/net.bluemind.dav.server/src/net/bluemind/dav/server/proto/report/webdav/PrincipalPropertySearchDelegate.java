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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.proto.report.ReportSaxDelegate;
import net.bluemind.dav.server.store.DavResource;

public class PrincipalPropertySearchDelegate extends ReportSaxDelegate {

	private static final Logger logger = LoggerFactory.getLogger(PrincipalPropertySearchDelegate.class);
	private static final QName root = new QName(NS.WEBDAV, "principal-property-search");

	private StringBuilder mv;
	private String curValue;
	private QName prop;
	private MatchStyle curStyle;
	private boolean inPropertySearch;
	private boolean inMatch;
	private boolean inProp;

	private List<PropMatch> matches;
	private List<QName> results;

	public PrincipalPropertySearchDelegate() {
		this.mv = new StringBuilder(64);
		matches = new ArrayList<>(6);
		results = new ArrayList<>(10);
	}

	@Override
	public QName getRoot() {
		return root;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (NS.WEBDAV.equals(uri)) {
			if ("property-search".equals(localName)) {
				inPropertySearch = true;
			} else if ("prop".equals(localName)) {
				inProp = true;
			} else if ("match".equals(localName)) {
				inMatch = true;
				mv.setLength(0);
				switch (attributes.getValue("match-type")) {
				default:
				case "starts-with":
					curStyle = MatchStyle.STARTS_WITH;
					break;
				}
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (inProp && !localName.equals("prop")) {
			prop = new QName(uri, localName);
			if (!inPropertySearch) {
				results.add(prop);
			}
		}
		if (inMatch) {
			curValue = mv.toString();
		}

		if (NS.WEBDAV.equals(uri)) {
			if ("property-search".equals(localName)) {
				inPropertySearch = false;
				PropMatch pm = new PropMatch();
				pm.setProp(prop);
				pm.setStyle(curStyle);
				pm.setValue(curValue);
				matches.add(pm);
			} else if ("prop".equals(localName)) {
				inProp = false;
			} else if ("match".equals(localName)) {
				inMatch = false;
			}
		}
	}

	@Override
	public ReportQuery endDocument(DavResource path) throws SAXException {
		PrincipalPropertySearchQuery ppsq = new PrincipalPropertySearchQuery(path, root);
		ppsq.setExpectedResults(results);
		ppsq.setMatches(matches);
		logger.info("match on " + matches.size() + " props, return " + results.size() + " props.");
		return ppsq;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (inMatch) {
			mv.append(ch, start, length);
		}
	}

}
