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
package net.bluemind.imip.parser.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.TextBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEvent.Transparency;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.parser.ICal4jEventHelper;
import net.bluemind.icalendar.parser.ICal4jHelper;
import net.bluemind.icalendar.parser.ObservanceMapper;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.lib.ical4j.util.IcalConverter;
import net.bluemind.todolist.api.VTodo;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.Sequence;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import net.freeutils.tnef.Attachment;
import net.freeutils.tnef.Attr;
import net.freeutils.tnef.MAPIProp;
import net.freeutils.tnef.MAPIPropName;
import net.freeutils.tnef.MAPIProps;

public class ITIPPartParser {

	private static final Logger logger = LoggerFactory.getLogger(ITIPPartParser.class);

	private IMIPInfos imip;

	public ITIPPartParser(IMIPInfos imip) {
		this.imip = imip;
	}

	public IMIPInfos parse(Entity e) throws IOException, ParserException, ServerFault {
		TextBody body = (TextBody) e.getBody();
		Reader reader = null;
		if ("us-ascii".equalsIgnoreCase(body.getMimeCharset())) {
			// outlook does not set the charset on its ICS parts
			// and if it is really us-ascii we don't care as utf-8 == ascii for
			// ascii chars
			reader = new InputStreamReader(body.getInputStream(), "utf-8");
		} else {
			reader = body.getReader();
		}

		List<CalendarComponent> calendarComponents = IMIPParserHelper.fromICS(reader);
		if (calendarComponents.isEmpty()) {
			throw new IOException("Neither VEvent or VTodo found");
		}

		// X-WR-TIMEZONE
		Optional<String> globalTZ = calendarComponents.get(0).getProperty("X-WR-TIMEZONE") != null
				? Optional.of(calendarComponents.get(0).getProperty("X-WR-TIMEZONE").getValue())
				: Optional.empty();

		Summary summary = null;
		Uid uid = null;
		Organizer orga = null;
		Sequence seq = null;

		Map<String, String> tzMapping = ObservanceMapper.fromCalendarComponents(calendarComponents)
				.getTimezoneMapping();

		for (CalendarComponent part : calendarComponents) {
			if ((part instanceof net.fortuna.ical4j.model.component.VEvent)) {
				net.fortuna.ical4j.model.component.VEvent icalVEvent = (net.fortuna.ical4j.model.component.VEvent) part;
				summary = icalVEvent.getSummary();
				uid = icalVEvent.getUid();
				orga = icalVEvent.getOrganizer();
				seq = icalVEvent.getSequence();
				VEvent calElement = new VEvent();
				calElement = new ICal4jEventHelper<VEvent>().parseIcs(calElement, part, globalTZ, tzMapping,
						Optional.empty()).value;

				// DTEND
				calElement.dtend = IcalConverter.convertToDateTime(icalVEvent.getEndDate(), globalTZ, tzMapping);

				// TRANSPARANCY
				if (icalVEvent.getTransparency() != null) {
					String transparency = icalVEvent.getTransparency().getValue().toLowerCase();
					if ("opaque".equals(transparency)) {
						calElement.transparency = Transparency.Opaque;
					} else if ("transparent".equals(transparency)) {
						calElement.transparency = Transparency.Transparent;
					} else {
						logger.error("Unsupported Transparency " + transparency);

					}
				}

				Iterator<VEvent.Attendee> it = calElement.attendees.iterator();
				while (it.hasNext()) {
					VEvent.Attendee at = it.next();
					if (at.mailto == null || !Regex.EMAIL.validate(at.mailto.toLowerCase())) {
						it.remove();
						logger.warn("[{}] Removing invalid attendee '{}' from ICS", imip.messageId, at.mailto);
					} else {
						at.mailto = at.mailto.toLowerCase();
					}
				}

				imip.iCalendarElements.add(calElement);
			} else {
				// VToDo
				if ((part instanceof net.fortuna.ical4j.model.component.VToDo)) {
					VToDo icalVTodo = (VToDo) part;
					summary = icalVTodo.getSummary();
					uid = icalVTodo.getUid();
					orga = icalVTodo.getOrganizer();
					seq = icalVTodo.getSequence();
					VTodo calElement = new VTodo();
					new ICal4jHelper<VTodo>().parseIcs(calElement, part, globalTZ, tzMapping, Optional.empty());

					// DUE
					calElement.due = IcalConverter.convertToDateTime(icalVTodo.getDue(), globalTZ, tzMapping);

					// PERCENT
					if (icalVTodo.getPercentComplete() != null) {
						calElement.percent = new Integer(icalVTodo.getPercentComplete().getValue());
					}

					// COMPLETE
					if (icalVTodo.getDateCompleted() != null) {
						calElement.completed = IcalConverter.convertToDateTime(icalVTodo.getDateCompleted(), globalTZ,
								tzMapping);
					}
					imip.iCalendarElements.add(calElement);
					break;
				}
			}

		}

		String summaryAsString = null != summary ? summary.getValue() : "";
		logger.info("[" + imip.messageId + "] summary: " + summaryAsString);
		logger.info("[" + imip.messageId + "] uid: " + uid.getValue());
		imip.uid = uid.getValue();

		if (orga != null) {
			URI uri = orga.getCalAddress();
			if (uri != null && "MAILTO".equalsIgnoreCase(uri.getScheme())) {
				String mail = uri.getSchemeSpecificPart();
				logger.info("[" + imip.messageId + "] organizer email: " + mail);
				imip.organizerEmail = mail;
			} else {
				logger.warn("[" + imip.messageId + "] unhandled cal address: '" + uri + "', orga: " + orga);
			}
		} else {
			logger.warn("[" + imip.messageId + "] organizer is null");
		}

		if (seq != null) {
			int seqNum = seq.getSequenceNo();
			logger.info("[" + imip.messageId + "] seq: " + seqNum);
			imip.sequence = seqNum;
		}

		return imip;
	}

