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
package net.bluemind.core.utils;

import java.util.Comparator;

import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;

public class DateTimeComparator implements Comparator<BmDateTime> {

	private final String timezone;

	public DateTimeComparator(String timezone) {
		this.timezone = timezone;
	}

	@Override
	public int compare(BmDateTime o1, BmDateTime o2) {
		return Long.compare(new BmDateTimeWrapper(o1).toTimestamp(timezone),
				new BmDateTimeWrapper(o2).toTimestamp(timezone));
	}

}
