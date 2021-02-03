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
package net.bluemind.lib.ical4j.util;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.date.TimezoneExtensions;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.property.DateProperty;

public class IcalConverter {

	private static final Logger logger = LoggerFactory.getLogger(IcalConverter.class);

	public static BmDateTime convertToDateTime(Date date, String timezone) {
		if (null == date) {
			return null;
		}
		Precision precision = (date instanceof DateTime) ? Precision.DateTime : Precision.Date;
		return BmDateTimeWrapper.fromTimestamp(date.getTime(), timezone, precision);
	}

	public static BmDateTime convertToDateTime(DateProperty property, Optional<String> globalTZ,
			Map<String, String> tzMapping) {
		if (property == null) {
			return null;
		}

		Date date = property.getDate();

		Precision precision = (date instanceof DateTime) ? Precision.DateTime : Precision.Date;

		String timezone = null;
		if (property.getTimeZone() == null && precision == Precision.DateTime) {
			timezone = detectTimeZone(property, globalTZ, tzMapping);
		} else if (property.getTimeZone() != null) {
			timezone = sanitizeTimeZone(property, tzMapping);
		}

		BmDateTime bmDate = BmDateTimeWrapper.create(date.toString(), timezone, precision);
		// FIXME : If property.getTimezone is not null, and date.getTime !=
		// bmDate.getTime we have failed to interpret timezone.
		// Maybe if property.getTilezone is not null we should use
		// fromTimestamp...
		return bmDate;

	}

	/**
	 * @param property
	 * @param tzMapping
	 * @return
	 */
	private static String sanitizeTimeZone(DateProperty property, Map<String, String> tzMapping) {
		TimeZone timeZone = property.getTimeZone();
		Date date = property.getDate();
		String id = TimezoneExtensions.translate(timeZone.getID());
		if (id == null) {
			id = timeZone.getID();
		}

		try {
			ZoneId.of(id);
			return id;
		} catch (DateTimeException e) {
			if (tzMapping.containsKey(timeZone.getID())) {
				return tzMapping.get(timeZone.getID());
			}
			int offset = timeZone.getOffset(date.getTime()) / 1000;
			logger.error("unknow timezone {} with offset {}", property, offset);
			ZoneOffset zo = ZoneOffset.ofTotalSeconds(offset);
			return ZoneId.of(zo.getId()).getId();
		}
	}

	private static String detectTimeZone(DateProperty property, Optional<String> globalTZ,
			Map<String, String> tzMapping) {
		String timezone = null;
		Parameter p = property.getParameter("TZID");
		if (p != null) {
			timezone = p.getValue();
		}

		if (timezone == null && property.getValue().endsWith("Z")) {
			timezone = "UTC";
		}

		if (timezone == null && globalTZ.isPresent()) {
			timezone = globalTZ.get();
		}

		if (timezone != null) {

			String rz = TimezoneExtensions.translate(timezone);
			if (rz != null) {
				timezone = rz;
			} else {
				if (tzMapping.containsKey(timezone)) {
					timezone = tzMapping.get(timezone);
				}
			}

			try {
				ZoneId.of(timezone);
			} catch (DateTimeException e) {
				logger.error("unknow timezone {}", timezone, e);
			}
		}

		return timezone;
	}

}
