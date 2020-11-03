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
import java.util.List;

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

	public static IMIPResponse createNeedResponse(String itemUid, ICalendarElement calElement) {
		return createNeedResponse(itemUid, calElement, true);
	}

	public static IMIPResponse createCanceledResponse(String itemUid) {
		IMIPResponse ret = new IMIPResponse();

		StringBuilder eventIcsUid = new StringBuilder(itemUid);

		RawField rf = new RawField("X-BM-Event-Canceled", eventIcsUid.toString());
		UnstructuredField bmExtId = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
		ret.headerFields = Arrays.asList(bmExtId);
		return ret;

	}

	public static IMIPResponse createNeedResponse(String itemUid, ICalendarElement calElement, boolean needResponse) {
		LoggerFactory.getLogger(IMIPResponse.class).info("need resp {} {}", itemUid, needResponse);
		IMIPResponse ret = new IMIPResponse();

		StringBuilder eventIcsUid = new StringBuilder(itemUid);
		if (calElement instanceof VEventOccurrence) {
			eventIcsUid.append("; recurid=\"" + ((VEventOccurrence) calElement).recurid.iso8601 + "\"");
		}
		eventIcsUid.append("; rsvp=\"" + needResponse + "\"");

		RawField rf = new RawField("X-BM-Event", eventIcsUid.toString());
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

}
