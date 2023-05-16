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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.xml.bind.DatatypeConverter;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEvent.Transparency;
import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.icalendar.parser.CalendarOwner;
import net.bluemind.icalendar.parser.ICal4jEventHelper;
import net.bluemind.icalendar.parser.ICal4jHelper;
import net.bluemind.icalendar.parser.ObservanceMapper;
import net.bluemind.lib.ical4j.data.CalendarBuilder;
import net.bluemind.tag.api.TagRef;
import net.bluemind.videoconferencing.utils.TeamsHeaders;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.LastModified;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;

public class VEventServiceHelper extends ICal4jEventHelper<VEvent> {

	public static String convertToIcs(Method method, List<ItemValue<VEventSeries>> vevents, Property... properties) {
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
			Property... paramProperties) {
		Calendar calendar = initCalendar();

		Property[] properties = paramProperties != null ? paramProperties : new Property[0];

		for (Property property : properties) {
			calendar.getProperties().add(property);
		}

		if (method != null) {
			calendar.getProperties().add(method);
		}

		Set<String> timezones = new HashSet<>();

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
			event.counters.forEach(counter -> {
				timezones.add(counter.counter.dtstart.timezone);
				timezones.add(counter.counter.dtend.timezone);
			});
		}

		addVTimezone(calendar, timezones);

