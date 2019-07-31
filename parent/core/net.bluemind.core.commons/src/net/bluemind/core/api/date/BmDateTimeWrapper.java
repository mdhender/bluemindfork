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
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import net.bluemind.core.api.date.BmDateTime.Precision;

public class BmDateTimeWrapper {

	private final static DateTimeFormatter complexParser = DateTimeFormatter.ofPattern(
			"[yyyyMMdd][yyyy-MM-dd][yyyy-DDD]['T'[HHmmss][HHmm][HH:mm:ss][HH:mm:s][HH:mm][.SSSSSSSSS][.SSSSSS][.SSS][.SS][.S]][OOOO][O][z][XXXXX][XXXX]['['VV']']");

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
	 * </ul>
	 * 
	 * To parse iso8601 those rules will be applied in this order. With this rules
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
	 * @param iso8601   Iso8601 representation of a {@link ZonedDateTime},
	 *                  {@link LocalDate}, {@link LocalDateTime}
	 * @param timezone  {@link ZoneId#getId()} for ZonedDateTime, null for Local
	 *                  date or local datetime. If the timezone is not recognized,
	 *                  the timezone will be parsed from iso8601.
	 * @param precision {@link Precision#Date} or {@link Precision#DateTime}
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
	 * @param iso8601   Iso8601 representation of a {@link ZonedDateTime},
	 *                  {@link LocalDate}, {@link LocalDateTime}
	 * @param precision {@link Precision#Date} or {@link Precision#DateTime}
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
	 * @param iso8601  Iso8601 representation of a {@link ZonedDateTime},
	 *                 {@link LocalDate}, {@link LocalDateTime}
	 * @param timezone {@link ZoneId#getId()} for ZonedDateTime, null for Local date
	 *                 or local datetime. If the timezone is not recognized, null
	 *                 will be used.
	 */
	public static BmDateTime create(String iso8601, String timezone) {
		return BmDateTimeWrapper.create(iso8601, timezone, evaluatePrecision(iso8601));
	}

	/**
	 * Creates a BmDateTime based on a string.
	 * 
	 * Precision and timezone are parsed from iso8601.
	 * 
	 * @see #create(String, String, Precision)
	 * @param iso8601 Iso8601 representation of a {@link ZonedDateTime},
	 *                {@link LocalDate}, {@link LocalDateTime}
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
	 * @param precision {@link Precision#Date} or {@link Precision#DateTime}
	 */
	public static BmDateTime create(ZonedDateTime dateTime, Precision precision) {
		return BmDateTimeWrapper.create(dateTime.toOffsetDateTime().toString(), dateTime.getZone().getId(), precision);
	}

	/**
	 * Timezone will be set to UTC.
	 * 
	 * @see #create(String, String, Precision)
	 * 
	 * @param localDateTime
	 * @param precision     {@link Precision#Date} or {@link Precision#DateTime}
	 */
	public static BmDateTime create(LocalDateTime localDateTime, Precision precision) {
		return BmDateTimeWrapper.create(ZonedDateTime.of(localDateTime, ZoneId.of("UTC")), precision);
	}

	/**
	 * Create a new BmDateTime using {@link BmDateTime#timezone},
	 * {@link BmDateTime#precision} and timestamp.
	 * 
	 * @see #fromTimestamp(long, String, Precision)
	 * @param timestamp
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
	 * @param timestamp
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
	 * @param timestamp
	 * @param timezone
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
	 * @param timezone  {@link ZoneId#getId()} for ZonedDateTime, null for Local
	 *                  date or local datetime. If the timezone is not recognized
	 *                  UTC will be used.
	 * @param precision If precision is {@link Precision#Date} : No timezone nor
	 *                  time will be printed in the iso string
	 */
	public static BmDateTime fromTimestamp(long timestamp, String timezone, Precision precision) {
		ZonedDateTime dt;
		if (timezone == null) {
			dt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("UTC"));
		} else {
			timezone = sanitizeTimeZone(timezone, ZoneId.of("UTC").getId());
			dt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of(timezone));
		}
		String iso8601 = dt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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

	private static String detectTimeZone(String iso8601) {
		if (iso8601.contains("Z") || iso8601.contains("z")) {
			return ZoneId.of("UTC").getId();
		} else if (iso8601.contains("+")) {
			return iso8601.substring(iso8601.lastIndexOf("+"));
		} else if (isoContainsMinusOffset(iso8601)) {
			return iso8601.substring(iso8601.lastIndexOf("-"));
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
		DateTimeFormatter printer = computePrinter(iso8601, precision, timezone);
		DateTimeFormatter parser;

		boolean hasTime = containsTime(iso8601);
		boolean hasValidTz = containsTimeZone(timezone);
		ZoneId tz = hasValidTz ? ZoneId.of(timezone) : null;
		boolean isoContainsValidTz = (detectTimeZone(iso8601) != null);

		if (precision == Precision.Date && hasTime && hasValidTz) {
			return ZonedDateTime.parse(iso8601, complexParser).format(printer);
		} else if (precision == Precision.Date) {
			if (hasTime) {
				iso8601 = removeTime(iso8601);
			}
			if (isICalDateFormat(iso8601)) {
				parser = DateTimeFormatter.BASIC_ISO_DATE;
			} else {
				parser = DateTimeFormatter.ISO_LOCAL_DATE;
			}
			return LocalDate.parse(iso8601, parser).format(printer);
		}

		if (!hasTime) {
			iso8601 = isoAddTime(iso8601);
		}
		if (hasValidTz) {
			if (!isoContainsValidTz) {
				return ZonedDateTime.parse(iso8601, complexParser.withZone(tz)).format(printer);
			}
			return ZonedDateTime.parse(iso8601, complexParser).withZoneSameInstant(tz).format(printer);
		}

		if (isoContainsValidTz) {
			iso8601 = removeTimezoneFromIso(iso8601);
		}
		if (!containsTime(iso8601)) {
			iso8601 = isoAddTime(iso8601);
		}
		printer = computePrinter(iso8601, precision, timezone);
		if (isICalDateTimeFormat(iso8601)) {
			parser = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
		} else {
			parser = DateTimeFormatter.ISO_DATE_TIME;
		}
		return LocalDateTime.parse(iso8601, parser).format(printer);
	}

	private static boolean isICalDateFormat(String iso8601) {
		return !containsTime(iso8601) && !iso8601.contains("-");
	}

	private static boolean isICalDateTimeFormat(String iso8601) {
		return containsTime(iso8601) && !iso8601.contains(":");
	}

	private static DateTimeFormatter computePrinter(String iso8601, Precision precision, String timezone) {
		DateTimeFormatter printer;
		if (precision == Precision.Date) {
			if (timezone != null) {
				printer = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.of(timezone));
			} else {
				printer = DateTimeFormatter.ISO_LOCAL_DATE;
			}
		} else {
			DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();

			builder.appendPattern("yyyy-MM-dd'T'HH:mm:ss");
			int nanoSecondsNumber = isoCountNanoSecondsDigits(iso8601);
			if (nanoSecondsNumber == 6 || nanoSecondsNumber == 9) {
				builder.optionalStart()
						.appendFraction(ChronoField.NANO_OF_SECOND, nanoSecondsNumber, nanoSecondsNumber, true)
						.optionalEnd();
			} else {
				builder.optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 3, 3, true).optionalEnd();
			}
			if (timezone != null || detectTimeZone(iso8601) != null) {
				builder.appendPattern("XXX");
			}
			printer = builder.toFormatter();
		}
		return printer;
	}

	private static int isoCountNanoSecondsDigits(String iso8601) {
		if (containsNanoSecond(iso8601)) {
			int plusIndex = iso8601.indexOf("+");
			int zIndex = iso8601.indexOf("Z");
			int dotIndex = iso8601.indexOf(".");

			if (zIndex == -1 && plusIndex == -1) {
				return iso8601.substring(dotIndex).length();
			} else if (zIndex != -1) {
				return iso8601.substring(dotIndex, zIndex).length();
			} else if (plusIndex != -1) {
				return iso8601.substring(dotIndex, plusIndex).length();
			}
		}
		return 0;
	}

	private static String isoAddTime(String iso8601) {
		return iso8601.concat("T00:00:00.000");
	}

	private static String removeTimezoneFromIso(String iso8601) {
		int zIndex = iso8601.indexOf("Z");
		int plusIndex = iso8601.indexOf("+");
		if (zIndex != -1) {
			return iso8601.substring(0, zIndex);
		} else if (plusIndex != -1) {
			return iso8601.substring(0, plusIndex);
		}
		return iso8601;
	}

	private static boolean containsNanoSecond(String iso8601) {
		return iso8601.contains(".");
	}

	private static Precision evaluatePrecision(String iso8601) {
		return containsTime(iso8601) ? Precision.DateTime : Precision.Date;
	}

	private static boolean containsTime(String iso8601) {
		return iso8601.contains("T");
	}

	private static String removeTime(String iso8601) {
		if (containsTime(iso8601)) {
			return iso8601.substring(0, iso8601.indexOf("T"));
		}
		return iso8601;
	}

	/**
	 * Convert {@link BmDateTime} from bmDateTime.timezone to timezone.
	 * 
	 * If bmDateTime.timezone is null timezone will be used without conversion. If
	 * bmDateTime.timezone is not null, bmDateTime will be converted to timezone.
	 * 
	 * @param timezone {@link ZoneId#getId()} for ZonedDateTime, null for Local date
	 *                 or local datetime. If the timezone is not recognized, the
	 *                 default timezone ID will be used (UTC).
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
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

		if (bmDateTime.precision == Precision.Date) {
			LocalDate ld = LocalDate.parse(bmDateTime.iso8601);
			if (format.contains("T") || format.contains(":")) {
				return ld.atStartOfDay().format(formatter);
			}
			return ld.format(formatter);
		}

		if (containsTimeZone() || bmDateTime.timezone != null) {
			DateTimeFormatter dateTimeParser = DateTimeFormatter.ISO_OFFSET_DATE_TIME
					.withZone(ZoneId.of(bmDateTime.timezone));
			return ZonedDateTime.parse(bmDateTime.iso8601, dateTimeParser).format(formatter);
		} else {
			return LocalDateTime.parse(bmDateTime.iso8601, DateTimeFormatter.ISO_LOCAL_DATE_TIME).format(formatter);
		}
	}

	public String toIso8601() {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(toDateTime());
	}

	public static String toIso8601(long timestamp, String timezone) {
		ZonedDateTime ldt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of(timezone));
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ldt);
	}

	public ZonedDateTime toDateTime() {
		String iso8601 = bmDateTime.iso8601;

		ZoneId utcTz = ZoneId.of("UTC");
		boolean isTzParamValid = containsTimeZone(bmDateTime.timezone);
		ZoneId tz = isTzParamValid ? ZoneId.of(bmDateTime.timezone) : null;
		boolean isoContainsValidTz = (detectTimeZone(iso8601) != null);

		if (bmDateTime.precision == Precision.Date) {
			LocalDate ld;
			if (isICalDateFormat(iso8601)) {
				ld = LocalDate.parse(iso8601, DateTimeFormatter.BASIC_ISO_DATE);
			} else {
				ld = LocalDate.parse(iso8601);
			}
			return ZonedDateTime.of(ld.atStartOfDay(), utcTz);
		} else if (isTzParamValid && isoContainsValidTz) {
			return ZonedDateTime.parse(iso8601).withZoneSameInstant(tz);
		} else {
			return ZonedDateTime.of(LocalDateTime.parse(iso8601), utcTz);
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
	 * according to {@link ZoneId#getAvailableZoneIds()}
	 * 
	 * @return True if {@link BmDateTime#timezone} is valid
	 */
	public boolean containsTimeZone() {
		return BmDateTimeWrapper.containsTimeZone(bmDateTime.timezone);
	}

	private static boolean containsTimeZone(String timezone) {
		if (timezone != null && !timezone.isEmpty()) {
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
	 * @param iso8601  Iso8601 representation of a {@link ZonedDateTime},
	 *                 {@link LocalDate}, {@link LocalDateTime}
	 * @param timezone {@link ZoneId#getId()} for ZonedDateTime. If the timezone is
	 *                 not recognized, the UTC timezone will be used.
	 */
	public static long toTimestamp(String iso8601, String timezone) {
		ZonedDateTime result = null;
		Precision precision = evaluatePrecision(iso8601);
		ZoneId tz = ZoneId.of("UTC");
		boolean isoContainsValidTz = (detectTimeZone(iso8601) != null);
		boolean isTzParamValid = containsTimeZone(timezone);
		if (isTzParamValid) {
			tz = ZoneId.of(timezone);
		}

		try {
			if (precision == Precision.Date) {
				result = LocalDate.parse(iso8601).atStartOfDay(tz);
			} else if (isTzParamValid && isoContainsValidTz) {
				result = ZonedDateTime.parse(iso8601);
			} else {
				result = LocalDateTime.parse(iso8601).atZone(tz).withZoneSameInstant(tz);
			}
		} catch (DateTimeException e) {
			result = ZonedDateTime.parse(iso8601).withZoneSameInstant(tz).truncatedTo(ChronoUnit.SECONDS);
		}
		return result.toInstant().toEpochMilli();
	}

	/**
	 * Convert {@link BmDateTime} to a {@link Date}. If {@link BmDateTime} is a
	 * {@link LocalDate} or a {@link LocalDateTime}, UTC timezone will be used.
	 * 
	 * @see BmDateTimeWrapper#toUTCTimestamp()
	 */
	public Date toDate() {
		String tz = ZoneId.systemDefault().getId();
		if (bmDateTime.timezone != null) {
			tz = bmDateTime.timezone;
		}
		return new Date(toTimestamp(tz));
	}

}
