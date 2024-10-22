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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import freemarker.template.TemplateException;
import net.bluemind.calendar.EventChangesMerge;
import net.bluemind.calendar.EventChangesMerge.VEventChangesWithDiff;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.calendar.helper.mail.CalendarMail;
import net.bluemind.calendar.helper.mail.CalendarMailHelper;
import net.bluemind.calendar.helper.mail.Messages;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.delivery.lmtp.common.LmtpAddress;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.delivery.lmtp.filters.PermissionDeniedException.MailboxInvitationDeniedException;
import net.bluemind.domain.api.Domain;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;
import net.fortuna.ical4j.model.property.Method;

public class EventRequestHandler extends AbstractLmtpHandler implements IIMIPHandler {
	private static final Logger logger = LoggerFactory.getLogger(EventRequestHandler.class);
	private static final Cache<String, ItemValue<User>> senderCache = Caffeine.newBuilder().recordStats()
			.expireAfterAccess(2, TimeUnit.MINUTES).build();
	private static final Cache<String, Map<String, String>> senderSettingsCache = Caffeine.newBuilder().recordStats()
			.expireAfterAccess(2, TimeUnit.MINUTES).build();

	private final String smtpFrom;
	private final ISendmail mailer;
	private final EventAttachmentHandler attachmentHandler;

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register("lmtp-filter-eventrequest-sender", senderCache);
			cr.register("lmtp-filter-eventrequest-settings", senderSettingsCache);
		}
	}

	public EventRequestHandler(ResolvedBox recipient, LmtpAddress sender) {
		this(new Sendmail(), recipient, sender);

	}

	public EventRequestHandler(ISendmail mailer, ResolvedBox recipient, LmtpAddress sender) {
		super(recipient, sender);
		this.smtpFrom = sender.email;
		this.mailer = mailer;
		this.attachmentHandler = new EventAttachmentHandler(provider(), getCoreUrl());
	}

	@Override
	public IMIPResponse handle(IMIPInfos imip, ResolvedBox recipient, ItemValue<Domain> domain,
			ItemValue<Mailbox> recipientMailbox) throws ServerFault {

		String calUid = getCalendarUid(recipientMailbox);

		IUser userService = provider().instance(IUser.class, recipient.dom.uid);
		ItemValue<User> resolvedRecipient = userService.byEmail(recipient.entry.email);
		Optional<String> userLogin = Optional.ofNullable(resolvedRecipient).map(r -> r.value.login);

		// BM-2892 invitation right
		ItemValue<User> sender = senderCache.getIfPresent(recipient.dom.uid + "#" + imip.organizerEmail);
		if (sender == null) {
			userService = provider().instance(IUser.class, recipient.dom.uid);
			sender = userService.byEmail(imip.organizerEmail);
			if (sender != null) {
				senderCache.put(recipient.dom.uid + "#" + imip.organizerEmail, sender);
				if (!userLogin.isPresent()) {
					userLogin = Optional.of(sender.value.login);
				}
			}
		}

		VEventSeries series = fromList(imip.properties, imip.iCalendarElements, imip.uid);

		if (sender != null) {
			boolean canInvite = checkInvitationRight(recipient, calUid, sender);
			if (!canInvite) {
				// BM-4186 notify sender
				notifyForbiddenToSender(imip, recipient, recipientMailbox, sender, series);
				ServerFault fault = new ServerFault(new MailboxInvitationDeniedException(recipientMailbox.uid));
				fault.setCode(ErrorCode.PERMISSION_DENIED);
				throw fault;
			}

		} // else external, don't care for now

		ICalendar cal = provider().instance(ICalendar.class, calUid);

		sanitizeOrganizer(series);
		ensureUserAttendance(domain, recipientMailbox, series.main);
		series.occurrences.forEach(occurrence -> ensureUserAttendance(domain, recipientMailbox, occurrence));

		List<ItemValue<VEventSeries>> vseries = cal.getByIcsUid(imip.uid);

		if (recipientIsOrganizer(domain, vseries, resolvedRecipient)) {
			logger.info("Ignoring request targeting event UID {}. Recipient is the event organizer", imip.uid);
			return IMIPResponse.createEmptyResponse();
		}

		attachmentHandler.detachCidAttachments(series, vseries, imip.cid, userLogin, recipient.dom.uid);

		setDefaultAlarm(domain, recipientMailbox.uid, series);

		VEventChangesWithDiff changesWithDiff = EventChangesMerge.getStrategy(vseries, series).merge(vseries, series);
		VEventChanges changes = changesWithDiff.changes;
		VEvent event = series.main == null ? series.occurrences.get(0) : series.main;

		cal.updates(changes);
		logger.info("[{}] {} new series, {} updated series, {} deleted series in BM (calendar {})", imip.messageId,
				changes.add == null ? 0 : changes.add.size(), changes.modify == null ? 0 : changes.modify.size(),
				changes.delete == null ? 0 : changes.delete.size(), calUid);

		return IMIPResponse.createEventResponse(imip.uid, event, needResponse(domain, recipientMailbox, event), calUid,
				changesWithDiff.diff);
	}

	private boolean recipientIsOrganizer(ItemValue<Domain> domain, List<ItemValue<VEventSeries>> vseries,
			ItemValue<User> resolvedRecipient) {
		if (vseries.isEmpty()) {
			return false;
		}

		if (resolvedRecipient == null) {
			return false; // recipient might be a resource
		}

		Organizer organizer = vseries.get(0).value.mainOccurrence().organizer;
		return resolvedRecipient.value.emails.stream()
				.anyMatch(email -> email.match(organizer.mailto, domain.value.aliases));
	}

	private void sanitizeOrganizer(VEventSeries series) {
		series.flatten().forEach(evt -> {
			if (evt.organizer == null) {
				evt.organizer = new Organizer(smtpFrom);
			}
		});

	}

	private void setDefaultAlarm(ItemValue<Domain> domain, String uid, VEventSeries series) {
		Map<String, String> settings = provider().instance(IUserSettings.class, domain.uid).get(uid);
		setAlarm(series.main, settings);
		series.occurrences.forEach(occurrence -> setAlarm(occurrence, settings));
	}

	private void setAlarm(VEvent evt, Map<String, String> settings) {
		if (evt == null) {
			return;
		}

		String trigger = settings.get("default_event_alert");
		if (evt.allDay()) {
			trigger = settings.get("default_allday_event_alert");
		}
		if (alarmIsActive(trigger)) {
			try {
				evt.alarm = Arrays.asList(VAlarm.create(-Integer.parseInt(trigger)));
			} catch (NumberFormatException e) {
				logger.warn("Failed to set alarm, invalid trigger {}", trigger);
			}
		}
	}

	private boolean alarmIsActive(String trigger) {
		return trigger != null && !trigger.isEmpty();
	}

	private boolean needResponse(ItemValue<Domain> domain, ItemValue<Mailbox> recipientMailbox, VEvent event) {
		for (Attendee att : event.attendees) {
			if (att.mailto != null && matchMailbox(domain, recipientMailbox, att.mailto)) {
				return !Boolean.FALSE.equals(att.rsvp);
			}
		}

		return true;
	}

	private void ensureUserAttendance(ItemValue<Domain> domain, ItemValue<Mailbox> recipientMailbox, VEvent vevent) {
		if (vevent == null || ((vevent.attendees == null || vevent.attendees.isEmpty()) && vevent.organizer == null)) {
			return;
		}

		if (vevent.attendees == null) {
			vevent.attendees = new ArrayList<>();
		}

		// do not add organiser to participants
		if (matchMailbox(domain, recipientMailbox, vevent.organizer.mailto)) {
			return;
		}

		for (Attendee att : vevent.attendees) {
			if (att.mailto != null && matchMailbox(domain, recipientMailbox, att.mailto)) {
				return;
			}
		}

		List<Attendee> attendees = new ArrayList<>(vevent.attendees.size() + 1);
		attendees.addAll(vevent.attendees);
		Attendee attendee = Attendee.create(CUType.Individual, "", Role.OptionalParticipant,
				ParticipationStatus.NeedsAction, Boolean.TRUE, null, null, null, null, null, null, null,
				recipientMailbox.value.defaultEmail().address);
		attendees.add(attendee);

		vevent.attendees = attendees;
	}

	private boolean matchMailbox(ItemValue<Domain> domain, ItemValue<Mailbox> recipientMailbox, String mailto) {
		Set<String> all = new HashSet<>(domain.value.aliases);
		all.add(domain.uid);
		return recipientMailbox.value.emails.stream().anyMatch(email -> email.match(mailto, all));

	}

	/**
	 * @param imip
	 * @param recipient
	 * @param recipientMailbox
	 * @param sender
	 * @param calElement
	 * @throws ServerFault
	 */
	private void notifyForbiddenToSender(IMIPInfos imip, ResolvedBox recipient, ItemValue<Mailbox> mailbox,
			ItemValue<User> sender, VEventSeries series) throws ServerFault {

		VEvent event = series.main != null ? series.main : series.occurrences.get(0);
		Attendee a = new VEvent.Attendee();
		a.mailto = mailbox.value.defaultEmail().address;
		a.partStatus = ParticipationStatus.Declined;
		event.attendees = Arrays.asList(a);

		org.apache.james.mime4j.dom.address.Mailbox from = SendmailHelper.formatAddress(mailbox.displayName,
				mailbox.value.defaultEmail().address);

		// to sender
		org.apache.james.mime4j.dom.address.Mailbox to = SendmailHelper.formatAddress(sender.displayName,
				sender.value.defaultEmail().address);

		Map<String, Object> data = new HashMap<>();

		data.put("attendee", mailbox.displayName);
		String ics = VEventServiceHelper.convertToIcs(Method.REPLY, ItemValue.create(imip.uid, series));
		if (ics == null) {
			throw new ServerFault("Fail to export ICS for event uid " + imip.uid);
		}
		CalendarMailHelper cmh = new CalendarMailHelper();

		BasicBodyFactory bodyFactory = new BasicBodyFactory();
		TextBody body = bodyFactory.textBody(ics, StandardCharsets.UTF_8);

		BodyPart icsPart = new BodyPart();
		icsPart.setText(body);

		Map<String, String> t = senderSettingsCache.getIfPresent(imip.organizerEmail);
		if (t == null) {
			IUserSettings userSettingsService = provider().instance(IUserSettings.class, recipient.dom.uid);
			t = userSettingsService.get(sender.uid);
			senderSettingsCache.put(imip.organizerEmail, t);
		}
		final Map<String, String> senderSettings = t;
		data.putAll(cmh.extractVEventData(event));

		String summary = series.main != null ? series.main.summary : series.occurrences.get(0).summary;
		Locale l = Locale.of(senderSettings.get("lang"));
		MessagesResolver resolver = new MessagesResolver(Messages.getEventDetailMessages(l),
				ResourceBundle.getBundle("lang", Locale.of(senderSettings.get("lang"))));
		String subject = resolver.translate("eventForbiddenAttendee", new Object[] { mailbox.displayName, summary });

		try (Message mail = buildMailMessage(from, to, subject, "EventForbiddenAttendee.ftl", resolver, data, icsPart,
				senderSettings, event, Method.REPLY)) {
			mailer.send(from, mail);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	private Message buildMailMessage(org.apache.james.mime4j.dom.address.Mailbox from,
			org.apache.james.mime4j.dom.address.Mailbox to, String subject, String templateName,
			MessagesResolver resolver, Map<String, Object> data, BodyPart icsPart, Map<String, String> settings,
			VEvent event, Method method) throws ServerFault {
		try {
			data.put("datetime_format", settings.get("date") + " " + settings.get("timeformat"));
			data.put("time_format", settings.get("timeformat"));
			data.put("date_format", "EEE, MMMM dd, yyyy");
			if ("fr".equals(settings.get("lang"))) {
				data.put("date_format", "EEEE d MMMM yyyy");
			}

			TimeZone tz = TimeZone.getTimeZone(settings.get("timezone"));
			data.put("timezone", tz.getID());

			if (event.timezone() != null && !event.timezone().equals(settings.get("timezone"))) {
				data.put("tz", tz.getDisplayName(Locale.of(settings.get("lang"))));
			}

			CalendarMail m = new CalendarMail.CalendarMailBuilder() //
					.from(from) //
					.to(new MailboxList(Arrays.asList(to), true)) //
					.method(method) //
					.ics(Optional.of(icsPart)) //
					.html(new CalendarMailHelper().buildBody(templateName, settings.get("lang"), resolver, data)) //
					.subject(subject).build();
			return m.getMessage();

		} catch (TemplateException | IOException e) {
			throw new ServerFault(e);
		}
	}

}
