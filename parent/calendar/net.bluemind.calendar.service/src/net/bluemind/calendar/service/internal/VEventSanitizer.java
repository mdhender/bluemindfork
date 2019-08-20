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
package net.bluemind.calendar.service.internal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.tag.service.TagsSanitizer;

public class VEventSanitizer {

	private static Logger logger = LoggerFactory.getLogger(VEventSanitizer.class);

	private TagsSanitizer tagsSanitizer;
	private String domainUid;

	private BmContext context;

	public VEventSanitizer(BmContext ctx, String domainUid) throws ServerFault {
		this.domainUid = domainUid;
		this.context = ctx;
		this.tagsSanitizer = new TagsSanitizer(ctx);
	}

	public void sanitize(VEventSeries vevent) throws ServerFault {
		if (null != vevent.main) {
			sanitize(vevent.main);
		}
		if (null == vevent.occurrences) {
			vevent.occurrences = Collections.emptyList();
		}
		vevent.occurrences.forEach(this::sanitize);
	}

	public void sanitize(VEvent vevent) throws ServerFault {

		// 3.8.1.9. Priority
		// This priority is specified as an integer in the range 0
		// to 9.
		if (vevent.priority != null) {
			if (vevent.priority < 0) {
				vevent.priority = 0;
			} else if (vevent.priority > 9) {
				vevent.priority = 9;
			}
		}

		// Fix Transparency BJR(48)
		if (vevent.transparency == null) {
			if (vevent.allDay()) {
				vevent.transparency = VEvent.Transparency.Transparent;
			} else {
				vevent.transparency = VEvent.Transparency.Opaque;
			}
		}

		resolveOrganizer(vevent.organizer);

		if (vevent.organizer != null) {

			// BM-7020 Organizer without email cannot create meeting
			if (StringUtils.isBlank(vevent.organizer.mailto)) {
				vevent.attendees = Collections.emptyList();
				vevent.organizer = null;
			} else {
				vevent.attendees = resolveAttendees(vevent.attendees);
				for (VEvent.Attendee attendee : vevent.attendees) {
					if (attendee.partStatus == null) {
						attendee.partStatus = ParticipationStatus.NeedsAction;
					}

					if (attendee.role == null) {
						attendee.role = Role.RequiredParticipant;
					}

					if (StringUtils.isNotBlank(attendee.mailto) && !Regex.EMAIL.validate(attendee.mailto)) {
						attendee.mailto = null;
					}
				}

				if (vevent.organizer.dir != null) {
					// internal organizer, remove from attendees
					vevent.attendees.removeIf(a -> vevent.organizer.dir.equals(a.dir));
				}
			}
		} else {
			vevent.attendees = Collections.emptyList();
		}

		// RFC 5545: 3.8.4.3. Organizer : This property MUST NOT be specified in
		// [...] calendar components that are not group-scheduled components,
		// but are components only on a single user's calendar.
		if (!vevent.exception() && vevent.attendees.size() == 0) {
			vevent.organizer = null;
		}

		// TODO BJR50 ?

		/*
		 * 3.6.1. Event Component For cases where a "VEVENT" calendar component
		 * specifies a "DTSTART" property with a DATE value type but no "DTEND" nor
		 * "DURATION" property, the event's duration is taken to be one day. For cases
		 * where a "VEVENT" calendar component specifies a "DTSTART" property with a
		 * DATE-TIME value type but no "DTEND" property, the event ends on the same
		 * calendar date and time of day specified by the "DTSTART" property.
		 */

		if (vevent.dtend == null) {
			if (vevent.allDay()) {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(new BmDateTimeWrapper(vevent.dtstart).toUTCTimestamp());
				cal.add(Calendar.DATE, 1);
				vevent.dtend = BmDateTimeWrapper.fromTimestamp(cal.getTimeInMillis(), vevent.timezone());
			} else {
				vevent.dtend = vevent.dtstart;
			}
		}

		// BM-7729
		if (vevent.allDay()) {
			long start = new BmDateTimeWrapper(vevent.dtstart).toUTCTimestamp();
			long end = new BmDateTimeWrapper(vevent.dtend).toUTCTimestamp();
			if (start == end) {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(end);
				cal.add(Calendar.DATE, 1);
				vevent.dtend = BmDateTimeWrapper.fromTimestamp(cal.getTimeInMillis(), null, Precision.Date);
			}
		}

		// cleanup alarm
		if (vevent.alarm != null) {
			ArrayList<VAlarm> cl = new ArrayList<>(vevent.alarm.size());

			for (VAlarm al : vevent.alarm) {
				if (al == null) {
					continue;
				}
				if (al.trigger == null) {
					// trigger is required
					continue;
				}

				cl.add(al);
			}
			vevent.alarm = cl;
		}

		// sanitize exdate, rrule
		if (vevent.rrule == null) {
			if (vevent.exdate != null && !vevent.exdate.isEmpty()) {
				vevent.exdate = null;
			}
		} else {
			vevent.rrule.bySecond = filterNull(vevent.rrule.bySecond);
			vevent.rrule.byMinute = filterNull(vevent.rrule.byMinute);
			vevent.rrule.byHour = filterNull(vevent.rrule.byHour);
			vevent.rrule.byMonthDay = filterNull(vevent.rrule.byMonthDay);
			vevent.rrule.byYearDay = filterNull(vevent.rrule.byYearDay);
			vevent.rrule.byWeekNo = filterNull(vevent.rrule.byWeekNo);
			vevent.rrule.byMonth = filterNull(vevent.rrule.byMonth);
		}

		tagsSanitizer.sanitize(vevent.categories);

		if (vevent.description != null && !vevent.description.isEmpty()) {
			vevent.description = removeOuterMarkups(vevent.description);
		}

	}

