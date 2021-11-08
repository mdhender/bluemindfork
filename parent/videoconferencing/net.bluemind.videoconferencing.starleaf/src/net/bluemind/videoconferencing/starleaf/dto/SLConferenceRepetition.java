/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.videoconferencing.starleaf.dto;

import io.vertx.core.json.JsonObject;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.fault.ServerFault;

/**
 * https://support.starleaf.com/integrating/cloud-api/request-objects/#conf_set
 */
public class SLConferenceRepetition {

	public enum Frequency {
		daily, weekly, monthly, yearly;
	}

	// The base units used for repetition frequency: permitted values are “daily”,
	// “weekly”,”monthly”,”yearly”.
	public final Frequency frequency;

	// The number of frequency base units between successive repetition occurrences
	// (valid range is 1 to 999).
	public final int interval;

	// The number of repetition occurrences to schedule (valid range is 1 to 999).
	// At least one of “count” and “until” must be null. If “count” and “until” are
	// both null, the conference repeats forever.
	public final Integer count;

	// The date in ISO 8601 format after which the repetition stops. At least one of
	// “count” and “until” must be null. If “count” and “until” are both null, the
	// conference repeats forever.
	public final String until;

	// May be not null only if the frequency is weekly. The presence of this field
	// allows a custom repetition pattern to be defined. Each day of the week on
	// which the conference is to occur is defined by setting the corresponding bit
	// in this integer. The mapping between days and bits is given below.
	public final Integer daysOfWeekMask;

	// May be not null only if the frequency is monthly. If “days_of_month_mask” is
	// not null then both “month_day_what” and “month_day_which” must be null. A not
	// null value for this field allows a custom repetition pattern to be defined.
	// Each day of the month on which the conference is to occur is defined by
	// setting the corresponding bit in this integer. The mapping between days and
	// bits is given below.
	public final Integer daysOfMonthMask;

	// May be not null only if the frequency is yearly. A not null value for this
	// field allows a custom repetition pattern to be defined. Each month of the
	// year on which the conference is to occur is defined by setting the
	// corresponding bit in this integer. The mapping between months and bits is
	// given below.
	public final Integer monthsOfYearMask;

	// May be not null only if the frequency is monthly or yearly. If
	// “month_day_what” is not null then “month_day_which” must also be not null and
	// “days_of_month_mask” must be null. A not null value for this field together
	// with “month_day_which” allows more complex repetition patterns to be defined,
	// of the form ‘repeat every <month_day_which> <month_day_what>’. The permitted
	// values for the “month_day_what” integer, together with their meanings, are
	// given below.
	public final Integer monthDayWhat;

	// May be not null only if the frequency is monthly or yearly. If
	// “month_day_which” is not null then “month_day_what” must also be not null and
	// “days_of_month_mask” must be null. A not null value for this field together
	// with “month_day_what” allows more complex repetition patterns to be defined,
	// of the form ‘repeat every <month_day_which> <month_day_what>’. The permitted
	// values for “month_day_which” are “first”, “second”, “third”, “fourth”,
	// “last”.
	public final Integer monthDayWhich;

	public SLConferenceRepetition(Frequency frequency, int interval, Integer count, String until,
			Integer daysOfWeekMask, Integer daysOfMonthMask, Integer monthsOfYearMask, Integer monthDayWhat,
			Integer monthDayWhich) {
		this.frequency = frequency;
		this.interval = interval;
		this.count = count;
		this.until = until;
		this.daysOfWeekMask = daysOfWeekMask;
		this.daysOfMonthMask = daysOfMonthMask;
		this.monthsOfYearMask = monthsOfYearMask;
		this.monthDayWhat = monthDayWhat;
		this.monthDayWhich = monthDayWhich;
	}

	public JsonObject asJson() {
		JsonObject ret = new JsonObject();

		ret.put("frequency", frequency);
		ret.put("interval", interval);

		if (count != null) {
			ret.put("count", count);
		}

		if (until != null) {
			ret.put("until", until);
		}

		if (daysOfWeekMask != null) {
			ret.put("days_of_week_mask", daysOfWeekMask);
		}

		if (daysOfMonthMask != null) {
			ret.put("days_of_month_mask", daysOfMonthMask);
		}

		if (monthsOfYearMask != null) {
			ret.put("months_of_year_mask", monthsOfYearMask);
		}

		if (monthDayWhat != null) {
			ret.put("month_day_what", monthDayWhat);
		}

		if (monthDayWhich != null) {
			ret.put("month_day_which", monthDayWhich);
		}

		return ret;
	}

	public static final SLConferenceRepetition fromJson(JsonObject json) {
		JsonObject repetition = json.getJsonObject("repetition");
		if (repetition == null) {
			return null;
		}

		return new SLConferenceRepetition(Frequency.valueOf(repetition.getString("frequency")),
				repetition.getInteger("interval"), repetition.getInteger("count"), repetition.getString("until"),
				repetition.getInteger("days_of_week_mask"), repetition.getInteger("days_of_month_mask"),
				repetition.getInteger("months_of_year_mask"), repetition.getInteger("month_day_what"),
				repetition.getInteger("month_day_which"));
	}

	public static final Frequency frequency(VEvent.RRule.Frequency freq) {
		if (freq == VEvent.RRule.Frequency.DAILY) {
			return Frequency.daily;
		}

		if (freq == VEvent.RRule.Frequency.WEEKLY) {
			return Frequency.weekly;
		}
		if (freq == VEvent.RRule.Frequency.MONTHLY) {
			return Frequency.monthly;
		}

		if (freq == VEvent.RRule.Frequency.YEARLY) {
			return Frequency.yearly;
		}

		throw new ServerFault("Unsupported repetition frequency " + freq.name());
	}

	@Override
	public String toString() {
		return "SLConferenceRepetition [frequency=" + frequency + ", interval=" + interval + ", count=" + count
				+ ", until=" + until + ", daysOfWeekMask=" + daysOfWeekMask + ", daysOfMonthMask=" + daysOfMonthMask
				+ ", monthsOfYearMask=" + monthsOfYearMask + ", monthDayWhat=" + monthDayWhat + ", monthDayWhich="
				+ monthDayWhich + "]";
	}

}
