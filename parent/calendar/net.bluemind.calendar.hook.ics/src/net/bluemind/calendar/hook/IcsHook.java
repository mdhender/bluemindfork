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
package net.bluemind.calendar.hook;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.message.BodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import freemarker.template.TemplateException;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.auditlog.CalendarAuditor;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.calendar.helper.mail.CalendarMail.CalendarMailBuilder;
import net.bluemind.calendar.helper.mail.CalendarMailHelper;
import net.bluemind.calendar.helper.mail.EventAttachment;
import net.bluemind.calendar.helper.mail.EventAttachmentHelper;
import net.bluemind.calendar.helper.mail.Messages;
import net.bluemind.calendar.hook.internal.VEventMessage;
import net.bluemind.common.freemarker.FreeMarkerMsg;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.common.freemarker.MessagesResolverProvider;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlog.IAuditManager;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailCredentials;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.core.sendmail.SendmailResponse;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.IDomains;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;
import net.fortuna.ical4j.model.property.Method;

/**
 * Send an email to attendees {@link ICalendarElement.Attendee} when a
 * {@link VEvent} is created, updated or deleted in the future. No email if
 * {@link VEvent} occurs in the past. An ICS is attached if created or updated.
 *
 */
public class IcsHook implements ICalendarHook {

	private static final Logger logger = LoggerFactory.getLogger(IcsHook.class);
	private static final String DEFAULT_LANG = "fr";
	private ISendmail mailer;

	public IcsHook() {
		mailer = new Sendmail();
	}

	public IcsHook(ISendmail mailer) {
		this.mailer = mailer;
	}

	@Override
	public void onEventCreated(VEventMessage message) {
		if (!mustSendNotification(message)) {
			return;
		}

		try {
			if (isMasterVersion(message.vevent, message.container)) {
				sendSeriesInvitation(message, message.vevent);
				sendEventInvitations(message);
			} else {
				DirEntry dirEntry = getMyDirEntry(message);
				List<VEvent> flat = message.vevent.flatten();
				for (VEvent vEvent : flat) {
					Optional<EventAttendeeTuple> attendee = getMatchingAttendeeForEvent(vEvent, dirEntry);
					if (attendee.isPresent()) {
						sendParticipationToOrganizer(message, attendee.get(), vEvent);
					}
				}
			}
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}

	}

	@Override
	public void onEventUpdated(VEventMessage message) {
		VEventSeries updatedEvent = message.vevent;

		if (!mustSendNotification(message)) {
			return;
		}

		VEventSeries oldEventSeries = message.oldEvent;
		List<VEvent> flatten = updatedEvent.flatten();
		try {
			if (isMasterVersion(message.oldEvent, message.container)) {
				onMasterVersionUpdated(message, updatedEvent, oldEventSeries, flatten);
			} else {
				onAttendeeVersionUpdated(message, oldEventSeries, flatten);
			}
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}

	}

	private void onMasterVersionUpdated(VEventMessage message, VEventSeries updatedEvent, VEventSeries oldEventSeries,
			List<VEvent> flatten) {
		if (!isMasterVersion(message.vevent, message.container)) {
			// Changing owner
			sendInvitationToOrganizer(message);
		}

		if (oldEventSeries != null && oldEventSeries.counters.size() > updatedEvent.counters.size()) {
			for (VEventCounter counter : oldEventSeries.counters) {
				if (!updatedEvent.counters.contains(counter)) {
					// counter has either been accepted or refused. If the counter has been
					// accepted, the standard update request will be send, if it has been
					// refused, we send a declinecounter here
					VEventOccurrence correspondingEvent = getCorrespondingEvent(counter.counter, updatedEvent);
					VEvent correspondingOldEvent = getCorrespondingEvent(counter.counter, oldEventSeries);
					if (correspondingEvent == null && correspondingOldEvent == null) {
						sendDeclineCounterToAttendee(message, counter);
					} else if (correspondingEvent != null && correspondingOldEvent == null) {
						if (!correspondingEvent.dtstart.equals(counter.counter.dtstart)
								|| !correspondingEvent.dtend.equals(counter.counter.dtend)) {
							sendDeclineCounterToAttendee(message, counter);
						}
					} else if (!VEventUtil.eventChanged(correspondingEvent, correspondingOldEvent)
							&& (!correspondingEvent.dtstart.equals(counter.counter.dtstart)
									|| !correspondingEvent.dtend.equals(counter.counter.dtend))) {
						sendDeclineCounterToAttendee(message, counter);
					}
				}
			}
		}

		if (updatedEvent.main != null) {
			// exdate stuff
			Set<BmDateTime> newExdates = getNewExceptionList(oldEventSeries.main, updatedEvent.main);
			if (!newExdates.isEmpty()) {
				if (oldEventSeries.occurrences != null) {
					for (VEventOccurrence oldOcc : oldEventSeries.occurrences) {
						if (newExdates.contains(oldOcc.recurid)) {
							sendCancelToAttendees(message, oldOcc, oldOcc.attendees);
							newExdates.remove(oldOcc.recurid);
						}
					}
				}
				if (!newExdates.isEmpty()) {
					// FIXME: This is not necessary the only modification.
					sendExceptionsToAttendees(message, newExdates);
				}
				return;
			}
		}
		Set<Attendee> userAttendingToSeries = new HashSet<>();
		Set<Attendee> userDeletedFromSeries = new HashSet<>();
		for (VEvent evt : flatten) {
			VEvent oldEvent = findCorrespondingEvent(oldEventSeries, evt);
			if (null == oldEvent) {
				oldEvent = new VEvent();
				if (evt.exception() && null != updatedEvent.main) {
					oldEvent.attendees = updatedEvent.main.attendees;
				}
			}
			List<ICalendarElement.Attendee> oldEventAttendees = oldEvent.attendees;
			List<ICalendarElement.Attendee> updatedEventAttendees = evt.attendees;

			handleAddedAttendees(message, updatedEvent, userAttendingToSeries, evt, oldEventAttendees,
					updatedEventAttendees);
			handleDeletedAttendees(message, userDeletedFromSeries, evt, oldEventAttendees, updatedEventAttendees);
			handleUpdatedAttendees(message, userAttendingToSeries, evt, oldEvent, oldEventAttendees,
					updatedEventAttendees);
		}

		if (oldEventSeries != null) {
			for (VEventOccurrence occ : oldEventSeries.occurrences) {
				VEvent matchingNewEvent = findCorrespondingEvent(updatedEvent, occ);
				if (null == matchingNewEvent) {
					sendCancelToAttendees(message, occ, occ.attendees);
				}
			}
		}

	}

