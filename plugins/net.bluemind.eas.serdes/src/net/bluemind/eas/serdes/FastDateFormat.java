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
package net.bluemind.eas.serdes;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class FastDateFormat {

	private FastDateFormat() {

	}

	private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
			.withZone(ZoneId.of("UTC"));

	public static final String format(Date d) {
		return format.format(d.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime());
	}

	public static final String format(Date d, String timezone) {
		if (timezone == null) {
			return format(d);
		}
		return format.format(d.toInstant().atZone(ZoneId.of(timezone)).toLocalDateTime());
	}

}
