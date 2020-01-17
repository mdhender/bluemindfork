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
		SystemTime st = new SystemTime(year, month, dayOfWeek, day, hour, minute, second, ms);
		return st;
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
		case 0:
			return "SUNDAY";
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
		default:
		case 6:
			return "SATURDAY";
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

}
