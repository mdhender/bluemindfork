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

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.XProperty;

public class DefaultAlarmVEventDateTime implements IPropertyValue {

	private static final String X_WR_ALARMUID = "1DD32BED-6D6C-4DB1-96B9-CA0693BCB443";
	private static final String UID = "1DD32BED-6D6C-4DB1-96B9-CA0693BCB443";

	public static final QName NAME = new QName(NS.CALDAV, "default-alarm-vevent-datetime");
	private static final Logger logger = LoggerFactory.getLogger(DefaultAlarmVEventDateTime.class);

	private VAlarm alarm;

	@Override
	public QName getName() {
		return NAME;
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new DefaultAlarmVEventDateTime();
			}
		};
	}

	@Override
	public void appendValue(Element parent) {
		CDATASection cdata = parent.getOwnerDocument().createCDATASection(alarm.toString());
		parent.appendChild(cdata);
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		logger.debug("Default alarm on non-allday in BM ?");
		alarm = new VAlarm();
		PropertyList ap = alarm.getProperties();
		ap.add(new XProperty("X-WR-ALARMUID", X_WR_ALARMUID));
		ap.add(new Uid(UID));
		// TRIGGER;VALUE=DATE-TIME:19760401T005545Z
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.set(Calendar.YEAR, 1976);
		Trigger tg = new Trigger(new DateTime(cal.getTime()));
		ap.add(tg);
		Action action = new Action("NONE");
		ap.add(action);
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
