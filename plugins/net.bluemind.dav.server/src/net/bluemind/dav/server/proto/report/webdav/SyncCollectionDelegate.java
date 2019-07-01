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

import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableList;

import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.QN;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.proto.report.ReportSaxDelegate;
import net.bluemind.dav.server.store.DavResource;

/**
 * <code>
 * <?xml version="1.0" encoding="UTF-8"?>
 * <A:sync-collection xmlns:A="DAV:">
 *   <A:sync-token>data:311718971_1386277360546936000</A:sync-token>
 *   <A:prop>
 *     <B:notificationtype xmlns:B="http://calendarserver.org/ns/"/>
 *     <A:getetag/>
 *   </A:prop>
 * </A:sync-collection>
 * </code>
 * 
 */
public final class SyncCollectionDelegate extends ReportSaxDelegate {

	private static final Logger logger = LoggerFactory.getLogger(SyncCollectionDelegate.class);
	private static final QName root = WDReports.SYNC_COLLECTION;

	private String syncToken;
	private List<QName> props;
	private boolean onToken;
	private boolean onProps;
	private StringBuilder sb;

	public SyncCollectionDelegate() {
		this.props = new LinkedList<>();
	}

	@Override
	public QName getRoot() {
		return root;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if ("sync-token".equals(localName) && NS.WEBDAV.equals(uri)) {
			onToken = true;
			this.sb = new StringBuilder(32);
		} else if ("prop".equals(localName) && NS.WEBDAV.equals(uri)) {
			onProps = true;
		} else if (onProps) {
			props.add(QN.qn(uri, localName));
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("sync-token".equals(localName) && NS.WEBDAV.equals(uri)) {
			onToken = false;
			syncToken = sb.toString();
			if (sb.length() == 0) {
				logger.info("================= INITIAL_SYNC ===================");
			}
			sb = null;
			logger.info("Got token: '{}'", syncToken);
		} else if ("prop".equals(localName) && NS.WEBDAV.equals(uri)) {
			onProps = false;
			logger.info("{} props required.", props.size());
		}
	}

	@Override
	public ReportQuery endDocument(DavResource path) throws SAXException {
		SyncCollectionQuery scq = new SyncCollectionQuery(path, root);
		scq.setSyncToken(syncToken);
		scq.setProps(ImmutableList.copyOf(props));
		return scq;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (onToken) {
			sb.append(ch, start, length);
		}
	}

}
