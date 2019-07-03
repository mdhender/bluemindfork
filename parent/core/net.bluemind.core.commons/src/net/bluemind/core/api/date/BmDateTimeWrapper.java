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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.ISODateTimeFormat;

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
	 * Timezone will be set after {@link DateTime#getZone()}.
	 * 
	 * @see #create(String, String, Precision)
	 * 
	 * @param dateTime
	 * @param timezone {@link DateTimeZone#getID()} for DateTime, null for Local
	 *                 date or local datetime. If the timezone is not recognized,
	 *                 the default timezone ID will be used (UTC).
	 * @return
	 */
	public static BmDateTime create(org.joda.time.DateTime dateTime, Precision precision) {
		return BmDateTimeWrapper.create(dateTime.toString(), dateTime.getZone().getID(), precision);
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
		return new BmDateTime(new Date(timestamp).toInstant().toString(), DateTimeZone.UTC.getID(), Precision.DateTime);
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
		DateTime dt;
		if (timezone == null) {
			dt = new DateTime(timestamp, DateTimeZone.UTC);
		} else {
			timezone = sanitizeTimeZone(timezone, DateTimeZone.UTC.getID());
			dt = new DateTime(timestamp, DateTimeZone.forID(timezone));
		}
		String iso8601 = dt.toString(ISODateTimeFormat.dateTime());
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
				return DateTimeZone.UTC.getID();
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
			printer = ISODateTimeFormat.date().withZone(DateTimeZone.forID(timezone));
		} else if (containsTimeZone(timezone)) {
			printer = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forID(timezone));
		} else {
			printer = ISODateTimeFormat.dateHourMinuteSecondMillis();
		}
		// If iso8601 or return value is a LocalDate(Time), iso8601 must not be
		// converted to default timezone.
		if (timezone != null && detectTimeZone(iso8601) != null) {
			return printer.print(iso8601Parser.parseDateTime(iso8601));
		} else {
			return printer.print(iso8601Parser.parseLocalDateTime(iso8601).toDateTime(DateTimeZone.forID(timezone)));
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
	 * @see org.joda.time.format.DateTimeFormat
	 * @see DateTime#toString
	 *
	 * @param pattern the pattern specification, null means use
	 *                <code>toString</code>
	 * @return the formatted string, not null
	 * 
	 */
	public String format(String format) {
		DateTimeFormatter dateTimeParser = ISODateTimeFormat.dateTimeParser();
		if (containsTimeZone()) {
			dateTimeParser = dateTimeParser.withZone(DateTimeZone.forID(bmDateTime.timezone));
		}

		return dateTimeParser.parseDateTime(bmDateTime.iso8601).toString(format);
	}

	public String toIso8601() {
		return ISODateTimeFormat.dateTime().print(toJodaTime());
	}

	public static String toIso8601(long timestamp, String timezone) {
		DateTime ldt = new DateTime(timestamp, DateTimeZone.forID(timezone));
		return ISODateTimeFormat.dateTime().print(ldt);
	}

	@Deprecated
	public DateTime toJodaTime() {
		if (containsTimeZone(bmDateTime.timezone)) {
			return new DateTime(bmDateTime.iso8601, DateTimeZone.forID(bmDateTime.timezone));
		} else {
			return new DateTime(bmDateTime.iso8601, DateTimeZone.UTC);
		}
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
		return toJodaTime().isBefore(new BmDateTimeWrapper(date).toJodaTime());
	}

	public boolean isAfter(BmDateTime date) {
		return toJodaTime().isAfter(new BmDateTimeWrapper(date).toJodaTime());
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
				DateTimeZone.forID(timezone).getID();
				return true;
			} catch (Exception e) {

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
		return BmDateTimeWrapper.toTimestamp(bmDateTime.iso8601, DateTimeZone.UTC.getID());
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
				return new DateTime(iso8601, DateTimeZone.forID(timezone)).getMillis();
			} else {
				return ISODateTimeFormat.dateOptionalTimeParser().withZoneUTC().parseMillis(iso8601);
			}
		} catch (IllegalArgumentException e) {
			return ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().parseMillis(iso8601);
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
		return new Date(toTimestamp(DateTimeZone.getDefault().getID()));
	}

	/**
	 * @return
	 */
	private static DateTimeFormatter buildIso8601Parser() {
		DateTimeFormatter basicDateHourMinuteSecond = new DateTimeFormatterBuilder()
				.append(ISODateTimeFormat.basicDate().getParser()).appendLiteral("T").appendHourOfDay(2)
				.appendMinuteOfHour(2).appendSecondOfMinute(2).toFormatter();

		DateTimeFormatter basicDateHourMinuteSecondMillis = new DateTimeFormatterBuilder()
				.append(basicDateHourMinuteSecond).appendMillisOfSecond(3).toFormatter();

		DateTimeParser[] parsers = new DateTimeParser[] { ISODateTimeFormat.basicDateTime().getParser(),
				ISODateTimeFormat.basicDateTimeNoMillis().getParser(), basicDateHourMinuteSecond.getParser(),
				basicDateHourMinuteSecondMillis.getParser(), ISODateTimeFormat.basicDate().getParser(),
				ISODateTimeFormat.dateOptionalTimeParser().getParser() };

		return new DateTimeFormatterBuilder().append(null, parsers).toFormatter();
	}
}
