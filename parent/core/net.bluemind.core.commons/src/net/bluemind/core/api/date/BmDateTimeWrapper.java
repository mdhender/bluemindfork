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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import net.bluemind.core.api.date.BmDateTime.Precision;

public class BmDateTimeWrapper {

	static private DateTimeFormatter iso8601Parser = buildIso8601Parser();

	public final BmDateTime bmDateTime;

	public BmDateTimeWrapper(BmDateTime bmDateTime) {
		this.bmDateTime = bmDateTime;
	}

	/**
	 * Creates a BmDateTime based on a string, a timezone and a precision.
	 * <ul>
	 * <li>If precision is a {@link Precision#Date}, timezone parameter ignored and
	 * null will be used.</li>
	 * <li>If timezone is null and precision is a {@link Precision#DateTime},
	 * iso8601 timezone will be used. If there is no timezone in iso8601, null will
	 * be used.
	 * 
	 * FIXME: This does not seems right. We should parse iso8601 timezone only in
	 * {@link #create(String, String)}.</li>
	 * </ul>
	 * 
	 * To parse iso8601 those rules will be applied in this order With this rules
	 * applied, iso8601 will be parsed this way:
	 * <ul>
	 * <li>Either iso8601 and timezone contain a tz: iso8601 will be converted to
	 * timezone</li>
	 * <li>timezone do not contain a tz and iso8601 do: tz offset will be stripped
	 * from iso8601</li>
	 * <li>timezone do contain a tz and iso8601 do not: tz offset will be added to
	 * iso8601 without conversion</li>
	 * <li>iso8601 is a {@link Precision#DateTime}and precision is
	 * {@link Precision#Date} : time will be stripped from iso8601</li>
	 * <li>iso8601 is a {@link Precision#Date}and precision is
	 * {@link Precision#DateTime} : time will be set to 00:00:00 at timezone
	 * offset</li>
	 * </ul>
	 * 
	 * @param iso8601   Iso8601 representation of a {@link DateTime},
	 *                  {@link LocalDate}, {@link LocalDateTime}
	 * @param timezone  {@link DateTimeZone#getID()} for DateTime, null for Local
	 *                  date or local datetime. If the timezone is not recognized,
	 *                  the timezone will be parsed from iso8601.
	 * @param precision {@link Precision#Date} or {@link Precision#DateTime}
	 * @return
	 */
	public static BmDateTime create(String iso8601, String timezone, Precision precision) {
		timezone = sanitizeTimeZone(timezone, detectTimeZone(iso8601));

		iso8601 = sanitizeIso8601String(iso8601, precision, timezone);
		if (precision == Precision.Date) {
			timezone = null;
		}
		return new BmDateTime(iso8601, timezone, precision);
	}

	/**
	 * Creates a BmDateTime based on a string and a precision.
	 * 
	 * timezone is parsed from iso8601.
	 * 
	 * @see #create(String, String, Precision)
	 * @param iso8601   Iso8601 representation of a {@link DateTime},
	 *                  {@link LocalDate}, {@link LocalDateTime}
	 * @param precision {@link Precision#Date} or {@link Precision#DateTime}
	 * @return
	 */

	public static BmDateTime create(String iso8601, Precision precision) {
		return BmDateTimeWrapper.create(iso8601, detectTimeZone(iso8601), precision);
	}

	/**
	 * Creates a BmDateTime based on a string and a timezone.
	 * 
	 * Precision is parsed from iso8601.
	 * 
	 * @see #create(String, String, Precision)
	 * @param iso8601  Iso8601 representation of a {@link DateTime},
	 *                 {@link LocalDate}, {@link LocalDateTime}
	 * @param timezone {@link DateTimeZone#getID()} for DateTime, null for Local
	 *                 date or local datetime. If the timezone is not recognized,
	 *                 null will be used.
	 * @return
	 */
	public static BmDateTime create(String iso8601, String timezone) {
		return BmDateTimeWrapper.create(iso8601, timezone, evaluatePrecision(iso8601));
	}

	/**
	 * Creates a BmDateTime based on a string.
	 * 
	 * FIXME: timezone is set automatically set to null. It should be parsed from
	 * iso8601.
	 * 
	 * Precision and timezone are parsed from iso8601.
	 * 
	 * @see #create(String, String, Precision)
	 * @param iso8601 Iso8601 representation of a {@link DateTime},
	 *                {@link LocalDate}, {@link LocalDateTime}
	 * @return
	 */
	public static BmDateTime create(String iso8601) {
		return BmDateTimeWrapper.create(iso8601, detectTimeZone(iso8601), evaluatePrecision(iso8601));
	}

