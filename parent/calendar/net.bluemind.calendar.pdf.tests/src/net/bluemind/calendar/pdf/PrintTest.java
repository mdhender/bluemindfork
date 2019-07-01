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
package net.bluemind.calendar.pdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.PrintOptions;
import net.bluemind.calendar.api.PrintOptions.CalendarMetadata;
import net.bluemind.calendar.api.PrintOptions.PrintFormat;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.pdf.internal.ColorPalette;
import net.bluemind.calendar.pdf.internal.PrintCalendar;
import net.bluemind.calendar.pdf.internal.PrintCalendar.CalInfo;
import net.bluemind.calendar.pdf.internal.PrintCalendarDay;
import net.bluemind.calendar.pdf.internal.PrintCalendarList;
import net.bluemind.calendar.pdf.internal.PrintCalendarMonth;
import net.bluemind.calendar.pdf.internal.PrintContext;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeHelper;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.tag.api.TagRef;
import net.bluemind.utils.FileUtils;

public class PrintTest {

	private SecurityContext userSecurityContext = new SecurityContext("test", "johndow", Arrays.asList(),
			Arrays.asList(), "testDomain");

	/**
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	private String getSvgFromFile(String filename) throws IOException {
		InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename);
		String ics = FileUtils.streamString(in, true);
		in.close();
		return ics;
	}

	@Test
	public void testSimpleEvent() throws ServerFault, IOException {
		CalInfo cal = defaultCalendar();
		VEvent event = defaultVEvent();
		event.summary = "printSimpleEvent";

		event.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 8, 0, 0, 0, ZoneId.of("Europe/Paris")));
		event.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 10, 0, 0, 0, ZoneId.of("Europe/Paris")));
		String uid = "test_" + System.nanoTime();

		PrintOptions options = new PrintOptions();
		options.dateBegin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 10, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		options.dateEnd = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 17, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		options.calendars.add(CalendarMetadata.create(cal.uid, "#3D99FF"));
		options.format = PrintFormat.SVG;

		List<ItemContainerValue<VEvent>> vevents = Arrays
				.asList(ItemContainerValue.create(cal.uid, ItemValue.create(uid, event), event));
		PrintCalendarDay p = new PrintCalendarDay(printContext(cal), options, vevents, 7);
		p.process();
		byte[] data = p.sendSVGString();

		String s = new String(data);
		assertNotNull(s);

		System.out.println("value \n" + s);

		String expected = getSvgFromFile("testSimpleEvent.svg");
		assertEquals(expected, s);
	}

	private CalInfo defaultCalendar() {
		CalInfo ret = new PrintCalendar.CalInfo();
		ret.uid = "test";
		ret.name = "John Doe";
		ret.color = "#3D99FF";
		ret.colorDarker = ColorPalette.darker(ret.color);
		ret.colorDarkerDarker = ColorPalette.darker(ret.colorDarker);
		ret.textColor = ColorPalette.textColor(ret.colorDarker);
		return ret;
	}

	private PrintContext printContext(List<CalInfo> calendars) {
		Map<String, String> settings = new HashMap<>();
		settings.put("lang", "en");
		settings.put("timeformat", "HH:mm");
		settings.put("timezone", "GMT");
		settings.put("work_hours_start", "8");
		settings.put("work_hours_end", "19");
		settings.put("day_weekstart", "monday");
		settings.put("showweekends", "yes");
		settings.put("date", "dd/MM/YY");
		settings.put("timeformat", "HH:mm");
		return new PrintContext(settings, calendars, userSecurityContext);
	}

	private PrintContext printContext(CalInfo calendar) {
		return printContext(Arrays.asList(calendar));
	}

	@Test
	public void testConflictEvent() throws ServerFault, IOException {
		CalInfo cal = defaultCalendar();
		VEvent event = defaultVEvent();
		event.summary = "e1";
		event.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 10, 0, 0, 0, ZoneId.of("UTC")));
		event.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 12, 0, 0, 0, ZoneId.of("UTC")));
		String uid = "test_" + System.nanoTime();

		VEvent event2 = defaultVEvent();
		event2.summary = "e2";
		event2.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 11, 0, 0, 0, ZoneId.of("UTC")));
		event2.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 12, 0, 0, 0, ZoneId.of("UTC")));
		String uid2 = "test_" + System.nanoTime();

		PrintOptions options = new PrintOptions();
		options.dateBegin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 10, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		options.dateEnd = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 17, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		options.calendars.add(CalendarMetadata.create(cal.uid, "#3D99FF"));

		options.format = PrintFormat.SVG;

		List<ItemContainerValue<VEvent>> vevents = Arrays.asList(
				ItemContainerValue.create(cal.uid, ItemValue.create(uid, event), event),
				ItemContainerValue.create(cal.uid, ItemValue.create(uid2, event2), event2));

		PrintCalendarDay p = new PrintCalendarDay(printContext(cal), options, vevents, 7);
		p.process();
		byte[] data = p.sendSVGString();

		String s = new String(data);
		assertNotNull(s);
		System.out.println("value \n" + s);

		assertTrue(s.contains(
				"<clipPath id=\"clip-test-1392285600000\"><rect x=\"484\" width=\"61\" y=\"183.45454\" height=\"113.454544\"/></clipPath><g><rect x=\"484\" y=\"183.45454\" width=\"64\" style=\"fill:#3D99FF;stroke-width:1;stroke:#1d4a7c;\" rx=\"3\" height=\"113.454544\"/><text x=\"487\" y=\"192.45454\" style=\"fill:#ffffff;font-weight:bolder;font-size:8px;\" clip-path=\"url(#clip-test-1392285600000)\">10:00 - 12:00</text><text><tspan x=\"487\" y=\"201.45454\" style=\"fill:#ffffff;font-weight:bolder;font-size:8px;\" clip-path=\"url(#clip-test-1392285600000)\">e1, </tspan><tspan style=\"fill:#ffffff;font-style:italic;font-size:8px;\">Toulouse</tspan></text></g>"));
		assertTrue(s.contains(
				"<clipPath id=\"clip-test-1392289200000\"><rect x=\"549\" width=\"61\" y=\"241.18182\" height=\"55.727272\"/></clipPath><g><rect x=\"549\" y=\"241.18182\" width=\"64\" style=\"fill:#3D99FF;stroke-width:1;stroke:#1d4a7c;\" rx=\"3\" height=\"55.727272\"/><text x=\"552\" y=\"250.18182\" style=\"fill:#ffffff;font-weight:bolder;font-size:8px;\" clip-path=\"url(#clip-test-1392289200000)\">11:00 - 12:00</text><text><tspan x=\"552\" y=\"259.18182\" style=\"fill:#ffffff;font-weight:bolder;font-size:8px;\" clip-path=\"url(#clip-test-1392289200000)\">e2, </tspan><tspan style=\"fill:#ffffff;font-style:italic;font-size:8px;\">Toulouse</tspan></text></g>"));

	}

	@Test
	public void testAllDayEvent() throws ServerFault, IOException {
		CalInfo cal = defaultCalendar();

		VEvent event = defaultVEvent();
		event.summary = "testAllDayEvent";
		event.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 13, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		event.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 14, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);

		String uid = "test_" + System.nanoTime();

		PrintOptions options = new PrintOptions();
		options.dateBegin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 10, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		options.dateEnd = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 17, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		options.calendars.add(CalendarMetadata.create(cal.uid, "#3D99FF"));

		options.format = PrintFormat.SVG;

		List<ItemContainerValue<VEvent>> vevents = Arrays
				.asList(ItemContainerValue.create(cal.uid, ItemValue.create(uid, event), event));

		PrintCalendarDay p = new PrintCalendarDay(printContext(cal), options, vevents, 7);
		p.process();
		byte[] data = p.sendSVGString();

		String s = new String(data);
		assertNotNull(s);
		System.out.println("value \n" + s);

		String expected = getSvgFromFile("testAllDayEvent.svg");
		System.out.println("expected \n" + expected);

		assertEquals(expected, s);
	}

	@Test
	public void testTwoDaysEvent() throws ServerFault, IOException {
		CalInfo cal = defaultCalendar();

		VEvent event = defaultVEvent();
		event.summary = "testTwoDaysEvent";
		event.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 13, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		event.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 15, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		String uid = "test_" + System.nanoTime();

		PrintOptions options = new PrintOptions();
		options.dateBegin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 10, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		options.dateEnd = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 17, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		options.calendars.add(CalendarMetadata.create(cal.uid, "#3D99FF"));

		options.format = PrintFormat.SVG;

		List<ItemContainerValue<VEvent>> vevents = Arrays
				.asList(ItemContainerValue.create(cal.uid, ItemValue.create(uid, event), event));

		PrintCalendarDay p = new PrintCalendarDay(printContext(cal), options, vevents, 7);
		p.process();
		byte[] data = p.sendSVGString();

		String s = new String(data);

		assertNotNull(s);
		System.out.println("value \n" + s);

		String expected = getSvgFromFile("testTwoDaysEvent.svg");
		System.out.println("expected \n" + expected);

		assertEquals(expected, s);
	}

	@Test
	public void testSundayMondayEvent() throws ServerFault, IOException {
		CalInfo cal = defaultCalendar();

		VEvent event = defaultVEvent();
		event.summary = "testSundayMondayEvent";
		event.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 6, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		event.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 8, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		String uid = "test_" + System.nanoTime();

		// part1
		PrintOptions options = new PrintOptions();
		options.dateBegin = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 2, 29, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		options.dateEnd = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 7, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		options.calendars.add(CalendarMetadata.create(cal.uid, "#3D99FF"));
		options.format = PrintFormat.SVG;

		List<ItemContainerValue<VEvent>> vevents = Arrays
				.asList(ItemContainerValue.create(cal.uid, ItemValue.create(uid, event), event));

		PrintCalendarDay p = new PrintCalendarDay(printContext(cal), options, vevents, 7);
		p.process();
		byte[] data = p.sendSVGString();

		String s = new String(data);

		assertNotNull(s);
		System.out.println("value \n" + s);

		String expected = getSvgFromFile("testSundayMondayEvent_part1.svg");
		System.out.println("expected \n" + expected);

		// assertEquals(expected, s);

		// part2

		options = new PrintOptions();
		options.dateBegin = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 7, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		options.dateEnd = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 14, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		options.calendars.add(CalendarMetadata.create(cal.uid, "#3D99FF"));
		options.format = PrintFormat.SVG;

		p = new PrintCalendarDay(printContext(cal), options, vevents, 7);
		p.process();
		data = p.sendSVGString();

		s = new String(data);

		assertNotNull(s);
		System.out.println("value \n" + s);

		expected = getSvgFromFile("testSundayMondayEvent_part2.svg");
		System.out.println("expected \n" + expected);

		assertEquals(expected, s);

	}

	@Test
	public void testMonth() throws ServerFault, IOException {
		CalInfo cal = defaultCalendar();

		VEvent event = defaultVEvent();
		event.summary = "testMonth";
		event.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 6, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		event.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 3, 8, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		String uid = "test_" + System.nanoTime();

		VEvent event2 = defaultVEvent();
		event2.summary = "testMonth2";
		event2.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 12, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		event2.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 13, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		String uid2 = "test_" + System.nanoTime();

		VEvent event3 = defaultVEvent();
		event3.summary = "testMonth3";
		event3.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 18, 12, 0, 0, 0, ZoneId.of("Europe/Paris")),
				Precision.DateTime);
		event3.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 18, 14, 0, 0, 0, ZoneId.of("Europe/Paris")),
				Precision.DateTime);
		String uid3 = "test_" + System.nanoTime();

		VEvent event4 = defaultVEvent();
		event4.summary = "testMonth4";
		event4.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 21, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		event4.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 4, 8, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		String uid4 = "test_" + System.nanoTime();

		VEvent event5 = defaultVEvent();
		event5.summary = "testMonth5";
		event5.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 31, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		event5.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 4, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		String uid5 = "test_" + System.nanoTime();

		VEvent event6 = defaultVEvent();
		event6.summary = "testMonth6";
		event6.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 4, 10, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		event6.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 4, 11, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		String uid6 = "test_" + System.nanoTime();

		VEvent event7 = defaultVEvent();
		event7.summary = "testMonth7";
		event7.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 20, 16, 0, 0, 0, ZoneId.of("Europe/Paris")),
				Precision.DateTime);
		event7.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 20, 18, 0, 0, 0, ZoneId.of("Europe/Paris")),
				Precision.DateTime);
		String uid7 = "test_" + System.nanoTime();

		PrintOptions options = new PrintOptions();
		options.dateBegin = BmDateTimeWrapper
				.create(ZonedDateTime.of(2016, 2, 29, 0, 0, 0, 0, ZoneId.of("Europe/Paris")), Precision.Date);
		options.dateEnd = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 4, 11, 0, 0, 0, 0, ZoneId.of("Europe/Paris")),
				Precision.Date);
		options.calendars.add(CalendarMetadata.create(cal.uid, "#3D99FF"));

		options.format = PrintFormat.SVG;

		List<ItemContainerValue<VEvent>> vevents = Arrays.asList(
				ItemContainerValue.create(cal.uid, ItemValue.create(uid, event), event),
				ItemContainerValue.create(cal.uid, ItemValue.create(uid2, event2), event2),
				ItemContainerValue.create(cal.uid, ItemValue.create(uid3, event3), event3),
				ItemContainerValue.create(cal.uid, ItemValue.create(uid4, event4), event4),
				ItemContainerValue.create(cal.uid, ItemValue.create(uid5, event5), event5),
				ItemContainerValue.create(cal.uid, ItemValue.create(uid6, event6), event6),
				ItemContainerValue.create(cal.uid, ItemValue.create(uid7, event7), event7));

		PrintCalendarMonth p = new PrintCalendarMonth(printContext(cal), options, vevents);
		p.process();
		byte[] data = p.sendSVGString();

		String s = new String(data);
		System.out.println("value : " + s);
		assertNotNull(s);
		assertEquals(s,
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" contentScriptType=\"text/ecmascript\" width=\"1042\" zoomAndPan=\"magnify\" contentStyleType=\"text/css\" height=\"744\" preserveAspectRatio=\"xMidYMid meet\" version=\"1.0\"><text><tspan x=\"30\" y=\"30\">Monday 29 February 2016 - Sunday 10 April 2016</tspan></text><text x=\"30\" y=\"42\"><tspan style=\"fill:#3D99FF\">John Doe</tspan></text><rect width=\"952\" x=\"60\" height=\"636\" y=\"66\" style=\"fill:white;stroke:#CCC;stroke-width:1;\"/><text x=\"128\" y=\"63\" text-anchor=\"middle\">Mon</text><line y2=\"702\" style=\"fill:white;stroke:#CCC;stroke-width:1;\" x1=\"60\" x2=\"60\" y1=\"66\"/><text x=\"264\" y=\"63\" text-anchor=\"middle\">Tue</text><line y2=\"702\" style=\"fill:white;stroke:#CCC;stroke-width:1;\" x1=\"196\" x2=\"196\" y1=\"66\"/><text x=\"400\" y=\"63\" text-anchor=\"middle\">Wed</text><line y2=\"702\" style=\"fill:white;stroke:#CCC;stroke-width:1;\" x1=\"332\" x2=\"332\" y1=\"66\"/><text x=\"536\" y=\"63\" text-anchor=\"middle\">Thu</text><line y2=\"702\" style=\"fill:white;stroke:#CCC;stroke-width:1;\" x1=\"468\" x2=\"468\" y1=\"66\"/><text x=\"672\" y=\"63\" text-anchor=\"middle\">Fri</text><line y2=\"702\" style=\"fill:white;stroke:#CCC;stroke-width:1;\" x1=\"604\" x2=\"604\" y1=\"66\"/><text x=\"808\" y=\"63\" text-anchor=\"middle\">Sat</text><line y2=\"702\" style=\"fill:white;stroke:#CCC;stroke-width:1;\" x1=\"740\" x2=\"740\" y1=\"66\"/><text x=\"944\" y=\"63\" text-anchor=\"middle\">Sun</text><line y2=\"702\" style=\"fill:white;stroke:#CCC;stroke-width:1;\" x1=\"876\" x2=\"876\" y1=\"66\"/><line y2=\"66\" style=\"fill:white;stroke:#CCC;stroke-width:1;\" x1=\"60\" x2=\"1012\" y1=\"66\"/><line y2=\"172\" style=\"fill:white;stroke:#CCC;stroke-width:1;\" x1=\"60\" x2=\"1012\" y1=\"172\"/><line y2=\"278\" style=\"fill:white;stroke:#CCC;stroke-width:1;\" x1=\"60\" x2=\"1012\" y1=\"278\"/><line y2=\"384\" style=\"fill:white;stroke:#CCC;stroke-width:1;\" x1=\"60\" x2=\"1012\" y1=\"384\"/><line y2=\"490\" style=\"fill:white;stroke:#CCC;stroke-width:1;\" x1=\"60\" x2=\"1012\" y1=\"490\"/><line y2=\"596\" style=\"fill:white;stroke:#CCC;stroke-width:1;\" x1=\"60\" x2=\"1012\" y1=\"596\"/><text x=\"181\" y=\"78\">01</text><text x=\"30\" y=\"119\">10</text><text x=\"317\" y=\"78\">02</text><text x=\"453\" y=\"78\">03</text><text x=\"589\" y=\"78\">04</text><text x=\"725\" y=\"78\">05</text><text x=\"861\" y=\"78\">06</text><text x=\"997\" y=\"78\">07</text><text x=\"181\" y=\"184\">08</text><text x=\"30\" y=\"225\">11</text><text x=\"317\" y=\"184\">09</text><text x=\"453\" y=\"184\">10</text><text x=\"589\" y=\"184\">11</text><text x=\"725\" y=\"184\">12</text><text x=\"861\" y=\"184\">13</text><text x=\"997\" y=\"184\">14</text><text x=\"181\" y=\"290\">15</text><text x=\"30\" y=\"331\">12</text><text x=\"317\" y=\"290\">16</text><text x=\"453\" y=\"290\">17</text><text x=\"589\" y=\"290\">18</text><text x=\"725\" y=\"290\">19</text><text x=\"861\" y=\"290\">20</text><text x=\"997\" y=\"290\">21</text><text x=\"181\" y=\"396\">22</text><text x=\"30\" y=\"437\">13</text><text x=\"317\" y=\"396\">23</text><text x=\"453\" y=\"396\">24</text><text x=\"589\" y=\"396\">25</text><text x=\"725\" y=\"396\">26</text><text x=\"861\" y=\"396\">27</text><text x=\"997\" y=\"396\">28</text><text x=\"181\" y=\"502\">29</text><text x=\"30\" y=\"543\">14</text><text x=\"317\" y=\"502\">30</text><text x=\"453\" y=\"502\">31</text><text x=\"589\" y=\"502\">01</text><text x=\"725\" y=\"502\">02</text><text x=\"861\" y=\"502\">03</text><text x=\"997\" y=\"502\">04</text><text x=\"181\" y=\"608\">05</text><text x=\"30\" y=\"649\">15</text><text x=\"317\" y=\"608\">06</text><text x=\"453\" y=\"608\">07</text><text x=\"589\" y=\"608\">08</text><text x=\"725\" y=\"608\">09</text><text x=\"861\" y=\"608\">10</text><rect x=\"876\" y=\"184\" width=\"136\" style=\"fill:#3D99FF;stroke-width:1;stroke:#1d4a7c;\" rx=\"3\" height=\"12\"><clipPath id=\"clip-test-1457308800000\"/></rect><text x=\"879\" y=\"193\" style=\"fill:#ffffff;font-weight:bolder;font-size:8px;\">testMonth, <tspan style=\"fill:#ffffff;font-style:italic;font-size:8px;\">Toulouse</tspan></text><rect x=\"60\" y=\"608\" width=\"544\" style=\"fill:#3D99FF;stroke-width:1;stroke:#1d4a7c;\" rx=\"3\" height=\"12\"><clipPath id=\"clip-test-1460073600000\"/></rect><text x=\"63\" y=\"617\" style=\"fill:#ffffff;font-weight:bolder;font-size:8px;\">testMonth4, <tspan style=\"fill:#ffffff;font-style:italic;font-size:8px;\">Toulouse</tspan></text><rect x=\"60\" y=\"502\" width=\"952\" style=\"fill:#3D99FF;stroke-width:1;stroke:#1d4a7c;\" rx=\"3\" height=\"12\"><clipPath id=\"clip-test-1459728000000\"/></rect><text x=\"63\" y=\"511\" style=\"fill:#ffffff;font-weight:bolder;font-size:8px;\">testMonth4, <tspan style=\"fill:#ffffff;font-style:italic;font-size:8px;\">Toulouse</tspan></text><rect x=\"468\" y=\"516\" width=\"136\" style=\"fill:#3D99FF;stroke-width:1;stroke:#1d4a7c;\" rx=\"3\" height=\"12\"><clipPath id=\"clip-test-1459468800000\"/></rect><text x=\"471\" y=\"525\" style=\"fill:#ffffff;font-weight:bolder;font-size:8px;\">testMonth5, <tspan style=\"fill:#ffffff;font-style:italic;font-size:8px;\">Toulouse</tspan></text><rect x=\"60\" y=\"396\" width=\"952\" style=\"fill:#3D99FF;stroke-width:1;stroke:#1d4a7c;\" rx=\"3\" height=\"12\"><clipPath id=\"clip-test-1459123200000\"/></rect><text x=\"63\" y=\"405\" style=\"fill:#ffffff;font-weight:bolder;font-size:8px;\">testMonth4, <tspan style=\"fill:#ffffff;font-style:italic;font-size:8px;\">Toulouse</tspan></text><rect x=\"60\" y=\"184\" width=\"136\" style=\"fill:#3D99FF;stroke-width:1;stroke:#1d4a7c;\" rx=\"3\" height=\"12\"><clipPath id=\"clip-test-1457395200000\"/></rect><text x=\"63\" y=\"193\" style=\"fill:#ffffff;font-weight:bolder;font-size:8px;\">testMonth, <tspan style=\"fill:#ffffff;font-style:italic;font-size:8px;\">Toulouse</tspan></text><rect x=\"740\" y=\"184\" width=\"136\" style=\"fill:#3D99FF;stroke-width:1;stroke:#1d4a7c;\" rx=\"3\" height=\"12\"><clipPath id=\"clip-test-1457827200000\"/></rect><text x=\"743\" y=\"193\" style=\"fill:#ffffff;font-weight:bolder;font-size:8px;\">testMonth2, <tspan style=\"fill:#ffffff;font-style:italic;font-size:8px;\">Toulouse</tspan></text><clipPath id=\"clip-testt-1458385200000\"><rect width=\"136\" x=\"604\" height=\"12\" y=\"290\" style=\"fill:none\"/></clipPath><text x=\"607\" y=\"299\" style=\"fill:#3D99FF;font-weight:bolder;font-size:8px;width:136;\" clip-path=\"url(#clip-testt-1458385200000)\">11:00 - testMonth3, <tspan style=\"fill:#3D99FF;font-weight:bolder;font-size:8px;width:136;\">Toulouse</tspan></text><clipPath id=\"clip-testt-1458572400000\"><rect width=\"136\" x=\"876\" height=\"12\" y=\"396\" style=\"fill:none\"/></clipPath><text x=\"879\" y=\"405\" style=\"fill:#3D99FF;font-weight:bolder;font-size:8px;width:136;\" clip-path=\"url(#clip-testt-1458572400000)\">15:00 - testMonth7, <tspan style=\"fill:#3D99FF;font-weight:bolder;font-size:8px;width:136;\">Toulouse</tspan></text></svg>");

	}

	@Test
	public void testListWithColor() throws ServerFault, IOException {
		CalInfo cal = defaultCalendar();

		CalInfo otherCal = new PrintCalendar.CalInfo();
		otherCal.uid = "test2";
		otherCal.name = "Michael Smith";
		otherCal.color = "#FFA53D";
		otherCal.colorDarker = ColorPalette.darker(otherCal.color);
		otherCal.colorDarkerDarker = ColorPalette.darker(otherCal.color);
		otherCal.textColor = ColorPalette.textColor(otherCal.color);

		VEvent event = defaultVEvent();
		event.summary = "testList1";
		event.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 6, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		event.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 8, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		event.attendees.get(0).uri = "test"; // uri = calInfo uid
		String uid = "test_" + System.nanoTime();

		VEvent event2 = defaultVEvent();
		event2.summary = "testList2";
		event2.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 12, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		event2.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 13, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		event2.attendees.get(0).uri = "test";
		String uid2 = "test_" + System.nanoTime();

		VEvent event3 = defaultVEvent();
		event3.summary = "testList3";
		event3.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 18, 12, 0, 0, 0, ZoneId.systemDefault()),
				Precision.DateTime);
		event3.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 3, 18, 14, 0, 0, 0, ZoneId.systemDefault()),
				Precision.DateTime);
		event3.attendees.get(0).uri = "test2";
		String uid3 = "test_" + System.nanoTime();

		PrintOptions options = new PrintOptions();
		options.dateBegin = BmDateTimeWrapper
				.create(ZonedDateTime.of(2016, 2, 29, 0, 0, 0, 0, ZoneId.of("Europe/Paris")), Precision.Date);
		options.dateEnd = BmDateTimeWrapper.create(ZonedDateTime.of(2016, 4, 11, 0, 0, 0, 0, ZoneId.of("Europe/Paris")),
				Precision.Date);
		options.calendars.add(CalendarMetadata.create(cal.uid, "#3D99FF"));
		options.calendars.add(CalendarMetadata.create(otherCal.uid, "#FFA53D"));

		options.format = PrintFormat.SVG;

		List<ItemContainerValue<VEvent>> vevents = Arrays.asList(
				ItemContainerValue.create(cal.uid, ItemValue.create(uid, event), event),
				ItemContainerValue.create(cal.uid, ItemValue.create(uid2, event2), event2),
				ItemContainerValue.create(otherCal.uid, ItemValue.create(uid3, event3), event3));

		PrintCalendarList p = new PrintCalendarList(printContext(Arrays.asList(cal, otherCal)), options, vevents);
		p.process();

		byte[] data = p.sendSVGString();

		String s = new String(data);
		assertNotNull(s);
		// assertEquals(s,
		// "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg
		// xmlns=\"http://www.w3.org/2000/svg\"
		// xmlns:xlink=\"http://www.w3.org/1999/xlink\"
		// contentScriptType=\"text/ecmascript\" width=\"1042\" zoomAndPan=\"magnify\"
		// contentStyleType=\"text/css\" height=\"744\" preserveAspectRatio=\"xMidYMid
		// meet\" version=\"1.0\"><text><tspan x=\"30\" y=\"30\">Monday 29 February 2016
		// - Sunday 10 April 2016</tspan></text><text x=\"30\" y=\"42\"><tspan
		// style=\"fill:#3D99FF\">John Doe</tspan><tspan>, </tspan><tspan
		// style=\"fill:#FFA53D\">Michael Smith</tspan></text></svg>");

	}

	/**
	 * We print the 3 default calendars of John, Duncan and Toto. The MerguezParty
	 * event has been accepted by John and Duncan. Since Toto has already planned to
	 * do <b><font size="3" color="#ff66ff">Aqua-Poney</font></b>, he cannot attend
	 * and has declined the event.<br>
	 * <br>
	 * The print option "show_declined_events" is set to "false".<br>
	 * <br>
	 * In the print result, we expect to see 2 events: one for John and one for
	 * Duncan. None for Toto.
	 */
	@Test
	public void testDontShowDeclinedEvent() throws ServerFault, IOException {

		final String johnDoeDefaultCalUid = ICalendarUids.defaultUserCalendar("jdoe-jdoe-jdoe-jdoe");
		final String totoMaticDefaultCalUid = ICalendarUids.defaultUserCalendar("tmatic-tmatic-tmatic");
		final String duncanMacLeodDefaultCalUid = ICalendarUids.defaultUserCalendar("dmleod-dmleod-dmleod");

		final String svg = this.buildSVGForDeclinedEventsTests(johnDoeDefaultCalUid, totoMaticDefaultCalUid,
				duncanMacLeodDefaultCalUid, false);
		assertNotNull(svg);

		assertTrue("John has accepted, the event should be displayed for his calendar",
				svg.contains(johnDoeDefaultCalUid));
		assertTrue("Duncan has accepted, the event should be displayed for his calendar",
				svg.contains(duncanMacLeodDefaultCalUid));
		assertTrue("Toto has declined, the event should NOT be displayed for his calendar",
				!svg.contains(totoMaticDefaultCalUid));
	}