	private String removeOuterMarkups(String html) {
		// FIXME use neko or an unreadable regexp here
		String ret = html;
		String lower = html.toLowerCase();
		int bodyStart = lower.indexOf("<body");
		if (bodyStart >= 0) {
			int bodyTagEnd = lower.indexOf('>', bodyStart + 4);
			int endOfBody = lower.indexOf("</body>", bodyTagEnd);
			if (bodyTagEnd > 0 && endOfBody > 0) {
				ret = html.substring(bodyTagEnd + 1, endOfBody);
			}
		}
		return ret;
	}

	private <T> List<T> filterNull(List<T> l) {
		if (l == null) {
			return null;
		} else {
			return l.stream().filter(o -> o != null).collect(Collectors.toList());
		}
	}

	public void resolveAttendeesAndOrganizer(VEventSeries vevent) throws ServerFault {
		if (vevent.main != null) {
			resolveOrganizer(vevent.main.organizer);
			vevent.main.attendees = resolveAttendees(vevent.main.attendees);
		}
		vevent.occurrences.forEach(evt -> {
			resolveOrganizer(evt.organizer);
			evt.attendees = resolveAttendees(evt.attendees);
		});
	}

	/**
	 * @param organizer
	 * @throws ServerFault
	 */
	private void resolveOrganizer(VEvent.Organizer organizer) throws ServerFault {
		if (organizer == null) {
			return;
		}

		if (StringUtils.isNotEmpty(organizer.mailto)) {
			if (!Regex.EMAIL.validate(organizer.mailto)) {
				organizer.mailto = null;
			}
		}

		DirEntry dirEntry = resolve(organizer.dir, organizer.mailto);
		if (dirEntry != null) {
			organizer.dir = "bm://" + dirEntry.path;
			organizer.mailto = dirEntry.email;
			organizer.commonName = dirEntry.displayName;
		} else {
			organizer.dir = null;
			if (organizer.commonName == null) {
				organizer.commonName = organizer.mailto;
			}
		}

	}

	/**
	 * @param attendees
	 * @throws ServerFault
	 */
	private List<VEvent.Attendee> resolveAttendees(List<VEvent.Attendee> attendees) throws ServerFault {
		if (attendees == null) {
			return Collections.emptyList();
		}

		Set<VEvent.Attendee> ret = new LinkedHashSet<VEvent.Attendee>(attendees.size() * 2);

		for (VEvent.Attendee attendee : attendees) {

			if (attendee.commonName == null) {
				attendee.commonName = attendee.mailto;
			}

			if (StringUtils.isNotEmpty(attendee.mailto) && !Regex.EMAIL.validate(attendee.mailto)) {
				attendee.mailto = null;
			}
			DirEntry dir = resolve(attendee.dir, attendee.mailto);
			if (dir != null) {
				attendee.dir = "bm://" + dir.path;
				attendee.commonName = dir.displayName;
				attendee.mailto = dir.email;
				attendee.internal = true;
				ret.add(attendee);
			} else {
				attendee.dir = null;
				attendee.internal = false;
				ret.add(attendee);
			}
		}

		ArrayList<Attendee> list = new ArrayList<VEvent.Attendee>(ret.size());
		list.addAll(ret);

		return list;
	}

	private DirEntry resolve(String dir, String mailto) throws ServerFault {
		if (dir != null && dir.startsWith("bm://")) {
			return directory().getEntry(dir.substring("bm://".length()));
		}

		if (mailto != null) {
			return directory().getByEmail(mailto);
		}

		return null;
	}

	private IDirectory _dir;

	private IDirectory directory() throws ServerFault {
		if (_dir == null) {
			_dir = context.provider().instance(IDirectory.class, domainUid);
		}

		return _dir;
	}
}
