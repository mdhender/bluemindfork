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
package net.bluemind.reminder.job;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.scheduledjob.scheduler.IScheduler;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.job.TodoListAlarmSupport;
import net.bluemind.user.api.User;

public class TodoReminderTest {

	MockSendMail mailer;

	@Before
	public void setup() {
		mailer = new MockSendMail();
	}

	@Test
	public void testMailCreationFrench() throws Exception {
		IAlarmSupport<VTodo> todoListalarmSupport = spy(new TodoListAlarmSupport());
		List<Reminder<VTodo>> reminders = new ArrayList<>();
		reminders.add(getTodoReminder());
		doReturn(reminders).when(todoListalarmSupport).getReminder(Matchers.<BmDateTime>any(),
				Matchers.<BaseContainerDescriptor>any());
		ReminderJob reminderJob = createReminderJob(todoListalarmSupport, "fr");

		reminderJob.tick(mock(IScheduler.class), true, "test.loc", new Date());

		Assert.assertEquals(templateFrenchSubject().replaceAll("\\\\s+", ""), mailer.subject.replaceAll("\\\\s+", ""));
		Assert.assertEquals(templateFrench().replaceAll("\\\\s+", ""), mailer.html.replaceAll("\\\\s+", ""));
	}

	@Test
	public void testMailCreationEnglish() throws Exception {
		IAlarmSupport<VTodo> todoListalarmSupport = spy(new TodoListAlarmSupport());
		List<Reminder<VTodo>> reminders = new ArrayList<>();
		reminders.add(getTodoReminder());
		doReturn(reminders).when(todoListalarmSupport).getReminder(Matchers.<BmDateTime>any(),
				Matchers.<BaseContainerDescriptor>any());
		ReminderJob reminderJob = createReminderJob(todoListalarmSupport, "en");

		reminderJob.tick(mock(IScheduler.class), true, "test.loc", new Date());

		Assert.assertEquals(templateEnglish().replaceAll("\\\\s+", ""), mailer.html.replaceAll("\\\\s+", ""));
		Assert.assertEquals(templateEnglishSubject().replaceAll("\\\\s+", ""), mailer.subject.replaceAll("\\\\s+", ""));
	}

	@Test
	public void testMailCount() throws Exception {
		IAlarmSupport<VTodo> todoListalarmSupport = spy(new TodoListAlarmSupport());
		List<Reminder<VTodo>> reminders = new ArrayList<>();
		reminders.add(getTodoReminder());
		reminders.add(getTodoReminder());
		reminders.add(getTodoReminder());
		reminders.add(getTodoReminder());
		doReturn(reminders).when(todoListalarmSupport).getReminder(Matchers.<BmDateTime>any(),
				Matchers.<BaseContainerDescriptor>any());
		ReminderJob reminderJob = createReminderJob(todoListalarmSupport, "fr");

		reminderJob.tick(mock(IScheduler.class), true, "test.loc", new Date());

		verify(reminderJob, times(4)).sendMessage(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
				Matchers.any(), Matchers.any());
	}

	@Test
	public void testMailValues() throws Exception {
		IAlarmSupport<VTodo> todoListalarmSupport = spy(new TodoListAlarmSupport());
		List<Reminder<VTodo>> reminders = new ArrayList<>();
		reminders.add(getTodoReminder());
		doReturn(reminders).when(todoListalarmSupport).getReminder(Matchers.<BmDateTime>any(),
				Matchers.<BaseContainerDescriptor>any());
		ReminderJob reminderJob = createReminderJob(todoListalarmSupport, "fr");

		reminderJob.tick(mock(IScheduler.class), true, "test.loc", new Date());

		Assert.assertEquals("testuser@test.loc", mailer.from);
		Assert.assertEquals("testuser@test.loc", mailer.to);
	}