	private VEventOccurrence getCorrespondingEvent(VEventOccurrence evt, VEventSeries series) {
		if (evt.recurid == null) {
			return VEventOccurrence.fromEvent(series.main, null);
		} else {
			return series.occurrence(evt.recurid);
		}
	}

	private void handleUpdatedAttendees(VEventMessage message, Set<Attendee> userAttendingToSeries, VEvent evt,
			VEvent oldEvent, List<ICalendarElement.Attendee> oldEventAttendees,
			List<ICalendarElement.Attendee> updatedEventAttendees) {
		// Update invitation to other attendees
		List<ICalendarElement.Attendee> updatedAttendees = ICalendarElement.same(updatedEventAttendees,
				oldEventAttendees);

		if (!updatedAttendees.isEmpty() && VEventUtil.eventChanged(oldEvent, evt)) {
			updatedAttendees = updatedAttendees.stream().filter(a -> !userAttendingToSeries.contains(a))
					.collect(Collectors.toList());
			if (!evt.exception()) {
				sendUpdateToAttendees(message, evt, updatedAttendees);
				userAttendingToSeries.addAll(updatedAttendees);
			} else {
				sendUpdateToAttendees(message, evt, updatedAttendees);
			}
		}
	}

	private void handleDeletedAttendees(VEventMessage message, Set<Attendee> userDeletedFromSeries, VEvent evt,
			List<ICalendarElement.Attendee> oldEventAttendees, List<ICalendarElement.Attendee> updatedEventAttendees) {
		// Cancel invitation to removed attendees
		List<ICalendarElement.Attendee> deletedAttendees = ICalendarElement.diff(oldEventAttendees,
				updatedEventAttendees);

		if (!deletedAttendees.isEmpty()) {
			deletedAttendees = deletedAttendees.stream().filter(a -> !userDeletedFromSeries.contains(a))
					.collect(Collectors.toList());

			if (!evt.exception()) {
				userDeletedFromSeries.addAll(deletedAttendees);
			}
			sendCancelToAttendees(message, evt, deletedAttendees);
		}
	}

	private void handleAddedAttendees(VEventMessage message, VEventSeries updatedEvent,
			Set<Attendee> userAttendingToSeries, VEvent evt, List<ICalendarElement.Attendee> oldEventAttendees,
			List<ICalendarElement.Attendee> updatedEventAttendees) {
		// Send invitation to added attendees
		Set<ICalendarElement.Attendee> addedAttendees = new HashSet<>(
				ICalendarElement.diff(updatedEventAttendees, oldEventAttendees));
		if (!addedAttendees.isEmpty()) {
			if (!evt.exception()) {
				VEventMessage copy = message.copy();
				for (Attendee att : addedAttendees) {
					VEventSeries seriesForAttendee = getSeriesForAttendee(updatedEvent, att);
					copy.vevent = seriesForAttendee;
					copy.vevent.icsUid = message.vevent.icsUid;
					String ics = getIcsPart(copy.vevent.icsUid, Method.REQUEST, seriesForAttendee, att);
					sendInvitationToAttendees(copy, Arrays.asList(att), evt, ics);
				}
				userAttendingToSeries.addAll(addedAttendees);
			} else {
				addedAttendees = addedAttendees.stream().filter(a -> !userAttendingToSeries.contains(a))
						.collect(Collectors.toSet());
				String ics = getIcsPart(Optional.of(updatedEvent.acceptCounters), message.vevent.icsUid, Method.REQUEST,
						evt);
				sendInvitationToAttendees(message, new ArrayList<>(addedAttendees), evt, ics);
			}
		}
	}

	private void onAttendeeVersionUpdated(VEventMessage message, VEventSeries oldEventSeries, List<VEvent> flatten) {
		DirEntry dirEntry = getMyDirEntry(message);

		boolean counterHandled = false;
		for (VEventCounter currentCounter : message.vevent.counters) {
			Optional<VEventCounter> oldVersion = oldEventSeries.counters.stream().filter(c -> {
				return ((currentCounter.counter.recurid == null && c.counter.recurid == null)
						|| (currentCounter.counter.recurid != null
								&& currentCounter.counter.recurid.equals(c.counter.recurid)));
			}).findAny();
			if (!oldVersion.isPresent() || counterUpdated(currentCounter, oldVersion.get())) {
				counterHandled = true;
				onCounterProposalAdded(message, currentCounter, dirEntry);
			}
		}

		if (!counterHandled) {
			onAttendeeEventVersionUpdated(message, oldEventSeries, flatten, dirEntry);
		}
	}

	private boolean counterUpdated(VEventCounter currentCounter, VEventCounter oldCounter) {
		return (!currentCounter.counter.dtstart.equals(oldCounter.counter.dtstart))
				|| (!currentCounter.counter.dtend.equals(oldCounter.counter.dtend));
	}

	private void onAttendeeEventVersionUpdated(VEventMessage message, VEventSeries oldEventSeries, List<VEvent> flatten,
			DirEntry dirEntry) {
		for (VEvent evt : flatten) {
			VEvent oldEvent = findCorrespondingEvent(oldEventSeries, evt);
			Optional<EventAttendeeTuple> attendee = getMatchingAttendeeForEvent(evt, dirEntry);
			if (attendee.isPresent()) {
				Optional<EventAttendeeTuple> oldAttendee = getMatchingAttendeeForEvent(oldEvent, dirEntry);
				// check if the new participation != the old one
				if (!oldAttendee.isPresent()
						|| oldAttendee.get().attendee.partStatus != attendee.get().attendee.partStatus) {
					sendParticipationToOrganizer(message, attendee.get(), evt);
				}
				if (!evt.exception()) {
					Set<BmDateTime> exdates = getNewExceptionList(oldEventSeries.main, evt);
					if (exdates != null && !exdates.isEmpty()) {
						sendExceptionsToOrganizer(message, attendee.get(), exdates);
					}
				}
			}
		}
	}

