/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.calendar.service.internal;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;

abstract class InternalDate {

	protected abstract DayOfWeek getDayOfWeek();

	protected abstract InternalDate plusDays(int distance);

	protected abstract InternalDate minusDays(int distance);

	protected abstract BmDateTime toBmDateTime();

	static InternalDate of(BmDateTime bmDateTime) {
		if (bmDateTime.precision == Precision.DateTime) {
			return new InternalDateTime(new BmDateTimeWrapper(bmDateTime).toDateTime(bmDateTime.timezone));
		} else {
			return new InternalLocalDate(LocalDate.parse(bmDateTime.iso8601));
		}
	}

	static class InternalDateTime extends InternalDate {
		private final ZonedDateTime dateTime;

		public InternalDateTime(ZonedDateTime dateTime) {
			this.dateTime = dateTime;
		}

		@Override
		protected DayOfWeek getDayOfWeek() {
			return dateTime.getDayOfWeek();
		}

		@Override
		protected InternalDate plusDays(int distance) {
			return new InternalDateTime(dateTime.plusDays(distance));
		}

		@Override
		protected InternalDate minusDays(int distance) {
			return new InternalDateTime(dateTime.minusDays(distance));
		}

		@Override
		protected BmDateTime toBmDateTime() {
			return BmDateTimeWrapper.create(dateTime, Precision.DateTime);
		}

	}

	static class InternalLocalDate extends InternalDate {
		private final LocalDate date;

		public InternalLocalDate(LocalDate date) {
			this.date = date;
		}

		@Override
		protected DayOfWeek getDayOfWeek() {
			return date.getDayOfWeek();
		}

		@Override
		protected InternalDate plusDays(int distance) {
			return new InternalLocalDate(date.plusDays(distance));
		}

		@Override
		protected InternalDate minusDays(int distance) {
			return new InternalLocalDate(date.minusDays(distance));
		}

		@Override
		protected BmDateTime toBmDateTime() {
			return BmDateTimeWrapper.create(date.toString(), Precision.Date);
		}

	}

}
