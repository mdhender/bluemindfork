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
package net.bluemind.core;

import org.junit.Assert;
import org.junit.Test;

import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.utils.DateTimeComparator;

public class DateTimeComparatorTest {

	@Test
	public void testComparingSameDateTimesInSameTimeZone() {
		String iso8601 = "2015-03-15T12:10:00.000Z";
		String timezone = "UTC";
		BmDateTime dt1 = BmDateTimeWrapper.create(iso8601, timezone);
		BmDateTime dt2 = BmDateTimeWrapper.create(iso8601, timezone);

		int comparison = new DateTimeComparator(timezone).compare(dt1, dt2);

		Assert.assertEquals(0, comparison);
	}

	@Test
	public void testComparingDifferentDateTimes_DifferentTimeInSameTimeZone() {
		String iso8601 = "2015-03-15T12:10:00.000Z";
		String iso8601_2 = "2015-03-15T13:10:00.000Z";
		String timezone = "UTC";
		BmDateTime dt1 = BmDateTimeWrapper.create(iso8601, timezone);
		BmDateTime dt2 = BmDateTimeWrapper.create(iso8601_2, timezone);

		int comparison = new DateTimeComparator(timezone).compare(dt1, dt2);

		Assert.assertFalse(comparison == 0);
	}

	@Test
	public void testComparingSameDateTimes_differentTimeButDifferentTimeZones() {
		String iso8601 = "2015-03-15T12:10:00.000Z";
		String iso8601_2 = "2015-03-15T13:10:00.000+01:00";
		String timezone = "UTC";
		BmDateTime dt1 = BmDateTimeWrapper.create(iso8601, timezone);
		BmDateTime dt2 = BmDateTimeWrapper.create(iso8601_2, "Europe/Paris");

		int comparison = new DateTimeComparator(timezone).compare(dt1, dt2);

		Assert.assertEquals(0, comparison);
	}

	@Test
	public void testComparingDifferentDateTimes_sameTimeButDifferentTimeZones() {
		String iso8601 = "2015-03-15T12:10:00.000Z";
		String iso8601_2 = "2015-03-15T12:10:00.000+01:00";
		String timezone = "UTC";
		BmDateTime dt1 = BmDateTimeWrapper.create(iso8601, timezone);
		BmDateTime dt2 = BmDateTimeWrapper.create(iso8601_2, "Europe/Paris");

		int comparison = new DateTimeComparator(timezone).compare(dt1, dt2);

		Assert.assertFalse(comparison == 0);
	}

	@Test
	public void testComparingSameDateTimesWithoutTimeZone() {
		String iso8601 = "2015-03-15T12:10:00.000";
		String timezone = "UTC";
		BmDateTime dt1 = BmDateTimeWrapper.create(iso8601, timezone);
		BmDateTime dt2 = BmDateTimeWrapper.create(iso8601, timezone);

		int comparison = new DateTimeComparator(timezone).compare(dt1, dt2);

		Assert.assertEquals(0, comparison);
	}

	@Test
	public void testComparingSameDateTimesWithoutTimeZoneAndtime() {
		String iso8601 = "2015-03-15";
		String timezone = "UTC";
		BmDateTime dt1 = BmDateTimeWrapper.create(iso8601, timezone);
		BmDateTime dt2 = BmDateTimeWrapper.create(iso8601, timezone);

		int comparison = new DateTimeComparator(timezone).compare(dt1, dt2);

		Assert.assertEquals(0, comparison);
	}

	@Test
	public void testComparingSameDateTimes_sameTimeButOneWithoutTimeZone() {
		String iso8601 = "2015-03-15T12:10:00.000+08:00";
		String iso8601_2 = "2015-03-15T12:10:00.000";
		String timezone = "UTC";
		BmDateTime dt1 = BmDateTimeWrapper.create(iso8601, timezone);
		BmDateTime dt2 = BmDateTimeWrapper.create(iso8601_2);

		int comparison = new DateTimeComparator(timezone).compare(dt1, dt2);

		Assert.assertFalse(comparison == 0);
	}

}
