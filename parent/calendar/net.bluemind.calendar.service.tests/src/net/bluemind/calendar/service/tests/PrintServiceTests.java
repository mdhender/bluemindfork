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
package net.bluemind.calendar.service.tests;

import java.io.IOException;
import java.time.ZonedDateTime;

import org.junit.Test;

import net.bluemind.calendar.api.IPrint;
import net.bluemind.calendar.api.PrintOptions;
import net.bluemind.calendar.api.PrintOptions.CalendarMetadata;
import net.bluemind.calendar.api.PrintOptions.PrintFormat;
import net.bluemind.calendar.api.PrintOptions.PrintView;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.pdf.internal.PrintCalendar;
import net.bluemind.calendar.service.AbstractCalendarTests;
import net.bluemind.calendar.service.internal.PrintService;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;

public class PrintServiceTests extends AbstractCalendarTests {

	@Test
	public void tesGeneratePdf() throws ServerFault, IOException {
		PrintOptions options = new PrintOptions();
		options.dateBegin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 10, 0, 0, 0, 0, utcTz), Precision.Date);
		options.dateEnd = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 17, 0, 0, 0, 0, utcTz), Precision.Date);
		options.calendars.add(CalendarMetadata.create(userCalendarContainer.uid, "#3D99FF"));

		options.format = PrintFormat.PDF;
		getPrintService(userSecurityContext).print(options);
	}

	@Test
	public void testPrintList() throws ServerFault, IOException {
		VEventSeries event = defaultVEvent();
		event.main.summary = "printSimpleEvent";
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 8, 0, 0, 0, utcTz));
		event.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 10, 0, 0, 0, utcTz));
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
		PrintOptions options = new PrintOptions();
		options.dateBegin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 10, 0, 0, 0, 0, utcTz), Precision.Date);
		options.dateEnd = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 17, 0, 0, 0, 0, utcTz), Precision.Date);
		options.calendars.add(CalendarMetadata.create(userCalendarContainer.uid, "#3D99FF"));

		options.view = PrintView.AGENDA;
		options.format = PrintFormat.PDF;
		getPrintService(userSecurityContext).print(options);
	}

	@Test
	public void tesGeneratePng() throws ServerFault, IOException {
		PrintOptions options = new PrintOptions();
		options.dateBegin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 10, 0, 0, 0, 0, utcTz), Precision.Date);
		options.dateEnd = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 17, 0, 0, 0, 0, utcTz), Precision.Date);
		options.calendars.add(CalendarMetadata.create(userCalendarContainer.uid, "#3D99FF"));

		options.format = PrintFormat.PNG;
		getPrintService(userSecurityContext).print(options);
	}

	protected IPrint getPrintService(SecurityContext context) throws ServerFault {
		Thread.currentThread().setContextClassLoader(PrintCalendar.class.getClassLoader());
		return new PrintService(new BmTestContext(context));
	}

}
