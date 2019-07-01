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

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.calendar.api.IVFreebusy;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEvent.Transparency;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.api.VFreebusy;
import net.bluemind.calendar.api.VFreebusyQuery;
import net.bluemind.calendar.helper.ical4j.VFreebusyServiceHelper;
import net.bluemind.calendar.occurrence.OccurrenceHelper;
import net.bluemind.calendar.persistance.FreebusyStore;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.service.ContainerSettings;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.icalendar.api.ICalendarElement.Classification;
import net.bluemind.user.api.IUserSettings;

public class VFreebusyService implements IVFreebusy {

	private static Logger logger = LoggerFactory.getLogger(VFreebusyService.class);

	private BmContext context;
	private Container container;

	private RBACManager rbacManager;
	private RBACManager defaultCalendarRbacManager;

	public VFreebusyService(BmContext context, Container container) {
		this.context = context;
		this.container = container;
		rbacManager = RBACManager.forContext(context).forContainer(container);
		final String defaultCalendarContainerUid = ICalendarUids
				.defaultUserCalendar(IFreebusyUids.extractUserUid(container.uid));
		try {
			defaultCalendarRbacManager = RBACManager.forContext(context).forContainer(defaultCalendarContainerUid);
		} catch (ServerFault e) {
			// container does not exist
			defaultCalendarRbacManager = null;
		}

	}