	private ReminderJob createReminderJob(IAlarmSupport<VTodo> todoListalarmSupport, String locale) throws ServerFault {
		ReminderJob reminderJob = spy(new ReminderJob());
		ReminderJob.pendingUserUidsByDomain = null;
		doReturn(MockHelper.getMockDomainService()).when(reminderJob).getDomainService();
		doReturn(getUserSettings(locale)).when(reminderJob).getUserSettings(Matchers.<ItemValue<Domain>>any(),
				Matchers.<String>any());
		doReturn(MockHelper.getMockDirectoryService()).when(reminderJob)
				.getDirService(Matchers.<ItemValue<Domain>>any());
		doReturn(MockHelper.getMockMailboxesService()).when(reminderJob)
				.getMailboxesService(Matchers.<ItemValue<Domain>>any());
		doReturn(getContainerDescriptors()).when(reminderJob).getContainers(Matchers.<String>any(), Matchers.any());
		doNothing().when(todoListalarmSupport).initContainerItemsCache(Matchers.<BmDateTime>any(),
				Matchers.<BaseContainerDescriptor>any());
		doNothing().when(reminderJob).sendToMQ(Matchers.<ItemValue<Domain>>any(), Matchers.<ItemValue<Mailbox>>any(),
				Matchers.any(), Matchers.<VAlarm>any(), Matchers.any());
		reminderJob.mailer = mailer;
		doReturn(Arrays.asList(new IAlarmSupport[] { todoListalarmSupport })).when(reminderJob).getJobImplementations();
		return reminderJob;
	}

	private Reminder<VTodo> getTodoReminder() {
		ItemValue<VTodo> todo = defaultVTodo();
		User user = new User();
		VAlarm alarm = new VAlarm();
		alarm.trigger = 600;
		Reminder<VTodo> reminder = new Reminder<>(user, todo, alarm);
		return reminder;
	}

	protected ItemValue<VTodo> defaultVTodo() {

		VTodo todo = new VTodo();
		ZoneId tz = ZoneId.of("UTC");
		ZonedDateTime temp = ZonedDateTime.of(2024, 12, 28, 0, 0, 0, 0, tz);
		todo.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(temp.plusMonths(1), Precision.DateTime);
		todo.summary = "Test Todo";
		todo.location = "Toulouse";
		todo.description = "Lorem ipsum";
		todo.classification = VTodo.Classification.Private;
		todo.status = Status.NeedsAction;
		todo.priority = 3;

		todo.organizer = new VTodo.Organizer("mehdi@bm.lan");

		List<VTodo.Attendee> attendees = new ArrayList<>(2);

		VTodo.Attendee john = VTodo.Attendee.create(VTodo.CUType.Individual, "", VTodo.Role.Chair,
				VTodo.ParticipationStatus.Accepted, true, "", "", "", "John Bang", "", "", "uid1", "john.bang@bm.lan");
		attendees.add(john);

		VTodo.Attendee jane = VTodo.Attendee.create(VTodo.CUType.Individual, "", VTodo.Role.RequiredParticipant,
				VTodo.ParticipationStatus.NeedsAction, true, "", "", "", "Jane Bang", "", "", "uid2",
				"jane.bang@bm.lan");
		attendees.add(jane);

		todo.attendees = attendees;

		todo.attendees = attendees;

		todo.categories = new ArrayList<>();
		return ItemValue.create(UUID.randomUUID().toString(), todo);
	}

