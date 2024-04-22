/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.eas.http.tests.helpers;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import org.w3c.dom.Document;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.eas.client.ProtocolVersion;
import net.bluemind.eas.http.tests.builders.CalendarBuilder;
import net.bluemind.utils.DOMUtils;

public class CoreCalendarHelper {

	private CoreCalendarHelper() {

	}

	public static long getUserCalendarId(String user, String domainUid) {
		List<ItemValue<ContainerHierarchyNode>> list = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainersFlatHierarchy.class, domainUid, user).list();
		long calId = 0;
		for (ItemValue<ContainerHierarchyNode> node : list) {
			if (node.value.containerType.equals("calendar")) {
				calId = node.internalId;
			}
		}
		return calId;
	}

	public static void addEvent() {
		addEvent(UUID.randomUUID().toString());
	}

	public static void addEvent(String uid) {
		VEventSeries series = CalendarBuilder.defaultEvent();
		addEvent(uid, series);
	}

	private static void addEvent(String uid, VEventSeries series) {
		ICalendar service = getService();
		service.create(uid, series, false);
	}

	private static ICalendar getService() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("user"));
	}

	public static List<ItemValue<VEventSeries>> getAllEvents() {
		List<ItemValue<VEventSeries>> list = getService().all().stream().map(uid -> getService().getComplete(uid))
				.toList();
		list.forEach(e -> {
			System.err.println(e.value.main.location);
		});
		return list;
	}

	public static Runnable validateEventCount(int count) {
		return () -> assertEquals(count, CoreCalendarHelper.getAllEvents().size());
	}

	public static Runnable validateDefaultEvent(ProtocolVersion version,
			Predicate<ItemValue<VEventSeries>> testDataFilter) {
		return validateEvent(version, CalendarBuilder.defaultEvent(), CoreCalendarHelper.getAllEvents().stream()
				.filter(testDataFilter).findAny().map(series -> series.value).orElseThrow());
	}

	public static Runnable validateEvent(ProtocolVersion version, VEventSeries expected,
			Predicate<ItemValue<VEventSeries>> testDataFilter) {
		return validateEvent(version, expected, CoreCalendarHelper.getAllEvents().stream().filter(testDataFilter)
				.findAny().map(series -> series.value).orElseThrow());
	}

	public static Runnable validateEvent(ProtocolVersion version, VEventSeries expected, VEventSeries testData) {
		return () -> {
			Document testDataEvent = null;
			Document expectedEvent = null;
			try {
				expectedEvent = CalendarBuilder.getEvent(version, expected);
				testDataEvent = CalendarBuilder.getEvent(version, testData);
			} catch (Exception e) {
				e.printStackTrace();
			}

			String locationServer = null;
			String locationSource = null;

			if (Float.parseFloat(version.toString()) < 16.0) {
				locationServer = DOMUtils.getElementText(testDataEvent.getDocumentElement(), "Location").trim();
				locationSource = DOMUtils.getElementText(expectedEvent.getDocumentElement(), "Location").trim();
			} else {
				locationServer = DOMUtils.getElementText(DOMUtils.getUniqueElement(
						DOMUtils.getUniqueElement(testDataEvent.getDocumentElement(), "Location"), "DisplayName"));
				locationSource = DOMUtils.getElementText(DOMUtils.getUniqueElement(
						DOMUtils.getUniqueElement(expectedEvent.getDocumentElement(), "Location"), "DisplayName"));
			}

			assertElement(expectedEvent, testDataEvent, "Subject");
			assertElement(expectedEvent, testDataEvent, "OrganizerName");
			assertElement(expectedEvent, testDataEvent, "StartTime");
			assertElement(expectedEvent, testDataEvent, "EndTime");
			assertElement(expectedEvent, testDataEvent, "AllDayEvent");
			assertElement(expectedEvent, testDataEvent, "Sensitivity");
			assertElement(expectedEvent, testDataEvent, "BusyStatus");
			assertElement(expectedEvent, testDataEvent, "MeetingStatus");
			assertElement(expectedEvent, testDataEvent, "DisallowNewTimeProposal");

			assertEquals(locationSource.trim(), locationServer.trim());

		};
	}

	private static void assertElement(Document expected, Document test, String element) {
		String text1 = DOMUtils.getElementText(DOMUtils.getUniqueElement(expected.getDocumentElement(), element))
				.trim();
		String text2 = DOMUtils.getElementText(DOMUtils.getUniqueElement(test.getDocumentElement(), element)).trim();
		assertEquals(text1, text2);
	}

	public static Predicate<ItemValue<VEventSeries>> eventBySummary(String summary) {
		return evt -> evt.value.flatten().stream().anyMatch(event -> event.summary.equals(summary));
	}

}