	/**
	 * We print the 3 default calendars of John, Duncan and Toto. The MerguezParty
	 * event has been accepted by John and Duncan. Since Toto has already planned to
	 * do <b><font size="3" color="#ff66ff">Aqua-Poney</font></b>, he cannot attend
	 * and has declined the event.<br>
	 * <br>
	 * The print option "show_declined_events" is set to "true".<br>
	 * <br>
	 * In the print result, we expect to see 3 events: one for John, one for Duncan
	 * and even one for Toto since show_declined_events is on.
	 */
	@Test
	public void testShowDeclinedEvent() throws ServerFault, IOException {

		final String johnDoeDefaultCalUid = ICalendarUids.defaultUserCalendar("jdoe-jdoe-jdoe-jdoe");
		final String totoMaticDefaultCalUid = ICalendarUids.defaultUserCalendar("tmatic-tmatic-tmatic");
		final String duncanMacLeodDefaultCalUid = ICalendarUids.defaultUserCalendar("dmleod-dmleod-dmleod");

		final String svg = this.buildSVGForDeclinedEventsTests(johnDoeDefaultCalUid, totoMaticDefaultCalUid,
				duncanMacLeodDefaultCalUid, true);
		assertNotNull(svg);

		assertTrue("John has accepted, the event should be displayed for his calendar",
				svg.contains(johnDoeDefaultCalUid));
		assertTrue("Duncan has accepted, the event should be displayed for his calendar",
				svg.contains(duncanMacLeodDefaultCalUid));
		assertTrue(
				"Toto has declined but 'show_declined_events' is true, the event should be displayed for his calendar",
				svg.contains(totoMaticDefaultCalUid));
	}

