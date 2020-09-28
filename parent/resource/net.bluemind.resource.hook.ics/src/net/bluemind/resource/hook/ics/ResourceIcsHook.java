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
package net.bluemind.resource.hook.ics;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.UnstructuredField;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.field.UnstructuredFieldImpl;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.TemplateException;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.helper.mail.CalendarMailHelper;
import net.bluemind.calendar.helper.mail.Messages;
import net.bluemind.calendar.hook.ICalendarHook;
import net.bluemind.calendar.hook.internal.VEventMessage;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.user.api.IUserSettings;

/**
 * Send an email to attendees {@link VEvent.Attendee} when a {@link VEvent} is
 * created, updated or deleted in the future. No email if {@link VEvent} occurs
 * in the past. An ICS is attached if created or updated.
 *
 */
public class ResourceIcsHook implements ICalendarHook {
	private static final Logger logger = LoggerFactory.getLogger(ResourceIcsHook.class);
	private ISendmail mailer;

	private enum Operation {
		Create, Update, Delete
	}

	/**
	 *
	 */
	private class Mail {
		public Mailbox from;
		public Mailbox sender;
		public Mailbox to;
		public String subject;
		public BodyPart html;
		public VEventMessage message;
		public VEvent event;

		public Message getMessage() throws TemplateException, IOException, ServerFault {
			MessageBuilder builder = null;
			try {
				builder = MessageServiceFactory.newInstance().newMessageBuilder();
			} catch (MimeException e) {
				throw new ServerFault("Cannot create MessageBuilder", e);
			}

			MessageImpl m = new MessageImpl();
			m.setDate(new Date());
			m.setSubject(subject);
			m.setSender(sender);
			m.setFrom(from);
			m.setTo(to);

			Header h = builder.newHeader();
			h = builder.newHeader();
			h.setField(Fields.contentType("text/html; charset=UTF-8;"));
			h.setField(Fields.contentTransferEncoding("quoted-printable"));

			RawField rf = new RawField("X-BM-ResourceBooking", message.container.owner);
			UnstructuredField bmExtId = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
			m.getHeader().addField(bmExtId);

			boolean rsvp = needResponse(message.container.domainUid, event.attendees);

			StringBuilder header = new StringBuilder(message.itemUid);
			if (event instanceof VEventOccurrence) {
				header.append("; recurid=\"" + ((VEventOccurrence) event).recurid.iso8601 + "\"");
			}
			header.append("; rsvp=\"" + rsvp + "\"");
			rf = new RawField("X-BM-Event", header.toString());

			bmExtId = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
			m.getHeader().addField(bmExtId);

			html.setHeader(h);

			Multipart alternative = new MultipartImpl("alternative");
			alternative.addBodyPart(html);

			MessageImpl alternativeMessage = new MessageImpl();
			alternativeMessage.setMultipart(alternative);

			BodyPart alternativePart = new BodyPart();
			alternativePart.setMessage(alternativeMessage);

			Multipart mixed = new MultipartImpl("mixed");
			mixed.addBodyPart(alternativeMessage);

			m.setMultipart(mixed);

			return m;
		}

		private boolean needResponse(String domainUid, List<Attendee> attendees) {
			for (Attendee a : attendees) {
				if (("bm://" + domainUid + "/resources/" + message.container.owner).equals(a.dir)) {
					return a.rsvp != null ? a.rsvp : false;
				}
			}
			return false;
		}

	}

	public ResourceIcsHook() {
		mailer = new Sendmail();
	}

	public ResourceIcsHook(ISendmail mailer) {
		this.mailer = mailer;
	}

	@Override
	public void onEventCreated(VEventMessage message) {
		if (!sendNotification(message)) {
			return;
		}

		try {
			if (message.vevent.main != null) {
				process(message, message.vevent.main, Operation.Create);
			}
			for (VEventOccurrence occurrence : message.vevent.occurrences) {
				process(message, occurrence, Operation.Create);
			}
		} catch (ServerFault | IOException | TemplateException e) {
			logger.error("Unable to notify administrators of resource: " + message.container.owner, e);
		}
	}

