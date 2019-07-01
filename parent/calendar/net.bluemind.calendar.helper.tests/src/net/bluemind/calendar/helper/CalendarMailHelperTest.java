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
package net.bluemind.calendar.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.BodyPart;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import freemarker.template.TemplateException;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.helper.mail.CalendarMailHelper;
import net.bluemind.calendar.helper.mail.Messages;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.utils.FileUtils;

public class CalendarMailHelperTest {

	@Test
	public void testConvertEventToMap() {

		VEvent vevent = defaultVEvent();
		Map<String, Object> data = new CalendarMailHelper().extractVEventDataToMap(vevent, vevent.alarm.get(0));

		assertNotNull(data);
		assertEquals(vevent.summary, data.get("title"));
		assertEquals(vevent.location, data.get("location"));
		assertEquals(new BmDateTimeWrapper(vevent.dtstart).toDate(), data.get("datebegin"));
		assertEquals(new BmDateTimeWrapper(vevent.dtstart).toDate(), data.get("dateend"));
		assertEquals(vevent.allDay(), "true".equals(data.get("allday")));
		assertEquals(vevent.description, data.get("description"));
		assertEquals(vevent.organizer.commonName, data.get("owner"));
		@SuppressWarnings("unchecked")
		List<String> attendees = (List<String>) data.get("attendees");
		assertEquals(2, attendees.size());
		assertEquals("minutes", data.get("reminder_unit"));
		assertEquals(10, data.get("reminder_duration"));
		assertNull(data.get("reminder_summary"));
		assertEquals(vevent.rrule.frequency, data.get("recurrenceKind"));
		assertEquals(vevent.rrule.interval, data.get("recurrenceFreq"));
		assertEquals(vevent.rrule.byDay, data.get("recurrenceDays"));
		assertEquals(vevent.rrule.until, data.get("recurrenceEnd"));

		// 2nd reminder
		data = new CalendarMailHelper().extractVEventDataToMap(vevent, vevent.alarm.get(1));
		assertEquals("hours", data.get("reminder_unit"));
		assertEquals(1, data.get("reminder_duration"));

		// 3rd reminder
		data = new CalendarMailHelper().extractVEventDataToMap(vevent, vevent.alarm.get(2));
		assertEquals("minutes", data.get("reminder_unit"));
		assertEquals(1, data.get("reminder_duration"));
		assertEquals("il va falloir y aller!", data.get("reminder_summary"));

	}

	@Test
	public void testExtractSubject() {
		VEvent vevent = defaultVEvent();
		vevent.summary = "testExtractSubject";
		Map<String, Object> data = new CalendarMailHelper().extractVEventDataToMap(vevent, vevent.alarm.get(0));
		Locale l = new Locale("fr");
		MessagesResolver resolver = new MessagesResolver(Messages.getEventDetailMessages(l),
				Messages.getEventAlertMessages(l));

		String subject = new CalendarMailHelper().buildSubject("EventSubjectAlert.ftl", "fr", resolver, data);

		resolver = new MessagesResolver(Messages.getEventDetailMessages(l), Messages.getEventCreateMessages(l));
		assertEquals("Rappel: testExtractSubject commence dans 10 minutes", subject);

		subject = new CalendarMailHelper().buildSubject("EventCreateSubject.ftl", "fr", resolver, data);
		assertEquals("Invitation: testExtractSubject", subject);
		resolver = new MessagesResolver(Messages.getEventDetailMessages(l), Messages.getEventUpdateMessages(l));

		subject = new CalendarMailHelper().buildSubject("EventUpdateSubject.ftl", "fr", resolver, data);
		assertEquals("Invitation modifiée: testExtractSubject", subject);

		resolver = new MessagesResolver(Messages.getEventDetailMessages(l), Messages.getEventDeleteMessages(l));

		subject = new CalendarMailHelper().buildSubject("EventDeleteSubject.ftl", "fr", resolver, data);
		assertEquals("Événement annulé: testExtractSubject", subject);

		resolver = new MessagesResolver(Messages.getEventDetailMessages(l), Messages.getEventAlertMessages(l));
		// 2nd reminder
		data = new CalendarMailHelper().extractVEventDataToMap(vevent, vevent.alarm.get(1));
		subject = new CalendarMailHelper().buildSubject("EventSubjectAlert.ftl", "fr", resolver, data);
		assertEquals("Rappel: testExtractSubject commence dans 1 heure", subject);

	}