	private void onCounterProposalAdded(VEventMessage message, VEventCounter counter, DirEntry dirEntry) {
		Optional<EventAttendeeTuple> attendee = getMatchingAttendeeForEvent(counter.counter, dirEntry);
		sendCounterProposalToOrganizer(message, attendee.get(), counter.counter);
	}

	@Override
	public void onEventDeleted(VEventMessage message) {
		if (!mustSendNotification(message)) {
			return;
		}
		try {
			if (isMasterVersion(message.vevent, message.container)) {
				if (message.vevent.main != null && !message.vevent.main.draft) {
					sendCancelSeries(message, message.vevent);
					sendCancelExceptions(message);
				}
			} else {
				DirEntry dirEntry = getMyDirEntry(message);
				boolean seriesSent = false;
				if (message.vevent.main != null) {
					Optional<EventAttendeeTuple> attendee = getMatchingAttendeeForEvent(message.vevent.main, dirEntry);
					if (attendee.isPresent() && attendee.get().attendee.partStatus != ParticipationStatus.Declined) {
						attendee.get().attendee.partStatus = ParticipationStatus.Declined;
						sendParticipationToOrganizer(message, attendee.get(), null);
						seriesSent = true;
					}
				}
				if (!seriesSent) {
					for (VEvent vEvent : message.vevent.occurrences) {
						Optional<EventAttendeeTuple> attendee = getMatchingAttendeeForEvent(vEvent, dirEntry);
						if (attendee.isPresent()) {
							if (attendee.get().attendee.partStatus != ParticipationStatus.Declined) {
								attendee.get().attendee.partStatus = ParticipationStatus.Declined;
								sendParticipationToOrganizer(message, attendee.get(), vEvent);
							}
						}
					}
				}
			}
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
	}

	private VEvent findCorrespondingEvent(VEventSeries otherSeries, VEvent evt) {
		VEvent match = null;
		if (evt instanceof VEventOccurrence) {
			match = otherSeries.occurrence(((VEventOccurrence) evt).recurid);
		} else {
			match = otherSeries.main;
		}
		if (match != null && match.draft) {
			return null;
		}
		return match;
	}

	private void sendSeriesInvitation(VEventMessage message, VEventSeries vevent) {
		if (vevent.main == null) {
			return;
		}
		for (Attendee attendee : vevent.main.attendees) {
			if (attendsToSeries(vevent, attendee)) {
				String ics = getIcsPart(message.vevent.icsUid, Method.REQUEST, message.vevent, attendee);
				sendInvitationToAttendees(message, Arrays.asList(attendee), vevent.main, ics);
			}
		}
	}

	private void sendEventInvitations(VEventMessage message) {
		VEvent master = message.vevent.main;
		List<Attendee> seriesAttendees = master != null ? master.attendees.stream().filter(a -> {
			return attendsToSeries(message.vevent, a);
		}).collect(Collectors.toList()) : Collections.emptyList();
		List<VEvent> events = message.vevent.flatten();

		for (VEvent evt : events) {
			for (Attendee attendee : evt.attendees) {
				if (!seriesAttendees.contains(attendee)) {
					CalendarAuditor auditor = CalendarAuditor.auditor(IAuditManager.instance(), message.securityContext,
							message.container);
					auditor.parentEventId(message.auditEventId).action("send-mail").addActionMetadata("kind", "ics")
							.addActionMetadata("icsKind", "invitation").addObjectMetadata("attendee", attendee);

					String ics = getIcsPart(Optional.of(message.vevent.acceptCounters), message.vevent.icsUid,
							Method.REQUEST, evt);

					auditor.audit(() -> sendInvitationToAttendees(message, Arrays.asList(attendee), evt, ics));
				}
			}

		}
	}

	private void sendDeclineCounterToAttendee(VEventMessage message, VEventCounter counter) {
		MailData md = MailData.declineCounter(message, counter.counter);

		String ics = getIcsPart(Optional.empty(), message.vevent.icsUid, Method.DECLINE_COUNTER, counter.counter);
		Attendee attendee = counter.counter.attendees.get(0);
		Mailbox recipient = SendmailHelper.formatAddress(attendee.commonName, attendee.mailto);

		sendNotificationToAttendee(message, counter.counter, md.senderSettings, md.subject, md.body, (locale) -> {
			return new MessagesResolver(Messages.getEventDetailMessages(locale),
					Messages.getDeclineCounterMessages(locale));
		}, Method.DECLINE_COUNTER, md.from, recipient, ics, md.data);

	}

	/**
	 * @param message
	 * @throws ServerFault
	 */
	private void sendInvitationToOrganizer(VEventMessage message) throws ServerFault {
		MailData md = MailData.organizer(message, message.vevent.main);

		Mailbox recipient = SendmailHelper.formatAddress(md.organizer.commonName, md.organizer.mailto);
		String ics = getIcsPart(Optional.empty(), message.vevent.icsUid, Method.REQUEST, message.vevent.main);

		sendNotificationToAttendee(message, message.vevent.main, md.senderSettings, md.subject, md.body, (locale) -> {
			return new MessagesResolver(Messages.getEventDetailMessages(locale),
					Messages.getEventOrganizationMessages(locale));
		}, Method.REQUEST, md.from, recipient, ics, md.data);
	}

	private void sendCounterProposalToOrganizer(VEventMessage message, EventAttendeeTuple event,
			VEventOccurrence counter) {
		Organizer organizer = event.event.organizer;
		if (attendeeIsOrganizer(event.attendee, organizer)) {
			return;
		}

		String subject = "EventCounterProposalUpdateSubject.ftl";
		String body = "EventCounterProposal.ftl";
		Method method = Method.COUNTER;

		Mailbox from = SendmailHelper.formatAddress(event.attendee.commonName, event.attendee.mailto);
		DirEntry fromDirEntry = provider().instance(IDirectory.class, message.container.domainUid)
				.getEntry(event.attendee.dir.substring("bm://".length()));

		Mailbox recipient = SendmailHelper.formatAddress(organizer.commonName, organizer.mailto);

		Map<String, Object> data = new HashMap<>();
		data.put("attendee", event.attendee.commonName);
		data.put("state", CalendarMailHelper.extractPartState(event.attendee.partStatus));
		data.putAll(new CalendarMailHelper().extractVEventData(event.event));
		if (event.attendee.responseComment != null && !event.attendee.responseComment.trim().isEmpty()) {
			data.put("note", event.attendee.responseComment);
		} else {
			data.put("note", "");
		}

		String ics = null;
		VEventOccurrence attendeeCounter = counter.copy();
		attendeeCounter.attendees = Arrays.asList(event.attendee);
		ics = VEventServiceHelper.convertToIcs(Optional.empty(), message.vevent.icsUid, method, attendeeCounter);

		Map<String, String> senderSettings = getSenderSettings(message, fromDirEntry);
		sendNotificationToOrganizer(message, attendeeCounter, senderSettings, subject, body, (locale) -> {
			return new MessagesResolver(Messages.getEventCounterMessages(locale),
					Messages.getEventUpdateMessages(locale), Messages.getEventParitipactionUpdateMessages(locale));
		}, method, from, recipient, ics, data);

	}

	private void sendInvitationToAttendees(VEventMessage message, List<Attendee> attendees, VEvent event, String ics)
			throws ServerFault {
		MailData md = MailData.create(message, event);

		boolean inPast = occursInThePast(event);

		for (ICalendarElement.Attendee attendee : attendees) {
			SendmailResponse ret = sendNotification(message, event, ics, md.subject, md.body, Method.REQUEST,
					md.organizer, md.from, md.senderSettings, md.data, inPast, attendee);
			if (ret.isError()) {
				sendInvitationErrorToOrganizer(message, event, attendee);
			}
		}

	}

	private void sendInvitationErrorToOrganizer(VEventMessage message, VEvent event, Attendee attendee) {
		Organizer organizer = event.organizer;

		String subject = "EventInvitationErrorSubject.ftl";
		String body = "EventInvitationError.ftl";

		String defaultAlias = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class)
				.get(message.container.domainUid).value.defaultAlias;

		String noReply = "noreply@" + defaultAlias;
		Mailbox from = SendmailHelper.formatAddress(noReply, noReply);
		Mailbox recipient = SendmailHelper.formatAddress(organizer.commonName, organizer.mailto);

		Map<String, Object> data = new HashMap<>();
		data.put("attendee", attendee.commonName + " (" + attendee.mailto + ")");
		data.putAll(new CalendarMailHelper().extractVEventData(event));

		sendNotificationToOrganizer(message, event, Collections.emptyMap(), subject, body, (locale) -> {
			return new MessagesResolver(Messages.getEventInvitationErrorMessages(locale));
		}, Method.REPLY, from, recipient, null, data);

	}