	@Override
	public String getAsString(VFreebusyQuery query) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		VFreebusy fb = get(query);
		return VFreebusyServiceHelper.asIcs(fb);
	}

	@Override
	public VFreebusy get(VFreebusyQuery query) throws ServerFault {
		// if the user can read the default calendar of the other user then we consider
		// he has freebusy access as well
		final boolean canReadDefaultCalendar = defaultCalendarRbacManager != null
				&& defaultCalendarRbacManager.can(Verb.Read.name());
		if (!canReadDefaultCalendar) {
			rbacManager.check(Verb.Read.name());
		}

		Set<String> calendars = new HashSet<String>();

		// ensure that we have default calendar
		String userCal = ICalendarUids.defaultUserCalendar(container.owner);
		String resourceCal = ICalendarUids.resourceCalendar(container.owner);
		addCalendarIfPresent(calendars, userCal, resourceCal);

		return VFreebusyServiceHelper.convertToFreebusy(container.domainUid, container.owner, query.dtstart,
				query.dtend, getVEvents(query, calendars), getOutOfOffice(query, calendars));
	}

	/**
	 * @param query
	 * @return
	 * @throws ServerFault
	 */
	private List<ItemValue<VEvent>> getVEvents(VFreebusyQuery query, Set<String> calendars) throws ServerFault {
		try {
			FreebusyStore fbStore = new FreebusyStore(context.getDataSource(), container);
			List<String> fb = fbStore.get();
			if (fb != null) {
				calendars.addAll(fb);
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		List<ItemValue<VEvent>> values = new ArrayList<>();
		VEventQuery calQuery = VEventQuery.create(query.dtstart, query.dtend);
		calQuery.resolveAttendees = false;
		for (String calendar : calendars) {
			try {
				RBACManager forUser = RBACManager.forContext(context).forContainer(calendar);
				ListResult<ItemValue<VEventSeries>> list = context.su().provider().instance(ICalendar.class, calendar)
						.search(calQuery);
				list.values.forEach(v -> {
					values.addAll(OccurrenceHelper.list(v, query.dtstart, query.dtend).stream().map(evt -> {
						return ItemValue.create(v.uid, evt);
					}).map(event -> {
						return filter(forUser, event);
					}).collect(Collectors.toList()));
				});
			} catch (ServerFault e) {
				logger.error(e.getMessage(), e);
			}
		}
		if (null == query.excludedEvents || query.excludedEvents.isEmpty()) {
			return values;
		} else {
			return values.stream().filter(event -> {
				return !query.excludedEvents.contains(event.uid);
			}).collect(Collectors.toList());
		}

	}

	private ItemValue<VEvent> filter(RBACManager rbac, ItemValue<VEvent> iv) {
		if (iv == null) {
			return null;
		}
		if (!rbac.can(Verb.Read.name())) {
			iv.value = iv.value.filtered();
			iv.value.summary = "";
		} else if (iv.value.classification != Classification.Public && !rbac.can(Verb.All.name())) {
			iv.value = iv.value.filtered();
		}
		return iv;
	}

	private void addCalendarIfPresent(Set<String> calendars, String... calendarNames) {
		IContainers containerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainers.class);
		for (String calendar : calendarNames) {
			if (containerService.getIfPresent(calendar) != null) {
				calendars.add(calendar);
			}
		}
	}

	/**
	 * BM-6493 oof as busy AS BUSY-UNAVAILABLE
	 * 
	 * @param query
	 * @param calendars
	 * @throws ServerFault
	 */
	private List<ItemValue<VEvent>> getOutOfOffice(VFreebusyQuery query, Set<String> calendars) throws ServerFault {

		if (!query.withOOFSlots) {
			return Collections.emptyList();
		}

		Map<String, String> settings = null;

		for (String calendar : calendars) {
			ContainerSettings containerSettings;
			try {
				DataSource ds = DataSourceRouter.get(context, calendar);
				ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());
				containerSettings = new ContainerSettings(context, containerStore.get(calendar));
				Map<String, String> cSettings = containerSettings.get();
				if (null != cSettings && cSettings.containsKey("calendar.workingDays")) {
					settings = new HashMap<>();
					settings.put("working_days", toDaySettings(cSettings.get("calendar.workingDays")));
					settings.put("work_hours_start", toTimeString(cSettings.get("calendar.dayStart")));
					settings.put("work_hours_end", toTimeString(cSettings.get("calendar.dayEnd")));
					settings.put("timezone", cSettings.get("calendar.timezone"));
					break;
				}
			} catch (Exception e) {
				logger.warn("Cannot load container settings of container {}", calendar, e);
			}

		}

		List<ItemValue<VEvent>> ret = new ArrayList<ItemValue<VEvent>>();

		if (settings == null) {
			IUserSettings isettings = context.su().provider().instance(IUserSettings.class, container.domainUid);
			settings = isettings.get(container.owner);
		}

		String[] workingDays = settings.get("working_days").split(",");
		List<Integer> days = new ArrayList<Integer>();
		for (String day : workingDays) {
			if ("mon".equals(day)) {
				days.add(2);
			}
			if ("tue".equals(day)) {
				days.add(3);
			}
			if ("wed".equals(day)) {
				days.add(4);
			}
			if ("thu".equals(day)) {
				days.add(5);
			}
			if ("fri".equals(day)) {
				days.add(6);
			}
			if ("sat".equals(day)) {
				days.add(7);
			}
			if ("sun".equals(day)) {
				days.add(1);
			}
		}

		String workHoursStart = settings.get("work_hours_start");
		int startMin = 0;
		int startHour = 0;
		if (workHoursStart.endsWith(".5")) {
			startHour = Integer.parseInt(workHoursStart.split("\\.")[0]);
			startMin = 30;
		} else {
			startHour = Integer.parseInt(workHoursStart);
		}

		String workHoursEnd = settings.get("work_hours_end");
		int endMin = 0;
		int endHour = 0;
		if (workHoursEnd.endsWith(".5")) {
			endHour = Integer.parseInt(workHoursEnd.split("\\.")[0]);
			endMin = 30;
		} else {
			endHour = Integer.parseInt(workHoursEnd);
		}

		String tz = settings.get("timezone");

		Calendar from = Calendar.getInstance(TimeZone.getTimeZone(tz));
		from.setTimeInMillis(BmDateTimeWrapper.toTimestamp(query.dtstart.iso8601, tz));

		Calendar to = Calendar.getInstance();
		to.setTimeInMillis(BmDateTimeWrapper.toTimestamp(query.dtend.iso8601, tz));

		while (from.before(to)) {
			if (!days.contains(from.get(Calendar.DAY_OF_WEEK))) {
				VEvent dayOff = new VEvent();
				Calendar begin = (Calendar) from.clone();
				dayOff.dtstart = BmDateTimeWrapper.fromTimestamp(begin.getTimeInMillis(), tz, Precision.Date);
				Calendar end = (Calendar) begin.clone();
				end.add(Calendar.DATE, 1);
				dayOff.dtend = BmDateTimeWrapper.fromTimestamp(end.getTimeInMillis(), tz, Precision.Date);
				dayOff.transparency = Transparency.Opaque;
				ret.add(ItemValue.create(new Item(), dayOff));
			} else {
				if (startHour < endHour) {
					VEvent e = new VEvent();
					Calendar start = (Calendar) from.clone();
					start.set(Calendar.HOUR_OF_DAY, 0);
					e.dtstart = BmDateTimeWrapper.fromTimestamp(start.getTimeInMillis(), tz);

					Calendar dayStart = (Calendar) start.clone();
					dayStart.add(Calendar.HOUR, startHour);
					if (startMin > 0) {
						dayStart.add(Calendar.MINUTE, startMin);
					}

					e.dtend = BmDateTimeWrapper.fromTimestamp(dayStart.getTimeInMillis(), tz);
					e.transparency = Transparency.Opaque;
					ret.add(ItemValue.create(new Item(), e));

					VEvent e2 = new VEvent();
					Calendar dayEnd = (Calendar) start.clone();
					dayEnd.set(Calendar.HOUR, endHour);
					if (endMin > 0) {
						dayEnd.add(Calendar.MINUTE, endMin);
					}
					e2.dtstart = BmDateTimeWrapper.fromTimestamp(dayEnd.getTimeInMillis(), tz);
					Calendar end = (Calendar) start.clone();
					end.add(Calendar.DATE, 1);
					e2.dtend = BmDateTimeWrapper.fromTimestamp(end.getTimeInMillis(), tz);
					e2.transparency = Transparency.Opaque;
					ret.add(ItemValue.create(new Item(), e2));
				} else {
					// for people who work at night and sleep at day
					VEvent e = new VEvent();
					Calendar start = (Calendar) from.clone();
					start.add(Calendar.HOUR, endHour);
					if (endMin > 0) {
						start.add(Calendar.MINUTE, endMin);
					}
					e.dtstart = BmDateTimeWrapper.fromTimestamp(start.getTimeInMillis(), tz);

					Calendar end = (Calendar) from.clone();
					end.add(Calendar.HOUR, startHour);
					if (startMin > 0) {
						end.add(Calendar.MINUTE, startMin);
					}
					e.dtend = BmDateTimeWrapper.fromTimestamp(end.getTimeInMillis(), tz);
					e.transparency = Transparency.Opaque;
					ret.add(ItemValue.create(new Item(), e));
				}
			}

			from.add(Calendar.DATE, 1);
		}

		return ret;
	}

	private String toTimeString(String time) {
		String[] parts = time.split(":");
		float converted = Float.parseFloat(parts[0]);
		if (parts[1].equals("30")) {
			converted += 0.5;
			return new DecimalFormat("#.0").format(converted);
		}
		return String.valueOf((int) converted);
	}

	private String toDaySettings(String input) {
		List<String> days = new ArrayList<String>();
		for (String day : input.split(",")) {
			if ("MO".equals(day)) {
				days.add("mon");
			}
			if ("TU".equals(day)) {
				days.add("tue");
			}
			if ("WE".equals(day)) {
				days.add("wed");
			}
			if ("TH".equals(day)) {
				days.add("thu");
			}
			if ("FR".equals(day)) {
				days.add("fri");
			}
			if ("SA".equals(day)) {
				days.add("sat");
			}
			if ("SU".equals(day)) {
				days.add("sun");
			}
		}
		return String.join(",", days);
	}
}
