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
package net.bluemind.calendar.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.date.BmDateTime;

@BMApi(version = "3")
public class PrintOptions {

	@BMApi(version = "3")
	public static class CalendarMetadata {
		public String uid;
		public String color;

		public static CalendarMetadata create(String uid, String color) {
			CalendarMetadata ret = new CalendarMetadata();
			ret.uid = uid;
			ret.color = color;
			return ret;
		}
	}

	@BMApi(version = "3")
	public enum PrintFormat {
		SVG, PDF, PNG, JPEG;
	}

	@BMApi(version = "3")
	public enum PrintView {
		DAY, WEEK, MONTH, AGENDA;
	}

	@BMApi(version = "3")
	public enum PrintLayout {
		PORTRAIT, LANDSCAPE
	}

	public PrintView view;
	public PrintFormat format;
	public BmDateTime dateBegin;
	public BmDateTime dateEnd;
	public boolean color;
	public boolean showDetail;
	public PrintLayout layout;
	public List<CalendarMetadata> calendars;
	public Set<String> tagsFilter = null;

	public PrintOptions() {
		view = PrintView.WEEK;
		format = PrintFormat.PDF;
		layout = PrintLayout.LANDSCAPE;
		color = true;
		showDetail = false;
		calendars = new ArrayList<PrintOptions.CalendarMetadata>();
	}
}