	private SendmailResponse sendNotification(VEventMessage message, VEvent event, String ics, String subject,
			String body, Method method, Organizer organizer, Mailbox from, Map<String, String> senderSettings,
			Map<String, Object> data, boolean inPast, ICalendarElement.Attendee attendee) {
		if (attendeeIsOrganizer(attendee, organizer)) {
			return SendmailResponse.success();
		}

		Mailbox recipient = SendmailHelper.formatAddress(attendee.commonName, attendee.mailto);

		if (inPast) {
			attendee.rsvp = false;
		}

		return sendNotificationToAttendee(message, event, senderSettings, subject, body, (locale) -> {
			return new MessagesResolver(Messages.getEventDetailMessages(locale),
					Messages.getEventCreateMessages(locale));
		}, method, from, recipient, ics, data);
	}

	/**
	 * @param message
	 * @param addedAttendees
	 * @throws ServerFault
	 */
	private void sendCancelSeries(VEventMessage message, VEventSeries series) throws ServerFault {
		if (series.main == null) {
			return;
		}

		MailData md = MailData.cancel(message, series.main);
		for (ICalendarElement.Attendee attendee : series.main.attendees) {
			if (attendeeIsOrganizer(attendee, md.organizer) || !attendsToSeries(series, attendee)) {
				continue;
			}

			Mailbox recipient = SendmailHelper.formatAddress(attendee.commonName, attendee.mailto);
			String ics = getIcsPart(Optional.empty(), message.vevent.icsUid, Method.CANCEL, series.main);
			sendNotificationToAttendee(message, series.main, md.senderSettings, md.subject, md.body, (locale) -> {
				return new MessagesResolver(Messages.getEventDetailMessages(locale),
						Messages.getEventDeleteMessages(locale));
			}, Method.CANCEL, md.from, recipient, ics, md.data);
		}

	}

	private void sendCancelExceptions(VEventMessage message) {
		VEvent master = message.vevent.main;
		List<Attendee> seriesAttendees = master != null ? master.attendees.stream().filter(a -> {
			return attendsToSeries(message.vevent, a);
		}).collect(Collectors.toList()) : Collections.emptyList();

		List<VEvent> events = message.vevent.flatten();

		for (VEvent evt : events) {
			for (Attendee attendee : evt.attendees) {
				if (!seriesAttendees.contains(attendee)) {
					MailData md = MailData.cancel(message, evt);
					String ics = getIcsPart(Optional.empty(), message.vevent.icsUid, Method.CANCEL, evt);

					Mailbox recipient = SendmailHelper.formatAddress(attendee.commonName, attendee.mailto);
					sendNotificationToAttendee(message, evt, md.senderSettings, md.subject, md.body, (locale) -> {
						return new MessagesResolver(Messages.getEventDetailMessages(locale),
								Messages.getEventDeleteMessages(locale));
					}, Method.CANCEL, md.from, recipient, ics, md.data);
				}

			}
		}

	}

	private void sendCancelToAttendees(VEventMessage message, VEvent evt, List<Attendee> deletedAttendees) {
		MailData md = MailData.cancel(message, evt);
		for (ICalendarElement.Attendee attendee : deletedAttendees) {
			Mailbox recipient = SendmailHelper.formatAddress(attendee.commonName, attendee.mailto);

			String ics = getIcsPart(Optional.empty(), message.vevent.icsUid, Method.CANCEL, evt);

			sendNotificationToAttendee(message, evt, md.senderSettings, md.subject, md.body, (locale) -> {
				return new MessagesResolver(Messages.getEventDetailMessages(locale),
						Messages.getEventDeleteMessages(locale));
			}, Method.CANCEL, md.from, recipient, ics, md.data);
		}

	}

