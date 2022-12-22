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
package net.bluemind.todolist.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.junit.Test;

import io.vertx.core.Vertx;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.tests.services.IRestStreamTestService;
import net.bluemind.core.rest.tests.services.RestStreamImpl;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm.Action;
import net.bluemind.lib.ical4j.util.IcalConverter;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.IVTodo;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.service.internal.VTodoService;
import net.bluemind.utils.FileUtils;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Status;

public class VTodoServiceTests extends AbstractServiceTests {

	private Vertx vertx;

	@Override
	public void before() throws Exception {
		super.before();
		vertx = VertxPlatform.getVertx();

	}

	@Override
	protected ITodoList getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(defaultSecurityContext).instance(ITodoList.class, container.uid);
	}

	@Test
	public void exportNullDtStart() throws Exception {
		VTodo todo = defaultVTodo();
		todo.dtstart = null;
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		assertNull(vtodo.getStartDate());
	}

	@Test
	public void exportDtStart() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		todo.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2015, 12, 25, 0, 0, 0, 0, utcTz), Precision.DateTime);
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		assertEquals(vtodo.getStartDate().getDate().getTime(), new BmDateTimeWrapper(todo.dtstart).toUTCTimestamp());
	}

	@Test
	public void exportDue() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		todo.due = BmDateTimeWrapper.create(ZonedDateTime.of(2015, 12, 26, 0, 0, 0, 0, utcTz), Precision.DateTime);
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		assertEquals(vtodo.getDue().getDate().getTime(), new BmDateTimeWrapper(todo.due).toUTCTimestamp());
	}

	@Test
	public void exportSummary() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		todo.summary = "Yellow Summary";
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		assertEquals(vtodo.getSummary().getValue(), todo.summary);
	}

	@Test
	public void exportClassification() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		todo.classification = VTodo.Classification.Confidential;
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		assertEquals(vtodo.getClassification().getValue().toLowerCase(), Clazz.CONFIDENTIAL.getValue().toLowerCase());
	}

	@Test
	public void exportLocation() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		todo.location = "NowThenHereThere";
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		assertEquals(vtodo.getLocation().getValue(), todo.location);
	}

	@Test
	public void exportDescription() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		todo.description = "Ok BlueMind, What's a Todo ?";
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		assertEquals(vtodo.getDescription().getValue(), todo.description);
	}

	@Test
	public void exportStatus() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		todo.status = VTodo.Status.Cancelled;
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		assertEquals(vtodo.getStatus().getValue().toLowerCase(), Status.VTODO_CANCELLED.getValue().toLowerCase());
	}

	@Test
	public void exportPriority() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		todo.priority = 6;
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		assertEquals(Integer.valueOf(vtodo.getPriority().getValue()), todo.priority);
	}

	@Test
	public void exportAlarm() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();

		todo.alarm = new ArrayList<ICalendarElement.VAlarm>(1);
		ICalendarElement.VAlarm alarm = ICalendarElement.VAlarm.create(Action.Email, -600, "exportAlarm", 15, 1,
				"w00t");
		todo.alarm.add(alarm);

		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		assertEquals(1, vtodo.getAlarms().size());
		VAlarm valarm = (VAlarm) vtodo.getAlarms().get(0);
		assertEquals("EMAIL", valarm.getAction().getValue());
		assertEquals(alarm.trigger.intValue(), valarm.getTrigger().getDuration().get(ChronoUnit.SECONDS));
		assertTrue(valarm.getTrigger().getDuration().get(ChronoUnit.SECONDS) < 0);
		assertEquals(alarm.description, valarm.getDescription().getValue());
		assertEquals(alarm.duration.intValue(), valarm.getDuration().getDuration().get(ChronoUnit.SECONDS));
		assertEquals(alarm.repeat.intValue(), valarm.getRepeat().getCount());
		assertEquals(alarm.summary, valarm.getSummary().getValue());
	}

	@Test
	public void exportMultipleAlarms() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();

		todo.alarm = new ArrayList<ICalendarElement.VAlarm>(1);
		ICalendarElement.VAlarm email = ICalendarElement.VAlarm.create(Action.Email, -600, "email alarm", 15, 1,
				"w00t");
		todo.alarm.add(email);

		ICalendarElement.VAlarm audio = ICalendarElement.VAlarm.create(Action.Audio, 10, "audio alarm", -10, 5,
				"tutu tu tu");
		todo.alarm.add(audio);

		ICalendarElement.VAlarm display = ICalendarElement.VAlarm.create(Action.Display, -10, "display alarm", 12, 0,
				"diiiisplay");
		todo.alarm.add(display);

		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		assertEquals(3, vtodo.getAlarms().size());

		boolean emailAlarm = false;
		boolean audioAlarm = false;
		boolean displayAlarm = false;
		for (int i = 0; i < vtodo.getAlarms().size(); i++) {
			VAlarm valarm = (VAlarm) vtodo.getAlarms().get(i);
			if ("EMAIL".equals(valarm.getAction().getValue())) {
				emailAlarm = true;
				assertEquals(email.trigger.intValue(), valarm.getTrigger().getDuration().get(ChronoUnit.SECONDS));
				assertTrue(valarm.getTrigger().getDuration().get(ChronoUnit.SECONDS) < 0);
				assertEquals(email.description, valarm.getDescription().getValue());
				assertEquals(email.duration.intValue(), valarm.getDuration().getDuration().get(ChronoUnit.SECONDS));
				assertFalse(valarm.getDuration().getDuration().get(ChronoUnit.SECONDS) < 0);
				assertEquals(email.repeat.intValue(), valarm.getRepeat().getCount());
				assertEquals(email.summary, valarm.getSummary().getValue());
			} else if ("AUDIO".equals(valarm.getAction().getValue())) {
				audioAlarm = true;
				assertEquals(audio.trigger.intValue(), valarm.getTrigger().getDuration().get(ChronoUnit.SECONDS));
				assertFalse(valarm.getTrigger().getDuration().get(ChronoUnit.SECONDS) < 0);
				assertEquals(audio.description, valarm.getDescription().getValue());
				assertEquals(audio.duration.intValue(), valarm.getDuration().getDuration().get(ChronoUnit.SECONDS));
				assertTrue(valarm.getDuration().getDuration().get(ChronoUnit.SECONDS) < 0);
				assertEquals(audio.repeat.intValue(), valarm.getRepeat().getCount());
				assertEquals(audio.summary, valarm.getSummary().getValue());
			} else if ("DISPLAY".equals(valarm.getAction().getValue())) {
				displayAlarm = true;
				assertEquals(display.trigger.intValue(), valarm.getTrigger().getDuration().get(ChronoUnit.SECONDS));
				assertTrue(valarm.getTrigger().getDuration().get(ChronoUnit.SECONDS) < 0);
				assertEquals(display.description, valarm.getDescription().getValue());
				assertEquals(display.duration.intValue(), valarm.getDuration().getDuration().get(ChronoUnit.SECONDS));
				assertFalse(valarm.getDuration().getDuration().get(ChronoUnit.SECONDS) < 0);
				assertEquals(display.repeat.intValue(), valarm.getRepeat().getCount());
				assertEquals(display.summary, valarm.getSummary().getValue());
			}

		}

		assertTrue(emailAlarm);
		assertTrue(audioAlarm);
		assertTrue(displayAlarm);

	}

	@Test
	public void exportAttendee() throws ServerFault, IOException, ParserException {
		// TODO BM-3947
	}

	@Test
	public void exportOrganizer() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		todo.percent = 75;
		String organizer = "ext" + System.currentTimeMillis() + "@extdomain.lan";
		todo.organizer = new VTodo.Organizer(organizer);
		todo.organizer.commonName = "External Organizer";
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		Organizer o = vtodo.getOrganizer();
		assertEquals(o.getParameter(Parameter.CN).getValue(), todo.organizer.commonName);
		assertEquals(o.getValue(), "mailto:" + todo.organizer.mailto);
	}

	@Test
	public void exportCategory() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		Categories c = (Categories) vtodo.getProperty(Property.CATEGORIES);
		assertNotNull(c);
		assertTrue(c.getValue().equals(tag1.label + "," + tag2.label)
				|| c.getValue().equals(tag2.label + "," + tag1.label));
	}

	@Test
	public void exportRRule() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		VTodo.RRule rrule = new VTodo.RRule();
		rrule.frequency = VTodo.RRule.Frequency.DAILY;
		rrule.interval = 1;
		rrule.count = 5;
		todo.rrule = rrule;
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		assertNotNull(vtodo.getProperties(Property.RRULE));

		RRule exported = (RRule) vtodo.getProperty(Property.RRULE);

		assertEquals(rrule.count.intValue(), exported.getRecur().getCount());
		assertEquals(rrule.frequency.toString(), exported.getRecur().getFrequency().toString());
		assertEquals(rrule.interval.intValue(), exported.getRecur().getInterval());
	}

	@Test
	public void exportExdate() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		Set<net.bluemind.core.api.date.BmDateTime> exdate = new HashSet<>(1);
		BmDateTime expected = BmDateTimeWrapper
				.create(ZonedDateTime.of(2015, 7, 6, 17, 0, 0, 0, ZoneId.of("Europe/London")), Precision.DateTime);
		exdate.add(expected);
		todo.exdate = exdate;
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);

		assertEquals(1, vtodo.getProperties(Property.EXDATE).size());

		BmDateTime exported = null;
		for (@SuppressWarnings("unchecked")
		Iterator<Property> it = vtodo.getProperties(Property.EXDATE).iterator(); it.hasNext();) {
			ExDate exDate = (ExDate) it.next();
			DateList dateList = exDate.getDates();
			for (Object o : dateList) {
				String oTimeZone = null != exDate.getTimeZone() ? exDate.getTimeZone().getID() : null;
				exported = IcalConverter.convertToDateTime((Date) o, oTimeZone);
			}
		}

		assertNotNull(exported);
		assertEquals(expected, exported);
	}

	@Test
	public void exportRDate() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		Set<net.bluemind.core.api.date.BmDateTime> rdate = new HashSet<>(1);
		BmDateTime expected = BmDateTimeWrapper
				.create(ZonedDateTime.of(2015, 7, 6, 16, 0, 0, 0, ZoneId.of("Europe/London")), Precision.DateTime);
		rdate.add(expected);
		todo.rdate = rdate;
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);

		assertEquals(1, vtodo.getProperties(Property.RDATE).size());

		BmDateTime exported = null;
		for (@SuppressWarnings("unchecked")
		Iterator<Property> it = vtodo.getProperties(Property.RDATE).iterator(); it.hasNext();) {
			RDate rDate = (RDate) it.next();
			DateList dateList = rDate.getDates();
			for (Object o : dateList) {
				String oTimeZone = null != rDate.getTimeZone() ? rDate.getTimeZone().getID() : null;
				exported = IcalConverter.convertToDateTime((Date) o, oTimeZone);
			}
		}

		assertNotNull(exported);
		assertEquals(expected, exported);
	}

	// @Test
	// public void exportRecurId() throws ServerFault, IOException,
	// ParserException {
	// VTodo todo = defaultVTodo();
	// todo.recurid = BmDateTimeWrapper.create(new DateTime(2015, 7, 6, 17, 0,
	// 0, DateTimeZone.forID("Europe/London")),
	// Precision.DateTime);
	// Calendar todolist = export(todo);
	// assertEquals(1, todolist.getComponents().size());
	// VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
	// assertEquals(IcalConverter.convertToDateTime(vtodo.getRecurrenceId(),
	// todo.recurid.timezone), todo.recurid);
	// }

	@Test
	public void exportPercent() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		todo.percent = 75;
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		assertEquals(Integer.valueOf(vtodo.getPercentComplete().getValue()), todo.percent);
	}

	@Test
	public void exportCompleted() throws ServerFault, IOException, ParserException {
		VTodo todo = defaultVTodo();
		todo.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2015, 12, 30, 0, 0, 0, 0, utcTz), Precision.DateTime);
		todo.status = ICalendarElement.Status.Completed;
		todo.completed = BmDateTimeWrapper.create(ZonedDateTime.of(2015, 12, 31, 0, 0, 0, 0, utcTz),
				Precision.DateTime);
		Calendar todolist = export(todo);
		assertEquals(1, todolist.getComponents().size());
		VToDo vtodo = (VToDo) todolist.getComponent(Component.VTODO);
		assertEquals(vtodo.getDateCompleted().getDate().getTime(),
				new BmDateTimeWrapper(todo.completed).toUTCTimestamp());
	}

	@Test
	public void exportMultiple() throws ServerFault, IOException, ParserException {
		VTodo todo1 = defaultVTodo();
		VTodo todo2 = defaultVTodo();
		Calendar todolist = export(todo1, todo2);
		assertEquals(2, todolist.getComponents().size());
	}

	@Test
	public void testSimpleImport() throws Exception {
		IVTodo vtodoService = getVTodoService();
		String ics = getIcsFromFile("testSimpleImport.ics");

		wait(vtodoService.importIcs(ics));

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete("99e00677-2935-4263-9109-d8ff54557e66");
		assertNotNull(vtodo);
		assertEquals("Test Todo", vtodo.value.summary);
		assertEquals("Toulouse", vtodo.value.location);
		assertEquals("Lorem ipsum", vtodo.value.description);
		assertEquals(VTodo.Status.NeedsAction, vtodo.value.status);
		assertEquals(2, vtodo.value.attendees.size());

		boolean john = false;
		boolean jane = false;
		for (VTodo.Attendee attendee : vtodo.value.attendees) {
			if ("john.bang@bm.lan".equals(attendee.mailto)) {
				john = true;
				assertEquals(VTodo.CUType.Individual, attendee.cutype);
				assertEquals(VTodo.Role.Chair, attendee.role);
				assertEquals(VTodo.ParticipationStatus.Accepted, attendee.partStatus);
			} else if ("jane.bang@bm.lan".equals(attendee.mailto)) {
				jane = true;
				assertEquals(VTodo.CUType.Individual, attendee.cutype);
				assertEquals(VTodo.Role.RequiredParticipant, attendee.role);
				assertEquals(VTodo.ParticipationStatus.NeedsAction, attendee.partStatus);
			}
		}

		assertTrue(john);
		assertTrue(jane);
		assertEquals(0, vtodo.value.percent.intValue());
		assertEquals(3, vtodo.value.priority.intValue());
		assertEquals(BmDateTimeWrapper.create(ZonedDateTime.of(2024, 12, 28, 0, 0, 0, 0, ZoneId.of("Europe/London")),
				Precision.DateTime), vtodo.value.dtstart);
		assertEquals(BmDateTimeWrapper.create(ZonedDateTime.of(2025, 1, 28, 0, 0, 0, 0, ZoneId.of("Europe/London")),
				Precision.DateTime), vtodo.value.due);
	}

	@Test
	public void testRRuleImport() throws Exception {
		IVTodo vtodoService = getVTodoService();
		String ics = getIcsFromFile("testRRuleImport.ics");

		wait(vtodoService.importIcs(ics));

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete("039bb0df-5259-413c-9395-69e6db0c784e");
		assertNotNull(vtodo);

		VTodo.RRule rrule = vtodo.value.rrule;
		assertNotNull(rrule);
		assertEquals(VTodo.RRule.Frequency.DAILY, rrule.frequency);
		assertEquals(1, rrule.interval.intValue());
		assertEquals(5, rrule.count.intValue());
	}

	@Test
	public void testExDateImport() throws Exception {
		IVTodo vtodoService = getVTodoService();
		String ics = getIcsFromFile("testExDateImport.ics");

		wait(vtodoService.importIcs(ics));
		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete("0b88bf21-dfd6-4a19-84f1-e36c10ddb5ed");
		assertNotNull(vtodo);
		assertEquals(1, vtodo.value.exdate.size());

		assertEquals(BmDateTimeWrapper.create(ZonedDateTime.of(2015, 7, 6, 16, 0, 0, 0, tz), Precision.DateTime),
				vtodo.value.exdate.iterator().next());
	}

	private void wait(TaskRef taskRef) throws Exception {
		long timeout = System.currentTimeMillis() + 30000;
		while (System.currentTimeMillis() < timeout) {
			ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class,
					taskRef.id);
			if (task.status().state.ended) {
				break;
			}
		}

	}

	@Test
	public void testRDateImport() throws Exception {
		IVTodo vtodoService = getVTodoService();
		String ics = getIcsFromFile("testRDateImport.ics");

		wait(vtodoService.importIcs(ics));

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext)
				.getComplete("0b88bf21-dfd6-4a19-84f1-e36c10ddb5edz");
		assertNotNull(vtodo);
		assertEquals(1, vtodo.value.rdate.size());

		assertEquals(BmDateTimeWrapper.create(ZonedDateTime.of(2015, 7, 6, 16, 0, 0, 0, tz), Precision.DateTime),
				vtodo.value.rdate.iterator().next());
	}

	@Test
	public void testVAlarmImport() throws Exception {
		IVTodo vtodoService = getVTodoService();
		String ics = getIcsFromFile("testVAlarmImport.ics");

		wait(vtodoService.importIcs(ics));

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete("99e00677-2935-4263-9109-d8ff54557e66");
		assertNotNull(vtodo);

		assertEquals(1, vtodo.value.alarm.size());
		ICalendarElement.VAlarm valarm = vtodo.value.alarm.get(0);
		assertEquals(ICalendarElement.VAlarm.Action.Email, valarm.action);
		assertEquals(-600, valarm.trigger.intValue());
		assertEquals(0, valarm.repeat.intValue());
		assertEquals(30, valarm.duration.intValue());
		assertEquals("email alarm", valarm.description);
		assertEquals("AA", valarm.summary);
	}

	@Test
	public void testMultipleVAlarmImport() throws Exception {
		IVTodo vtodoService = getVTodoService();
		String ics = getIcsFromFile("testMultipleVAlarmImport.ics");

		wait(vtodoService.importIcs(ics));

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete("99e00677-2935-4263-9109-d8ff54557e66");
		assertNotNull(vtodo);

		assertEquals(3, vtodo.value.alarm.size());

		boolean emailAlarm = false;
		boolean audioAlarm = false;
		boolean displayAlarm = false;
		for (int i = 0; i < vtodo.value.alarm.size(); i++) {
			ICalendarElement.VAlarm valarm = vtodo.value.alarm.get(i);
			if (ICalendarElement.VAlarm.Action.Email == valarm.action) {
				emailAlarm = true;
				assertEquals(ICalendarElement.VAlarm.Action.Email, valarm.action);
				assertEquals(-600, valarm.trigger.intValue());
				assertEquals(0, valarm.repeat.intValue());
				assertEquals(30, valarm.duration.intValue());
				assertEquals("email alarm", valarm.description);
				assertEquals("AA", valarm.summary);
			} else if (ICalendarElement.VAlarm.Action.Audio == valarm.action) {
				audioAlarm = true;
				assertEquals(200, valarm.trigger.intValue());
				assertEquals(5, valarm.repeat.intValue());
				assertEquals(5, valarm.duration.intValue());
				assertEquals("audio alarm", valarm.description);
				assertEquals("CC", valarm.summary);
			} else if (ICalendarElement.VAlarm.Action.Display == valarm.action) {
				displayAlarm = true;
				assertEquals(-60, valarm.trigger.intValue());
				assertEquals(0, valarm.repeat.intValue());
				assertEquals(-10, valarm.duration.intValue());
				assertEquals("display alarm", valarm.description);
				assertEquals("BB", valarm.summary);
			}
		}

		assertTrue(emailAlarm);
		assertTrue(audioAlarm);
		assertTrue(displayAlarm);

	}

	@Test
	public void testVAlarmDateTimeTriggerImport() throws Exception {
		IVTodo vtodoService = getVTodoService();
		String ics = getIcsFromFile("testVAlarmDateTimeTriggerImport.ics");

		wait(vtodoService.importIcs(ics));

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete("99e00677-2935-4263-9109-d8ff54557e66");
		assertNotNull(vtodo);

		assertEquals(1, vtodo.value.alarm.size());
		ICalendarElement.VAlarm valarm = vtodo.value.alarm.get(0);
		assertEquals(ICalendarElement.VAlarm.Action.Email, valarm.action);
		assertEquals(-600, valarm.trigger.intValue());
		assertEquals(0, valarm.repeat.intValue());
		assertEquals(30, valarm.duration.intValue());
		assertEquals("email alarm", valarm.description);
		assertEquals("AA", valarm.summary);
	}

	@Test
	public void testKerioAlarmImport() throws Exception {
		IVTodo vtodoService = getVTodoService();
		String ics = getIcsFromFile("kerio_alarm.ics");

		wait(vtodoService.importIcs(ics));

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete("5dbf2cbb-edae-4678-ab5a-dc891bf8242b");
		assertNotNull(vtodo);

		assertEquals(1, vtodo.value.alarm.size());
	}

	@Test
	public void testImportNullDtStart() throws Exception {
		IVTodo vtodoService = getVTodoService();
		String ics = getIcsFromFile("testImportNullDtStart.ics");

		wait(vtodoService.importIcs(ics));

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext)
				.getComplete("99e00677-2935-4263-9109-d8ff54557e66-null-dtstart");
		assertNotNull(vtodo);
		assertNull(vtodo.value.dtstart);
	}

	private String getIcsFromFile(String filename) throws IOException {
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("ics/" + filename);
		String ics = FileUtils.streamString(in, true);
		in.close();
		return ics;
	}

	private IVTodo getVTodoService() throws ServerFault {
		return new VTodoService(defaultContext, getService(defaultSecurityContext));

	}

	private IRestStreamTestService getStreamService() {
		return new RestStreamImpl(vertx);
	}

	private Calendar export(VTodo... todos) throws ServerFault, IOException, ParserException {
		LinkedList<String> uids = new LinkedList<String>();

		for (VTodo todo : todos) {
			String uid = "junit-" + System.nanoTime();
			getService(defaultSecurityContext).create(uid, todo);
			uids.add(uid);
		}

		IVTodo vtodoService = getVTodoService();
		IRestStreamTestService streamService = getStreamService();
		Stream stream = vtodoService.exportTodos(uids);
		String ics = streamService.out((stream));

		assertNotNull(ics);
		CalendarBuilder builder = new CalendarBuilder();
		UnfoldingReader ur = new UnfoldingReader(new StringReader(ics), true);
		Calendar todolist = builder.build(ur);

		return todolist;
	}

}