		for (ItemValue<VEventSeries> eventItem : vevents) {
			VEventSeries event = eventItem.value;
			List<net.fortuna.ical4j.model.component.VEvent> evts = null;
			if (method == Method.COUNTER) {
				evts = convertCountersToIcal4jVEvent(event.icsUid, event);
			} else {
				evts = convertToIcal4jVEvent(event.icsUid, event);
			}

			if (eventItem.updated != null) {
				evts.forEach(evt -> {
					evt.getProperties().add(new LastModified(new DateTime(eventItem.updated)));
				});
			}

			for (net.fortuna.ical4j.model.component.VEvent icalEvent : evts) {
				// DO NOT propagate alarm to attendees (or organiser)
				if (method != null) {
					icalEvent.getAlarms().clear();
				}
				// BM-10430
				if (method != Method.REPLY) {
					PropertyList<Property> listAttendees = icalEvent.getProperties(Property.ATTENDEE);
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
				if (method == Method.CANCEL) {
					PropertyList<Property> props = icalEvent.getProperties();
					Property val = props.getProperty("STATUS");
					if (val != null) {
						props.remove(val);
					}
					props.add(Status.VEVENT_CANCELLED);

					val = props.getProperty("TRANSP");
					if (val != null) {
						props.remove(val);
					}
					props.add(Transp.TRANSPARENT);

				}

				calendar.getComponents().add(icalEvent);
			}
		}

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

	public static CalendarProperties parseCalendar(InputStream ics, Optional<CalendarOwner> owner, List<TagRef> allTags,
			Consumer<ItemValue<VEventSeries>> consumer) {
		File rootFolder = null;
		CalendarProperties calendarProperties = null;
		try {
			rootFolder = Files.createTempDirectory(UUID.randomUUID().toString()).toFile();
			File icsFile = new File(rootFolder, System.currentTimeMillis() + ".ics");
			CalendarValues calendarValues = serializeToFile(ics, icsFile);
			TimezoneInfo tzInfo = calendarValues.timezoneInfo;
			calendarProperties = calendarValues.calendarProperties;
			ObservanceMapper tzMapper = new ObservanceMapper(tzInfo.timezones);
			Map<String, String> tzMapping = tzMapper.getTimezoneMapping();
			parseICS(rootFolder, icsFile, tzInfo);
			parseEvents(owner, tzMapping, consumer, rootFolder, tzInfo, allTags);
		} catch (Exception e) {
			throw new ServerFault(e);
		} finally {
			deleteTmpFolder(rootFolder);
		}
		return calendarProperties;
	}

	private static void deleteTmpFolder(File rootFolder) {
		if (rootFolder != null && rootFolder.exists()) {
			try (Stream<Path> walker = Files.walk(rootFolder.toPath())) {
				walker.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			} catch (IOException e) {
			}
		}
	}

	private static void parseEvents(Optional<CalendarOwner> owner, Map<String, String> tzMapping,
			Consumer<ItemValue<VEventSeries>> consumer, File rootFolder, TimezoneInfo tzInfo, List<TagRef> allTags)
			throws IOException {
		File[] seriesFolders = rootFolder.listFiles(file -> file.isDirectory());
		for (File seriesFolder : seriesFolders) {
			List<ItemValue<VEvent>> events = Arrays.asList(seriesFolder.listFiles()).stream()
					.filter(f -> f.getName().endsWith(".ics")).map(asFile -> {
						AtomicReference<Component> ref = new AtomicReference<>(null);
						try (Reader reader = new InputStreamReader(Files.newInputStream(asFile.toPath()));
								UnfoldingReader unfoldingReader = new UnfoldingReader(reader, true)) {
							CalendarBuilder builder = new CalendarBuilder(tzInfo.timezones);
							BiConsumer<Calendar, Component> componentConsumer = (calendar, component) -> {
								if (!Component.VEVENT.equals(component.getName())) {
									return;
								}
								ref.set(component);
							};
							builder.build(unfoldingReader, componentConsumer);
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
						}
						return fromComponent(ref.get(), tzInfo.globalTZ, tzMapping, owner, allTags);
					}).collect(Collectors.toList());

			String uid = new String(Files.readAllBytes(new File(seriesFolder, "uid").toPath()));
			ItemValue<VEventSeries> series = normalizeEvent(uid, events);
			consumer.accept(series);
		}
	}

	private static Optional<String> parseICS(File rootFolder, File icsFile, TimezoneInfo tzInfo) {
		AtomicReference<String> globalTz = new AtomicReference<>(null);

		try (Reader reader = new InputStreamReader(Files.newInputStream(icsFile.toPath()));
				UnfoldingReader unfoldingReader = new UnfoldingReader(reader, true)) {
			CalendarBuilder builder = new CalendarBuilder(tzInfo.timezones);
			BiConsumer<Calendar, Component> componentConsumer = (calendar, component) -> {
				if (!Component.VEVENT.equals(component.getName())) {
					return;
				}

				Property uidProp = component.getProperty(Property.UID);
				String uid = uidProp != null ? uidProp.getValue() : UUID.randomUUID().toString();

				File folder = new File(rootFolder, hash(uid));
				if (!folder.exists()) {
					folder.mkdir();
				}
				try {
					Files.write(new File(folder, "uid").toPath(), uid.getBytes());
					File eventIcs = new File(folder, UUID.randomUUID().toString() + ".ics");

					String cal = String.format("%s\r\n%s%s", "BEGIN:VCALENDAR", component.toString(), "END:VCALENDAR");
					Files.write(eventIcs.toPath(), cal.getBytes());
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}

			};

			builder.build(unfoldingReader, componentConsumer);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return Optional.ofNullable(globalTz.get());
	}

	public static String hash(String uid) throws ServerFault {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			return uid;
		}
		md.update(uid.getBytes());
		return DatatypeConverter.printHexBinary(md.digest());
	}

	private static CalendarValues serializeToFile(InputStream ics, File icsFile) throws IOException, ParserException {
		List<VTimeZone> tz = new ArrayList<>();
		AtomicReference<String> globalTz = new AtomicReference<>();
		AtomicReference<Calendar> calendarRef = new AtomicReference<>();
		try (FileWriter writer = new FileWriter(icsFile);
				Reader reader = new IcsReader(ics, writer);
				UnfoldingReader unfoldingReader = new UnfoldingReader(reader, true)) {
			CalendarBuilder builder = new CalendarBuilder();
			BiConsumer<Calendar, Component> componentConsumer = (calendar, component) -> {

				calendarRef.set(calendar);

				if (Component.VTIMEZONE.equals(component.getName())) {
					tz.add((VTimeZone) component);
				}
				if (globalTz.get() == null && calendar.getProperty("X-WR-TIMEZONE") != null) {
					globalTz.set(calendar.getProperty("X-WR-TIMEZONE").getValue());
				}

			};
			builder.build(unfoldingReader, componentConsumer);
		}

		CalendarProperties calendarProperties = new CalendarProperties();
		if (calendarRef.get() != null) {
			calendarProperties.putAll(calendarRef.get().getProperties().stream()
					.collect(Collectors.toMap(Property::getName, Property::getValue)));
		}

		return new CalendarValues(new TimezoneInfo(tz, Optional.ofNullable(globalTz.get())), calendarProperties);
	}

	private static ItemValue<VEvent> fromComponent(Component component, Optional<String> globalTZ,
			Map<String, String> tzMapping, Optional<CalendarOwner> owner, List<TagRef> allTags) {
		net.fortuna.ical4j.model.component.VEvent ical4j = (net.fortuna.ical4j.model.component.VEvent) component;

		ItemValue<VEvent> vevent = (ItemValue<VEvent>) new ICal4jEventHelper<>().parseIcs(new VEvent(), ical4j,
				globalTZ, tzMapping, owner, allTags);
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
		Property dtEndProperty = ical4j.getProperty(Property.DTEND);
		if (dtEndProperty == null) {
			vevent.value.dtend = vevent.value.dtstart;
		} else {
			vevent.value.dtend = ICal4jHelper.parseIcsDate(ical4j.getEndDate(), globalTZ, tzMapping);
		}

		// TRANSPARENCY
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

		return vevent;

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
		series.icsUid = uid;
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
	public static List<net.fortuna.ical4j.model.component.VEvent> convertCountersToIcal4jVEvent(String uid,
			VEventSeries vevent) {
		List<net.fortuna.ical4j.model.component.VEvent> ret = new ArrayList<>();

		for (VEventCounter counter : vevent.counters) {
			ret.add(parse(uid, counter.counter));
		}
		return ret;
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
		for (VEventCounter counter : vevent.counters) {
			ret.add(parse(uid, counter.counter));
		}

		ret.stream().map(evt -> {
			XProperty acceptCounters = new XProperty("X-MICROSOFT-DISALLOW-COUNTER",
					Boolean.toString(!vevent.acceptCounters));
			evt.getProperties().add(acceptCounters);
			return evt;
		}).collect(Collectors.toList());

		return ret;
	}

	public static <T extends VEvent> net.fortuna.ical4j.model.component.VEvent parse(String uid, T vevent) {
		net.fortuna.ical4j.model.component.VEvent ret = new net.fortuna.ical4j.model.component.VEvent();
		parseICalendarElement(uid, ret, vevent);

		PropertyList<Property> properties = ret.getProperties();
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

		return ret;
	}

	private static void appendXMozProperties(PropertyList<Property> properties) {
		XProperty p = new XProperty("X-MOZ-LASTACK", new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'").format(new Date()));
		properties.add(p);
	}

	private static void appendXMsProperties(PropertyList<Property> properties, VEvent vevent) {

		if (vevent.transparency != null) {
			XProperty busyStatus = new XProperty("X-MICROSOFT-CDO-BUSYSTATUS",
					vevent.transparency == Transparency.Opaque ? "BUSY" : "FREE");
			properties.add(busyStatus);
		}

		if (vevent.conference != null && vevent.conference.startsWith("https://teams.microsoft.com")) {
			XProperty teamsUrl = new XProperty(TeamsHeaders.X_MICROSOFT_SKYPETEAMSMEETINGURL, vevent.conference);
			properties.add(teamsUrl);
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
		Property[] props = xProperty != null ? new Property[] { xProperty } : null;
		return convertToIcs(method, events, props);
	}

	public static String convertToIcs(List<ItemValue<VEventSeries>> vevents) {
		return convertToIcs(null, vevents, new Property[0]);
	}

	public static String convertToIcs(String uid, Method method, VEventSeries series) {
		return convertToIcs(method, ItemValue.create(uid, series));
	}

	public static String convertToIcs(Optional<Boolean> acceptCounters, String uid, Method method, VEvent vevent) {
		VEventSeries series = new VEventSeries();
		if (method == Method.COUNTER) {
			VEventCounter c = new VEventCounter();
			c.counter = (VEventOccurrence) vevent;
			series.counters = Arrays.asList(c);
		} else {
			series.main = vevent;
		}
		series.icsUid = uid;
		acceptCounters.ifPresent(acceptCounterPropositions -> series.acceptCounters = acceptCounterPropositions);
		return convertToIcs(method, ItemValue.create(uid, series));
	}

	public static class CalendarValues {
		public final TimezoneInfo timezoneInfo;
		public final CalendarProperties calendarProperties;

		public CalendarValues(TimezoneInfo timezoneInfo, CalendarProperties calendarProperties) {
			this.timezoneInfo = timezoneInfo;
			this.calendarProperties = calendarProperties;
		}
	}

	private static class TimezoneInfo {
		final List<VTimeZone> timezones;
		final Optional<String> globalTZ;

		TimezoneInfo(List<VTimeZone> timezones, Optional<String> globalTZ) {
			this.timezones = timezones;
			this.globalTZ = globalTZ;
		}
	}

	@SuppressWarnings("serial")
	public static class CalendarProperties extends HashMap<String, String> {

	}

}