	/**
	 * Timezone will be set after {@link ZonedDateTime#getZone()}.
	 * 
	 * @see #create(String, String, Precision)
	 * 
	 * @param dateTime
	 * @param timezone {@link ZoneId#getId()} for DateTime, null for Local date or
	 *                 local datetime. If the timezone is not recognized, the
	 *                 default timezone ID will be used (UTC).
	 * @return
	 */
	public static BmDateTime create(ZonedDateTime dateTime, Precision precision) {
		return BmDateTimeWrapper.create(dateTime.toOffsetDateTime().toString(), dateTime.getZone().getId(), precision);
	}

	public static BmDateTime create(java.time.LocalDateTime dateTime, Precision precision) {
		return BmDateTimeWrapper.create(dateTime.toString(), ZoneId.of("UTC").getId(), precision);
	}

	/**
	 * Create a new BmDateTime using {@link BmDateTime#timezone},
	 * {@link BmDateTime#precision} and timestamp.
	 * 
	 * @see #fromTimestamp(long, String, Precision)
	 * @param timestamp
	 * @return
	 */
	public BmDateTime copy(long timestamp) {
		return fromTimestamp(timestamp, bmDateTime.timezone, bmDateTime.precision);
	}

	/**
	 * Creates a BmDateTime based on a timestamp.
	 * 
	 * timezone is set to <code>UTC</code> and precision to
	 * {@link Precision#DateTime}
	 * 
	 * @see #create(long, String, Precision)
	 * @param iso8601 Iso8601 representation of a {@link DateTime},
	 *                {@link LocalDate}, {@link LocalDateTime}
	 * @return
	 */
	public static BmDateTime fromTimestamp(long timestamp) {
		return new BmDateTime(new Date(timestamp).toInstant().toString(), ZoneId.of("UTC").getId(), Precision.DateTime);
	}

	/**
	 * Creates a BmDateTime based on a timestamp and a timezone.
	 * 
	 * precision to {@link Precision#DateTime}
	 * 
	 * @see #create(long, String, Precision)
	 * @param iso8601 Iso8601 representation of a {@link DateTime},
	 *                {@link LocalDate}, {@link LocalDateTime}
	 * @return
	 */
	public static BmDateTime fromTimestamp(long timestamp, String timezone) {
		return fromTimestamp(timestamp, timezone, Precision.DateTime);
	}