	@Test
	public void testExtractAlertBody() throws IOException, TemplateException {
		VEvent vevent = defaultVEvent();
		vevent.summary = "testExtractBody";
		Map<String, Object> data = new CalendarMailHelper().extractVEventDataToMap(vevent, vevent.alarm.get(0));

		// FIXME userPrefs
		data.put("datetime_format", "yyyy-MM-dd HH:mm");
		data.put("time_format", "HH:mm");
		data.put("date_format", "EEE, MMMM dd, yyyy");
		TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
		data.put("timezone", tz.getID());
		Locale l = new Locale("fr");
		MessagesResolver resolver = new MessagesResolver(Messages.getEventDetailMessages(l),
				Messages.getEventAlertMessages(l));

		BodyPart body = new CalendarMailHelper().buildBody("EventAlert.ftl", "fr", resolver, data);

		assertEquals("7bit", body.getContentTransferEncoding());
		assertEquals("UTF-8", body.getCharset());

		TextBody tb = (TextBody) body.getBody();

		InputStream in = tb.getInputStream();
		String partContent = new String(ByteStreams.toByteArray(in), body.getCharset());
		in.close();

		String expected = getExpectedFromFile("EventAlert.html");
		assertEquals(expected, partContent);
	}

	@Test
	public void testExtractAlertBodyWithSummary() throws IOException, TemplateException {
		VEvent vevent = defaultVEvent();
		vevent.rrule = null;
		vevent.summary = "testExtractAlertBodyWithSummary";
		Map<String, Object> data = new CalendarMailHelper().extractVEventDataToMap(vevent, vevent.alarm.get(2));

		// FIXME userPrefs
		data.put("datetime_format", "yyyy-MM-dd HH:mm");
		data.put("time_format", "HH:mm");
		data.put("date_format", "EEE, MMMM dd, yyyy");
		TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
		data.put("timezone", tz.getID());
		Locale l = new Locale("fr");
		MessagesResolver resolver = new MessagesResolver(Messages.getEventDetailMessages(l),
				Messages.getEventAlertMessages(l));

		BodyPart body = new CalendarMailHelper().buildBody("EventAlert.ftl", "fr", resolver, data);

		assertEquals("7bit", body.getContentTransferEncoding());
		assertEquals("UTF-8", body.getCharset());

		TextBody tb = (TextBody) body.getBody();

		InputStream in = tb.getInputStream();
		String partContent = new String(ByteStreams.toByteArray(in), body.getCharset());
		in.close();

		// VAlarm summary
		assertTrue(partContent.contains("il va falloir y aller!"));

		String expected = getExpectedFromFile("EventAlertSummary.html");
		assertEquals(expected, partContent);
	}

	@Test
	public void testDateTimeOfDeserializedEventToDate() {
		VEvent vevent = defaultVEvent();
		// timestamp is not set after deserialization
		Map<String, Object> data = new CalendarMailHelper().extractVEventDataToMap(vevent, vevent.alarm.get(0));
		ZonedDateTime temp = ZonedDateTime.of(2022, 2, 13, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(temp.toInstant().toEpochMilli(), ((Date) data.get("datebegin")).getTime());
	}

	@Test
	public void testExtractInvitationBody() throws IOException, TemplateException {
		VEvent vevent = defaultVEvent();
		vevent.summary = "testExtractBody";
		Map<String, Object> data = new CalendarMailHelper().extractVEventDataToMap(vevent, vevent.alarm.get(0));

		// FIXME userPrefs
		data.put("datetime_format", "yyyy-MM-dd HH:mm");
		data.put("time_format", "HH:mm");
		data.put("date_format", "EEE, MMMM dd, yyyy");
		TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
		data.put("timezone", tz.getID());
		Locale l = new Locale("fr");
		MessagesResolver resolver = new MessagesResolver(Messages.getEventDetailMessages(l),
				Messages.getEventCreateMessages(l));

		BodyPart body = new CalendarMailHelper().buildBody("EventCreate.ftl", "fr", resolver, data);

		assertEquals("7bit", body.getContentTransferEncoding());
		assertEquals("UTF-8", body.getCharset());

		TextBody tb = (TextBody) body.getBody();

		InputStream in = tb.getInputStream();
		String partContent = new String(ByteStreams.toByteArray(in), body.getCharset());
		in.close();

		String expected = getExpectedFromFile("EventCreate.html");
		assertEquals(expected, partContent);

	}

	@Test
	public void testExtractBody_attendeeCommonNameEmpty() throws IOException, TemplateException {
		VEvent vevent = defaultVEvent();
		vevent.summary = "testExtractBody";

		VEvent.Attendee myNameIsEmtpy = VEvent.Attendee.create(VEvent.CUType.Individual, "",
				VEvent.Role.RequiredParticipant, VEvent.ParticipationStatus.NeedsAction, false, "", "", "", null, "",
				"", null, "empty.bang@bm.lan");
		vevent.attendees.add(myNameIsEmtpy);

		Map<String, Object> data = new CalendarMailHelper().extractVEventDataToMap(vevent, vevent.alarm.get(0));

		// FIXME userPrefs
		data.put("datetime_format", "yyyy-MM-dd HH:mm");
		data.put("time_format", "HH:mm");
		data.put("date_format", "EEE, MMMM dd, yyyy");
		TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
		data.put("timezone", tz.getID());
		Locale l = new Locale("fr");
		MessagesResolver resolver = new MessagesResolver(Messages.getEventDetailMessages(l),
				Messages.getEventCreateMessages(l));

		try {
			BodyPart body = new CalendarMailHelper().buildBody("EventCreate.ftl", "fr", resolver, data);
		} catch (Exception e) {
			e.printStackTrace();
			fail("should not fail");
		}
	}

	private VEvent defaultVEvent() {
		VEvent vevent = new VEvent();
		ZoneId tz = ZoneId.of("UTC");

		vevent.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, tz), Precision.Date);
		vevent.location = "Toulouse";
		vevent.description = "Lorem ipsum";
		vevent.transparency = VEvent.Transparency.Opaque;
		vevent.classification = VEvent.Classification.Private;
		vevent.status = VEvent.Status.Confirmed;
		vevent.priority = 3;

