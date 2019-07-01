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
package net.bluemind.dav.server.proto.props.webdav;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.proto.report.caldav.CDReports;
import net.bluemind.dav.server.proto.report.calendarserver.CSReports;
import net.bluemind.dav.server.proto.report.carddav.RDReports;
import net.bluemind.dav.server.proto.report.webdav.WDReports;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;
import net.bluemind.dav.server.store.ResType;
import net.bluemind.dav.server.xml.DOMUtils;

public class SupportedReportSet implements IPropertyValue {
	private static final Logger logger = LoggerFactory.getLogger(SupportedReportSet.class);

	public static final QName NAME = new QName(NS.WEBDAV, "supported-report-set");

	private Set<QName> supportedReports;

	@Override
	public QName getName() {
		return NAME;
	}

	@Override
	public void appendValue(Element parent) {
		for (QName qn : supportedReports) {
			Element sre = DOMUtils.createElement(parent, "d:supported-report");
			Element re = DOMUtils.createElement(sre, "d:report");
			DOMUtils.createElement(re, qn.getPrefix() + ":" + qn.getLocalPart());
		}
	}

	public static final IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new SupportedReportSet();
			}
		};
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		supportedReports = new HashSet<>();
		ResType rt = dr.getResType();
		if (rt == ResType.PRINCIPAL) {
			supportedReports.add(WDReports.ACL_PRINCIPAL_PROP_SET);
			supportedReports.add(WDReports.PRINCIPAL_MATCH);
			supportedReports.add(WDReports.PRINCIPAL_PROPERTY_SEARCH);
			supportedReports.add(WDReports.EXPAND_PROPERTY);
			supportedReports.add(CSReports.CALENDARSERVER_PRINCIPAL_SEARCH);
		} else if (rt.isCalChild() || rt == ResType.CALENDAR) {
			supportedReports.add(WDReports.ACL_PRINCIPAL_PROP_SET);
			supportedReports.add(WDReports.PRINCIPAL_MATCH);
			supportedReports.add(WDReports.PRINCIPAL_PROPERTY_SEARCH);
			supportedReports.add(WDReports.EXPAND_PROPERTY);
			supportedReports.add(WDReports.SYNC_COLLECTION);
			supportedReports.add(CSReports.CALENDARSERVER_PRINCIPAL_SEARCH);
			supportedReports.add(CDReports.CALENDAR_QUERY);
			supportedReports.add(CDReports.CALENDAR_MULTIGET);
			supportedReports.add(CDReports.FREE_BUSY_QUERY);
		} else if (rt == ResType.ADDRESSBOOK || rt == ResType.VCARDS_CONTAINER) {
			supportedReports.add(WDReports.ACL_PRINCIPAL_PROP_SET);
			supportedReports.add(WDReports.PRINCIPAL_MATCH);
			supportedReports.add(WDReports.PRINCIPAL_PROPERTY_SEARCH);
			supportedReports.add(WDReports.EXPAND_PROPERTY);
			supportedReports.add(WDReports.SYNC_COLLECTION);
			supportedReports.add(RDReports.ADDRESSBOOK_QUERY);
			supportedReports.add(RDReports.ADDRESSBOOK_MULTIGET);

		} else {
			String txt = "need supportedReports for " + dr.getPath();
			logger.error(txt);
			throw new Exception(txt);
		}
	}

	@Override
	public void expand(LoggedCore lc, DavResource dr, List<Property> scope) throws Exception {
		logger.info("expand");
	}

	@Override
	public void set(LoggedCore lc, DavResource dr, Element value) throws Exception {
		logger.info("[{}] set on {}", dr.getResType(), dr.getPath());
	}

}
