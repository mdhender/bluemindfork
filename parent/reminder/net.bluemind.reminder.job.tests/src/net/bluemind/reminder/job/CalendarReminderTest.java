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

import static org.junit.Assert.assertEquals;
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.job.CalendarAlarmSupport;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeHelper;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.scheduledjob.scheduler.IScheduler;
import net.bluemind.tag.api.TagRef;
import net.bluemind.user.api.User;

public class CalendarReminderTest {

	MockSendMail mailer;

	@Before
	public void setup() {
		mailer = new MockSendMail();
	}

	@Test
	public void testGetOwnerReminders() throws Exception {
		BaseContainerDescriptor container = new BaseContainerDescriptor();
		container.uid = "dummy-calendar-container";
		container.ownerDirEntryPath = "bm.lan/users/me";
		VEvent vevent = defaultVEvent().value;
		VAlarm alarm = VAlarm.create(-600);
		vevent.alarm = Arrays.asList(alarm);
		vevent.organizer.dir = "bm://bm.lan/users/me";
		VEventSeries series = new VEventSeries();
		series.main = vevent;

		CalendarAlarmSupport calendarAlarmSupport = new CalendarAlarmSupport();

		BmDateTime expected = BmDateTimeWrapper.fromTimestamp(
				new BmDateTimeWrapper(vevent.dtstart).toUTCTimestamp() + (alarm.trigger * 1000),
				vevent.dtstart.timezone);

		try {
			// Initialize static prop CalendarAlarmSupport.day to expected
			// value, to prevent
			// eventsOfTheDay to be cleansed...
			calendarAlarmSupport.getReminder(expected, container);
		} catch (Throwable e) {

		}
		CalendarAlarmSupport.eventsOfTheDay.put(container.uid, Arrays.asList(ItemValue.create("dummy-series", series)));
		List<Reminder<VEvent>> reminders = calendarAlarmSupport.getReminder(expected, container);

		assertEquals(1, reminders.size());
	}

	@Test
	public void testMailCreationFrench() throws Exception {
		IAlarmSupport<VEvent> calendarAlarmSupport = spy(new CalendarAlarmSupport());
		List<Reminder<VEvent>> reminders = new ArrayList<>();
		reminders.add(getEventReminder());
		doReturn(reminders).when(calendarAlarmSupport).getReminder(Matchers.<BmDateTime>any(),
				Matchers.<BaseContainerDescriptor>any());
		ReminderJob reminderJob = createReminderJob(calendarAlarmSupport, "fr");
		reminderJob.tick(mock(IScheduler.class), true, "test.loc", new Date());

		Assert.assertEquals(templateFrench().replaceAll("\\\\s+", ""), mailer.html.replaceAll("\\\\s+", ""));
		Assert.assertEquals(templateFrenchSubject().replaceAll("\\\\s+", ""), mailer.subject.replaceAll("\\\\s+", ""));
	}

	@Test
	public void testMailCreationEnglish() throws Exception {
		IAlarmSupport<VEvent> calendarAlarmSupport = spy(new CalendarAlarmSupport());
		List<Reminder<VEvent>> reminders = new ArrayList<>();
		reminders.add(getEventReminder());
		doReturn(reminders).when(calendarAlarmSupport).getReminder(Matchers.<BmDateTime>any(),
				Matchers.<BaseContainerDescriptor>any());
		ReminderJob reminderJob = createReminderJob(calendarAlarmSupport, "en");

		reminderJob.tick(mock(IScheduler.class), true, "test.loc", new Date());

		Assert.assertEquals(templateEnglish().replaceAll("\\\\s+", ""), mailer.html.replaceAll("\\\\s+", ""));
		Assert.assertEquals(templateEnglishSubject().replaceAll("\\\\s+", ""), mailer.subject.replaceAll("\\\\s+", ""));
	}

	@Test
	public void testMailCount() throws Exception {
		IAlarmSupport<VEvent> calendarAlarmSupport = spy(new CalendarAlarmSupport());
		List<Reminder<VEvent>> reminders = new ArrayList<>();
		reminders.add(getEventReminder());
		reminders.add(getEventReminder());
		reminders.add(getEventReminder());
		reminders.add(getEventReminder());
		doReturn(reminders).when(calendarAlarmSupport).getReminder(Matchers.<BmDateTime>any(),
				Matchers.<BaseContainerDescriptor>any());
		ReminderJob reminderJob = createReminderJob(calendarAlarmSupport, "en");

		reminderJob.tick(mock(IScheduler.class), true, "test.loc", new Date());

		verify(reminderJob, times(4)).sendMessage(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
				Matchers.any(), Matchers.any());
	}

	@Test
	public void testMailValues() throws Exception {
		IAlarmSupport<VEvent> calendarAlarmSupport = spy(new CalendarAlarmSupport());
		List<Reminder<VEvent>> reminders = new ArrayList<>();
		reminders.add(getEventReminder());
		doReturn(reminders).when(calendarAlarmSupport).getReminder(Matchers.<BmDateTime>any(),
				Matchers.<BaseContainerDescriptor>any());
		ReminderJob reminderJob = createReminderJob(calendarAlarmSupport, "en");

		reminderJob.tick(mock(IScheduler.class), true, "test.loc", new Date());

		Assert.assertEquals("testuser@test.loc", mailer.from);
		Assert.assertEquals("testuser@test.loc", mailer.to);
	}

