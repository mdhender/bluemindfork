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

package net.bluemind.dav.server.ics;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VFreebusyQuery;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.parser.ICal4jHelper;
import net.bluemind.lib.ical4j.data.CalendarBuilder;
import net.bluemind.lib.ical4j.util.IcalConverter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VFreeBusy;
import net.fortuna.ical4j.model.property.Attendee;

public class FreeBusy {

	private static final Logger logger = LoggerFactory.getLogger(FreeBusy.class);

	public static class CalRequest {
		/**
		 * @param calUid
		 * @param range
		 */
		public CalRequest(String calUid, VFreebusyQuery range) {
			super();
			this.calUid = calUid;
			this.range = range;
		}

		public String calUid;
		public VFreebusyQuery range;

	}

	public static List<CalRequest> parseRequests(byte[] ics, LoggedCore lc) {
		logger.info("[{}] Parse cal requests:\n{}", lc.getUser().value.login, new String(ics));

		List<CalRequest> requests = new ArrayList<>();
		try (Reader reader = new InputStreamReader(new ByteArrayInputStream(ics));
				UnfoldingReader unfoldingReader = new UnfoldingReader(reader, true)) {

			new CalendarBuilder().build(unfoldingReader, ((cal, component) -> {
				if (Component.VFREEBUSY.equals(component.getName())) {
					VFreeBusy fb = (VFreeBusy) component;

					BmDateTime rangeStart = IcalConverter.convertToDateTime(fb.getStartDate(), Optional.empty(),
							Collections.emptyMap());
					BmDateTime rangeEnd = IcalConverter.convertToDateTime(fb.getEndDate(), Optional.empty(),
							Collections.emptyMap());
					VFreebusyQuery range = VFreebusyQuery.create(rangeStart, rangeEnd);

					PropertyList<Property> attendess = fb.getProperties(Attendee.ATTENDEE);
					attendess.forEach(attendeeProp -> {
						net.fortuna.ical4j.model.property.Attendee attendee = (net.fortuna.ical4j.model.property.Attendee) attendeeProp;
						try {
							String mailto = attendee.getCalAddress().toURL().getPath().toLowerCase()
									.replace(ICal4jHelper.MAIL_TO, "");
							mailtoToCalRequest(lc, mailto, range).ifPresent(r -> requests.add(r));
						} catch (MalformedURLException e) {
							logger.warn("Cannot parse attendee value {}", attendee.toString(), e);
						}
					});
				}
			}));

		} catch (IOException | ParserException e) {
			logger.warn("Cannot read freebusy ICS of user {}", lc.getUser().value.login, e);
		}

		return requests;
	}

	private static Optional<CalRequest> mailtoToCalRequest(LoggedCore lc, String mailto, VFreebusyQuery range) {
		String domain = lc.getDomain();
		DirEntry byEmail = lc.getCore().instance(IDirectory.class, domain).getByEmail(mailto);
		return Optional.ofNullable(byEmail).map(dirEntry -> {
			if (dirEntry.kind == Kind.RESOURCE) {
				return new CalRequest(ICalendarUids.resourceCalendar(byEmail.entryUid), range);
			} else {
				return new CalRequest(ICalendarUids.defaultUserCalendar(byEmail.entryUid), range);
			}
		});
	}

}
