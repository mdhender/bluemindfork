/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.calendar.service.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.VEventUtil;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventCounter.CounterOriginator;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.directory.api.IDirEntryPath;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.Role;

/**
 * This sanitizer modifies events descriptions. For now it only modifies the
 * description due to the presence of a template in invited resources.
 * 
 * @see ResourceTemplateHandler
 */
public class VEventSeriesSanitizer implements ISanitizer<VEventSeries> {
	private static final Logger LOGGER = LoggerFactory.getLogger(VEventSeriesSanitizer.class);
	private ResourceTemplateHandler resourceTemplateHandler;
	private BmContext bmContext;
	private Container container;

	public VEventSeriesSanitizer(final BmContext bmContext, Container container) {
		this.bmContext = bmContext;
		this.container = container;
		this.resourceTemplateHandler = new ResourceTemplateHandler();
	}

	@Override
	public void create(final VEventSeries vEventSeries) {
		if (this.isMasterVersionAndHasAttendees(vEventSeries)) {
			// handle resources having templates
			this.resourceTemplateHandler.handleCreatedEvent(vEventSeries,
					this.bmContext.getSecurityContext().getContainerUid());
		}
	}

	/**
	 * @param oldVEventMessage if <code>null</code>, use
	 *                         currentVEventMessage.oldEvent
	 */
	@Override
	public void update(final VEventSeries oldVEventSeries, final VEventSeries currentVEventSeries) {
		try {
			if (this.isMasterVersionAndHasAttendees(currentVEventSeries)) {
				this.onMasterVersionUpdated(currentVEventSeries, oldVEventSeries,
						this.bmContext.getSecurityContext().getContainerUid());
				// FIXME : And sendNotification === true
			} else if (isAttendeeVersionAndHasAttendees(currentVEventSeries)) {
				this.onAttendeeVersionUpdated(currentVEventSeries, oldVEventSeries,
						this.bmContext.getSecurityContext().getContainerUid());
			}
		} catch (ServerFault e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	// mostly a copy from IcsHook
	private boolean isMasterVersionAndHasAttendees(final VEventSeries message) throws ServerFault {
		return message.meeting() && message.master(bmContext.getSecurityContext().getContainerUid(), container.owner);
	}

	private boolean isAttendeeVersionAndHasAttendees(final VEventSeries message) throws ServerFault {
		return message.meeting() && !message.master(bmContext.getSecurityContext().getContainerUid(), container.owner);
	}

	// mostly a copy from IcsHook
	private void onMasterVersionUpdated(VEventSeries currentVEventSeries, VEventSeries oldEventSeries,
			final String domainUid) {
		sanitizeSequence(currentVEventSeries, oldEventSeries);
		sanitizeDraft(currentVEventSeries, oldEventSeries);
		sanitizeResourceTemplate(currentVEventSeries, oldEventSeries, domainUid);
	}

	private void sanitizeSequence(VEventSeries currentVEventSeries, VEventSeries oldEventSeries) {
		sanitizeSequence(currentVEventSeries.main, oldEventSeries.main);
		currentVEventSeries.occurrences
				.forEach(current -> sanitizeSequence(current, oldEventSeries.occurrence(current.recurid)));
	}

	private void sanitizeSequence(VEvent current, VEvent old) {
		if (old != null && current.sequence == null) {
			current.sequence = old.sequence;
		}
	}

	private void sanitizeDraft(VEventSeries currentVEventSeries, VEventSeries oldEventSeries) {
		sanitizeDraft(currentVEventSeries.main, oldEventSeries.main, null);
		currentVEventSeries.occurrences.forEach(current -> sanitizeDraft(current,
				oldEventSeries.occurrence(current.recurid), currentVEventSeries.main.draft));
	}

	private void sanitizeDraft(VEvent current, VEvent old, Boolean forceDraft) {
		if (current.draft && old != null && !old.draft) {
			current.draft = false;
		} else if (!current.draft && forceDraft != null) {
			current.draft = forceDraft;
		}
	}

	private void sanitizeResourceTemplate(VEventSeries currentVEventSeries, VEventSeries oldEventSeries,
			final String domainUid) {
		final List<VEvent> flatten = currentVEventSeries.flatten();
		Set<Attendee> userAttendingToSeries = new HashSet<>();
		Set<Attendee> userDeletedFromSeries = new HashSet<>();
		for (VEvent evt : flatten) {
			VEvent oldEvent = findCorrespondingEvent(oldEventSeries, evt);
			if (null == oldEvent) {
				oldEvent = new VEvent();
				if (evt.exception() && null != currentVEventSeries.main) {
					oldEvent.attendees = currentVEventSeries.main.attendees;
				}
			}
			List<VEvent.Attendee> oldEventAttendees = oldEvent.attendees;
			List<VEvent.Attendee> updatedEventAttendees = evt.attendees;

			handleAddedAttendees(currentVEventSeries, userAttendingToSeries, evt, oldEventAttendees,
					updatedEventAttendees, domainUid);
			handleDeletedAttendees(userDeletedFromSeries, evt, oldEventAttendees, updatedEventAttendees);
		}
	}

	private void handleDeletedAttendees(Set<Attendee> userDeletedFromSeries, VEvent evt,
			List<VEvent.Attendee> oldEventAttendees, List<VEvent.Attendee> updatedEventAttendees) {
		List<VEvent.Attendee> deletedAttendees = VEvent.diff(oldEventAttendees, updatedEventAttendees);
		if (!deletedAttendees.isEmpty()) {
			// handle resources having templates
			this.resourceTemplateHandler.handleDeletedResources(evt, deletedAttendees);
		}
	}

	private void handleAddedAttendees(VEventSeries updatedEvent, Set<Attendee> userAttendingToSeries, VEvent evt,
			List<VEvent.Attendee> oldEventAttendees, List<VEvent.Attendee> updatedEventAttendees,
			final String domainUid) {
		List<VEvent.Attendee> addedAttendees = VEvent.diff(updatedEventAttendees, oldEventAttendees);
		if (!addedAttendees.isEmpty()) {
			// handle resources having templates
			this.resourceTemplateHandler.handleAddedResources(evt, addedAttendees, domainUid);
		}
	}

	// copy from IcsHook
	private VEvent findCorrespondingEvent(VEventSeries otherSeries, VEvent evt) {
		if (evt instanceof VEventOccurrence) {
			VEventOccurrence match = otherSeries.occurrence(((VEventOccurrence) evt).recurid);
			if (match != null) {
				return match;
			}
		} else {
			if (null != otherSeries.main) {
				return otherSeries.main;
			}
		}
		return null;
	}

	public static class Factory implements ISanitizerFactory<VEventSeries> {

		@Override
		public Class<VEventSeries> support() {
			return VEventSeries.class;
		}

		@Override
		public ISanitizer<VEventSeries> create(final BmContext context, Container container) {
			return new VEventSeriesSanitizer(context, container);
		}
	}

	private void onAttendeeVersionUpdated(VEventSeries currentVEventSeries, VEventSeries oldEventSeries,
			final String domainUid) {
		sanitizeForward(currentVEventSeries, oldEventSeries);
	}

	private void sanitizeForward(VEventSeries current, VEventSeries old) {
		List<VEvent> flatten = current.flatten();
		for (VEvent event : flatten) {
			VEvent oldEvent = VEventUtil.findOrCalculateCorrespondingEvent(old, event);
			sanitizeForward(current, event, oldEvent);
		}
	}

	private void sanitizeForward(VEventSeries series, VEvent current, VEvent old) {
		Optional<Attendee> me = getCalendarOwnerAttendeeEntry(current);
		if (me.isPresent()) {
			Set<Attendee> attendees = ICalendarElement.diff(current.attendees, old.attendees).stream()
					.filter(attendee -> attendee.role == Role.NonParticipant)
					.filter(attendee -> me.get().mailto.equals(attendee.sentBy)).collect(Collectors.toSet());
			if (!attendees.isEmpty()) {
				VEventCounter counter = createCounter(current, attendees, me.get());
				addToSeries(series, counter);
			}
		}

	}

	private void addToSeries(VEventSeries series, VEventCounter counter) {
		series.counters = series.counters.stream().filter(existing -> !sameCounter(existing, counter))
				.collect(Collectors.toList());
		series.counters.add(counter);
	}

	private boolean sameCounter(VEventCounter existing, VEventCounter counter) {
		boolean same = true;
		same &= counter.originator.equals(existing.originator);
		same &= counter.counter.recurid == null ? existing.counter.recurid == null
				: counter.counter.recurid.equals(existing.counter.recurid);
		return same;
	}

	private VEventCounter createCounter(VEvent event, Set<Attendee> attendees, Attendee me) {
		VEventOccurrence occurrence = event instanceof VEventOccurrence ? (VEventOccurrence) event.copy()
				: VEventOccurrence.fromEvent(event, null);
		VEventCounter counter = new VEventCounter();
		counter.counter = occurrence;
		counter.originator = new CounterOriginator();
		counter.originator.email = me.mailto;
		counter.originator.commonName = me.commonName;
		counter.counter.attendees = new ArrayList<>(attendees);
		counter.counter.attendees.add(me);
		return counter;
	}

	private Optional<Attendee> getCalendarOwnerAttendeeEntry(VEvent current) {
		return current.attendees.stream().filter(attendee -> isCalendarOwner(attendee)).findAny();
	}

	private boolean isCalendarOwner(Attendee attendee) {
		if (attendee.dir != null) {
			String path = attendee.dir.substring("bm://".length());
			return IDirEntryPath.getDomain(path).equals(container.domainUid)
					&& IDirEntryPath.getEntryUid(path).equals(container.owner);
		}
		return false;
	}

}
