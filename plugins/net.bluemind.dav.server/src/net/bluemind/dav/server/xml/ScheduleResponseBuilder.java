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
import org.vertx.java.core.http.HttpServerResponse;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.netty.handler.codec.http.HttpHeaders.Names;
import net.bluemind.calendar.api.VFreebusy;
import net.bluemind.calendar.helper.ical4j.VFreebusyServiceHelper;
import net.bluemind.dav.server.DavActivator;
import net.bluemind.dav.server.proto.NS;

/**
 * @author tom
 * 
 */
public final class ScheduleResponseBuilder {

	private static final Logger logger = LoggerFactory.getLogger(ScheduleResponseBuilder.class);
	private final Element root;

	public ScheduleResponseBuilder() {
		try {
			Document doc = DOMUtils.createDocNS(NS.CALDAV, "cal:schedule-response");
			Element r = doc.getDocumentElement();
			r.setAttribute("xmlns:d", NS.WEBDAV);
			r.setAttribute("xmlns:cd", NS.CARDDAV);
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
	 * <code> <response> <recipient> <href xmlns=
	 * 'DAV:'>mailto:tcataldo@gmail.com</href> </recipient>
	 * <request-status>3.7;Invalid Calendar User</request-status> <error
	 * xmlns='DAV:'> <recipient-exists xmlns='urn:ietf:params:xml:ns:caldav'/>
	 * </error> <responsedescription xmlns='DAV:'>Unknown
	 * recipient</responsedescription> </response> </code>
	 */
	public void newUnknownRecipientResponse(String href) {
		Element re = DOMUtils.createElement(root, "cal:response");
		Element rece = DOMUtils.createElement(re, "cal:recipient");
		DOMUtils.createElementAndText(rece, "d:href", href);
		DOMUtils.createElementAndText(re, "cal:request-status", "3.7;Invalid Calendar User");
		Element errore = DOMUtils.createElement(re, "d:error");
		DOMUtils.createElement(errore, "cal:recipient-exists");
		DOMUtils.createElementAndText(re, "d:responsedescription", "Unknown recipient");
	}

	/**
	 * <code> <response> <recipient> <href xmlns=
	 * 'DAV:'>mailto:pierre@buffy.vmw</href> </recipient>
	 * <request-status>2.0;Success</request-status>
	 * <calendar-data><![CDATA[BEGIN:VCALENDAR VERSION:2.0 METHOD:REPLY
	 * PRODID:-//CALENDARSERVER.ORG//NONSGML Version 1//EN BEGIN:VFREEBUSY
	 * UID:493CD316-6B33-4231-8F4D-1B05088DE6AC DTSTART:20131213T230000Z
	 * DTEND:20131214T230000Z ATTENDEE:mailto:pierre@buffy.vmw
	 * DTSTAMP:20131214T180500Z
	 * FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20131213T230000Z/20131214T230000Z
	 * ORGANIZER:mailto:pierre@buffy.vmw END:VFREEBUSY END:VCALENDAR
	 * ]]></calendar-data> <responsedescription
	 * xmlns='DAV:'>OK</responsedescription> </response> </code>
	 * 
	 * @param href
	 * @param fb
	 */
	public void newResponse(String href, VFreebusy fb) {
		Element re = DOMUtils.createElement(root, "cal:response");
		Element rece = DOMUtils.createElement(re, "cal:recipient");
		DOMUtils.createElementAndText(rece, "d:href", href);
		DOMUtils.createElementAndText(re, "cal:request-status", "2.0;Success");
		Element cde = DOMUtils.createElement(re, "cal:calendar-data");
		CDATASection cdata = cde.getOwnerDocument().createCDATASection(toVfreeBusy(fb));
		cde.appendChild(cdata);
		DOMUtils.createElementAndText(re, "d:responsedescription", "OK");
	}

	private String toVfreeBusy(VFreebusy fb) {
		return VFreebusyServiceHelper.convertToFreebusyString(fb);
	}

	public void sendAs(HttpServerResponse sr) {
		try {
			if (DavActivator.devMode) {
				DOMUtils.logDom(root.getOwnerDocument());
			}
			sr.headers().set(Names.CONTENT_TYPE, "application/xml; charset=\"utf-8\"");
			String dom = DOMUtils.asString(root.getOwnerDocument());
			sr.setStatusCode(200).end(dom);
			logger.info("[{}][{}Chars] schedule-response sent.\n\n\n", Thread.currentThread().getName(), dom.length());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
