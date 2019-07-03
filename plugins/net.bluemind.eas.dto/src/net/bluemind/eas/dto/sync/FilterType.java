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
package net.bluemind.eas.dto.sync;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a maximum sync period
 */
public enum FilterType {

	ALL_ITEMS(0, 0), // 0

	ONE_DAY_BACK(1, 1), // 1

	THREE_DAYS_BACK(2, 3), // 2

	ONE_WEEK_BACK(3, 7), // 3

	TWO_WEEKS_BACK(4, 14), // 4

	ONE_MONTHS_BACK(5, 31), // 5

	THREE_MONTHS_BACK(6, 90), // 6

	SIX_MONTHS_BACK(7, 180), // 7

	/**
	 * FIXME: not implemented
	 */
	FILTER_BY_NO_INCOMPLETE_TASKS(8, 3);// 8

	protected static final Logger logger = LoggerFactory.getLogger(FilterType.class);

	private final int days;
	private final int xmlIntValue;

	private FilterType(int xmlIntValue, int days) {
		this.days = days;
		this.xmlIntValue = xmlIntValue;
	}

	public static FilterType getFilterType(String number) {
		FilterType ret = FilterType.ONE_DAY_BACK;
		int xmlValue = ret.xmlIntValue;

		try {
			xmlValue = Integer.parseInt(number);
		} catch (Exception t) {
			logger.error(t.getMessage(), t);
			return ret;
		}

		for (FilterType ft : values()) {
			if (ft.xmlIntValue == xmlValue) {
				ret = ft;
				break;
			}
		}
		return ret;
	}

	public int getXmlIntValue() {
		return xmlIntValue;
	}

	public void filter(SyncState st, boolean hasChanged) {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		if (this.days > 0) {
			cal.add(Calendar.SECOND, (int) -TimeUnit.DAYS.toSeconds(days));
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
		} else {
			cal.setTimeInMillis(0);
		}
		Date fromFilter = cal.getTime();
		if (st.date == null || fromFilter.after(st.date.toDate()) || hasChanged || st.version == 0) {
			logger.info(
					"Set st.version to 0 and st.highestUid to 0 (st.date: '{}', fromFilter: '{}', filter has changed: '{}', st.version: '{}')",
					st.date, fromFilter, hasChanged, st.version);

			st.date = new DateTime(fromFilter);
			st.version = 0;
		}

	}
}
