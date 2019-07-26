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
package net.bluemind.calendar.helper.ical4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEvent.Transparency;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.icalendar.parser.CalendarOwner;
import net.bluemind.icalendar.parser.ICal4jEventHelper;
import net.bluemind.icalendar.parser.ICal4jHelper;
import net.bluemind.lib.ical4j.data.CalendarBuilder;
import net.bluemind.lib.ical4j.model.PropertyFactoryRegistry;
import net.fortuna.ical4j.data.CalendarParser;
import net.fortuna.ical4j.data.CalendarParserFactory;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterFactoryRegistry;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactoryImpl;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.LastModified;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;

public class VEventServiceHelper extends ICal4jEventHelper<VEvent> {

	public static String convertToIcs(Method method, List<ItemValue<VEventSeries>> vevents, Object... properties) {
		return convertToIcal4jCalendar(method, vevents, properties).toString();
	}

	public static Calendar convertToIcal4jCalendar(Method method, List<VEventSeries> vevents) {
		return convertToIcal4jCalendar(method,
				vevents.stream().map(v -> ItemValue.create((String) null, v)).collect(Collectors.toList()));
	}

	/**
	 * @param vevent
	 * @param method
	 * @return
	 */
	// FIXME Object... should be Property...
	public static Calendar convertToIcal4jCalendar(Method method, List<ItemValue<VEventSeries>> vevents,
			Object... paramProperties) {
		Calendar calendar = initCalendar();

		Object[] properties = paramProperties != null ? paramProperties : new Object[0];

		for (Object property : properties) {
			calendar.getProperties().add(property);
		}

		if (method != null) {
			calendar.getProperties().add(method);
		}

		Set<String> timezones = new HashSet<String>();

		for (ItemValue<VEventSeries> eventItem : vevents) {
			VEventSeries event = eventItem.value;
			if (null != event.main) {
				timezones.add(event.main.dtstart.timezone);
				timezones.add(event.main.dtend.timezone);
			}
			event.occurrences.forEach(occurrence -> {
				timezones.add(occurrence.dtstart.timezone);
				timezones.add(occurrence.dtend.timezone);
			});
			List<net.fortuna.ical4j.model.component.VEvent> evts = convertToIcal4jVEvent(event.icsUid, event);

			if (eventItem.updated != null) {
				String iso8601 = BmDateTimeWrapper.toIso8601(eventItem.updated.getTime(), "UTC");
				evts.forEach(evt -> {
					try {
						evt.getProperties().add(new LastModified(iso8601));
					} catch (ParseException e) {
						logger.warn("Cannot parse ICS Last modified date {}:{}", eventItem.updated, e.getMessage());
					}
				});
			}

			for (net.fortuna.ical4j.model.component.VEvent icalEvent : evts) {
				// DO NOT propagate alarm to attendees (or organiser)
				if (method != null) {
					icalEvent.getAlarms().clear();
				}
				// BM-10430
				if (method != Method.REPLY) {
					PropertyList listAttendees = icalEvent.getProperties(Property.ATTENDEE);
					if (listAttendees != null && !listAttendees.isEmpty()) {
						for (@SuppressWarnings("unchecked")
						Iterator<Property> it = listAttendees.iterator(); it.hasNext();) {
							net.fortuna.ical4j.model.property.Attendee prop = (net.fortuna.ical4j.model.property.Attendee) it
									.next();
							ParameterList parameters = prop.getParameters();
							parameters.remove(prop.getParameter("X-RESPONSE-COMMENT"));
						}
					}
				}
				calendar.getComponents().add(icalEvent);
			}
		}

		addVTimezone(calendar, timezones);

		return calendar;
	}

	/**
	 * @param vevent
	 * @param method
	 * @return
	 */
	public static String convertToExceptionIcs(ItemValue<VEventSeries> vevent) {
		return convertToIcs(Method.CANCEL, vevent);
	}

	/**
	 * @return
	 */
	public static Calendar initCalendar() {
		Calendar calendar = new Calendar();

		// TODO ProdId: add BlueMind version
		// -//BlueMind//BlueMind Calendar version XXX//FR
		calendar.getProperties().add(new ProdId("-//BlueMind//BlueMind Calendar//FR"));
		calendar.getProperties().add(Version.VERSION_2_0);
		calendar.getProperties().add(CalScale.GREGORIAN);
		return calendar;

	}

	/**
	 * @param ics
	 * @param owner
	 * @return
	 * @throws ServerFault
	 */
	public static List<ItemValue<VEventSeries>> convertToVEventList(String ics, Optional<CalendarOwner> owner)
			throws ServerFault {

		List<String> icsCalendarList = splitIcs(ics);
		List<ItemValue<VEventSeries>> ret = new ArrayList<>();
		for (String cal : icsCalendarList) {
			ret.addAll(parseCalendar(cal, owner));

		}
		return ret;

	}

