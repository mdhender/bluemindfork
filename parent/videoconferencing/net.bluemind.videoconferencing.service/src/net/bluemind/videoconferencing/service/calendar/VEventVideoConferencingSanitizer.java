/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.videoconferencing.service.calendar;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.videoconferencing.api.IVideoConferenceUids;
import net.bluemind.videoconferencing.api.IVideoConferencing;

public class VEventVideoConferencingSanitizer implements ISanitizer<VEventSeries> {

	private static final Logger logger = LoggerFactory.getLogger(VEventVideoConferencingSanitizer.class);

	private IVideoConferencing videoConferencingService;
	private BmContext context;
	private Container container;

	public VEventVideoConferencingSanitizer(BmContext context, Container container) {
		this.context = context;
		this.container = container;
		videoConferencingService = context.provider().instance(IVideoConferencing.class,
				context.getSecurityContext().getContainerUid());

	}

	@Override
	public void create(VEventSeries evt) {
		if (isMasterVersionAndHasAttendees(evt)) {
			videoConferencingService.add(evt.main);
		}
	}

	@Override
	public void update(VEventSeries old, VEventSeries current) {
		if (isMasterVersionAndHasAttendees(old)) {
			final List<VEvent> flatten = current.flatten();
			for (VEvent evt : flatten) {
				VEvent oldEvent = findCorrespondingEvent(old, evt);
				if (null == oldEvent) {
					oldEvent = new VEvent();
					if (evt.exception() && null != current.main) {
						oldEvent.attendees = current.main.attendees;
					}
				}

				boolean hadVideoConfResource = hasVideoConferencingResource(oldEvent.attendees);
				boolean hasVideoConfResource = hasVideoConferencingResource(evt.attendees);

				String newDescription = evt.description != null ? evt.description : "";
				if ((hadVideoConfResource != hasVideoConfResource) || !newDescription.equals(oldEvent.description)) {
					logger.info("Update videoconferencing infos for occurrence '{}' dtstart: {}", evt.summary,
							evt.dtstart);
					videoConferencingService.update(oldEvent, evt);
				}
			}
		}
	}

	private boolean hasVideoConferencingResource(List<Attendee> attendees) {
		IResources resourceService = context.getServiceProvider().instance(IResources.class,
				context.getSecurityContext().getContainerUid());
		return !attendees.stream().filter(a -> a.cutype == CUType.Resource).map(a -> getResource(a, resourceService))
				.filter(res -> res.isPresent()
						&& res.get().value.typeIdentifier.equals(IVideoConferenceUids.RESOURCETYPE_UID))
				.map(Optional::get).collect(Collectors.toList()).isEmpty();
	}

	private Optional<ItemValue<ResourceDescriptor>> getResource(Attendee a, IResources service) {
		String uid = a.dir.substring(a.dir.lastIndexOf("/") + 1);
		ResourceDescriptor res = service.get(uid);
		if (res != null) {
			return Optional.ofNullable(ItemValue.create(uid, res));
		}
		return Optional.empty();
	}

	// mostly a copy from VEventSeriesSanitizer
	private boolean isMasterVersionAndHasAttendees(final VEventSeries evt) throws ServerFault {
		return evt.meeting() && evt.master(context.getSecurityContext().getContainerUid(), container.owner);
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
		public ISanitizer<VEventSeries> create(BmContext context, Container container) {
			return new VEventVideoConferencingSanitizer(context, container);
		}

	}

}