	/**
	 * @param message
	 * @param addedAttendees
	 * @throws ServerFault
	 */
	private void sendUpdateToAttendees(VEventMessage message, VEvent event, List<ICalendarElement.Attendee> attendees) {
		MailData md = MailData.update(message, event);
		boolean inPast = occursInThePast(event);
		String ics = !event.exception() ? getIcsPart(message.vevent.icsUid, Method.REQUEST, message.vevent, null)
				: getIcsPart(Optional.of(message.vevent.acceptCounters), message.vevent.icsUid, Method.REQUEST, event);

		for (ICalendarElement.Attendee attendee : attendees) {
			if (attendeeIsOrganizer(attendee, md.organizer)) {
				continue;
			}

			Mailbox recipient = SendmailHelper.formatAddress(attendee.commonName, attendee.mailto);

			if (inPast) {
				attendee.rsvp = false;
			}

			sendNotificationToAttendee(message, event, md.senderSettings, md.subject, md.body, (locale) -> {
				return new MessagesResolver(Messages.getEventDetailMessages(locale),
						Messages.getEventUpdateMessages(locale));
			}, Method.REQUEST, md.from, recipient, ics, md.data);
		}
	}

	/**
	 * Send exceptions notifications
	 * 
	 * @param message
	 * @param exdates
	 * @throws ServerFault
	 */
	private void sendExceptionsToAttendees(VEventMessage message, Set<BmDateTime> exdates) throws ServerFault {
		MailData md = MailData.exception(message, message.vevent.main);
		for (ICalendarElement.Attendee attendee : message.vevent.main.attendees) {
			attendee.rsvp = false;
		}

		for (BmDateTime exdate : exdates) {
			// add some magic
			VEventOccurrence occurrence = VEventOccurrence.fromEvent(message.vevent.main, exdate);
			occurrence.dtstart = exdate;
			occurrence.exdate = null;

			String ics = getIcsPart(Optional.of(message.vevent.acceptCounters), message.vevent.icsUid, Method.CANCEL,
					occurrence);

			HashMap<String, Object> data = new HashMap<>();
			data.putAll(new CalendarMailHelper().extractVEventData(occurrence));

			for (ICalendarElement.Attendee attendee : message.vevent.main.attendees) {
				if (attendeeIsOrganizer(attendee, md.organizer)) {
					continue;
				}

				Mailbox recipient = SendmailHelper.formatAddress(attendee.commonName, attendee.mailto);

				sendNotificationToAttendee(message, occurrence, md.senderSettings, md.subject, md.body, (locale) -> {
					return new MessagesResolver(Messages.getEventDetailMessages(locale),
							Messages.getExceptions(locale));
				}, Method.CANCEL, md.from, recipient, ics, data);
			}

		}
	}

	/**
	 * @param message
	 * @param event
	 * @throws ServerFault
	 */
	private void sendParticipationToOrganizer(VEventMessage message, EventAttendeeTuple event, VEvent evt)
			throws ServerFault {

		Organizer organizer = event.event.organizer;
		if (attendeeIsOrganizer(event.attendee, organizer)) {
			return;
		}

		String subject = "EventParticipationUpdateSubject.ftl";
		String body = "EventParticipationUpdate.ftl";
		Method method = Method.REPLY;

		Mailbox from = SendmailHelper.formatAddress(event.attendee.commonName, event.attendee.mailto);
		DirEntry fromDirEntry = provider().instance(IDirectory.class, message.container.domainUid)
				.getEntry(event.attendee.dir.substring("bm://".length()));

		Mailbox recipient = SendmailHelper.formatAddress(organizer.commonName, organizer.mailto);

		Map<String, Object> data = new HashMap<>();
		data.put("attendee", event.attendee.commonName);
		data.put("state", CalendarMailHelper.extractPartState(event.attendee.partStatus));
		data.putAll(new CalendarMailHelper().extractVEventData(event.event));
		if (event.attendee.responseComment != null && !event.attendee.responseComment.trim().isEmpty()) {
			data.put("note", event.attendee.responseComment);
		} else {
			data.put("note", "");
		}

		String ics = null;
		if (null == evt) {
			// evt is null when we change participation on the series (not a
			// particular occurence)
			VEventSeries seriesForAttendee = reduceToAttendee(message.vevent.copy(), event.attendee);
			ics = getIcsPart(message.vevent.icsUid, method, seriesForAttendee, event.attendee);
			evt = seriesForAttendee.main;
		} else {
			VEvent eventForAttendee = evt.copy();
			eventForAttendee.attendees = Arrays.asList(event.attendee);
			ics = VEventServiceHelper.convertToIcs(Optional.empty(), message.vevent.icsUid, method, eventForAttendee);
		}
		Map<String, String> senderSettings = getSenderSettings(message, fromDirEntry);
		sendNotificationToOrganizer(message, evt, senderSettings, subject, body, (locale) -> {
			return new MessagesResolver(Messages.getEventDetailMessages(locale),
					Messages.getEventUpdateMessages(locale), Messages.getEventParitipactionUpdateMessages(locale));
		}, method, from, recipient, ics, data);

	}

	private String getLocale(Map<String, String> senderSettings) {
		String lang = senderSettings.get("lang");
		if (lang == null) {
			lang = DEFAULT_LANG;
		}

		return lang;
	}

	/**
	 * @param message
	 * @param attendee
	 * @param declined
	 * @throws ServerFault
	 */
	private void sendExceptionsToOrganizer(VEventMessage message, EventAttendeeTuple attendee, Set<BmDateTime> exdates)
			throws ServerFault {

		Organizer organizer = attendee.event.organizer;
		if (attendeeIsOrganizer(attendee.attendee, organizer)) {
			return;
		}
		for (BmDateTime exdate : exdates) {
			attendee = new EventAttendeeTuple(attendee.attendee, VEventOccurrence.fromEvent(attendee.event, exdate));
			attendee.event.dtstart = exdate;
			attendee.attendee.partStatus = ParticipationStatus.Declined;
			sendParticipationToOrganizer(message, attendee, attendee.event);
		}
	}