	private String templateFrench() {
		return "<html>\n" + "<head>\n" + "  <style type=\"text/css\">\n" + "\n" + "body {\n"
				+ "  background: #F8F8F8;\n" + "  color: #666;\n" + "  padding: 15px;\n" + "  font-size: small;\n"
				+ "  font-family: arial, sans-serif;\n" + "}\n" + "\n" + "table {\n" + "  border-collapse: collapse;\n"
				+ "  border-spacing: 0;\n" + "  border: 1px solid #CCC;\n" + "  background: #FFF;\n"
				+ "  width: 100%;\n" + "  margin-top: 10px;\n" + "}\n" + "\n" + "\n" + "/** Header */\n" + "\n"
				+ "h1 {\n" + "  display: inline;\n" + "}\n" + "\n" + "h1.info {\n" + " color: #049cdb;\n" + "}\n" + "\n"
				+ "h1.update {\n" + "  color: orange;\n" + "}\n" + "\n" + "h1.cancel {\n" + "  color: red;\n" + "}\n"
				+ "\n" + "h1.accepted {\n" + "  color: green;\n" + "}\n" + "\n" + "h1.declined {\n" + "  color:red;\n"
				+ "}\n" + "\n" + "/** Detail */\n" + "\n" + "h2 {\n" + "  text-align:center;\n" + "}\n" + "\n"
				+ ".updated {\n" + "  color: orange;\n" + "  font-size: 80%;\n" + "  font-style: italic;\n" + "}\n"
				+ "\n" + ".key {\n" + "   text-align: right;\n" + "   vertical-align: top;\n"
				+ "   padding-right: 15px;\n" + "   color: #CCC;\n" + "   font-style: italic;\n" + "}\n" + "\n"
				+ ".value {\n" + "  color:#666;\n" + "}\n" + "\n" + ".date {\n" + "  color: #2D2D2D;\n"
				+ "  font-weight:bold;\n" + "  font-size: medium;\n" + "}\n" + "\n" + ".tz {\n"
				+ "  font-size: smaller;\n" + "  font-style: italic;\n" + "  font-weight: normal;\n" + "}\n" + "\n"
				+ ".attendees {\n" + "  list-style-type: none;\n" + "  padding: 0px;\n" + "  margin: 0px;\n" + "}\n"
				+ "\n" + ".note {\n" + "  padding: 5px;\n" + "  border: 1px solid #C6EDF9;\n"
				+ "  background: #DDF4FB;\n" + "}\n" + "\n" + ".at {\n" + "   text-align: right;\n"
				+ "   vertical-align: top;\n" + "   padding-right: 5px;\n" + "   color: #CCC;\n"
				+ "   font-style: italic;\n" + "   font-size: 90%;\n" + "}\n" + "\n" + "</style></head>\n"
				+ "<body><h1>Rappel: Test Todo arrive à échéance dans 10 minutes</h1>\n" + "<table>\n" + "  <tr>\n"
				+ "    <td colspan=\"2\"><h2>Test Todo </h2></td>\n" + "  </tr>\n" + "  <tr>\n"
				+ "    <td class=\"key\">Quand</td>\n" + "    <td class=\"value\">\n"
				+ "    	date d’échéance :<span class=\"date\">mardi 28 janvier 2025</span>\n" + "    </td>\n"
				+ "  </tr>\n" + "    <tr>\n" + "      <td class=\"key\">Description</td>\n"
				+ "      <td class=\"value\">Lorem ipsum </td>\n" + "    </tr>\n" + "  <tr>\n"
				+ "    <td colspan=\"2\"><h2>&nbsp;</h2></td>\n" + "  </tr>\n" + "</table>\n" + "</body>\n" + "</html>";
	}

	private String templateFrenchSubject() {
		return "Rappel: Test Todo arrive à échéance dans 10 minutes";
	}

