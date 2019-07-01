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
package net.bluemind.dav.server.proto.props.caldav;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.dav.server.ics.ICS;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VTimeZone;

public class CalendarTimezone implements IPropertyValue {
	public static final QName NAME = new QName(NS.CALDAV, "calendar-timezone");
	private static final Logger logger = LoggerFactory.getLogger(CalendarTimezone.class);
	private Calendar cal;

	@Override
	public QName getName() {
		return NAME;
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new CalendarTimezone();
			}
		};
	}

	@Override
	public void appendValue(Element parent) {
		CDATASection cdata = parent.getOwnerDocument().createCDATASection(cal.toString());
		parent.appendChild(cdata);
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		// logger.info("fetch");
		Map<String, String> prefs = lc.getPrefs();
		// for (String k : prefs.keySet()) {
		// logger.info(" * {} => {}", k, prefs.get(k));
		// }
		String tz = prefs.get("timezone");
		if (tz == null) {
			tz = "Europe/Paris";
		}
		this.cal = VEventServiceHelper.initCalendar();
		VTimeZone vtz = ICS.getVTimeZone(tz);
		cal.getComponents().add(vtz);
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
