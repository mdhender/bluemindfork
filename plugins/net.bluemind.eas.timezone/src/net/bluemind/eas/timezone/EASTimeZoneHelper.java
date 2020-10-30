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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneOffsetTransitionRule.TimeDefinition;
import java.time.zone.ZoneRules;
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EASTimeZoneHelper {

	private static final Logger logger = LoggerFactory.getLogger(EASTimeZoneHelper.class);
	public static final TimeZone EUROPE_PARIS = TimeZone.getTimeZone("Europe/Paris");

	public static TimeZone from(EASTimeZone easZone) {
		if (easZone.bias == -60 && easZone.standardBias == 0 && easZone.daylightBias == -60
				&& easZone.standardDate.month != 0) {
			return EUROPE_PARIS;
		} else if (easZone.standardName == null || easZone.standardName.isEmpty()) {
			String[] maybe = TimeZone.getAvailableIDs(0 - (easZone.bias * 60 * 1000));
			if (maybe == null || maybe.length == 0) {
				logger.warn("Null standard name, failed bias method returning Europe/Paris");
				return EUROPE_PARIS;
			} else {
				TimeZone best = TimeZone.getTimeZone(maybe[0]);
				boolean fixedOffset = false;
				if (easZone.standardDate.month == 0 && easZone.daylightDate.month == 0) {
					fixedOffset = true;
					logger.debug("Fixed offset zone lookup");
				}
				for (String tzId : maybe) {
					TimeZone javaTz = TimeZone.getTimeZone(tzId);
					try {
						ZoneId zid = ZoneId.of(javaTz.getID());
						ZoneOffsetTransitionRule dayRule = null;
						List<ZoneOffsetTransitionRule> rules = zid.getRules().getTransitionRules();
						if (fixedOffset) {
							if (rules.isEmpty()) {
								best = javaTz;
								break;
							}
						} else {
							for (ZoneOffsetTransitionRule rule : rules) {
								boolean isStandardTime = rule.getStandardOffset().getTotalSeconds() == rule
										.getOffsetAfter().getTotalSeconds();
								if (!isStandardTime) {
									dayRule = rule;
									break;
								}
							}
							if (dayRule != null) {
								printRule(dayRule);
								int dayBias = 0 - ((dayRule.getOffsetAfter().getTotalSeconds()
										- dayRule.getOffsetBefore().getTotalSeconds()) / 60);
								logger.debug("dayBias {} for {}, looking for {}", dayBias, javaTz.getID(),
										easZone.daylightBias);
								if (dayBias == easZone.daylightBias) {
									logger.debug("Got match");
									best = javaTz;
									break;
								}
							}
						}
					} catch (ZoneRulesException zre) {
						logger.warn(zre.getMessage());
					}
				}
				logger.warn("Bias method found {}", best.getID());
				return best;
			}
		} else {
			return TimeZone.getTimeZone(easZone.standardName);
		}
	}

	private static void printRule(ZoneOffsetTransitionRule dayRule) {
		if (logger.isDebugEnabled()) {
			logger.debug("aft: {}, bef: {}, std: {}, ", dayRule.getOffsetAfter().getTotalSeconds(),
					dayRule.getOffsetBefore().getTotalSeconds(), dayRule.getStandardOffset().getTotalSeconds());
		}
	}

	public static EASTimeZone from(TimeZone javaTz) {
		ZoneId id = null;
		try {
			id = ZoneId.of(javaTz.getID());
		} catch (ZoneRulesException e) {
			if (ZoneId.SHORT_IDS.containsKey(javaTz.getID())) {
				id = ZoneId.of(ZoneId.SHORT_IDS.get(javaTz.getID()));
			} else {
				throw e;
			}
		}

		ZoneRules rules = id.getRules();
		List<ZoneOffsetTransitionRule> transRules = rules.getTransitionRules();
		ZoneOffsetTransitionRule stdRule = null;
		ZoneOffsetTransitionRule daylightRule = null;
		int bias = 0 - ((int) TimeUnit.MILLISECONDS.toMinutes(javaTz.getRawOffset()));
		String standardName = javaTz.getDisplayName(false, TimeZone.SHORT);
		String daylightName = javaTz.getDisplayName(true, TimeZone.SHORT);
		if (logger.isDebugEnabled()) {
			logger.debug("{} has {} transition rules", id, transRules.size());
		}
		if (rules.isFixedOffset() || transRules.isEmpty()) {
			ZoneOffset offset = rules.getOffset(Instant.now());
			logger.debug("fixed offset: {} {}", offset, offset.getTotalSeconds());
			// WAT
			SystemTime std = new SystemTime(0, 0, 0, 0, 0, 0, 0, 0);
			int daylightBias = 0;
			return new EASTimeZone(bias, standardName, std, 0, daylightName, std, daylightBias);
		} else {
			logger.debug("not fixed offset");
			for (ZoneOffsetTransitionRule zotr : transRules) {
				boolean isStandardTime = zotr.getStandardOffset().getTotalSeconds() == zotr.getOffsetAfter()
						.getTotalSeconds();
				printRule(zotr);
				if (isStandardTime) {
					stdRule = zotr;
				} else {
					daylightRule = zotr;
				}
			}
			if (stdRule == null || daylightRule == null) {
				throw new NullPointerException("rules not found std: " + stdRule + ", day: " + daylightRule);
			}
			SystemTime std = asSystemTime(stdRule);
			SystemTime daylight = asSystemTime(daylightRule);

			int daylightBiasSeconds = 0 - (daylightRule.getOffsetAfter().getTotalSeconds()
					- daylightRule.getOffsetBefore().getTotalSeconds());
			int daylightBias = daylightBiasSeconds / 60;

			return new EASTimeZone(bias, standardName, std, 0, daylightName, daylight, daylightBias);
		}
	}

	private static SystemTime asSystemTime(ZoneOffsetTransitionRule rule) {
		int month = rule.getMonth().getValue();
		YearMonth yearMonth = YearMonth.of(Year.now().getValue(), month);
		int daysInMonth = yearMonth.lengthOfMonth();
		int dayOfMonthIndicator = rule.getDayOfMonthIndicator();
		int dayOfMonth = dayOfMonthIndicator;
		if (dayOfMonthIndicator == -1) {
			dayOfMonth = daysInMonth;
		} else {
			if (dayOfMonthIndicator < 0) {
				dayOfMonth = daysInMonth - dayOfMonthIndicator;
			}
		}

		int dayPositionInMonth = 0;
		if (dayOfMonth + 7 > daysInMonth) {
			// fix day position in month
			// day position in month is not week number.
			// ex: Sunday, 25th October 2020 is the last Sunday but on week number 4
			// add 7 to dayOfMonth. if > daysInMonth we assume it is the last of month
			dayPositionInMonth = 5; // last
		} else {
			WeekFields weekFields = WeekFields.of(Locale.getDefault());
			LocalDate date = LocalDate.of(Year.now().getValue(), month, 1);
			int addToWeek = date.get(weekFields.weekOfMonth()) == 0 ? 1 : 0;
			date = LocalDate.of(Year.now().getValue(), month, dayOfMonth);
			dayPositionInMonth = date.get(WeekFields.of(Locale.getDefault()).weekOfMonth()) + addToWeek;
		}

		LocalTime localTime = rule.getLocalTime();
		int hour = localTime.getHour();

		if (rule.getTimeDefinition() != TimeDefinition.WALL) {
			// "WALL" hour
			hour = hour + rule.getOffsetBefore().getTotalSeconds() / 3600;
		}

		return new SystemTime(0, month, rule.getDayOfWeek().getValue() % 7, dayPositionInMonth, hour,
				localTime.getMinute(), localTime.getSecond(), 0);

	}

}
