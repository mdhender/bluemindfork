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
package net.bluemind.eas.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateHelper {

	private static final Logger logger = LoggerFactory.getLogger(DateHelper.class);

	public static Date parseDate(String str) {
		SimpleDateFormat date;
		// Doc : [MS-ASDTYPE] 2.6 Date/Time
		try {
			if (str.matches("^....-..-..T..:..:..\\....Z$")) {
				date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				date.setTimeZone(TimeZone.getTimeZone("GMT"));
				return date.parse(str);
			} else if (str.matches("^........T......Z$")) {
				date = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
				date.setTimeZone(TimeZone.getTimeZone("GMT"));
				return date.parse(str);
			}
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
		}

		return null;
	}
}
