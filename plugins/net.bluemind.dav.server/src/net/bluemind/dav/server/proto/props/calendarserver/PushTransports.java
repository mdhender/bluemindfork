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
package net.bluemind.dav.server.proto.props.calendarserver;

import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import net.bluemind.dav.server.Proxy;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.push.xmpp.Xmpp;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Path;
import net.bluemind.dav.server.store.Property;
import net.bluemind.dav.server.xml.DOMUtils;

public class PushTransports implements IPropertyValue {
	public static final QName NAME = new QName(NS.CSRV_ORG, "push-transports");

	private static final Logger logger = LoggerFactory.getLogger(PushTransports.class);
	private String xmppServer;
	private String xmppUri;
	private String subUrl;
	private String bundleId;

	@Override
	public QName getName() {
		return NAME;
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new PushTransports();
			}
		};
	}

	@Override
	public void appendValue(Element parent) {
		Element transport = DOMUtils.createElement(parent, "cso:transport");
		transport.setAttribute("type", "XMPP");
		DOMUtils.createElementAndText(transport, "cso:xmpp-server", xmppServer);
		DOMUtils.createElementAndText(transport, "cso:xmpp-uri", xmppUri);
		DOMUtils.createElementAndText(transport, "cso:refresh-interval", "172800");

		logger.debug("subUrl: " + subUrl + ", " + bundleId);
		// transport = DOMUtils.createElement(parent, "cso:transport");
		// transport.setAttribute("type", "APSD");
		// Element subUrlE = DOMUtils.createElement(transport,
		// "cso:subscription-url");
		// DOMUtils.createElementAndText(subUrlE, "d:href", subUrl);
		// DOMUtils.createElementAndText(transport, "cso:apsbundleid",
		// bundleId);
		// DOMUtils.createElementAndText(transport, "cso:env", "PRODUCTION");
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		this.xmppServer = Xmpp.server();
		this.xmppUri = Xmpp.pubsubUri(lc, dr);

		this.subUrl = "https://" + Path.getExtUrl() + "/" + Proxy.path + "/apns";
		// this.bundleId = "net.bluemind.dav.server." + ApnsKey.uuid();
		this.bundleId = "com.apple.calendar.XServer.9c9741b4-3c92-45fb-9882-208f5ee9bb42";
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
