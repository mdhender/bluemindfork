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
package net.bluemind.milter.action.eventforwarding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventCounter.CounterOriginator;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.config.Token;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.icalendar.parser.Sudo;
import net.bluemind.imip.parser.IIMIPParser;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.IMIPParserConfig;
import net.bluemind.imip.parser.IMIPParserFactory;
import net.bluemind.imip.parser.ITIPMethod;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.milter.action.MilterAction;
import net.bluemind.milter.action.MilterActionsFactory;
import net.bluemind.milter.action.UpdatedMailMessage;
import net.bluemind.network.topology.Topology;

public class EventForwardingAction implements MilterAction {

	private static final Logger logger = LoggerFactory.getLogger(EventForwardingAction.class);
	public static final String identifier = "EventForwardingAction";

	@Override
	public String identifier() {
		return identifier;
	}

	@Override
	public String description() {
		return "Handle forwarded events";
	}

	public static class EventForwardingActionFactory implements MilterActionsFactory {
		@Override
		public MilterAction create() {
			return new EventForwardingAction();
		}
	}

	@Override
	public void execute(UpdatedMailMessage modifier, Map<String, String> configuration,
			Map<String, String> evaluationData, IClientContext mailflowContext) {

		IIMIPParser parser = IMIPParserFactory
				.create(new IMIPParserConfig.IMIPParserConfigBuilder().failOnMissingMethod(false).create());
		IMIPInfos imip = parser.parse(modifier.getMessage());

		if (imip == null || imip.method != ITIPMethod.REQUEST) {
			return;
		}

		VEventSeries series = listOfEventsToSeries(imip.properties, imip.iCalendarElements, imip.uid);

		if (series.flatten().size() == 1) {
			VEvent event = series.mainOccurrence();
			BmDateTime recurId = event instanceof VEventOccurrence ? ((VEventOccurrence) event).recurid : null;

			String from = modifier.getMessage().getFrom().get(0).getAddress();
			Set<String> tos = new HashSet<>(
					modifier.getMessage().getTo().stream().map((addr -> ((Mailbox) addr).getAddress())).toList());

			String organizer = event.organizer.mailto;
			String coreHost = Topology.get().core().value.address();
			ClientSideServiceProvider provider = ClientSideServiceProvider.getProvider("http://" + coreHost + ":8090",
					Token.admin0());
			checkForForwards(modifier, mailflowContext, imip, recurId, from, tos, organizer, coreHost, provider);
		}
	}

	private void checkForForwards(UpdatedMailMessage modifier, IClientContext mailflowContext, IMIPInfos imip,
			BmDateTime recurId, String from, Set<String> tos, String organizer, String coreHost,
			ClientSideServiceProvider provider) {
		try (Sudo sudo = Sudo.byEmail(from, mailflowContext.getSenderDomain().uid, provider)) {
			String cal = ICalendarUids.defaultUserCalendar(sudo.getUser().uid);
			ClientSideServiceProvider userProvider = ClientSideServiceProvider
					.getProvider("http://" + coreHost + ":8090", sudo.context.getSessionId());

			ICalendar service = userProvider.instance(ICalendar.class, cal);
			List<ItemValue<VEventSeries>> existingSeries = service.getByIcsUid(imip.uid);
			if (existingSeries.isEmpty()) {
				return;
			}
			ItemValue<VEventSeries> existingEventValue = existingSeries.get(0);
			VEventOccurrence existingEvent = findEvent(existingEventValue, recurId);
			Set<String> attendees = new HashSet<>(existingEvent.attendees.stream().map(att -> att.mailto).toList());

			Set<String> newAttendees = new HashSet<>();
			if (!from.equals(organizer) && attendees.contains(from)) {
				for (String addr : tos) {
					if (!attendees.contains(addr) && !addr.equals(organizer)) {
						logger.info("Message contains a forwarded event. Creating counter from {} to {}", from, addr);
						newAttendees.add(addr);
					}
				}
			}

			if (!newAttendees.isEmpty()) {
				sendCounter(modifier, from, newAttendees, existingEvent, service, sudo.getUser().displayName,
						existingEventValue);
			}

		}
	}

	private void sendCounter(UpdatedMailMessage modifier, String from, Set<String> newAttendees,
			VEventOccurrence existingEvent, ICalendar service, String originator,
			ItemValue<VEventSeries> existingSeries) {
		VEventCounter counter = new VEventCounter();

		counter.counter = existingEvent.copy();
		counter.originator = new CounterOriginator();
		counter.originator.email = from;
		counter.originator.commonName = originator;
		counter.counter.attendees = new ArrayList<>(
				counter.counter.attendees.stream().filter(att -> att.mailto.equals(from)).toList());
		newAttendees.forEach(to -> {
			Attendee newAttendee = Attendee.create(CUType.Individual, null, Role.NonParticipant,
					ParticipationStatus.Tentative, null, null, null, null, null, null, null, null, to);
			counter.counter.attendees.add(newAttendee);
		});
		existingSeries.value.counters = Arrays.asList(counter);
		service.update(existingSeries.uid, existingSeries.value, true);
		existingSeries.value.counters = Collections.emptyList();
		service.update(existingSeries.uid, existingSeries.value, false);
	}

	private VEventOccurrence findEvent(ItemValue<VEventSeries> byIcsUid, BmDateTime recurId) {
		if (recurId == null) {
			return VEventOccurrence.fromEvent(byIcsUid.value.main, null);
		}
		return byIcsUid.value.occurrence(recurId);
	}

	protected VEventSeries listOfEventsToSeries(Map<String, String> properties, List<ICalendarElement> elements,
			String icsUid) {
		List<ICalendarElement> mutableList = new ArrayList<>(elements);
		VEvent master = null;
		for (Iterator<ICalendarElement> iter = mutableList.iterator(); iter.hasNext();) {
			VEvent next = (VEvent) iter.next();
			if (!(next instanceof VEventOccurrence)) {
				master = next;
				iter.remove();
			}
		}
		VEventSeries series = new VEventSeries();
		series.acceptCounters = !Boolean.parseBoolean(properties.getOrDefault("X-MICROSOFT-DISALLOW-COUNTER", "false"));
		series.main = null != master ? master : null;
		series.occurrences = mutableList.stream().map(v -> (VEventOccurrence) v).collect(Collectors.toList());
		series.icsUid = icsUid;
		return series;
	}
}