		vevent.organizer = new VEvent.Organizer("Organizer", "organizer@bm.lan");

		vevent.attendees = new ArrayList<>(2);

		VEvent.Attendee john = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.RequiredParticipant,
				VEvent.ParticipationStatus.NeedsAction, false, "", "", "", "John Bang", "", "", null,
				"john.bang@bm.lan");
		vevent.attendees.add(john);

		VEvent.Attendee jane = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.RequiredParticipant,
				VEvent.ParticipationStatus.NeedsAction, false, "", "", "", "Jane Bang", "", "", null,
				"jane.bang@bm.lan");
		vevent.attendees.add(jane);

		vevent.alarm = new ArrayList<>(1);
		vevent.alarm.add(ICalendarElement.VAlarm.create(-600));
		vevent.alarm.add(ICalendarElement.VAlarm.create(3600));
		vevent.alarm.add(ICalendarElement.VAlarm.create(-60, "il va falloir y aller!"));

		Set<BmDateTime> exdate = new HashSet<BmDateTime>(3);
		ZonedDateTime exDate = ZonedDateTime.of(1983, 2, 13, 10, 0, 0, 0, tz);
		exdate.add(BmDateTimeWrapper.create(exDate, Precision.DateTime));
		ZonedDateTime exDate2 = ZonedDateTime.of(2012, 3, 31, 8, 30, 0, 0, tz);
		exdate.add(BmDateTimeWrapper.create(exDate2, Precision.DateTime));
		ZonedDateTime exDate3 = ZonedDateTime.of(2014, 7, 14, 1, 2, 3, 0, tz);
		exdate.add(BmDateTimeWrapper.create(exDate3, Precision.DateTime));

		// add duplicate
		exdate.add(BmDateTimeWrapper.create(exDate3, Precision.DateTime));
		ZonedDateTime exDate4 = ZonedDateTime.of(2014, 7, 14, 1, 2, 3, 0, tz);
		exdate.add(BmDateTimeWrapper.create(exDate4, Precision.DateTime));

		vevent.exdate = exdate;

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.WEEKLY;
		rrule.interval = 2;
		rrule.until = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 12, 25, 13, 30, 0, 0, tz), Precision.DateTime);

		rrule.bySecond = Arrays.asList(10, 20);

		rrule.byMinute = Arrays.asList(1, 2, 3);

		rrule.byHour = Arrays.asList(2, 22);

		List<VEvent.RRule.WeekDay> weekDay = new ArrayList<VEvent.RRule.WeekDay>(4);
		weekDay.add(VEvent.RRule.WeekDay.MO);
		weekDay.add(VEvent.RRule.WeekDay.TU);
		weekDay.add(VEvent.RRule.WeekDay.TH);
		weekDay.add(VEvent.RRule.WeekDay.FR);
		rrule.byDay = weekDay;

		rrule.byMonthDay = Arrays.asList(2, 3);

		rrule.byYearDay = Arrays.asList(8, 13, 42);

		rrule.byWeekNo = Arrays.asList(8, 13, 42);

		rrule.byMonth = Arrays.asList(8);

		vevent.rrule = rrule;

		// FIXME: does not seem to be important for this test
		// vevent.recurid = BmDateTimeWrapper.create(new
		// org.joda.time.DateTime(1983, 2, 13, 0, 0, 0, tz), Precision.Date);

		return vevent;
	}

	/**
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	private String getExpectedFromFile(String filename) throws IOException {
		InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename);
		String ics = FileUtils.streamString(in, true);
		in.close();
		return ics;
	}

}