	private String templateEnglish() {
		return "<html>\n" + "<head>\n" + "  <style type=\"text/css\">\n" + "\n" + "body {\n"
				+ "  background: #F8F8F8;\n" + "  color: #666;\n" + "  padding: 15px;\n" + "  font-size: small;\n"
				+ "  font-family: arial, sans-serif;\n" + "}\n" + "\n" + "table {\n" + "  border-collapse: collapse;\n"
				+ "  border-spacing: 0;\n" + "  border: 1px solid #CCC;\n" + "  background: #FFF;\n"
				+ "  width: 100%;\n" + "  margin-top: 10px;\n" + "}\n" + "\n" + "\n" + "/** Header */\n" + "\n"
				+ "h1 {\n" + "  display: inline;\n" + "}\n" + "\n" + "h1.info {\n" + " color: #049cdb;\n" + "}\n" + "\n"
				+ "h1.update {\n" + "  color: orange;\n" + "}\n" + "\n" + "h1.cancel {\n" + "  color: red;\n" + "}\n"
				+ "\n" + "h1.accepted {\n" + "  color: green;\n" + "}\n" + "\n" + "h1.declined {\n" + "  color:red;\n"
				+ "}\n" + "\n" + "/** Detail */\n" + "\n" + "h2 {\n" + "  text-align:center;\n" + "}\n" + "\n"
				+ ".updated {\n" + "  color: orange;\n" + "  font-size: 80%;\n" + "  font-style: italic;\n" + "}\n"
				+ "\n" + ".key {\n" + "   text-align: right;\n" + "   vertical-align: top;\n"
				+ "   padding-right: 15px;\n" + "   color: #CCC;\n" + "   font-style: italic;\n" + "}\n" + "\n"
				+ ".value {\n" + "  color:#666;\n" + "}\n" + "\n" + ".date {\n" + "  color: #2D2D2D;\n"
				+ "  font-weight:bold;\n" + "  font-size: medium;\n" + "}\n" + "\n" + ".tz {\n"
				+ "  font-size: smaller;\n" + "  font-style: italic;\n" + "  font-weight: normal;\n" + "}\n" + "\n"
				+ ".attendees {\n" + "  list-style-type: none;\n" + "  padding: 0px;\n" + "  margin: 0px;\n" + "}\n"
				+ "\n" + ".note {\n" + "  padding: 5px;\n" + "  border: 1px solid #C6EDF9;\n"
				+ "  background: #DDF4FB;\n" + "}\n" + "\n" + ".at {\n" + "   text-align: right;\n"
				+ "   vertical-align: top;\n" + "   padding-right: 5px;\n" + "   color: #CCC;\n"
				+ "   font-style: italic;\n" + "   font-size: 90%;\n" + "}\n" + "\n" + "</style></head>\n"
				+ "<body><h1>Reminder: Test Todo is due in 10 minutes</h1>\n" + "<table>\n" + "  <tr>\n"
				+ "    <td colspan=\"2\"><h2>Test Todo </h2></td>\n" + "  </tr>\n" + "  <tr>\n"
				+ "    <td class=\"key\">When</td>\n" + "    <td class=\"value\">\n"
				+ "    	due date:<span class=\"date\">Tue, January 28, 2025</span>\n" + "    </td>\n" + "  </tr>\n"
				+ "    <tr>\n" + "      <td class=\"key\">Description</td>\n"
				+ "      <td class=\"value\">Lorem ipsum </td>\n" + "    </tr>\n" + "  <tr>\n"
				+ "    <td colspan=\"2\"><h2>&nbsp;</h2></td>\n" + "  </tr>\n" + "</table>\n" + "</body>\n" + "</html>";
	}

	private String templateEnglishSubject() {
		return "Reminder: Test Todo is due in 10 minutes";
	}

	public Map<String, String> getUserSettings(String locale) {
		Map<String, String> settings = new HashMap<>();
		settings.put("locale", locale);
		settings.put("lang", locale);
		settings.put("timezone", "UTC");
		settings.put("time_format", "hh:mm:ss");
		return settings;
	}

	public List<ContainerDescriptor> getContainerDescriptors() {
		List<ContainerDescriptor> descriptors = new ArrayList<>();
		ContainerDescriptor desc = new ContainerDescriptor();
		desc.uid = ITodoUids.defaultUserTodoList("testuser");
		desc.ownerDirEntryPath = "test.loc/users/testuser";
		desc.domainUid = MockHelper.getFakeDomain().uid;
		desc.owner = "testuser";
		desc.type = "todolist";
		desc.settings = new HashMap<>();
		descriptors.add(desc);
		return descriptors;
	}

}
