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
import java.util.Stack;

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
import net.bluemind.dav.server.store.Property;

/**
 * <code>
 * <?xml version="1.0" encoding="UTF-8"?>
 * <A:expand-property xmlns:A="DAV:">
 *  <A:property name="calendar-proxy-write-for" namespace="http://calendarserver.org/ns/">
 *     <A:property name="displayname" namespace="DAV:"/>
 *     <A:property name="calendar-user-address-set" namespace="urn:ietf:params:xml:ns:caldav"/>
 *     <A:property name="email-address-set" namespace="http://calendarserver.org/ns/"/>
 *   </A:property>
 *   <A:property name="calendar-proxy-read-for" namespace="http://calendarserver.org/ns/">
 *     <A:property name="displayname" namespace="DAV:"/>
 *     <A:property name="calendar-user-address-set" namespace="urn:ietf:params:xml:ns:caldav"/>
 *     <A:property name="email-address-set" namespace="http://calendarserver.org/ns/"/>
 *   </A:property>
 * </A:expand-property>
 * </code>
 * 
 */
public class ExpandPropertyDelegate extends ReportSaxDelegate {

	private static final Logger logger = LoggerFactory.getLogger(ExpandPropertyDelegate.class);
	private static final QName root = new QName(NS.WEBDAV, "expand-property");

	private Stack<Property> props;
	private List<Property> propRoots;

	public ExpandPropertyDelegate() {
		props = new Stack<>();
		propRoots = new LinkedList<>();
	}

	@Override
	public QName getRoot() {
		return root;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if ("property".equals(localName) && NS.WEBDAV.equals(uri)) {
			Property prop = new Property();
			prop.setQName(QN.qn(attributes.getValue("namespace"), attributes.getValue("name")));
			if (!props.isEmpty()) {
				props.peek().addChild(prop);
			}
			props.add(prop);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("property".equals(localName) && NS.WEBDAV.equals(uri)) {
			Property prop = props.pop();
			if (props.isEmpty()) {
				propRoots.add(prop);
				logger.info("Adding root prop with " + prop.getChildren().size() + " children.");
			}
		}
	}

	@Override
	public ReportQuery endDocument(DavResource path) throws SAXException {
		ExpandPropertyQuery epq = new ExpandPropertyQuery(path, root);
		logger.info("query for " + propRoots.size() + " property root(s).");
		epq.setProperties(propRoots);
		return epq;
	}

}
