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

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

import com.google.common.base.Splitter;

import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.dav.server.ics.ICS;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.WeekDayList;
import net.fortuna.ical4j.model.component.Available;
import net.fortuna.ical4j.model.component.VAvailability;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Uid;

/**
 * User's working hours.
 */
public class CalendarAvailability implements IPropertyValue {
	public static final QName NAME = new QName(NS.CSRV_ORG, "calendar-availability");
	private static final Logger logger = LoggerFactory.getLogger(CalendarAvailability.class);
	private String ics;

	@Override
	public QName getName() {
		return NAME;
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new CalendarAvailability();
			}
		};
	}

	@Override
	public void appendValue(Element parent) {
		CDATASection cdata = parent.getOwnerDocument().createCDATASection(ics);
		parent.appendChild(cdata);
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		Map<String, String> prefs = lc.getPrefs();
		double startTime = Double.parseDouble(prefs.get("work_hours_start"));
		int startHour = (int) startTime; // 8
		int startMinute = (int) ((startTime - startHour) * 60); // 8

		double endTime = Double.parseDouble(prefs.get("work_hours_end"));
		int endHour = (int) Double.parseDouble(prefs.get("work_hours_end")); // 18
		int endMinute = (int) ((endTime - endHour) * 60);
		String workDays = prefs.get("working_days"); // mon,tue,wed,thu,fri
		String first = Splitter.on(',').split(workDays).iterator().next();
		String tz = prefs.get("timezone"); // Europe/Paris
		if (tz == null) {
			tz = "Europe/Paris";
		}

		java.util.Calendar jcal = java.util.Calendar.getInstance(TimeZone.getTimeZone(tz), Locale.ENGLISH);
		jcal.add(java.util.Calendar.YEAR, -1);
		jcal.add(java.util.Calendar.MONTH, -1);
		jcal.set(java.util.Calendar.DAY_OF_WEEK, dayAsField(first));
		jcal.set(java.util.Calendar.HOUR_OF_DAY, startHour);
		jcal.set(java.util.Calendar.MINUTE, startMinute);
		jcal.set(java.util.Calendar.SECOND, 0);
		Date tzStart = jcal.getTime();
		jcal.set(java.util.Calendar.HOUR_OF_DAY, endHour);
		jcal.set(java.util.Calendar.MINUTE, endMinute);
		Date tzEnd = jcal.getTime();
		logger.info("start: {}, end: {}", tzStart, tzEnd);

		Calendar cal = VEventServiceHelper.initCalendar();
		net.fortuna.ical4j.model.TimeZone tz4j = ICS.getTimeZone(tz);
		VTimeZone vtz = ICS.getVTimeZone(tz);
		cal.getComponents().add(vtz);

		VAvailability va = new VAvailability();
		PropertyList vaProps = va.getProperties();
		vaProps.add(new Uid(UUID.randomUUID().toString()));
		Available avail = new Available();
		va.getAvailable().add(avail);
		PropertyList props = avail.getProperties();
		props.add(new Uid(UUID.randomUUID().toString()));
		DtStamp stamp = new DtStamp();
		props.add(stamp);

		DtStart dtStart = new DtStart(new DateTime(tzStart));
		dtStart.setTimeZone(tz4j);
		DtEnd dtEnd = new DtEnd(new DateTime(tzEnd));
		dtEnd.setTimeZone(tz4j);
		props.add(dtStart);
		props.add(dtEnd);
		Recur recur = new Recur(Recur.WEEKLY, null);
		WeekDayList dl = recur.getDayList();
		for (String day : Splitter.on(',').split(workDays)) {
			dl.add(dayAsWeekday(day));
		}
		props.add(new RRule(recur));
		cal.getComponents().add(va);

		this.ics = cal.toString();
	}

	@Override
	public void expand(LoggedCore lc, DavResource dr, List<Property> scope) throws Exception {
		logger.info("expand");
	}

	private int dayAsField(String day) {
		switch (day) {
		case "tue":
			return java.util.Calendar.TUESDAY;
		case "wed":
			return java.util.Calendar.WEDNESDAY;
		case "thu":
			return java.util.Calendar.THURSDAY;
		case "fri":
			return java.util.Calendar.FRIDAY;
		case "sat":
			return java.util.Calendar.SATURDAY;
		case "sun":
			return java.util.Calendar.SUNDAY;
		default:
		case "mon":
			return java.util.Calendar.MONDAY;
		}
	}

	private WeekDay dayAsWeekday(String day) {
		switch (day) {
		case "tue":
			return WeekDay.TU;
		case "wed":
			return WeekDay.WE;
		case "thu":
			return WeekDay.TH;
		case "fri":
			return WeekDay.FR;
		case "sat":
			return WeekDay.SA;
		case "sun":
			return WeekDay.SU;
		default:
		case "mon":
			return WeekDay.MO;
		}
	}

	@Override
	public void set(LoggedCore lc, DavResource dr, Element value) throws Exception {
		logger.info("[{}] set on {}", dr.getResType(), dr.getPath());
	}

}