	private String buildSVGForDeclinedEventsTests(final String johnDoeDefaultCalUid,
			final String totoMaticDefaultCalUid, final String duncanMacLeodDefaultCalUid,
			final boolean showDeclinedEvents) {
		final CalInfo johnDoeDefaultCal = defaultCalendar();
		johnDoeDefaultCal.name = "johnDoeDefaultCal";
		johnDoeDefaultCal.uid = johnDoeDefaultCalUid;
		johnDoeDefaultCal.color = "#FFA53D";
		johnDoeDefaultCal.colorDarker = ColorPalette.darker(johnDoeDefaultCal.color);
		johnDoeDefaultCal.colorDarkerDarker = ColorPalette.darker(johnDoeDefaultCal.color);
		johnDoeDefaultCal.textColor = ColorPalette.textColor(johnDoeDefaultCal.color);

		final CalInfo totoMaticDefaultCal = defaultCalendar();
		totoMaticDefaultCal.name = "totoMaticDefaultCal";
		totoMaticDefaultCal.uid = totoMaticDefaultCalUid;
		totoMaticDefaultCal.color = "#88A53D";
		totoMaticDefaultCal.colorDarker = ColorPalette.darker(totoMaticDefaultCal.color);
		totoMaticDefaultCal.colorDarkerDarker = ColorPalette.darker(totoMaticDefaultCal.color);
		totoMaticDefaultCal.textColor = ColorPalette.textColor(totoMaticDefaultCal.color);

		final CalInfo duncanMacLeodDefaultCal = defaultCalendar();
		duncanMacLeodDefaultCal.name = "duncanMacLeodDefaultCal";
		duncanMacLeodDefaultCal.uid = duncanMacLeodDefaultCalUid;
		duncanMacLeodDefaultCal.color = "#00A53D";
		duncanMacLeodDefaultCal.colorDarker = ColorPalette.darker(duncanMacLeodDefaultCal.color);
		duncanMacLeodDefaultCal.colorDarkerDarker = ColorPalette.darker(duncanMacLeodDefaultCal.color);
		duncanMacLeodDefaultCal.textColor = ColorPalette.textColor(duncanMacLeodDefaultCal.color);

		final VEvent event = this.defaultVEvent();
		event.summary = "MerguezParty";
		event.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 8, 0, 0, 0, ZoneId.of("Europe/Paris")));
		event.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 10, 0, 0, 0, ZoneId.of("Europe/Paris")));
		final List<VEvent.Attendee> attendees = new ArrayList<>(2);
		final VEvent.Attendee totoMatic = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Declined, true, "", "", "", "printMe",
				"bm://bm.lan/users/tmatic-tmatic-tmatic", null, "", "printMe@bm.lan");
		attendees.add(totoMatic);
		final VEvent.Attendee duncanMacLeod = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "printMe",
				"bm://bm.lan/users/dmleod-dmleod-dmleod", null, "", "printMe@bm.lan");
		attendees.add(duncanMacLeod);
		event.attendees = attendees;

		final PrintOptions options = new PrintOptions();
		options.dateBegin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 10, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		options.dateEnd = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 17, 0, 0, 0, 0, ZoneId.systemDefault()),
				Precision.Date);
		options.calendars.add(CalendarMetadata.create(johnDoeDefaultCal.uid, "#3D99FF"));
		options.calendars.add(CalendarMetadata.create(totoMaticDefaultCal.uid, "#0099FF"));
		options.calendars.add(CalendarMetadata.create(duncanMacLeodDefaultCal.uid, "#FF99FF"));
		options.format = PrintFormat.SVG;

		final List<ItemContainerValue<VEvent>> vevents = Arrays.asList(
				ItemContainerValue.create(johnDoeDefaultCal.uid, ItemValue.create("id1", event), event),
				ItemContainerValue.create(totoMaticDefaultCal.uid, ItemValue.create("id2", event), event),
				ItemContainerValue.create(duncanMacLeodDefaultCal.uid, ItemValue.create("id3", event), event));

		final PrintContext printContext = this
				.printContext(Arrays.asList(johnDoeDefaultCal, totoMaticDefaultCal, duncanMacLeodDefaultCal));
		printContext.userSettings.put("show_declined_events", Boolean.toString(showDeclinedEvents));

		final PrintCalendarDay printCalendarDay = new PrintCalendarDay(printContext, options, vevents, 7);
		printCalendarDay.process();
		final byte[] data = printCalendarDay.sendSVGString();

		return new String(data);
	}

	/**
	 * @return
	 */
	protected VEvent defaultVEvent() {
		VEvent event = new VEvent();
		ZoneId tz = ZoneId.of("Asia/Ho_Chi_Minh");
		event.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, tz));
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		event.organizer = new VEvent.Organizer("printMe@bm.lan");

		List<VEvent.Attendee> attendees = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "printMe", null, null, "", "printMe@bm.lan");
		attendees.add(me);

		event.attendees = attendees;

		event.categories = new ArrayList<TagRef>(2);
		// FIXME add tags
		// event.categories.add(TagRef);
		// event.categories.add(tagRef2);

		return event;
	}
}
