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
package net.bluemind.todolist.hook.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.message.BodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.TemplateException;
import net.bluemind.common.freemarker.FreeMarkerMsg;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.common.freemarker.MessagesResolverProvider;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailCredentials;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.core.sendmail.SendmailResponse;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirEntryPath;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.todolist.adapter.VTodoAdapter;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.hook.ITodoListHook;
import net.bluemind.todolist.hook.internal.TodoMail.TodoMailBuilder;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;
import net.fortuna.ical4j.model.property.Method;

public class IcsHook implements ITodoListHook {

	private ISendmail mailer = new Sendmail();
	private static final String DEFAULT_LANG = "fr";

	private static final Logger logger = LoggerFactory.getLogger(IcsHook.class);

	@Override
	public void onTodoCreated(VTodoMessage message) {

	}

	@Override
	public void onTodoUpdated(VTodoMessage message) {
		if (!isMasterVersion(message)) {

			if (todoHasBeenCompleted(message)) {
				sendCompletionToOrganizer(message);
			}
		}
	}

	@Override
	public void onTodoDeleted(VTodoMessage message) {

	}

	private boolean todoHasBeenCompleted(VTodoMessage message) {
		return message.vtodo.status != null && message.vtodo.status == Status.Completed;
	}

	private void sendCompletionToOrganizer(VTodoMessage message) {
		DirEntry dirEntry = getMyDirEntry(message);
		String subject = "TodoCompletionSubject.ftl";
		String body = "TodoCompletion.ftl";
		Method method = Method.REPLY;
		Organizer organizer = message.vtodo.organizer;

		Mailbox from = SendmailHelper.formatAddress(dirEntry.displayName, dirEntry.email);
		Mailbox recipient = SendmailHelper.formatAddress(organizer.commonName, organizer.mailto);

		Map<String, Object> data = new HashMap<>();
		data.put("author", dirEntry.displayName);
		data.putAll(new TodoMailHelper().extractVTodoData(message.vtodo));

		String ics = VTodoAdapter.convertToIcs(ItemValue.create(message.itemUid, message.vtodo));

		MessagesResolverProvider provider = (locale) -> new MessagesResolver(Messages.getTodoMessages(locale));

		Map<String, String> senderSettings = getSenderSettings(message, dirEntry);
		sendNotificationToOrganizer(message, message.vtodo, senderSettings, subject, body, provider, method, from,
				recipient, ics, data);

	}

	private boolean isMasterVersion(VTodoMessage message) {
		if (message.vtodo.organizer != null && message.vtodo.organizer.dir != null) {
			String todoOrganizerPath = message.vtodo.organizer.dir.substring("bm://".length());
			return IDirEntryPath.getDomain(todoOrganizerPath).equals(message.container.domainUid)
					&& IDirEntryPath.getEntryUid(todoOrganizerPath).equals(message.container.owner);
		} else {
			return true;
		}
	}

	private DirEntry getMyDirEntry(VTodoMessage message) throws ServerFault {
		SecurityContext context = message.securityContext;
		ServerSideServiceProvider sp = ServerSideServiceProvider.getProvider(context);

		IContainers ic = sp.instance(IContainers.class);
		BaseContainerDescriptor container = ic.getLight(message.container.uid);
		return sp.instance(IDirectory.class, message.container.domainUid).findByEntryUid(container.owner);
	}

	private void sendNotificationToOrganizer(VTodoMessage message, VTodo todo, Map<String, String> senderSettings,
			String subjectTemplate, String template, MessagesResolverProvider messagesResolverProvider, Method method,
			Mailbox from, Mailbox recipient, String ics, Map<String, Object> data) {
		sendNotification(message, todo, senderSettings, subjectTemplate, template, messagesResolverProvider, method,
				from, recipient, ics, data, Arrays.asList(recipient), null);
	}

	private SendmailResponse sendNotification(VTodoMessage message, VTodo todo, Map<String, String> senderSettings,
			String subjectTemplate, String template, MessagesResolverProvider messagesResolverProvider, Method method,
			Mailbox from, Mailbox recipient, String ics, Map<String, Object> data, List<Mailbox> attendeeListTo,
			List<Mailbox> attendeeListCc) {
		AtomicReference<SendmailResponse> ret = new AtomicReference<>(SendmailResponse.success());
		try {

			ServerSideServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			IUser userService = sp.instance(IUser.class, message.container.domainUid);
			IUserSettings userSettingsService = sp.instance(IUserSettings.class, message.container.domainUid);

			ItemValue<User> user = userService.byEmail(recipient.getAddress());
			Map<String, String> settings = senderSettings;
			if (user != null) {
				settings = userSettingsService.get(user.uid);
			}

			Locale locale = Locale.of(getLocale(settings));
			try (Message mail = buildMailMessage(from, attendeeListTo, attendeeListCc, subjectTemplate, template,
					messagesResolverProvider.getResolver(locale), data, createBodyPart(message.itemUid, ics), settings,
					todo, method, locale)) {
				ret.set(mailer.send(SendmailCredentials.asAdmin0(), from.getAddress(), from.getDomain(),
						new MailboxList(Arrays.asList(recipient), true), mail));
			} catch (Exception e) {
				if (e instanceof ServerFault) {
					throw (ServerFault) e;
				}
				throw new ServerFault(e);
			}
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
		return ret.get();
	}

	private Message buildMailMessage(Mailbox from, List<Mailbox> attendeeListTo, List<Mailbox> attendeeListCc,
			String subjectTemplate, String template, MessagesResolver resolver, Map<String, Object> data,
			Optional<BodyPart> body, Map<String, String> settings, VTodo todo, Method method, Locale locale) {
		try {
			String lang = settings.get("lang");
			String subject = new TodoMailHelper().buildSubject(subjectTemplate, lang, resolver, data);
			return getMessage(from, attendeeListTo, subject, template, lang, resolver, data, body, method, locale);
		} catch (TemplateException e) {
			throw new ServerFault(e);
		} catch (IOException e) {
			throw new ServerFault(e);
		}

	}

	private Optional<BodyPart> createBodyPart(String itemUid, String ics) {
		if (ics == null) {
			return Optional.empty();
		}

		return Optional.of(new TodoMailHelper().createTextPart(ics));

	}

	private static Map<String, String> getSenderSettings(VTodoMessage message, DirEntry fromDirEntry) {
		ServerSideServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IUserSettings userSettingsService = sp.instance(IUserSettings.class, message.container.domainUid);
		return userSettingsService.get(fromDirEntry.entryUid);
	}

	private Message getMessage(Mailbox from, List<Mailbox> attendeeListTo, String subject, String templateName,
			String string, MessagesResolver messagesResolver, Map<String, Object> data, Optional<BodyPart> ics,
			Method method, Locale locale) throws TemplateException, IOException, ServerFault {
		data.put("msg", new FreeMarkerMsg(messagesResolver));

		TodoMailBuilder mailBuilder = new TodoMailBuilder() //
				.from(from) //
				.to(new MailboxList(attendeeListTo, true)) //
				.method(method) //
				.html(new TodoMailHelper().buildBody(templateName, locale, messagesResolver, data)) //
				.subject(subject) //
				.ics(ics);

		return mailBuilder.build().getMessage();

	}

	private String getLocale(Map<String, String> senderSettings) {
		String lang = senderSettings.get("lang");
		if (lang == null) {
			lang = DEFAULT_LANG;
		}

		return lang;
	}

}