	public IMIPInfos parse(net.freeutils.tnef.Message tnef) throws IOException {

		if (tnef.getAttribute(Attr.attMessageClass) != null
				&& "IPM.TaskRequest".equals(tnef.getAttribute(Attr.attMessageClass).getValue())) {

			for (Attachment att : tnef.getAttachments()) {

				MAPIProps mapiProps = att.getNestedMessage().getMAPIProps();
				MAPIProp taskStatus = mapiProps.getProp(new MAPIPropName(MAPIProp.GUID_CDOPROPSETID2, 0x8101));
				if (taskStatus != null) {

					VTodo vtodo = new VTodo();
					MAPIProp percent = mapiProps.getProp(new MAPIPropName(MAPIProp.GUID_CDOPROPSETID2, 0x8102));
					MAPIProp startDate = mapiProps.getProp(new MAPIPropName(MAPIProp.GUID_CDOPROPSETID2, 0x8104));
					MAPIProp dueDate = mapiProps.getProp(new MAPIPropName(MAPIProp.GUID_CDOPROPSETID2, 0x8105));
					MAPIProp owner = mapiProps.getProp(new MAPIPropName(MAPIProp.GUID_CDOPROPSETID2, 0x811F));

					MAPIProp uid = mapiProps.getProp(new MAPIPropName(MAPIProp.GUID_CDOPROPSETID4, 0x8519));
					// MAPIProp subject = mapiProps.getProp(3613);

					// MAPIProp complete = mapiProps.getProp(new
					// MAPIPropName(MAPIProp.GUID_CDOPROPSETID2, 0x811C));
					// MAPIProp completeDate = mapiProps.getProp(new
					// MAPIPropName(MAPIProp.GUID_CDOPROPSETID2, 0x810F));

					// FIXME
					for (MAPIProp p : mapiProps.getProps()) {
						if (p.toString().contains("PR_NORMALIZED_SUBJECT")) {
							vtodo.summary = p.getValue().toString();
						}
					}

					vtodo.organizer = new ICalendarElement.Organizer(owner.getValue().toString());

					SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy");
					try {
						Date dtstart = sdf.parse(startDate.getValue().toString());
						vtodo.dtstart = BmDateTimeWrapper.fromTimestamp(dtstart.getTime());
					} catch (ParseException e) {
						logger.error("Fail to parse start date {}", startDate.getValue());
					}

					try {
						Date due = sdf.parse(dueDate.getValue().toString());
						vtodo.due = BmDateTimeWrapper.fromTimestamp(due.getTime());
					} catch (ParseException e) {
						logger.error("Fail to parse due date {}", dueDate.getValue());
					}

					vtodo.percent = new Float(percent.getValue().toString()).intValue();

					imip.iCalendarElements.add(vtodo);
					imip.uid = uid.getValue().toString();

					return imip;

				} else {
					continue;
				}

			}

		} else {
			return null;
		}

		return null;
	}
}
