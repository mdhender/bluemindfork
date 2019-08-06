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
package net.bluemind.resource.service.event;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.hook.ICalendarHook;
import net.bluemind.calendar.hook.internal.VEventMessage;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.ResourceReservationMode;
import net.bluemind.resource.service.event.BookingStrategyFactory.RecurringEventException;
import net.bluemind.resource.service.event.BookingStrategyFactory.TentativeEventException;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;

public class ResourceCalendarHook implements ICalendarHook {

	private static final Logger logger = LoggerFactory.getLogger(ResourceCalendarHook.class);
	private TimeUnit unit;
	private int delay;

	public ResourceCalendarHook() {
		this(20, TimeUnit.SECONDS);
	}

	public ResourceCalendarHook(int i, TimeUnit seconds) {
		this.delay = i;
		this.unit = seconds;
	}

	@Override
	public void onEventCreated(VEventMessage message) {
		handleEventInvitations(message);
	}

	@Override
	public void onEventUpdated(VEventMessage message) {
		handleEventInvitations(message);
	}

	private void handleEventInvitations(VEventMessage message) {
		// FIXME depth of the death
		try {
			if (isResourceAttendeeVersion(message.vevent, message.container)) {
				boolean needUpdate = false;
				ResourceDescriptor rd = provider().instance(IResources.class, message.container.domainUid)
						.get(message.container.owner);
				ItemValue<ResourceDescriptor> resource = ItemValue.create(message.container.owner, rd);

				for (VEvent vEvent : message.vevent.flatten()) {
					try {
						List<Attendee> attendees = vEvent.attendees;
						for (Attendee attendee : attendees) {
							if (isCalOwnerAttendee(message.container, attendee)
									&& attendee.partStatus == ParticipationStatus.NeedsAction) {
								if (resource.value.reservationMode != ResourceReservationMode.OWNER_MANAGED) {
									boolean alreadyBooked;

									alreadyBooked = BookingStrategyFactory.create(vEvent).isBusy(resource, vEvent);

									if (alreadyBooked) {
										if (resource.value.reservationMode == ResourceReservationMode.AUTO_ACCEPT_REFUSE) {
											attendee.partStatus = ParticipationStatus.Declined;
											attendee.responseComment = getMessages(
													getOwnerLocale(message.vevent.main, message.container.domainUid))
															.getString("autorefuse");
											needUpdate = true;
										}
									} else {
										attendee.partStatus = ParticipationStatus.Accepted;
										attendee.responseComment = getMessages(
												getOwnerLocale(message.vevent.main, message.container.domainUid))
														.getString("autoaccept");
										needUpdate = true;
									}
								}
							}
						}
					} catch (TentativeEventException | RecurringEventException e) {
						logger.warn("Could not automatically set a decision for {}", vEvent.summary, e);
					}
				}
				if (needUpdate) {
					// BM-14853 Outlook sends the meeting request email before creating the event in
					// the calendar
					VertxPlatform.getVertx().setTimer(unit.toMillis(delay), (id) -> {
						updateEvent(message, resource, message.vevent);
					});
				}
			}
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
	}

	private Locale getOwnerLocale(VEvent vevent, String domain) {
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain);
		ItemValue<User> user = userService.byEmail(vevent.organizer.mailto);
		if (user != null) {
			IUserSettings userSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IUserSettings.class, domain);
			Map<String, String> settings = userSettingsService.get(user.uid);
			return new Locale(settings.getOrDefault("lang", "en"));
		}
		return Locale.ENGLISH;
	}

	private boolean isCalOwnerAttendee(Container container, Attendee attendee) {
		if (null == attendee.dir) {
			return false;
		}
		return attendee.dir.substring(attendee.dir.lastIndexOf("/") + 1).equals(container.owner);
	}

	private void updateEvent(VEventMessage message, ItemValue<ResourceDescriptor> resource, VEventSeries vEvent) {
		ICalendar veventService = provider(message).instance(ICalendar.class,
				ICalendarUids.resourceCalendar(resource.uid));
		veventService.update(message.itemUid, vEvent, true);
	}

	private boolean isResourceAttendeeVersion(VEventSeries vevent, Container container) {
		if (isMasterVersion(vevent, container)) {
			return false;
		}

		IDirectory directoryService = provider().instance(IDirectory.class, container.domainUid);
		DirEntry dirEntry = directoryService.findByEntryUid(container.owner);
		return dirEntry != null && dirEntry.kind == DirEntry.Kind.RESOURCE;
	}

	private boolean isMasterVersion(VEventSeries vevent, Container container) throws ServerFault {
		if (vevent.main == null) {
			return false;
		}
		if (vevent.main.attendees.isEmpty()) {
			return true;
		}

		if (vevent.main.organizer == null || vevent.main.organizer.dir == null) {
			return false;
		}

		IDirectory directoryService = provider().instance(IDirectory.class, container.domainUid);
		DirEntry dirEntry = directoryService.getEntry(vevent.main.organizer.dir.substring("bm://".length()));

		// FIXME organizer.dir ~= container.owner (no need to retrieve dirEntry
		// for that..)
		// return
		// vevent.organizer.dir.substring(vevent.organizer.dir.lastIndexOf("/")
		// + 1).equals(container.owner);
		return dirEntry.entryUid.equals(container.owner);
	}

	private IServiceProvider provider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	private IServiceProvider provider(VEventMessage message) {
		return ServerSideServiceProvider.getProvider(message.securityContext);
	}

	@Override
	public void onEventDeleted(VEventMessage message) {

	}

	private ResourceBundle getMessages(Locale locale) {
		return ResourceBundle.getBundle("reservation", locale);
	}

}