	private void sendNotificationToOrganizer(VEventMessage message, VEvent vevent, Map<String, String> senderSettings,
			String subjectTemplate, String template, MessagesResolverProvider messagesResolverProvider, Method method,
			Mailbox from, Mailbox recipient, String ics, Map<String, Object> data) {
		sendNotification(message, vevent, senderSettings, subjectTemplate, template, messagesResolverProvider, method,
				from, recipient, ics, data, Arrays.asList(recipient), null);
	}

	private SendmailResponse sendNotificationToAttendee(VEventMessage message, VEvent event,
			Map<String, String> senderSettings, String subjectTemplate, String template,
			MessagesResolverProvider messagesResolverProvider, Method method, Mailbox from, Mailbox recipient,
			String ics, Map<String, Object> data) {

		List<Mailbox> attendeeListTo = new ArrayList<>(event.attendees.size());
		List<Mailbox> attendeeListCc = new ArrayList<>(event.attendees.size());
		for (ICalendarElement.Attendee attendee : event.attendees) {
			if (attendeeIsOrganizer(attendee, event.organizer)) {
				continue;
			}
			if (attendee.role == Role.RequiredParticipant) {
				attendeeListTo.add(SendmailHelper.formatAddress(attendee.commonName, attendee.mailto));
			} else {
				attendeeListCc.add(SendmailHelper.formatAddress(attendee.commonName, attendee.mailto));

			}
		}
		return sendNotification(message, event, senderSettings, subjectTemplate, template, messagesResolverProvider,
				method, from, recipient, ics, data, attendeeListTo, attendeeListCc);
	}

