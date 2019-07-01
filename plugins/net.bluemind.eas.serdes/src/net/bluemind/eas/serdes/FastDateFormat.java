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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class FastDateFormat {

	private static final ThreadLocal<SimpleDateFormat> local = new ThreadLocal<>();

	private static final SimpleDateFormat newFormatter() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf;
	}

	public static final String format(Date d) {
		SimpleDateFormat sdf = local.get();
		if (sdf == null) {
			sdf = newFormatter();
			local.set(sdf);
		}
		synchronized (sdf) {
			return sdf.format(d);
		}
	}

}
