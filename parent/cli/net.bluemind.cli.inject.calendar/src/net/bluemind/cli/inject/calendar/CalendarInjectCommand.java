/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.inject.calendar;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "calendar", description = "Injects a batch of calendar events")
public class CalendarInjectCommand extends SingleOrDomainOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("inject");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return CalendarInjectCommand.class;
		}
	}
	@Option(names = { "--start" }, required = true, description = "date of the first event, format: 'yyyy-MM-dd'")
	public String start;
	
	@Option(names = { "--end" }, required = true, description = "date of the last event, format: 'yyyy-MM-dd'")
	public String end;
	
	@Option(names = { "--count" }, required = false, description = "number of event per days, starting from 9am, then one each hours (default: 4)")
	public int count = 4;
	
	@Option(names = { "--weekday" }, required = false, description = "only weekday (default: true, ie exclude saturday & sunday)")
	public boolean weekday = false;
	
	@Option(names = { "--every" }, required = false, description = "minutes between events, for same day events (default: 60)")
	public int every = 60;
	
	@Option(names = { "--duration" }, required = false, description = "event duration in minutes (default: 60)")
	public int duration = 60;
	
	@Option(names = { "--starting-hour" }, required = false, description = "hour of the first event of each day (default: 9)")
	public int startingHour = 9;

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) throws Exception {
		ctx.info("Injecting calendar events");

		ICalendar calApi = ctx.adminApi().instance(ICalendar.class, ICalendarUids.defaultUserCalendar(de.uid));

		ZoneId tz = ZoneId.of("Europe/Paris");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = formatter.parse(start);
		Date endDate = formatter.parse(end); 
		
		ZonedDateTime start = startDate.toInstant().atZone(tz);
		ZonedDateTime end = endDate.toInstant().atZone(tz);
		for (ZonedDateTime date = start; date.isBefore(end); date = date.plusDays(1)) {
			if (weekday && (date.getDayOfWeek().equals(DayOfWeek.SATURDAY) || date.getDayOfWeek().equals(DayOfWeek.SUNDAY))) {
				continue;
			}
			ctx.info("Creating event at " + date);
			ZonedDateTime eventDate = date.withHour(startingHour);
			for (int i = 0; i < count; i++) {
				eventDate = eventDate.plusMinutes(every);
				VEventSeries event = defaultVEvent(eventDate, duration);
				String uid = "test_" + System.nanoTime();
				calApi.create(uid, event, false);
			}
		}
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.USER };
	}

	protected VEventSeries defaultVEvent(ZonedDateTime start, int duration) {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeWrapper.create(start, Precision.DateTime);
		event.dtend = BmDateTimeWrapper.create(start.plus(Duration.ofMinutes(duration)), Precision.DateTime);
		event.summary = "event " + System.currentTimeMillis();
		event.transparency = VEvent.Transparency.Transparent;
		event.classification = VEvent.Classification.Public;
		event.status = VEvent.Status.Confirmed;
		event.priority = 5;
		event.alarm = Arrays.asList(VAlarm.create(-900));
		series.main = event;
		return series;
	}
}