	private SendmailResponse sendNotification(VEventMessage message, VEvent event, Map<String, String> senderSettings,
			String subjectTemplate, String template, MessagesResolverProvider messagesResolverProvider, Method method,
			Mailbox from, Mailbox recipient, String ics, Map<String, Object> data, List<Mailbox> attendeeListTo,
			List<Mailbox> attendeeListCc) {
		AtomicReference<SendmailResponse> ret = new AtomicReference<>(SendmailResponse.success());
		try {
			IcsHookAuditor auditor = new IcsHookAuditor(IAuditManager.instance());
			auditor.forMessage(message).audit(() -> {
				ServerSideServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
				SecurityContext context = message.securityContext;
				IUser userService = sp.instance(IUser.class, context.getContainerUid());
				IUserSettings userSettingsService = sp.instance(IUserSettings.class, context.getContainerUid());

				// BM-8343
				if (message.oldEvent != null) {
					VEvent findCorrespondingEvent = findCorrespondingEvent(message.oldEvent, event);
					Map<String, Object> old = null != findCorrespondingEvent
							? new CalendarMailHelper().extractVEventData(findCorrespondingEvent)
							: new HashMap<>();
					for (Entry<String, Object> e : old.entrySet()) {
						data.put("old_" + e.getKey(), e.getValue());
					}

					// Fix highlight new location
					if (!data.containsKey("old_location")) {
						data.put("old_location", "");
					}

					// Fix highlight new description
					if (!data.containsKey("old_description")) {
						data.put("old_description", "");
					}

					// Fix highlight new url
					if (!data.containsKey("old_url")) {
						data.put("old_url", "");
					}
				}

				ItemValue<User> user = userService.byEmail(recipient.getAddress());
				Map<String, String> settings = senderSettings;
				if (user != null) {
					settings = userSettingsService.get(user.uid);
				}

				long maxMsgBytes = 10485760L;
				if (!event.attachments.isEmpty()) {
					SystemConf systemConf = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
							.instance(ISystemConfiguration.class).getValues();
					maxMsgBytes = systemConf.convertedValue(SysConfKeys.message_size_limit.name(), Long::parseLong,
							10485760L);

				}
				// attachment size ~= 60% message
				long maxAttachBytes = maxMsgBytes * 6 / 10;
				List<EventAttachment> attachments = EventAttachmentHelper.getAttachments(event, maxAttachBytes);
				if (!attachments.isEmpty() && !EventAttachmentHelper.hasBinaryAttachments(attachments)) {
					data.put("attachments", attachments);
				}

				try (Message mail = buildMailMessage(from, from, attendeeListTo, attendeeListCc, subjectTemplate,
						template, messagesResolverProvider.getResolver(new Locale(getLocale(settings))), data,
						createBodyPart(message.itemUid, ics), settings, event, method, attachments)) {
					ret.set(mailer.send(SendmailCredentials.asAdmin0(), from.getAddress(), from.getDomain(),
							new MailboxList(Arrays.asList(recipient), true), mail));
					auditor.actionSend(message.itemUid, recipient.getAddress(), ics, ret.toString());
				} catch (Exception e) {
					auditor.actionSend(message.itemUid, recipient.getAddress(), ics,
							String.format("Unable to send email %s", e.getMessage()));
					if (e instanceof ServerFault) {
						throw (ServerFault) e;
					}

					throw new ServerFault(e);
				}
			});
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
		return ret.get();
	}

	private static Map<String, String> getSenderSettings(VEventMessage message, DirEntry fromDirEntry) {
		ServerSideServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IUserSettings userSettingsService = sp.instance(IUserSettings.class, message.container.domainUid);
		return userSettingsService.get(fromDirEntry.entryUid);
	}

	private Message buildMailMessage(Mailbox from, Mailbox sender, List<Mailbox> attendeeListTo,
			List<Mailbox> attendeeListCc, String subjectTemplate, String templateName,
			MessagesResolver messagesResolver, Map<String, Object> data, Optional<BodyPart> ics,
			Map<String, String> settings, VEvent vevent, Method method, List<EventAttachment> attachments) {
		try {
			String subject = new CalendarMailHelper().buildSubject(subjectTemplate, settings.get("lang"),
					messagesResolver, data);
			data.put("datetime_format", settings.get("date") + " " + settings.get("timeformat"));
			data.put("time_format", settings.get("timeformat"));
			// FIXME date_format
			data.put("date_format", "EEE, MMMM dd, yyyy");
			if ("fr".equals(settings.get("lang"))) {
				data.put("date_format", "EEEE d MMMM yyyy");
			}

			TimeZone tz = TimeZone.getTimeZone(settings.get("timezone"));
			data.put("timezone", tz.getID());

			if (vevent.timezone() != null && !vevent.timezone().equals(settings.get("timezone"))) {
				data.put("tz", tz.getDisplayName(new Locale(settings.get("lang"))));
			}

			return getMessage(from, sender, attendeeListTo, attendeeListCc, subject, templateName, settings.get("lang"),
					messagesResolver, data, ics, method, attachments);
		} catch (TemplateException e) {
			throw new ServerFault(e);
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	/**
	 * @param message
	 * @return
	 * @throws ServerFault
	 */
	private DirEntry getMyDirEntry(VEventMessage message) throws ServerFault {
		SecurityContext context = message.securityContext;
		ServerSideServiceProvider sp = ServerSideServiceProvider.getProvider(context);

		IContainers ic = sp.instance(IContainers.class);
		ContainerDescriptor container = ic.get(message.container.uid);

		return sp.instance(IDirectory.class, message.container.domainUid).findByEntryUid(container.owner);
	}

	private Optional<EventAttendeeTuple> getMatchingAttendeeForEvent(VEvent vEvent, DirEntry user) {
		if (vEvent == null) {
			return Optional.empty();
		}
		String dirPath = "bm://" + user.path;
		for (ICalendarElement.Attendee a : vEvent.attendees) {
			if (a.dir != null && a.dir.equals(dirPath)) {
				return Optional.of(new EventAttendeeTuple(a, vEvent));
			}
		}
		return Optional.empty();
	}

	/**
	 * @param oldEvent
	 * @param updatedEvent
	 * @return
	 */
	private Set<BmDateTime> getNewExceptionList(VEvent oldEvent, VEvent updatedEvent) {
		Set<BmDateTime> oldListException = Collections.emptySet();
		if (oldEvent != null && oldEvent.exdate != null && !oldEvent.exdate.isEmpty()) {
			oldListException = new HashSet<>(oldEvent.exdate);
		}

		Set<BmDateTime> newListException = Collections.emptySet();

		if (updatedEvent.exdate != null && !updatedEvent.exdate.isEmpty()) {
			newListException = new HashSet<>(updatedEvent.exdate);
		}

		return new HashSet<>(Sets.difference(newListException, oldListException));
	}

	/**
	 * @param event
	 * @return
	 */
	private boolean mustSendNotification(VEventMessage message) {
		if (!message.sendNotifications) {
			logger.debug("Do not send notification email to {}. Event uid: {} Event icsUid: {}", message.container.name,
					message.itemUid, message.vevent.icsUid);
			return false;
		}

		if (message.vevent.main == null) {
			VEvent ref = message.vevent.occurrences.get(0);
			return ref.meeting();
		}

		if (!isDefaultContainer(message)) {
			return false;
		}

		return message.vevent.meeting() || (message.oldEvent != null && message.oldEvent.meeting());

	}

	private VEventSeries getSeriesForAttendee(VEventSeries updatedEvent, Attendee attendee) {
		return updatedEvent.flatten().stream() //
				.filter(evt -> userAttends(evt, attendee)) //
				.reduce(new VEventSeries(), reduceSeries(), combineSeries());
	}

	private VEventSeries reduceToAttendee(VEventSeries updatedEvent, Attendee attendee) {
		return updatedEvent.flatten().stream() //
				.map(evt -> {
					evt.attendees = Arrays.asList(attendee);
					return evt;
				}) //
				.reduce(new VEventSeries(), reduceSeries(), combineSeries());
	}

	private BinaryOperator<VEventSeries> combineSeries() {
		return (ser1, ser2) -> {
			ser1.main = ser1.main != null ? ser1.main : ser2.main;
			ser1.occurrences = new ArrayList<>(ser1.occurrences);
			ser1.occurrences.addAll(ser2.occurrences);
			return ser1;
		};
	}

	private BiFunction<VEventSeries, ? super VEvent, VEventSeries> reduceSeries() {
		return (series, evt) -> {
			if (!evt.exception()) {
				series.main = evt;
			} else {
				series.occurrences = new ArrayList<>(series.occurrences);
				series.occurrences.add((VEventOccurrence) evt);
			}
			return series;
		};
	}

	/**
	 * FIXME: Shouldn't we send an e-mail even if the event is in the past ?
	 * 
	 * @param event
	 * @return
	 */
	private boolean occursInThePast(VEvent event) {
		ZonedDateTime now = ZonedDateTime.now();
		// normally we would have to compare "now" with every attendees
		// timezone.
		// the -12 hours patterns enables acceptable behaviour without loading
		// all attendees data
		if (!new BmDateTimeWrapper(event.dtstart).containsTimeZone()) {
			now = now.minusHours(12);
		}
		if (event.rrule == null && event.dtend != null
				&& new BmDateTimeWrapper(event.dtend).toDateTime().isBefore(now)) {
			return true;
		}

		if (event.rrule == null && new BmDateTimeWrapper(event.dtstart).toDateTime().isBefore(now)) {
			return true;
		}

		if (event.rrule != null && event.rrule.until != null
				&& new BmDateTimeWrapper(event.rrule.until).toDateTime().isBefore(now)) {
			return true;
		}

		return false;
	}

	/**
	 * @param attendee
	 * @param organizer
	 * @return
	 */
	private boolean attendeeIsOrganizer(ICalendarElement.Attendee attendee, ICalendarElement.Organizer organizer) {
		if (organizer == null) {
			return false;
		}
		if (organizer.dir != null && attendee.dir != null && organizer.dir.equals(attendee.dir)) {
			return true;
		}

		if (organizer.mailto != null && attendee.mailto != null && organizer.mailto.equals(attendee.mailto)) {
			return true;
		}

		return false;
	}

	/**
	 * @param from
	 * @param sender
	 * @param attendeeListTo
	 * @param attendeeListTo
	 * @param subject
	 * @param templateName
	 * @param locale
	 * @param messagesResolver
	 * @param data
	 * @param ics
	 * @param method
	 * @param attachments
	 * @return
	 * @throws TemplateException
	 * @throws IOException
	 * @throws ServerFault
	 */
	private Message getMessage(Mailbox from, Mailbox sender, List<Mailbox> attendeeListTo, List<Mailbox> attendeeListCc,
			String subject, String templateName, String locale, MessagesResolver messagesResolver,
			Map<String, Object> data, Optional<BodyPart> ics, Method method, List<EventAttachment> attachments)
			throws TemplateException, IOException, ServerFault {

		data.put("msg", new FreeMarkerMsg(messagesResolver));

		CalendarMailBuilder mailBuilder = new CalendarMailBuilder() //
				.from(from) //
				.sender(sender) //
				.to(new MailboxList(attendeeListTo, true)) //
				.method(method) //
				.html(new CalendarMailHelper().buildBody(templateName, locale, messagesResolver, data)) //
				.subject(subject) //
				.ics(ics) //
				.attachments(attachments);

		if (attendeeListCc != null) {
			mailBuilder.cc(new MailboxList(attendeeListCc, true));
		}

		return mailBuilder.build().getMessage();
	}

	private String getIcsPart(String uid, Method method, VEventSeries series, Attendee attendee) throws ServerFault {
		String ics = null;
		if (null == attendee) {
			ics = VEventServiceHelper.convertToIcs(uid, method, series);
		} else {
			VEventSeries attendeeSeries = new VEventSeries();
			attendeeSeries.acceptCounters = series.acceptCounters;
			attendeeSeries.icsUid = uid;
			VEvent master = series.main;
			if (master != null) {
				if (userAttends(master, attendee)) {
					attendeeSeries.main = master;
				}
			}
			List<VEventOccurrence> occurrences = new ArrayList<>();
			series.occurrences.forEach(occurrence -> {
				if (userAttends(occurrence, attendee)) {
					occurrences.add(occurrence);
				}
			});
			attendeeSeries.occurrences = occurrences;
			ics = VEventServiceHelper.convertToIcs(method, attendeeSeries);
		}
		return ics;
	}

	private boolean attendsToSeries(VEventSeries series, Attendee attendee) {
		boolean attends = true;
		List<VEvent> flatten = series.flatten();
		for (VEvent vEvent : flatten) {
			attends = attends && userAttends(vEvent, attendee);
		}
		return attends;
	}

	private boolean userAttends(VEvent event, Attendee attendee) {
		for (Attendee att : event.attendees) {
			if ((att.dir != null && att.dir.equals(attendee.dir))
					|| (att.mailto != null && att.mailto.equals(attendee.mailto))) {
				return true;
			}
		}
		return false;
	}

	private String getIcsPart(Optional<Boolean> acceptCounters, String uid, Method method, VEvent vevent) {
		return VEventServiceHelper.convertToIcs(acceptCounters, uid, method, vevent);
	}

	private Optional<BodyPart> createBodyPart(String summary, String ics) {
		if (ics == null) {
			return Optional.empty();
		}

		return Optional.of(new CalendarMailHelper().createTextPart(ics));
	}

	private boolean isDefaultContainer(VEventMessage message) {
		return message.container.defaultContainer;
	}

	/**
	 * Check if working container equals owner calendar container
	 * 
	 * @param message
	 * @return
	 * @throws ServerFault
	 */
	private boolean isMasterVersion(VEventSeries message, Container container) {
		return message.master(container.domainUid, container.owner);
	}

	private IServiceProvider provider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	private static class EventAttendeeTuple {
		private final ICalendarElement.Attendee attendee;
		private final VEvent event;

		public EventAttendeeTuple(ICalendarElement.Attendee attendee, VEvent event) {
			this.attendee = attendee;
			this.event = event;
		}
	}

	private static class MailData {
		public final Organizer organizer;
		public final String subject;
		public final String body;
		public final Mailbox from;
		public final Map<String, Object> data;
		public final Map<String, String> senderSettings;

		public MailData(Organizer organizer, String subject, String body, Mailbox from, Map<String, Object> data,
				Map<String, String> senderSettings) {
			this.organizer = organizer;
			this.subject = subject;
			this.body = body;
			this.from = from;
			this.data = data;
			this.senderSettings = senderSettings;
		}

		private static MailData create(VEventMessage message, VEvent event) {
			String subject = "EventCreateSubject.ftl";
			String body = "EventCreate.ftl";
			return get(message, event, subject, body);
		}

		private static MailData cancel(VEventMessage message, VEvent event) {
			String subject = "EventDeleteSubject.ftl";
			String body = "EventDelete.ftl";
			return get(message, event, subject, body);
		}

		private static MailData update(VEventMessage message, VEvent event) {
			String subject = "EventUpdateSubject.ftl";
			String body = "EventUpdate.ftl";
			return get(message, event, subject, body);
		}

		private static MailData organizer(VEventMessage message, VEvent event) {
			String subject = "EventOrganizationSubject.ftl";
			String body = "EventOrganization.ftl";
			return get(message, event, subject, body);
		}

		private static MailData exception(VEventMessage message, VEvent event) {
			String subject = "NewExceptionSubject.ftl";
			String body = "NewException.ftl";
			return get(message, event, subject, body);
		}

		private static MailData declineCounter(VEventMessage message, VEvent event) {
			String subject = "DeclineCounterSubject.ftl";
			String body = "DeclineCounter.ftl";
			return get(message, event, subject, body);
		}

		private static MailData get(VEventMessage message, VEvent event, String subject, String body) {
			Organizer organizer = event.organizer;
			if (null == organizer) {
				if (null != message.oldEvent && null != message.oldEvent.main
						&& message.oldEvent.main.organizer != null) {
					organizer = message.oldEvent.main.organizer;
				} else {
					for (VEventOccurrence occurrence : message.vevent.occurrences) {
						if (null != occurrence.organizer) {
							organizer = occurrence.organizer;
						}
					}
				}
			}
			if (organizer == null) {
				throw new NullPointerException("Organizer is null");
			}
			Mailbox from = SendmailHelper.formatAddress(organizer.commonName, organizer.mailto);
			DirEntry fromDirEntry = null;
			if (organizer.dir != null) {
				fromDirEntry = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IDirectory.class, message.securityContext.getContainerUid())
						.getEntry(organizer.dir.substring("bm://".length()));
			} else {
				fromDirEntry = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IDirectory.class, message.securityContext.getContainerUid())
						.getByEmail(organizer.mailto);
			}
			Map<String, String> senderSettings = getSenderSettings(message, fromDirEntry);

			HashMap<String, Object> data = new HashMap<>();
			data.putAll(new CalendarMailHelper().extractVEventData(event));

			return new MailData(organizer, subject, body, from, data, senderSettings);
		}

	}

}
