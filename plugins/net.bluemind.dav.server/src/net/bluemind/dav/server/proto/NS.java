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
package net.bluemind.dav.server.proto;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public final class NS {

	public static final String WEBDAV = "DAV:";
	public static final String CALDAV = "urn:ietf:params:xml:ns:caldav";
	public static final String CARDDAV = "urn:ietf:params:xml:ns:carddav";
	public static final String CSRV_ORG = "http://calendarserver.org/ns/";
	public static final String APPLE_ICAL = "http://apple.com/ns/ical/";
	public static final String ME_COM = "http://me.com/_namespace/";

	private static final Map<String, String> nsPrefix;

	static {
		HashMap<String, String> nsPrefixes = Maps.newHashMap();
		nsPrefixes.put(WEBDAV, "d");
		nsPrefixes.put(CALDAV, "cal");
		nsPrefixes.put(CARDDAV, "rd");
		nsPrefixes.put(CSRV_ORG, "cso");
		nsPrefixes.put(APPLE_ICAL, "aic");
		nsPrefixes.put(ME_COM, "me");
		nsPrefix = ImmutableMap.copyOf(nsPrefixes);
	}

	public static final String prefix(String namespaceUri) {
		return nsPrefix.get(namespaceUri);
	}

	public static final String element(QName qn) {
		StringBuilder sb = new StringBuilder(64);
		return sb.append(prefix(qn.getNamespaceURI())).append(':').append(qn.getLocalPart()).toString();
	}

	public static final String element(String ns, String local) {
		StringBuilder sb = new StringBuilder(64);
		return sb.append(prefix(ns)).append(':').append(local).toString();
	}
}
