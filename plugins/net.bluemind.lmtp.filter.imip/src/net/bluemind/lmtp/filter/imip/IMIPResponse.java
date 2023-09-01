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
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.icalendar.api.ICalendarElement;

public class IMIPResponse {

	public List<Field> headerFields = new ArrayList<>();

	private static final String calHeader = "X-BM-Event";
	private static final String todoHeader = "X-BM-Todo";

	public static IMIPResponse createCanceledResponse(String itemUid) {
		IMIPResponse ret = new IMIPResponse();

		StringBuilder eventIcsUid = new StringBuilder(itemUid);

		return getCancelHeader(ret, eventIcsUid);

	}

	public static IMIPResponse createCanceledExceptionResponse(String uid, String iso8601) {
		IMIPResponse ret = new IMIPResponse();

		StringBuilder eventIcsUid = new StringBuilder(uid);
		eventIcsUid.append("; recurid=\"" + iso8601 + "\"");

		return getCancelHeader(ret, eventIcsUid);
	}

	private static IMIPResponse getCancelHeader(IMIPResponse ret, StringBuilder value) {
		RawField rf = new RawField("X-BM-Event-Canceled", value.toString());
		UnstructuredField bmExtId = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
		ret.headerFields = Arrays.asList(bmExtId);
		return ret;
	}

	public static IMIPResponse createRepliedResponse(String itemUid) {
		IMIPResponse ret = new IMIPResponse();

		StringBuilder eventIcsUid = new StringBuilder(itemUid);

		return getReplyHeader(ret, eventIcsUid);

	}

	public static IMIPResponse createRepliedToExceptionResponse(String itemUid, String iso8601) {
		IMIPResponse ret = new IMIPResponse();

		StringBuilder eventIcsUid = new StringBuilder(itemUid);
		eventIcsUid.append("; recurid=\"" + iso8601 + "\"");

		return getReplyHeader(ret, eventIcsUid);
	}

	private static IMIPResponse getReplyHeader(IMIPResponse ret, StringBuilder value) {
		RawField rf = new RawField("X-BM-Event-Replied", value.toString());
		UnstructuredField bmExtId = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
		ret.headerFields = Arrays.asList(bmExtId);
		return ret;
	}

	public static IMIPResponse createEventResponse(String itemUid, ICalendarElement calElement, boolean needResponse,
			Map<String, String> additionalAttributes) {
		return createNeedResponse(calHeader, itemUid, calElement, needResponse, additionalAttributes);
	}

	public static IMIPResponse createEventResponse(String itemUid, ICalendarElement calElement, boolean needResponse) {
		return createNeedResponse(calHeader, itemUid, calElement, needResponse, Collections.emptyMap());
	}

	public static IMIPResponse createTodoResponse(String itemUid, ICalendarElement calElement, String type) {
		return createNeedResponse(todoHeader, itemUid, calElement, false, Map.of("type", type));
	}

	private static IMIPResponse createNeedResponse(String header, String itemUid, ICalendarElement calElement,
			boolean needResponse, Map<String, String> additionalAttributes) {
		LoggerFactory.getLogger(IMIPResponse.class).info("need resp {} {}", itemUid, needResponse);
		IMIPResponse ret = new IMIPResponse();

		StringBuilder eventIcsUid = new StringBuilder(itemUid);
		if (calElement instanceof VEventOccurrence) {
			eventIcsUid.append("; recurid=\"" + ((VEventOccurrence) calElement).recurid.iso8601 + "\"");
		}
		eventIcsUid.append("; rsvp=\"" + needResponse + "\"");
		additionalAttributes.forEach((key, value) -> eventIcsUid.append("; " + key + "=\"" + value + "\""));

		RawField rf = new RawField(header, eventIcsUid.toString());
		UnstructuredField bmExtId = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
		ret.headerFields = Arrays.asList(bmExtId);
		return ret;
	}

	public static IMIPResponse createCounterResponse(String itemUid, String email, VEventOccurrence counterEvent) {
		IMIPResponse ret = new IMIPResponse();

		StringBuilder eventIcsUid = new StringBuilder(itemUid);
		eventIcsUid.append("; originator=\"" + email + "\"");

		if (counterEvent.recurid != null) {
			eventIcsUid.append("; recurid=\"" + counterEvent.recurid.iso8601 + "\"");
		}

		RawField rf = new RawField("X-BM-Event-Countered", eventIcsUid.toString());
		UnstructuredField bmExtId = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
		ret.headerFields = Arrays.asList(bmExtId);
		return ret;

	}

	public static IMIPResponse createEmptyResponse() {
		return new IMIPResponse();
	}

}
