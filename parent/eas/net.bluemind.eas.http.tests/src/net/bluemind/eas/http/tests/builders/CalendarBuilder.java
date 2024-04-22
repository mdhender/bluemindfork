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
package net.bluemind.eas.http.tests.builders;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import org.w3c.dom.Document;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.MSEvent;
import net.bluemind.eas.backend.bm.calendar.EventConverter;
import net.bluemind.eas.backend.bm.compat.OldFormats;
import net.bluemind.eas.client.ProtocolVersion;
import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.calendar.CalendarResponse;
import net.bluemind.eas.dto.user.MSUser;
import net.bluemind.eas.serdes.calendar.CalendarResponseFormatter;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.eas.wbxml.WbxmlOutput;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;

public class CalendarBuilder {

	public static Document getSimpleEvent(ProtocolVersion version) throws Exception {
		VEventSeries defaultEvent = defaultEvent();

		ByteArrayOutputStream bos = getEventSignature(version, defaultEvent);
		return WBXMLTools.toXml(bos.toByteArray());
	}

	public static Document getEvent(ProtocolVersion version, VEventSeries series) throws Exception {
		ByteArrayOutputStream bos = getEventSignature(version, series);
		return WBXMLTools.toXml(bos.toByteArray());
	}

	private static ByteArrayOutputStream getEventSignature(ProtocolVersion version, VEventSeries series) {
		MSUser msUser = new MSUser("user", "user", "user", "user", null, null, false, null, Collections.emptySet(),
				null);
		BackendSession bs = new BackendSession(msUser, null, 0);
		EventConverter converter = new EventConverter();
		MSEvent msEvent = converter.convert(bs, ItemValue.create(series.icsUid, series));
		CalendarResponse response = OldFormats.update(msEvent, bs.getUser());

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		WbxmlOutput output = WbxmlOutput.of(bos);
		double valueOfVersion = Double.parseDouble(version.toString());
		WbxmlResponseBuilder builder = new WbxmlResponseBuilder(valueOfVersion, null, output);
		CalendarResponseFormatter cf = new CalendarResponseFormatter();
		builder.start(NamespaceMapping.SYNC);
		cf.append(builder, valueOfVersion, response, (a) -> {
		});
		builder.end((a) -> {
		});
		return bos;
	}

	public static VEventSeries defaultEvent() {
		VEventSeries series = new VEventSeries();
		series.icsUid = UUID.randomUUID().toString();

		VEvent event = new VEvent();

		event.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh")));
		event.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 1, 0, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh")));
		event.summary = "event";
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		event.attendees = new ArrayList<>();
		series.main = event;
		return series;
	}

}