	/**
	 * Creates a BmDateTime based on a timestamp, a timezone and a precision.
	 * 
	 * timestamp will be print as iso8601 datetime. If timezone is null or timezone
	 * is invalid, the iso8601 string will be print with the UTC timezone.
	 * 
	 * @see #create(String, String, Precision)
	 * 
	 * @param timestamp the milliseconds from 1970-01-01T00:00:00Z
	 * @param timezone  {@link DateTimeZone#getID()} for DateTime, null for Local
	 *                  date or local datetime. If the timezone is not recognized
	 *                  UTC will be used.
	 * @param precision If precision is {@link Precision#Date} : No timezone nor
	 *                  time will be printed in the iso string
	 * @return
	 */
	public static BmDateTime fromTimestamp(long timestamp, String timezone, Precision precision) {
		ZonedDateTime dt;
		if (timezone == null) {
			dt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("UTC"));
		} else {
			timezone = sanitizeTimeZone(timezone, ZoneId.of("UTC").getId());
			dt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of(timezone));
		}
		String iso8601 = dt.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		return BmDateTimeWrapper.create(iso8601, timezone, precision);
	}

	private static String sanitizeTimeZone(String timezone, String fallback) {
		if (timezone != null && !containsTimeZone(timezone)) {
			String tz = TimezoneExtensions.translate(timezone);
			if (!containsTimeZone(tz)) {
				tz = fallback;
			}

			return tz;
		}
		return timezone;
	}

	public static String detectTimeZone(String iso8601) {
		try {
			if (iso8601.contains("Z") || iso8601.contains("z")) {
				return ZoneId.of("UTC").getId();
			} else {
				if (iso8601.contains("+")) {
					return iso8601.substring(iso8601.lastIndexOf("+"));
				} else {
					if (isoContainsMinusOffset(iso8601)) {
						return iso8601.substring(iso8601.lastIndexOf("-"));
					}
				}
			}
		} catch (Exception e) {
		}
		return null;

	}

	private static boolean isoContainsMinusOffset(String iso8601) {
		if (iso8601.contains("T")) {
			int start = iso8601.indexOf("T");
			return iso8601.substring(start).contains("-");
		}
		return false;
	}

	private static String sanitizeIso8601String(String iso8601, Precision precision, String timezone) {
		DateTimeFormatter printer = null;
		if (precision == Precision.Date) {
			printer = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.of(timezone));
		} else if (containsTimeZone(timezone)) {
			printer = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of(timezone));
		} else {
			printer = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
		}
		// If iso8601 or return value is a LocalDate(Time), iso8601 must not be
		// converted to default timezone.
		if (timezone != null && detectTimeZone(iso8601) != null) {
			return ZonedDateTime.parse(iso8601, iso8601Parser).format(printer);
		} else {
			return LocalDateTime.parse(iso8601, iso8601Parser).atZone(ZoneId.of(timezone)).format(printer);
		}

	}

	private static Precision evaluatePrecision(String iso8601) {
		return isoStringContainsTime(iso8601) ? Precision.DateTime : Precision.Date;
	}

	private static boolean isoStringContainsTime(String iso8601) {
		return iso8601.contains("T");
	}

	/**
	 * Convert {@link BmDateTime} from bmDateTime.timezone to timezone.
	 * 
	 * If bmDateTime.timezone is null timezone will be used without conversion. If
	 * bmDateTime.timezone is not null, bmDateTime will be converted to timezone.
	 * 
	 * @param timezone {@link DateTimeZone#getID()} for DateTime, null for Local
	 *                 date or local datetime. If the timezone is not recognized,
	 *                 the default timezone ID will be used (UTC).
	 * @return
	 */
	public BmDateTime withTimeZone(String timezone) {
		if (containsTimeZone(bmDateTime.timezone)) {
			return BmDateTimeWrapper.fromTimestamp(toUTCTimestamp(), timezone, bmDateTime.precision);
		} else {
			return BmDateTimeWrapper.create(bmDateTime.iso8601, timezone, bmDateTime.precision);
		}
	}

	/**
	 * Output the BmDateTime using the specified format pattern.
	 *
	 * @param pattern the pattern specification, null means use
	 *                <code>toString</code>
	 * @return the formatted string, not null
	 * 
	 */
	public String format(String format) {
		DateTimeFormatter dateTimeParser = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
		if (containsTimeZone()) {
			dateTimeParser = dateTimeParser.withZone(ZoneId.of(bmDateTime.timezone));
		}
		return ZonedDateTime.parse(bmDateTime.iso8601, dateTimeParser).format(DateTimeFormatter.ofPattern(format));
	}

	public String toIso8601() {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(toDateTime());
	}

	public static String toIso8601(long timestamp, String timezone) {
		ZonedDateTime ldt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of(timezone));
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ldt);
	}

	public ZonedDateTime toDateTime() {
		if (bmDateTime.precision == Precision.Date) {
			return ZonedDateTime.of(java.time.LocalDate.parse(bmDateTime.iso8601).atStartOfDay(), ZoneId.of("UTC"));
		} else if (containsTimeZone(bmDateTime.timezone)) {
			try {
				return ZonedDateTime.of(java.time.LocalDateTime.parse(bmDateTime.iso8601),
						ZoneId.of(bmDateTime.timezone));
			} catch (DateTimeException e) {
				return ZonedDateTime.parse(bmDateTime.iso8601);
			}
		} else {
			return ZonedDateTime.of(java.time.LocalDateTime.parse(bmDateTime.iso8601), ZoneId.of("UTC"));
		}
	}

	public boolean isBefore(BmDateTime date) {
		return toDateTime().isBefore(new BmDateTimeWrapper(date).toDateTime());
	}

	public boolean isAfter(BmDateTime date) {
		return toDateTime().isAfter(new BmDateTimeWrapper(date).toDateTime());
	}

	/**
	 * Test if {@link BmDateTime#timezone} is not null and is a valid identifier
	 * according to {@link DateTimeZone#getAvailableIDs()}
	 * 
	 * @return True if {@link BmDateTime#timezone} is valid
	 */
	public boolean containsTimeZone() {
		return BmDateTimeWrapper.containsTimeZone(bmDateTime.timezone);
	}

	private static boolean containsTimeZone(String timezone) {
		if (!StringUtils.isEmpty(timezone)) {
			try {
				ZoneId.of(timezone).getId();
				return true;
			} catch (DateTimeException e) {

			}
		}
		return false;
	}

	/**
	 * If {@link BmDateTime} is a representation of a {@link LocalDate} or a
	 * {@link LocalDateTime}, timezone is used as {@link BmDateTime#iso8601} tz
	 * 
	 * @see #toTimestamp(String, String)
	 * @param timezone
	 * @return Gets the milliseconds of the {@link BmDateTime} from the Java epoch
	 */
	public long toTimestamp(String timezone) {
		return BmDateTimeWrapper.toTimestamp(bmDateTime.iso8601, timezone);
	}

	/**
	 * Convert {@link BmDateTime#iso8601} to a timestamp. If {@link BmDateTime} is a
	 * {@link LocalDate} or a {@link LocalDateTime}, UTC timezone will be used.
	 * 
	 * @see BmDateTimeWrapper#toTimeZonedTimestamp(String, String)
	 * @return Gets the milliseconds of the {@link BmDateTime} from the Java epoch.
	 */
	public long toUTCTimestamp() {
		return BmDateTimeWrapper.toTimestamp(bmDateTime.iso8601, ZoneId.of("UTC").getId());
	}

	/**
	 * Gets the milliseconds of the {@link BmDateTime} from the Java epoch of
	 * 1970-01-01T00:00:00Z. If {@link BmDateTime} is a {@link LocalDate} or a
	 * {@link LocalDateTime}, timezone is used for the timestamp offset. Otherwise
	 * it is ignored.
	 * 
	 * @param iso8601  Iso8601 representation of a {@link DateTime},
	 *                 {@link LocalDate}, {@link LocalDateTime}
	 * @param timezone {@link DateTimeZone#getID()} for DateTime. If the timezone is
	 *                 not recognized, the UTC timezone will be used.
	 * @return
	 */
	public static long toTimestamp(String iso8601, String timezone) {
		try {
			if (containsTimeZone(timezone)) {
				return LocalDateTime.parse(iso8601).atZone(ZoneId.of(timezone)).toInstant().toEpochMilli();
			} else {
				try {
					return LocalDateTime.parse(iso8601).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
				} catch (DateTimeParseException e) {
					return LocalDate.parse(iso8601).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli();
				}
			}
		} catch (DateTimeException e) {
			return LocalDateTime.parse(iso8601).atZone(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS).toInstant()
					.toEpochMilli();
		}
	}

	/**
	 * Convert {@link BmDateTime} to a {@link Date}. If {@link BmDateTime} is a
	 * {@link LocalDate} or a {@link LocalDateTime}, UTC timezone will be used.
	 * 
	 * FIXME: Actually default timezone will be used. To change.
	 * 
	 * @see BmDateTimeWrapper#toUTCTimestamp()
	 * @return
	 */
	public Date toDate() {
		return new Date(toTimestamp(ZoneId.systemDefault().getId()));
	}

	/**
	 * @return
	 */
	private static DateTimeFormatter buildIso8601Parser() {
		DateTimeFormatter basicDateHourMinuteSecond = new DateTimeFormatterBuilder()
				.append(DateTimeFormatter.BASIC_ISO_DATE).appendLiteral("T").appendValue(ChronoField.HOUR_OF_DAY, 2)
				.appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendValue(ChronoField.SECOND_OF_MINUTE, 2).toFormatter();

		DateTimeFormatter basicDateHourMinuteSecondMillis = new DateTimeFormatterBuilder()
				.append(basicDateHourMinuteSecond).appendValue(ChronoField.MILLI_OF_SECOND, 3).toFormatter();

		return new DateTimeFormatterBuilder().append(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSZ"))
				.append(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssZ")).append(basicDateHourMinuteSecond)
				.append(basicDateHourMinuteSecondMillis).append(DateTimeFormatter.BASIC_ISO_DATE)
				.append(DateTimeFormatter.ISO_LOCAL_DATE).append(DateTimeFormatter.ISO_LOCAL_DATE_TIME).toFormatter();
	}
}