	private void process(VEventMessage message, VEvent event, Operation operation)
			throws ServerFault, IOException, TemplateException {
		String subjectTemplate = null;
		String template = null;
		if (operation == Operation.Create) {
			subjectTemplate = "ResourceEventCreateSubject.ftl";
			template = "ResourceEventCreate.ftl";
		} else if (operation == Operation.Update) {
			// FIXME BM-7500 correct email on booking update
			// FIXME BJR-75 major/minor update
			subjectTemplate = "ResourceEventUpdateSubject.ftl";
			template = "ResourceEventUpdate.ftl";
		} else {
			subjectTemplate = "ResourceEventDeleteSubject.ftl";
			template = "ResourceEventDelete.ftl";
		}

		IDirectory directoryService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDirectory.class, message.container.domainUid);
		Mailbox from = getResourceMailbox(directoryService.findByEntryUid(message.container.owner));

		IUserSettings userSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, message.container.domainUid);

		for (DirEntry admin : getResourcesAdmins(directoryService, message.container.domainUid,
				message.container.uid)) {

			if (event.organizer != null && (("bm://" + admin.path).equals(event.organizer.dir))) {
				logger.info("do not send email to organizer");
				continue;
			}

			if (admin.email == null || admin.email.isEmpty()) {
				logger.info("strange to have  resource ({}) admin ({}) ithouth email ", message.container.owner, admin);
				continue;
			}

			Map<String, String> prefs = userSettingsService.get(admin.entryUid);
			Locale l = new Locale(prefs.get("lang"));

			MessagesResolver resolver = new MessagesResolver(Messages.getEventDetailMessages(l),
					Messages.getResourceEventMessages(l));

			sendMessage(from, admin, subjectTemplate, template, resolver, prefs, message, event);
		}
	}

	private void sendMessage(Mailbox from, DirEntry to, String subjectTemplate, String template,
			MessagesResolver messagesResolver, Map<String, String> prefs, VEventMessage message, VEvent event)
			throws IOException, TemplateException, ServerFault {
		Map<String, Object> data = new CalendarMailHelper().extractVEventData(event);
		data.put("time_format", prefs.get("timeformat"));
		data.put("timezone", TimeZone.getTimeZone(prefs.get("timezone")).getID());
		data.put("date_format", "EEE, MMMM dd, yyyy");
		if ("fr".equals(prefs.get("lang"))) {
			data.put("date_format", "EEEE d MMMM yyyy");
		}

		Mail m = new Mail();
		m.from = from;
		m.sender = from;
		m.to = SendmailHelper.formatAddress(to.displayName, to.email);
		m.subject = new CalendarMailHelper().buildSubject(subjectTemplate, prefs.get("lang"), messagesResolver, data);
		m.html = new CalendarMailHelper().buildBody(template, prefs.get("lang"), messagesResolver, data);
		m.message = message;
		m.event = event;

		mailer.send(from, m.getMessage());
	}

	private Mailbox getResourceMailbox(DirEntry dirEntry) throws ServerFault {
		return SendmailHelper.formatAddress(dirEntry.displayName, dirEntry.email);
	}

	private Collection<DirEntry> getResourcesAdmins(IDirectory directoryService, String domainUid, String containerUid)
			throws ServerFault {
		IContainerManagement containerMgmt = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, containerUid);
		List<AccessControlEntry> acls = containerMgmt.getAccessControlList();

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domainUid);
		Map<String, DirEntry> adminsUsers = new HashMap<>();
		for (AccessControlEntry acl : acls) {
			if (acl.verb != Verb.Write && acl.verb != Verb.All) {
				continue;
			}

			DirEntry entry = directoryService.findByEntryUid(acl.subject);
			if (entry != null) {
				if (entry.kind == DirEntry.Kind.GROUP) {
					List<Member> users = groupService.getExpandedUserMembers(entry.entryUid);
					for (Member user : users) {
						if (adminsUsers.containsKey(user.uid)) {
							continue;
						}

						adminsUsers.put(user.uid, directoryService.findByEntryUid(user.uid));
					}
				} else {
					adminsUsers.put(entry.entryUid, entry);
				}
			}
		}
		return adminsUsers.values();
	}

	@Override
	public void onEventUpdated(VEventMessage message) {
		if (!sendNotification(message)) {
			return;
		}

		try {
			if (message.vevent.main != null) {
				VEvent oldEvent = message.oldEvent.main;
				if (areChangesImportant(oldEvent, message.vevent.main)) {
					process(message, message.vevent.main, Operation.Update);
				}
			}
			for (VEventOccurrence occurrence : message.vevent.occurrences) {
				VEvent oldEvent = message.oldEvent.occurrence(occurrence.recurid);
				if (oldEvent == null || areChangesImportant(oldEvent, occurrence)) {
					process(message, occurrence, Operation.Update);
				}
			}
		} catch (ServerFault | IOException | TemplateException e) {
			logger.error("Unable to notify administrators of resource: " + message.container.owner, e);
		}
	}

	@Override
	public void onEventDeleted(VEventMessage message) {
		if (!sendNotification(message)) {
			return;
		}
		try {
			if (message.vevent.main != null) {
				process(message, message.vevent.main, Operation.Delete);
			}
			for (VEventOccurrence occurrence : message.vevent.occurrences) {
				process(message, occurrence, Operation.Delete);
			}
		} catch (ServerFault | IOException | TemplateException e) {
			logger.error("Unable to notify administrators of resource: " + message.container.owner, e);
		}
	}

	/**
	 * @param main
	 * @return
	 */
	private boolean sendNotification(VEventMessage message) {
		if (!message.sendNotifications) {
			return false;
		}

		VEvent event = message.vevent.main;
		if (null == event) {
			event = message.vevent.occurrences.get(0);
		}

		if (!isResource(message.container.domainUid, message.container.owner)) {
			logger.debug("'{}' isn't a resource calendar", message.container.name);
			return false;
		}

		ParticipationStatus evtStatus = ParticipationStatus.NeedsAction;
		List<Attendee> attendees = event.attendees;
		for (Attendee attendee : attendees) {
			if (isCalOwnerAttendee(message.container, attendee)) {
				evtStatus = attendee.partStatus;
				break;
			}
		}

		if (evtStatus != ParticipationStatus.NeedsAction && evtStatus != ParticipationStatus.Tentative) {
			logger.debug("Event {} status isn't {} but {}", message.itemUid, ICalendarElement.Status.NeedsAction,
					event.status);
			return false;
		}

		return true;
	}

	private boolean isCalOwnerAttendee(Container container, Attendee attendee) {
		if (null == attendee.dir) {
			return false;
		}
		return attendee.dir.substring(attendee.dir.lastIndexOf("/") + 1).equals(container.owner);
	}

	private boolean isResource(String domainUid, String uid) {
		ResourceDescriptor resource = null;

		try {
			resource = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IResources.class, domainUid).get(uid);
		} catch (ServerFault e) {
			logger.error("Fail to check if uid: " + uid + " is a resource uid", e);
		}

		return resource != null;
	}

	/**
	 * @param oldEvent
	 * @param updatedEvent
	 * @return
	 */
	private boolean areChangesImportant(VEvent oldEvent, VEvent updatedEvent) {
		if (oldEvent.location == null) {
			oldEvent.location = "";
		}
		if (updatedEvent.location == null) {
			updatedEvent.location = "";
		}
		if (diff(oldEvent.location, updatedEvent.location)) {
			logger.info("Event modification is IMPORTANT (location)");
			return true;
		}
		if (diff(oldEvent.dtstart, updatedEvent.dtstart)) {
			logger.info("Event modification is IMPORTANT (dtstart)");
			return true;
		}

		if (diff(oldEvent.dtend, updatedEvent.dtend)) {
			logger.info("Event modification is IMPORTANT (dtend)");
			return true;
		}

		return false;
	}

	private boolean diff(Object v1, Object v2) {
		if (v1 == null && v2 == null) {
			return false;
		}
		return (v1 == null && v2 != null) || (v2 == null && v1 != null) || (!v2.equals(v1));
	}

}
