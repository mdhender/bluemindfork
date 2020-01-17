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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.PrintData;
import net.bluemind.calendar.api.PrintOptions;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.pdf.internal.PrintCalendar;
import net.bluemind.calendar.pdf.internal.PrintCalendarDay;
import net.bluemind.calendar.pdf.internal.PrintCalendarList;
import net.bluemind.calendar.pdf.internal.PrintCalendarMonth;
import net.bluemind.calendar.pdf.internal.PrintContext;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.rest.BmContext;

public class PrintCalendarHelper {

	private static final Logger logger = LoggerFactory.getLogger(PrintCalendarHelper.class);

	public static PrintData printCalendar(BmContext context, PrintOptions options,
			List<ItemContainerValue<VEvent>> vevents) throws ServerFault {

		PrintData pdata = new PrintData();
		byte[] b = null;
		PrintCalendar pc = null;
		PrintContext pContext = PrintContext.create(context, options);
		switch (options.view) {
		case DAY:
			logger.debug("print day calendar");
			pc = new PrintCalendarDay(pContext, options, vevents, 1);
			pc.process();
			break;

		case WEEK:
			logger.debug("print week calendar");
			boolean showWeekend = Boolean.parseBoolean(pContext.userSettings.getOrDefault("showweekends", "true"));
			int dayNumber = showWeekend ? 7 : 5;
			pc = new PrintCalendarDay(pContext, options, vevents, dayNumber);
			pc.process();
			break;

		case MONTH:
			logger.debug("print month calendar");
			pc = new PrintCalendarMonth(pContext, options, vevents);
			pc.process();
			break;

		case AGENDA:
			logger.debug("print list calendar");
			pc = new PrintCalendarList(pContext, options, vevents);
			pc.process();
			break;

		}

		switch (options.format) {
		case SVG:
			b = pc.sendSVGString();
			break;
		case PDF:
			b = pc.sendPDFString();
			break;
		case PNG:
			b = pc.sendPNGString();
			break;
		case JPEG:
			b = pc.sendJPEGString();
			break;
		}

		pdata.data = java.util.Base64.getEncoder().encodeToString(b);
		pdata.pages = pc.pages.size();
		return pdata;
	}

}