	private static List<String> splitIcs(String ics) {
		List<String> cals = new ArrayList<>();
		String start = "BEGIN:VCALENDAR";
		String end = "END:VCALENDAR";

		int firstCalendar = ics.indexOf(start);
		int lastCalendar = ics.lastIndexOf(start);
		if (firstCalendar == lastCalendar) {
			cals.add(ics);
		} else {
			String[] substringsBetween = StringUtils.substringsBetween(ics, start, end);
			for (int i = 0; i < substringsBetween.length; i++) {
				cals.add(String.format("%s%s%s", start, substringsBetween[i], end));
			}
		}
		return cals;
	}

	private static <T extends VEvent> List<ItemValue<VEventSeries>> parseCalendar(String ics,
			Optional<CalendarOwner> owner) throws ServerFault {
		CalendarParser parser = CalendarParserFactory.getInstance().createParser();
		PropertyFactoryRegistry propertyFactory = new PropertyFactoryRegistry();
		ParameterFactoryRegistry parameterFactory = new ParameterFactoryRegistry();

		InputStream is = new ByteArrayInputStream(ics.getBytes());
		Reader reader = new InputStreamReader(is);
		UnfoldingReader unfoldingReader = new UnfoldingReader(reader, true);

		CalendarBuilder builder = new CalendarBuilder(parser, propertyFactory, parameterFactory,
				TimeZoneRegistryFactory.getInstance().createRegistry());

		net.fortuna.ical4j.model.Calendar calendar = null;

		try {
			calendar = builder.build(unfoldingReader);
		} catch (IOException e) {
			logger.error("IOException during ICS import. {}", e.getMessage());
			throw new ServerFault(e);
		} catch (ParserException e) {
			logger.error("ParserException during ICS parsing. {}", e.getMessage());
			throw new ServerFault(e);
		} finally {
			try {
				is.close();
				reader.close();
				unfoldingReader.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}

		// X-WR-TIMEZONE
		String globalTZ = calendar.getProperty("X-WR-TIMEZONE") != null
				? calendar.getProperty("X-WR-TIMEZONE").getValue()
				: null;

		ComponentList componentList = calendar.getComponents(Component.VEVENT);

		List<ItemValue<VEventSeries>> ret = new ArrayList<>(componentList.size());

		HashMap<String, List<ItemValue<T>>> events = new HashMap<>();

		for (@SuppressWarnings("unchecked")
		Iterator<Component> componentListIterator = componentList.iterator(); componentListIterator.hasNext();) {
			net.fortuna.ical4j.model.component.VEvent ical4j = (net.fortuna.ical4j.model.component.VEvent) componentListIterator
					.next();

			@SuppressWarnings("unchecked")
			ItemValue<T> vevent = (ItemValue<T>) new ICal4jEventHelper<>().parseIcs(new VEvent(), ical4j, globalTZ,
					owner);
			if (ical4j.getCreated() != null) {
				vevent.created = ical4j.getCreated().getDate();
			}
			if (ical4j.getLastModified() != null) {
				vevent.updated = ical4j.getLastModified().getDate();
			}

			if (ical4j.getUid() != null) {
				vevent.externalId = ical4j.getUid().getValue();
			}

			// DTEND
			vevent.value.dtend = ICal4jHelper.parseIcsDate(ical4j.getEndDate(), globalTZ);

			// TRANSPARANCY
			if (ical4j.getTransparency() != null) {
				String transparency = ical4j.getTransparency().getValue().toLowerCase();
				if ("opaque".equals(transparency)) {
					vevent.value.transparency = Transparency.Opaque;
				} else if ("transparent".equals(transparency)) {
					vevent.value.transparency = Transparency.Transparent;
				} else {
					logger.error("Unsupported Transparency " + transparency);

				}
			}
			List<ItemValue<T>> storedEvents = events.containsKey(vevent.uid) ? events.get(vevent.uid)
					: new ArrayList<>();
			storedEvents.add(vevent);
			events.put(vevent.uid, storedEvents);

		}

		for (String uid : events.keySet()) {
			ret.add(normalizeEvent(uid, events.get(uid)));
		}

		return ret;

	}

	public static <T extends VEvent> VEventSeries normalizeEvent(List<T> list) {
		List<T> copy = new ArrayList<>(list);
		T master = null;
		for (Iterator<T> iter = copy.iterator(); iter.hasNext();) {
			T next = iter.next();
			if (!(next instanceof VEventOccurrence)) {
				master = next;
				iter.remove();
			}
		}
		VEventSeries series = new VEventSeries();
		series.main = null != master ? master : null;
		series.occurrences = copy.stream().map(v -> (VEventOccurrence) v).collect(Collectors.toList());
		return series;
	}

	public static <T extends VEvent> ItemValue<VEventSeries> normalizeEvent(String uid, List<ItemValue<T>> list) {
		List<ItemValue<T>> copy = new ArrayList<>(list);
		ItemValue<T> master = null;
		Date updated = null;
		for (Iterator<ItemValue<T>> iter = copy.iterator(); iter.hasNext();) {
			ItemValue<T> next = iter.next();
			if (!(next.value instanceof VEventOccurrence)) {
				master = next;
				iter.remove();
			}
			if (null == updated && null != next.updated) {
				updated = next.updated;
			}
		}
		VEventSeries series = new VEventSeries();
		series.main = null != master ? master.value : null;
		series.occurrences = copy.stream().map(v -> (VEventOccurrence) v.value).collect(Collectors.toList());
		ItemValue<VEventSeries> reduced = ItemValue.create(uid, series);
		reduced.updated = updated;
		return reduced;
	}

	/**
	 * @param vevent
	 * @return
	 */
	public static List<net.fortuna.ical4j.model.component.VEvent> convertToIcal4jVEvent(
			ItemValue<VEventSeries> vevent) {
		return VEventServiceHelper.convertToIcal4jVEvent(vevent.value.icsUid, vevent.value);
	}

	public static List<net.fortuna.ical4j.model.component.VEvent> convertToIcal4jVEvent(VEventSeries vevent) {
		return VEventServiceHelper.convertToIcal4jVEvent(vevent.icsUid, vevent);
	}

	/**
	 * @param vevent
	 * @return
	 */
	public static List<net.fortuna.ical4j.model.component.VEvent> convertToIcal4jVEvent(String uid,
			VEventSeries vevent) {
		List<net.fortuna.ical4j.model.component.VEvent> ret = new ArrayList<>();

		if (null != vevent.main) {
			ret.add(parse(uid, vevent.main));
		}
		for (VEventOccurrence occurrence : vevent.occurrences) {
			ret.add(parse(uid, occurrence));
		}

		return ret;
	}

	private static <T extends VEvent> net.fortuna.ical4j.model.component.VEvent parse(String uid, T vevent) {
		net.fortuna.ical4j.model.component.VEvent ret = new net.fortuna.ical4j.model.component.VEvent();
		parseICalendarElement(uid, ret, vevent);

		PropertyList properties = ret.getProperties();
		properties.add(Version.VERSION_2_0);

		// DTEND
		if (vevent.dtend != null) {
			DtEnd dtend = new DtEnd(convertToIcsDate(vevent.dtend));
			properties.add(dtend);
		}

		// TRANSP
		if (vevent.transparency != null) {
			properties.add(new Transp(vevent.transparency.name().toUpperCase()));
		}

		appendXMsProperties(properties, vevent);
		appendXMozProperties(properties);

		if (vevent.attachments != null) {
			for (AttachedFile file : vevent.attachments) {
				XProperty prop = new XProperty("X-BM-ATTACHMENT", "(" + file.name + ")" + file.publicUrl);
				properties.add(prop);
			}
		}

		return ret;
	}

	private static void appendXMozProperties(PropertyList properties) {
		java.util.Calendar cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		DateProperty p = new DateProperty("X-MOZ-LASTACK", PropertyFactoryImpl.getInstance()) {
			private static final long serialVersionUID = -4815580988688773123L;
		};
		p.setDate(new DateTime(cal.getTime()));
		properties.add(p);

	}

	private static void appendXMsProperties(PropertyList properties, VEvent vevent) {
		XProperty disallowCounter = new XProperty("X-MICROSOFT-DISALLOW-COUNTER", "TRUE");
		properties.add(disallowCounter);

		if (vevent.transparency != null) {
			XProperty busyStatus = new XProperty("X-MICROSOFT-CDO-BUSYSTATUS",
					vevent.transparency == Transparency.Opaque ? "BUSY" : "FREE");
			properties.add(busyStatus);
		}

	}

	public static String convertToIcs(ItemValue<VEventSeries> vevent) {
		return convertToIcs(null, vevent);
	}

	public static String convertToIcs(Method method, VEventSeries vevent) {
		ItemValue<VEventSeries> es = ItemValue.create(vevent.icsUid, vevent);
		return convertToIcs(method, es);
	}

	public static String convertToIcs(Method method, ItemValue<VEventSeries> vevent) {
		return convertToIcs(method, Arrays.asList(vevent));
	}

	public static String convertToIcsWithProperty(Method method, List<ItemValue<VEventSeries>> events,
			XProperty xProperty) {
		Object[] props = xProperty != null ? new Object[] { xProperty } : null;
		return convertToIcs(method, events, props);
	}

	public static String convertToIcs(List<ItemValue<VEventSeries>> vevents) {
		return convertToIcs(null, vevents, new Object[0]);
	}

	public static String convertToIcs(String uid, Method method, VEventSeries series) {
		return convertToIcs(method, ItemValue.create(uid, series));
	}

	public static String convertToIcs(String uid, Method method, VEvent vevent) {
		VEventSeries series = new VEventSeries();
		series.main = vevent;
		series.icsUid = uid;
		return convertToIcs(method, ItemValue.create(uid, series));
	}

}