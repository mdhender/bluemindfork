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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;

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

	public VEventSeriesSanitizer(final BmContext bmContext) {
		this.bmContext = bmContext;
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
			}
		} catch (ServerFault e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	// mostly a copy from IcsHook
	private boolean isMasterVersionAndHasAttendees(final VEventSeries message) throws ServerFault {
		return message.meeting() && message.master(bmContext.getSecurityContext().getContainerUid(),
				bmContext.getSecurityContext().getSubject());
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
		public ISanitizer<VEventSeries> create(final BmContext context) {
			return new VEventSeriesSanitizer(context);
		}
	}

}
