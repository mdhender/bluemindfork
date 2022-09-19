/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.ui.adminconsole.system.domains.edit.general.l10n;

import java.util.HashMap;
import java.util.Map;

public class TimeFormatTranslation {

	public static Map<String, String> formats = new HashMap<>();
	public static final String DEFAULT_TIME_FORMAT = "HH:mm";

	static {
		formats.put("h:mma", "1:00pm");
		formats.put(DEFAULT_TIME_FORMAT, "13:00");
	}

	public static String getKeyByFormat(String timeFormatStr) {
		return formats.entrySet().stream().filter(e -> timeFormatStr.equals(e.getValue())).map(e -> e.getKey())
				.findFirst().orElse(DEFAULT_TIME_FORMAT);
	}
}
