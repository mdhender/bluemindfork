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
package net.bluemind.core.api.date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.Test;

import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.utils.JsonUtils;

public class BmDateTimeTest {

	BmDateTimeValidator validator = new BmDateTimeValidator();

	@Test
	public void testCreateLocalDate() {
		String iso = "2015-05-28";

		BmDateTime dt = BmDateTimeWrapper.create(iso);
		validator.validate(dt);

		assertEquals(Precision.Date, dt.precision);
		assertNull(dt.timezone);
		assertEquals("2015-05-28", dt.iso8601);

		dt = BmDateTimeWrapper.create(iso, "UTC");
		validator.validate(dt);

		assertEquals(Precision.Date, dt.precision);
		assertNull(dt.timezone);
		assertEquals("2015-05-28", dt.iso8601);

		iso = "2015-01-28";
		dt = BmDateTimeWrapper.create(iso, "US/Alaska");
		validator.validate(dt);

		assertEquals(Precision.Date, dt.precision);
		assertNull(dt.timezone);
		assertEquals("2015-01-28", dt.iso8601);

		iso = "2015-05-28T01:00:00+0200";
		dt = BmDateTimeWrapper.create(iso, Precision.Date);
		validator.validate(dt);

		assertEquals(Precision.Date, dt.precision);
		assertNull(dt.timezone);
		assertEquals("2015-05-28", dt.iso8601);

		dt = BmDateTimeWrapper.create(iso, null, Precision.Date);
		validator.validate(dt);

		assertEquals(Precision.Date, dt.precision);
		assertNull(dt.timezone);
		assertEquals("2015-05-28", dt.iso8601);

		// FIXME: An other valid behaviour here would be to convert iso
		// to UTC, before stripping time.
		dt = BmDateTimeWrapper.create(iso, "UTC", Precision.Date);
		validator.validate(dt);

		assertEquals(Precision.Date, dt.precision);
		assertNull(dt.timezone);
		assertEquals("2015-05-27", dt.iso8601);

		dt = BmDateTimeWrapper.create(iso, "Europe/Paris", Precision.Date);
		validator.validate(dt);

		assertEquals(Precision.Date, dt.precision);
		assertNull(dt.timezone);
		assertEquals("2015-05-28", dt.iso8601);

		long timestamp = BmDateTimeWrapper.toTimestamp("2015-05-28", null);
		dt = BmDateTimeWrapper.fromTimestamp(timestamp, null, Precision.Date);
		validator.validate(dt);

		assertEquals(Precision.Date, dt.precision);
		assertNull(dt.timezone);
		assertEquals("2015-05-28", dt.iso8601);

		dt = BmDateTimeWrapper.fromTimestamp(timestamp, "US/Alaska", Precision.Date);
		validator.validate(dt);

		assertEquals(Precision.Date, dt.precision);
		assertNull(dt.timezone);
		assertEquals("2015-05-27", dt.iso8601);

		timestamp = BmDateTimeWrapper.toTimestamp("2015-05-28T00:00:00Z", null);
		dt = BmDateTimeWrapper.fromTimestamp(timestamp, null, Precision.Date);
		validator.validate(dt);

		assertEquals(Precision.Date, dt.precision);
		assertNull(dt.timezone);
		assertEquals("2015-05-28", dt.iso8601);

		dt = BmDateTimeWrapper.fromTimestamp(timestamp, "US/Alaska", Precision.Date);
		validator.validate(dt);

		assertEquals(Precision.Date, dt.precision);
		assertNull(dt.timezone);
		assertEquals("2015-05-27", dt.iso8601);
	}

