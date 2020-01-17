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
package net.bluemind.dav.server.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.netty.handler.codec.http.HttpHeaders.Names;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.dav.server.DavActivator;
import net.bluemind.dav.server.proto.NS;

public final class MultiStatusBuilder {

	private static final Logger logger = LoggerFactory.getLogger(MultiStatusBuilder.class);
	private final Element root;

	public MultiStatusBuilder() {
		try {
			Document doc = DOMUtils.createDocNS(NS.WEBDAV, "d:multistatus");
			Element r = doc.getDocumentElement();
			r.setAttribute("xmlns:cal", NS.CALDAV);
			r.setAttribute("xmlns:rd", NS.CARDDAV);
			r.setAttribute("xmlns:cso", NS.CSRV_ORG);
			r.setAttribute("xmlns:aic", NS.APPLE_ICAL);
			r.setAttribute("xmlns:me", NS.ME_COM);
			this.root = r;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Element root() {
		return root;
	}

	/**
	 * <code> <response xmlns='DAV:'>
	 * <href>/principals/__uids__/4B13E815-707E-4C3F-9B4C-46EB864CD8F4/</href>
	 * <propstat> <prop> ..... </prop> <status>HTTP/1.1 200 OK</status>
	 * </propstat> </response> </code>
	 * 
	 * @return the "DAV:prop" element
	 */
	public Element newResponse(String href, int status) {
		return newResponse(root, href, status);
	}

	public Element newResponse(Element parent, String href, int statusCode) {
		Element re = DOMUtils.createElement(parent, "d:response");
		DOMUtils.createElementAndText(re, "d:href", href);
		String status = "HTTP/1.1 200 OK";
		if (statusCode != 200) {
			status = "HTTP/1.1 " + statusCode + " Not200";
		}
		if (statusCode == 404) {
			DOMUtils.createElementAndText(re, "d:status", status);
			return null;
		} else {
			Element pse = DOMUtils.createElement(re, "d:propstat");
			DOMUtils.createElementAndText(pse, "d:status", status);
			Element aprop = DOMUtils.createElement(pse, "d:prop");
			return aprop;
		}
	}

	public void sendAs(HttpServerResponse sr) {
		sendAs(sr, DavActivator.devMode);
	}

	public void sendAs(HttpServerResponse sr, boolean dumpSent) {
		try {
			String dump = "";
			sr.headers().set(Names.CONTENT_TYPE, "application/xml; charset=\"utf-8\"");
			String dom = DOMUtils.asString(root.getOwnerDocument());
			sr.setStatusCode(207).end(dom);
			if (dumpSent) {
				dump = DOMUtils.asPrettyString(root.getOwnerDocument());
			}
			logger.info("[{}Chars] multistatus sent [{}].\n\n\n", dom.length(), dump);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