	private ReminderJob createReminderJob(IAlarmSupport<VEvent> calendarAlarmSupport, String locale)
			throws ServerFault {
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
		doNothing().when(calendarAlarmSupport).initContainerItemsCache(Matchers.<BmDateTime>any(),
				Matchers.<BaseContainerDescriptor>any());
		doNothing().when(reminderJob).sendToMQ(Matchers.<ItemValue<Domain>>any(), Matchers.<ItemValue<Mailbox>>any(),
				Matchers.any(), Matchers.<VAlarm>any(), Matchers.any());
		reminderJob.mailer = mailer;
		doReturn(Arrays.asList(new IAlarmSupport[] { calendarAlarmSupport })).when(reminderJob).getJobImplementations();
		return reminderJob;
	}

	private Reminder<VEvent> getEventReminder() {
		ItemValue<VEvent> event = defaultVEvent();
		User user = new User();
		VAlarm alarm = new VAlarm();
		alarm.trigger = 600;
		Reminder<VEvent> reminder = new Reminder<>(user, event, alarm);
		return reminder;
	}

	protected ItemValue<VEvent> defaultVEvent() {

		VEvent event = new VEvent();
		// DateTimeZone tz = DateTimeZone.forID("Asia/Ho_Chi_Minh");
		event.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, ZoneId.of("UTC")));
		event.summary = "event 324532532523523";
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		event.organizer = new VEvent.Organizer("superman", "me@bm.lan");

		List<VEvent.Attendee> attendees = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "",
				MockHelper.getFakeUser().value.contactInfos.identification.formatedName.value, null, null, null,
				"me@bm.lan");
		attendees.add(me);

		event.attendees = attendees;

		event.categories = new ArrayList<TagRef>(0);

		return ItemValue.create("324532532523523", event);

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
				+ "<body><h1>Rappel: event 324532532523523 commence dans 10 minutes</h1>\n" + "<table>\n" + "  <tr>\n"
				+ "    <td colspan=\"2\"><h2>event 324532532523523 </h2></td>\n" + "  </tr>\n" + "  <tr>\n"
				+ "    <td class=\"key\">Quand</td>\n" + "    <td class=\"value\">\n"
				+ "          le <span class=\"date\">samedi 12 février 2022</span> <br />\n"
				+ "          de <span class=\"date\">06:00:00</span> à <span class=\"date\">06:00:00</span>\n"
				+ "            <span class=\"tz\">(Temps universel coordonné)</span>\n" + "        \n" + "    </td>\n"
				+ "  </tr>\n" + "    <tr>\n" + "      <td class=\"key\">Où</td>\n"
				+ "      <td class=\"value\">Toulouse </td>\n" + "    </tr>\n" + "  <tr>\n"
				+ "    <td class=\"key\">Organisateur</td><td class=\"value\">superman</td>\n" + "  </tr>\n"
				+ "  <tr>\n" + "    <td class=\"key\">Participants</td>\n" + "    <td class=\"value\">\n"
				+ "      <ul class=\"attendees\">\n" + "          <li>Testuser</li>\n" + "      </ul>\n" + "    </td>\n"
				+ "  </tr>\n" + "    <tr>\n" + "      <td class=\"key\">Description</td>\n"
				+ "      <td class=\"value\">Lorem ipsum </td>\n" + "    </tr>\n" + "  <tr>\n"
				+ "    <td colspan=\"2\"><h2>&nbsp;</h2></td>\n" + "  </tr>\n" + "</table>\n" + "\n" + "</body>\n"
				+ "</html>";
	}

	private String templateFrenchSubject() {
		return "Rappel: event 324532532523523 commence dans 10 minutes";
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
				+ "<body><h1>Reminder: event 324532532523523 starts in 10 minutes</h1>\n" + "<table>\n" + "  <tr>\n"
				+ "    <td colspan=\"2\"><h2>event 324532532523523 </h2></td>\n" + "  </tr>\n" + "  <tr>\n"
				+ "    <td class=\"key\">When</td>\n" + "    <td class=\"value\">\n"
				+ "          on <span class=\"date\">Sat, February 12, 2022</span> <br />\n"
				+ "          from <span class=\"date\">06:00:00</span> to <span class=\"date\">06:00:00</span>\n"
				+ "            <span class=\"tz\">(Coordinated Universal Time)</span>\n" + "        \n" + "    </td>\n"
				+ "  </tr>\n" + "    <tr>\n" + "      <td class=\"key\">Where</td>\n"
				+ "      <td class=\"value\">Toulouse </td>\n" + "    </tr>\n" + "  <tr>\n"
				+ "    <td class=\"key\">Organizer</td><td class=\"value\">superman</td>\n" + "  </tr>\n" + "  <tr>\n"
				+ "    <td class=\"key\">Attendees</td>\n" + "    <td class=\"value\">\n"
				+ "      <ul class=\"attendees\">\n" + "          <li>Testuser</li>\n" + "      </ul>\n" + "    </td>\n"
				+ "  </tr>\n" + "    <tr>\n" + "      <td class=\"key\">Description</td>\n"
				+ "      <td class=\"value\">Lorem ipsum </td>\n" + "    </tr>\n" + "  <tr>\n"
				+ "    <td colspan=\"2\"><h2>&nbsp;</h2></td>\n" + "  </tr>\n" + "</table>\n" + "\n" + "</body>\n"
				+ "</html>";
	}

	private String templateEnglishSubject() {
		return "Reminder: event 324532532523523 starts in 10 minutes";
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
		desc.uid = ICalendarUids.defaultUserCalendar("testuser");
		desc.domainUid = MockHelper.getFakeDomain().uid;
		desc.ownerDirEntryPath = "bm.lan/users/testuser";
		desc.owner = "testuser";
		desc.type = "calendar";
		desc.settings = new HashMap<>();
		descriptors.add(desc);
		return descriptors;
	}
}