	@Test
	public void testCreateDateTime() {
		String iso = "2015-01-28T12:00:00+0100";
		BmDateTime dt = BmDateTimeWrapper.create(iso);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals("+0100", dt.timezone);
		assertEquals("2015-01-28T12:00:00.000+01:00", dt.iso8601);

		dt = BmDateTimeWrapper.create(iso, Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals("+0100", dt.timezone);
		assertEquals("2015-01-28T12:00:00.000+01:00", dt.iso8601);

		dt = BmDateTimeWrapper.create(iso, "Europe/Paris");
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals("Europe/Paris", dt.timezone);
		assertEquals("2015-01-28T12:00:00.000+01:00", dt.iso8601);

		dt = BmDateTimeWrapper.create(iso, "Europe/Paris", Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals("Europe/Paris", dt.timezone);
		assertEquals("2015-01-28T12:00:00.000+01:00", dt.iso8601);

		dt = BmDateTimeWrapper.create(iso, "Asia/Tokyo", Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals("Asia/Tokyo", dt.timezone);
		assertEquals("2015-01-28T20:00:00.000+09:00", dt.iso8601);

		iso = "2015-01-28T12:00:00";

		dt = BmDateTimeWrapper.create(iso, "Pacific/Auckland", Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals("Pacific/Auckland", dt.timezone);
		assertEquals("2015-01-28T12:00:00.000+13:00", dt.iso8601);

		dt = BmDateTimeWrapper.create(iso, "Brazil/East", Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals("Brazil/East", dt.timezone);
		assertEquals("2015-01-28T12:00:00.000-02:00", dt.iso8601);

		iso = "2015-01-28T12:00:00+0300";

		dt = BmDateTimeWrapper.create(iso, "Invalid TimeZone", Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals("+0300", dt.timezone);
		assertEquals("2015-01-28T12:00:00.000+03:00", dt.iso8601);

		long timestamp = BmDateTimeWrapper.toTimestamp("2015-05-28T12:00:00Z", null);
		dt = BmDateTimeWrapper.fromTimestamp(timestamp, "US/Alaska", Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals("US/Alaska", dt.timezone);
		assertEquals("2015-05-28T04:00:00.000-08:00", dt.iso8601);

		// FIXME: Inconsistent with create from iso string
		timestamp = BmDateTimeWrapper.toTimestamp("2015-05-28T12:00:00Z", null);
		dt = BmDateTimeWrapper.fromTimestamp(timestamp, "Invalid TimeZone", Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals(ZoneId.of("UTC").getId(), dt.timezone);
		assertEquals("2015-05-28T12:00:00.000Z", dt.iso8601);

	}

	@Test
	public void testCreateLocalDateTime() {
		String iso = "2015-01-28T12:00:00";

		BmDateTime dt = BmDateTimeWrapper.create(iso);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals(null, dt.timezone);
		assertEquals("2015-01-28T12:00:00.000", dt.iso8601);

		dt = BmDateTimeWrapper.create(iso, Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals(null, dt.timezone);
		assertEquals("2015-01-28T12:00:00.000", dt.iso8601);

		dt = BmDateTimeWrapper.create(iso, null, Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals(null, dt.timezone);
		assertEquals("2015-01-28T12:00:00.000", dt.iso8601);

		dt = BmDateTimeWrapper.create(iso, "Invalid TimeZone", Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals(null, dt.timezone);
		assertEquals("2015-01-28T12:00:00.000", dt.iso8601);

		iso = "2015-01-28T12:00:00+0200";

		dt = BmDateTimeWrapper.create(iso, null, Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertNull(dt.timezone);
		assertEquals("2015-01-28T12:00:00.000", dt.iso8601);

		iso = "2015-01-28";
		dt = BmDateTimeWrapper.create(iso, null, Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals(null, dt.timezone);
		assertEquals("2015-01-28T00:00:00.000", dt.iso8601);

		iso = "2015-01-28T12:00:00";

		dt = BmDateTimeWrapper.create(iso, "Invalid TimeZone", Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertNull(dt.timezone);
		assertEquals("2015-01-28T12:00:00.000", dt.iso8601);

		long timestamp = BmDateTimeWrapper.toTimestamp("2015-05-28T12:00:00Z", null);
		dt = BmDateTimeWrapper.fromTimestamp(timestamp, null, Precision.DateTime);
		validator.validate(dt);

		assertEquals(Precision.DateTime, dt.precision);
		assertNull(dt.timezone);
		assertEquals("2015-05-28T12:00:00.000", dt.iso8601);

	}

	@Test
	public void testClosureDate() {
		String isoWithTime = "20150101T000000.000+0200";

		BmDateTime dt = BmDateTimeWrapper.create(isoWithTime, "Etc/GMT+2");
		validator.validate(dt);

		BmDateTime dt2 = BmDateTimeWrapper.create(isoWithTime);
		validator.validate(dt2);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals(Precision.DateTime, dt2.precision);
	}

	@Test
	public void testEvaluatePrecisionWithTime() {
		String isoWithTime = "2015-05-28T16:45:43.355Z";

		BmDateTime dt = BmDateTimeWrapper.create(isoWithTime, "UTC");
		validator.validate(dt);

		BmDateTime dt2 = BmDateTimeWrapper.create(isoWithTime);
		validator.validate(dt2);

		assertEquals(Precision.DateTime, dt.precision);
		assertEquals(Precision.DateTime, dt2.precision);
	}

	@Test
	public void testCreatingLocalDateTimeShouldEvaluatePrecision() {
		String isoWithoutTime = "2015-05-28";

		BmDateTime dt = BmDateTimeWrapper.create(isoWithoutTime);
		validator.validate(dt);

		assertEquals(Precision.Date, dt.precision);
	}

	@Test
	public void testCreatingZonedDateTimeShouldAddTimeIfNotPresent() {
		String iso8601 = "2015-05-29";

		BmDateTime dt = BmDateTimeWrapper.create(iso8601, "Europe/Paris", Precision.DateTime);
		validator.validate(dt);

		assertEquals("2015-05-29T00:00:00.000+02:00", dt.iso8601);
	}

	@Test
	public void testCreatingLocalDateShouldRemoveTimeIfPresent() {
		String iso8601 = "2015-05-29T00:00:00.000+11:00";

		BmDateTime dt = BmDateTimeWrapper.create(iso8601, ZoneId.systemDefault().getId(), Precision.Date);
		validator.validate(dt);

		assertEquals("2015-05-28", dt.iso8601);
	}

	@Test
	public void testToTimeStamp() {
		String iso8601 = "2015-05-29T07:10:49.127Z";

		BmDateTime dt = BmDateTimeWrapper.create(iso8601, "UTC");
		validator.validate(dt);

		assertEquals(1432883449127l, new BmDateTimeWrapper(dt).toUTCTimestamp());
	}

	@Test
	public void testToTimeStampWithoutTimeZoneAndMillis() {
		String iso8601 = "2015-05-29T15:03:12Z";

		BmDateTime dt = BmDateTimeWrapper.create(iso8601, "UTC");
		validator.validate(dt);

		assertEquals(1432911792000l, new BmDateTimeWrapper(dt).toUTCTimestamp());
	}

	@Test
	public void testToTimeStampWithoutTimeZone() {
		String iso8601 = "2015-05-29T15:03:12.123Z";

		BmDateTime dt = BmDateTimeWrapper.create(iso8601, "UTC");
		validator.validate(dt);

		assertEquals(1432911792123l, new BmDateTimeWrapper(dt).toUTCTimestamp());
	}

	@Test
	public void testToTimeStampUsingLocalTimeWithoutTimeZoneShouldUseUTC() {
		String iso8601 = "2015-05-29T07:10:49.127Z";

		BmDateTime dt = BmDateTimeWrapper.create(iso8601);

		assertEquals(1432883449127l, new BmDateTimeWrapper(dt).toUTCTimestamp());
	}

	@Test
	public void testJsonSerialization() {
		String iso8601 = "2015-05-29T07:10:49.127Z";
		BmDateTime dt = BmDateTimeWrapper.create(iso8601);
		validator.validate(dt);

		String json = JsonUtils.asString(dt);

		assertNotNull(json);
	}

	@Test
	public void testJsonDeserialization() {
		String iso8601 = "2015-05-29T07:10:49.127Z";
		BmDateTime dt = BmDateTimeWrapper.create(iso8601, "UTC");
		validator.validate(dt);

		String json = JsonUtils.asString(dt);
		BmDateTime ldt = JsonUtils.read(json, BmDateTime.class);
		validator.validate(ldt);

		assertNotNull(ldt);
		assertEquals(iso8601, new BmDateTimeWrapper(ldt).toIso8601());
	}

	@Test
	public void testMillisValidation() {
		String iso8601 = "20130717T080000Z";

		BmDateTime dt = BmDateTimeWrapper.create(iso8601, ZoneId.of("UTC").getId());
		validator.validate(dt);

		assertTrue(dt.iso8601.startsWith("2013-07-17T"));
		try {
			new BmDateTimeWrapper(dt).format("yyyyMMdd");
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void testCallingPublicMethodsWithAnInstanceWithoutTimezoneAndTimeShouldNotThrowNPE() {
		String iso8601 = "2015-05-29";
		BmDateTimeWrapper dt = new BmDateTimeWrapper(BmDateTimeWrapper.create(iso8601, Precision.Date));
		validator.validate(dt.bmDateTime);

		assertTrue(!dt.containsTimeZone());
		dt.copy(new Date().getTime());
		assertTrue(dt.equals(dt));
		dt.format("yyyyMMdd");
		assertTrue(!dt.isBefore(dt.bmDateTime));
		dt.toString();
		dt.toIso8601();
		dt.toDateTime();
		dt.toUTCTimestamp();
		dt.toTimestamp(ZoneId.of("UTC").getId());
		dt.withTimeZone(ZoneId.of("UTC").getId());
	}

	@Test
	public void testCallingPublicMethodsWithAnInstanceWithoutTimezoneShouldNotThrowNPE() {
		String iso8601 = "2015-05-29T10:00:00.000";
		BmDateTimeWrapper dt = new BmDateTimeWrapper(BmDateTimeWrapper.create(iso8601, Precision.DateTime));
		validator.validate(dt.bmDateTime);

		assertTrue(!dt.containsTimeZone());
		dt.copy(new Date().getTime());
		assertTrue(dt.equals(dt));
		dt.format("yyyyMMdd");
		assertTrue(!dt.isBefore(dt.bmDateTime));
		dt.toString();
		dt.toIso8601();
		dt.toDateTime();
		dt.toUTCTimestamp();
		dt.toTimestamp(ZoneId.of("UTC").getId());
		dt.withTimeZone(ZoneId.of("UTC").getId());
	}

	@Test
	public void testParsingISO8601StringWithoutMillis() {
		String iso8601 = "20150623T120000+02:00";
		String format = "yyyy-MM-dd HH:mm:ss.S";
		BmDateTimeWrapper dt = new BmDateTimeWrapper(BmDateTimeWrapper.create(iso8601));
		validator.validate(dt.bmDateTime);

		String formatted = dt.format(format);

		assertNotNull(formatted);
	}

	@Test
	public void testISO8601WithTimeZone() {
		String iso8601 = "2015-05-29T00:00:0.000+02:00";
		BmDateTime dt = BmDateTimeWrapper.create(iso8601);
		validator.validate(dt);

		assertEquals("ISO8601: 2015-05-29T00:00:00.000+02:00, Precision: DateTime, Timezone: +02:00", dt.toString());

		iso8601 = "2015-05-29T00:00:0.000+04:00";
		dt = BmDateTimeWrapper.create(iso8601);
		validator.validate(dt);

		assertEquals("ISO8601: 2015-05-29T00:00:00.000+04:00, Precision: DateTime, Timezone: +04:00", dt.toString());

		iso8601 = "2015-05-29T00:00:0.000+02:00";
		dt = BmDateTimeWrapper.create(iso8601, "Europe/Paris", Precision.DateTime);
		validator.validate(dt);

		assertEquals("ISO8601: 2015-05-29T00:00:00.000+02:00, Precision: DateTime, Timezone: Europe/Paris",
				dt.toString());

		iso8601 = "2015-05-29T07:10:49.127Z";
		dt = BmDateTimeWrapper.create(iso8601);
		validator.validate(dt);

		assertEquals("ISO8601: 2015-05-29T07:10:49.127Z, Precision: DateTime, Timezone: UTC", dt.toString());
	}

	@Test
	public void testICalDate() {
		String iso8601 = "20190801";
		BmDateTime dt = BmDateTimeWrapper.create(iso8601);
		validator.validate(dt);

		assertEquals("2019-08-01", dt.iso8601);
	}

	@Test
	public void testICalDateTime() {
		String iso8601 = "20190801T192257";
		BmDateTime dt = BmDateTimeWrapper.create(iso8601);
		validator.validate(dt);

		assertEquals("2019-08-01T19:22:57.000", dt.iso8601);
	}

	@Test
	public void testFormatDateIntoDateTime() {
		String iso8601 = "2019-07-31";
		BmDateTime dt = BmDateTimeWrapper.create(iso8601);
		validator.validate(dt);

		String formattedDate = new BmDateTimeWrapper(dt).format("yyyy-MM-dd'T'HH:mm:ss.S");
		assertEquals("2019-07-31T00:00:00.0", formattedDate);

		iso8601 = "2022-12-31";
		dt = BmDateTimeWrapper.create(iso8601);
		validator.validate(dt);

		formattedDate = new BmDateTimeWrapper(dt).format("yyyy-MM-dd HH:mm:ss.SSS");
		assertEquals("2022-12-31 00:00:00.000", formattedDate);
	}

	@Test
	public void testToDateTimeIsConsistent() {
		ZonedDateTime originalZdt = ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
		BmDateTimeWrapper modifiedZdt = new BmDateTimeWrapper(
				BmDateTimeWrapper.create(originalZdt, Precision.DateTime));
		assertEquals(originalZdt, modifiedZdt.toDateTime());

		BmDateTime dt = new BmDateTime("20170512", "UTC", Precision.Date);
		BmDateTimeWrapper dtw = new BmDateTimeWrapper(dt);
		dtw.toDateTime(); // no DateTimeParseException
	}

}
