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
package net.bluemind.todolist.job;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.BodyPart;

import freemarker.template.TemplateException;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.reminder.job.IAlarmSupport;
import net.bluemind.reminder.job.Reminder;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.VTodo;

public class TodoListAlarmSupport implements IAlarmSupport<VTodo> {

	public static final String BODY_TEMPLATE = "TodoAlert.ftl";
	public static final String SUBJECT_TEMPLATE = "TodoSubjectAlert.ftl";

	@Override
	public String getContainerType() {
		return ITodoUids.TYPE;
	}

	@Override
	public List<Reminder<VTodo>> getReminder(BmDateTime dtalarm, BaseContainerDescriptor containerDescriptor)
			throws ServerFault {
		String containerUid = containerDescriptor.uid;
		ITodoList todoList = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITodoList.class,
				containerUid);
		List<Reminder<VTodo>> reminders = toReminder(todoList.getReminder(dtalarm));
		return reminders;
	}

	private List<Reminder<VTodo>> toReminder(List<net.bluemind.todolist.api.Reminder> reminder) {
		return reminder.stream().map((elem) -> {
			return new Reminder<VTodo>(elem.user, elem.todo, elem.valarm);
		}).collect(Collectors.toList());
	}

	@Override
	public String buildSubject(Map<String, String> settings, Map<String, Object> data) {
		String subject = new TodoListMailHelper().buildSubject(SUBJECT_TEMPLATE, settings.get("locale"),
				new MessagesResolver(ResourceBundle.getBundle("todoAlert", new Locale(settings.get("locale")))), data);
		return subject;
	}

	@Override
	public Map<String, Object> extractEntityDataToMap(VTodo entity, VAlarm valarm) {
		Map<String, Object> data = new TodoListMailHelper().extractVTodoDataToMap(entity, valarm);
		return data;
	}

	@Override
	public void logSchedInfo(Mailbox to, Reminder<VTodo> reminder, IScheduler scheduler, IScheduledJobRunId rid) {
		scheduler.info(rid, "fr",
				"Mail de rappel envoyé à " + to.getAddress() + " pour le tâche " + reminder.entity.value.summary);
		scheduler.info(rid, "en",
				"Reminder email sent to " + to.getAddress() + " for the task " + reminder.entity.value.summary);
	}

	@Override
	public BodyPart buildBody(String locale, Map<String, Object> data) throws IOException, TemplateException {
		return new TodoListMailHelper().buildBody(BODY_TEMPLATE, locale,
				new MessagesResolver(ResourceBundle.getBundle("todoAlert", new Locale(locale))), data);
	}

	@Override
	public void addMQProperties(OOPMessage msg, ItemValue<VTodo> entity, VAlarm valarm) {
		msg.putStringProperty("title", "Reminder");
		msg.putStringProperty("body", entity.value.description);
		// FIXME ? ItemValue.uid ?
		msg.putStringProperty("id", entity.uid);
		msg.putLongProperty("todoStart", new BmDateTimeWrapper(entity.value.dtstart).toUTCTimestamp());
		// FIXME
		msg.putIntProperty("alert", 0);
	}

	@Override
	public String getName() {
		return "TodoListReminderJob";
	}

	@Override
	public void initContainerItemsCache(BmDateTime dtalarm, BaseContainerDescriptor containerDescriptor) {
		// TODO Auto-generated method stub

	}

}
