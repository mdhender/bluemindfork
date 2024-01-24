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
package net.bluemind.lmtp.filter.imip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.field.UnstructuredField;
import org.apache.james.mime4j.field.UnstructuredFieldImpl;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.RawField;
import org.columba.ristretto.message.Address;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.helper.mail.MeetingUpdateDiff;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.Classification;

public class IMIPResponse {

	public List<Field> headerFields = new ArrayList<>();

	private static final String EVENT_HEADER = "X-BM-Event";
	private static final String TODO_HEADER = "X-BM-Todo";
	private static final String CAL_HEADER = "X-BM-Calendar";
	private static final String PRIVATE_CAL_HEADER = "X-BM-Event-Private";

	public static IMIPResponse createCanceledResponse(String itemUid, String calendarUid, boolean isPrivate) {
		StringBuilder eventIcsUid = new StringBuilder(itemUid);
		return getCancelHeader(eventIcsUid, calendarUid, isPrivate);
	}

	public static IMIPResponse createCanceledExceptionResponse(String uid, String iso8601, String calendarUid,
			boolean isPrivate) {
		StringBuilder eventIcsUid = new StringBuilder(uid);
		eventIcsUid.append("; recurid=\"" + iso8601 + "\"");
		return getCancelHeader(eventIcsUid, calendarUid, isPrivate);
	}

	public static IMIPResponse createRepliedResponse(String itemUid, String calendarUid, boolean isPrivate) {
		StringBuilder eventIcsUid = new StringBuilder(itemUid);
		return getReplyHeader(eventIcsUid, calendarUid, isPrivate);
	}

	public static IMIPResponse createRepliedToExceptionResponse(String itemUid, String iso8601, String calendarUid,
			boolean isPrivate) {
		StringBuilder eventIcsUid = new StringBuilder(itemUid);
		eventIcsUid.append("; recurid=\"" + iso8601 + "\"");
		return getReplyHeader(eventIcsUid, calendarUid, isPrivate);
	}

	public static IMIPResponse createCounterResponse(String itemUid, String email, VEventOccurrence counterEvent,
			List<Attendee> proposedAttendees, String calendarUid) {
		IMIPResponse ret = new IMIPResponse();

		StringBuilder eventIcsUid = new StringBuilder(itemUid);
		eventIcsUid.append("; originator=\"" + email + "\"");

		if (counterEvent.recurid != null) {
			eventIcsUid.append("; recurid=\"" + counterEvent.recurid.iso8601 + "\"");
		}

		RawField rf = new RawField("X-BM-Event-Countered", eventIcsUid.toString());
		UnstructuredField bmIcsUid = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
		prepareXBMHeaders(ret, bmIcsUid, calendarUid, counterEvent.classification == Classification.Private);

		if (!proposedAttendees.isEmpty()) {
			ret.headerFields = new ArrayList<>(ret.headerFields);
			String attendeeList = String.join(",",
					proposedAttendees.stream().map(att -> new Address(att.commonName, att.mailto).toString()).toList());
			RawField attendeeHeader = new RawField("X-BM-Counter-Attendee", attendeeList);
			UnstructuredField attendeeHeaderField = UnstructuredFieldImpl.PARSER.parse(attendeeHeader,
					DecodeMonitor.SILENT);
			ret.headerFields.add(attendeeHeaderField);
		}

		return ret;
	}

	public static IMIPResponse createDeclineCounterResponse(String itemUid, String calendarUid, String recurid,
			boolean isPrivate) {
		IMIPResponse ret = new IMIPResponse();

		StringBuilder eventIcsUid = new StringBuilder(itemUid);
		if (recurid != null) {
			eventIcsUid.append("; recurid=\"" + recurid + "\"");
		}

		RawField rf = new RawField("X-BM-Counter-Declined", eventIcsUid.toString());
		UnstructuredField bmIcsUid = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
		prepareXBMHeaders(ret, bmIcsUid, calendarUid, isPrivate);

		return ret;
	}

	public static IMIPResponse createEventResponse(String itemUid, ICalendarElement calElement, boolean needResponse,
			String calendarUid, MeetingUpdateDiff diff) {
		IMIPResponse ret = new IMIPResponse();
		Map<String, String> att = (diff == null) ? Collections.emptyMap() : diff.toMap();
		UnstructuredField bmEventHeader = createNeedResponseHeader(EVENT_HEADER, itemUid, calElement, needResponse,
				att);
		prepareXBMHeaders(ret, bmEventHeader, calendarUid, calElement.classification == Classification.Private);

		return ret;
	}

	private static IMIPResponse getCancelHeader(StringBuilder value, String calendarUid, boolean isPrivate) {
		IMIPResponse ret = new IMIPResponse();
		RawField rf = new RawField("X-BM-Event-Canceled", value.toString());
		UnstructuredField bmExtId = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
		prepareXBMHeaders(ret, bmExtId, calendarUid, isPrivate);

		return ret;
	}

	private static IMIPResponse getReplyHeader(StringBuilder value, String calendarUid, boolean isPrivate) {
		IMIPResponse ret = new IMIPResponse();
		RawField rf = new RawField("X-BM-Event-Replied", value.toString());
		UnstructuredField bmExtId = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
		prepareXBMHeaders(ret, bmExtId, calendarUid, isPrivate);
		return ret;
	}

	public static IMIPResponse createTodoResponse(String itemUid, ICalendarElement calElement, String type) {
		IMIPResponse ret = new IMIPResponse();
		UnstructuredField bmTodoHeader = createNeedResponseHeader(TODO_HEADER, itemUid, calElement, false,
				Map.of("type", type));
		ret.headerFields = Arrays.asList(bmTodoHeader);
		return ret;
	}

	private static void prepareXBMHeaders(IMIPResponse ret, UnstructuredField rf, String calUid, boolean isPrivate) {
		if (ret.headerFields == null) {
			ret.headerFields = new ArrayList<>();
		}
	
		ret.headerFields.add(rf);
		ret.headerFields.add(getImipHeader(calUid));
		if (isPrivate) {
			ret.headerFields.add(getPrivateEventHeader());
		}
	}

	private static UnstructuredField getImipHeader(String uid) {
		return UnstructuredFieldImpl.PARSER.parse(new RawField(CAL_HEADER, uid), DecodeMonitor.SILENT);
	}

	private static UnstructuredField getPrivateEventHeader() {
		return UnstructuredFieldImpl.PARSER.parse(new RawField(PRIVATE_CAL_HEADER, "true"), DecodeMonitor.SILENT);
	}

	private static UnstructuredField createNeedResponseHeader(String header, String itemUid,
			ICalendarElement calElement, boolean needResponse, Map<String, String> additionalAttributes) {
		LoggerFactory.getLogger(IMIPResponse.class).info("{} need resp {} {}", header, itemUid, needResponse);

		StringBuilder eventIcsUid = new StringBuilder(itemUid);
		if (calElement instanceof VEventOccurrence e) {
			eventIcsUid.append("; recurid=\"" + e.recurid.iso8601 + "\"");
		}
		eventIcsUid.append("; rsvp=\"" + needResponse + "\"");
		additionalAttributes.forEach((key, value) -> eventIcsUid.append("; " + key + "=\"" + value + "\""));

		RawField rf = new RawField(header, eventIcsUid.toString());
		return UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
	}

	public static IMIPResponse createEmptyResponse() {
		return new IMIPResponse();
	}

	private IMIPResponse() {
	}
}
