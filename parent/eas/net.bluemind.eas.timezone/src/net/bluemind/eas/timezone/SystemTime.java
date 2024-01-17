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
package net.bluemind.eas.timezone;

import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.vertx.core.json.JsonObject;

/**
 * 
 * The SYSTEMTIME structure is a date and time, in Coordinated Universal Time
 * (UTC), represented by using individual WORD-sized structure members for the
 * month, day, year, day of week, hour, minute, second, and millisecond.
 * 
 * https://msdn.microsoft.com/en-us/library/windows/desktop/ms724950(v=vs.85).
 * aspx
 * 
 * January: 1, Sunday: 0
 * 
 * <code> 
 * typedef struct _SYSTEMTIME {
 *  WORD wYear;
 *  WORD wMonth;
 *  WORD wDayOfWeek;
 *  WORD wDay;
 *  WORD wHour;
 *  WORD wMinute;
 *  WORD wSecond;
 *  WORD wMilliseconds;
 * } SYSTEMTIME,
 * *PSYSTEMTIME;
 * </code>
 * 
 */
public class SystemTime {

	private static final Logger logger = LoggerFactory.getLogger(SystemTime.class);

	public final int year;
	public final int month;
	public final int dayOfWeek;
	public final int day;
	public final int hour;
	public final int minute;
	public final int second;
	public final int ms;

	public SystemTime(int year, int month, int dayOfWeek, int day, int hour, int minute, int second, int ms) {
		this.year = year;
		this.month = month;
		this.dayOfWeek = dayOfWeek;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
		this.second = second;
		this.ms = ms;
	}

	public static SystemTime of(ByteBuf systime) {
		ByteBuf buf = systime.order(ByteOrder.LITTLE_ENDIAN);
		int year = buf.readShort();
		int month = buf.readShort();
		int dayOfWeek = buf.readShort();
		int day = buf.readShort();
		logger.debug("year: {}, month: {}, dayOfWeek: {}, day: {}", year, month, dayOfWeek, day);
		int hour = buf.readShort();
		int minute = buf.readShort();
		int second = buf.readShort();
		int ms = buf.readShort();
		return new SystemTime(year, month, dayOfWeek, day, hour, minute, second, ms);
	}

	public JsonObject toJson() {
		JsonObject js = new JsonObject();
		boolean recurring = year == 0;
		js.put("kind", recurring ? "RECURRING" : "FIXED");
		js.put("year", year);
		js.put("month", month);
		String dow = dayOfWeek(dayOfWeek);
		js.put("dayOfWeek", dow);
		if (!recurring) {
			js.put("day", day);
		} else {
			if (day == 5) {
				js.put("day", "last_" + dow + "_of_month");
			} else {
				js.put("day", day + "th_" + dow + "_of_month");
			}
		}
		js.put("hour", hour);
		js.put("minute", minute);
		js.put("second", second);
		js.put("ms", ms);
		return js;
	}

	private String dayOfWeek(int dayOfWeek) {
		switch (dayOfWeek) {
		case 1:
			return "MONDAY";
		case 2:
			return "TUESDAY";
		case 3:
			return "WEDNESDAY";
		case 4:
			return "THURSDAY";
		case 5:
			return "FRIDAY";
		case 6:
			return "SATURDAY";
		default:
		case 7:
			return "SUNDAY";
		}
	}

	public void writeTo(ByteBuf f) {
		f.writeShort(year);
		f.writeShort(month);
		f.writeShort(dayOfWeek);
		f.writeShort(day);
		f.writeShort(hour);
		f.writeShort(minute);
		f.writeShort(second);
		f.writeShort(ms);
	}

	@Override
	public String toString() {
		return "SystemTime [year=" + year + ", month=" + month + ", dayOfWeek=" + dayOfWeek + ", day=" + day + ", hour="
				+ hour + ", minute=" + minute + ", second=" + second + ", ms=" + ms + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + day;
		result = prime * result + dayOfWeek;
		result = prime * result + hour;
		result = prime * result + minute;
		result = prime * result + month;
		result = prime * result + ms;
		result = prime * result + second;
		result = prime * result + year;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SystemTime other = (SystemTime) obj;
		if (day != other.day)
			return false;
		if (dayOfWeek != other.dayOfWeek)
			return false;
		if (hour != other.hour)
			return false;
		if (minute != other.minute)
			return false;
		if (month != other.month)
			return false;
		if (ms != other.ms)
			return false;
		if (second != other.second)
			return false;
		if (year != other.year)
			return false;
		return true;
	}

}
