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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.i18n.client.DateTimeFormat;

public class DateFormatTranslation {

	public static Map<String, String> formats = new HashMap<>();
	public static final String DEFAULT_DATE_FORMAT = "dd/MM/yyyy";

	static {
		formats.put(DEFAULT_DATE_FORMAT, "31/12/2012");
		formats.put("yyyy-MM-dd", "2012-12-31");
		formats.put("MM/dd/yyyy", "12/31/2012");
		formats.put("dd.MM.yyyy", "31.12.2012");
	}

	public static String getKeyByFormat(String dateFormatStr) {
		return formats.entrySet().stream().filter(e -> dateFormatStr.equals(e.getValue())).map(e -> e.getKey())
				.findFirst().orElse(null);
	}

	public static String prettyDateFormatToDisplay(String domainDateFormat) {
		String dateStr = formats.get(DEFAULT_DATE_FORMAT);
		try {
			// 31/12/2012
			Date date = new Date(1356912001000L);
			dateStr = DateTimeFormat.getFormat(domainDateFormat).format(date);
		} catch (IllegalArgumentException e) {
			// use DEFAULT_DATE_FORMAT
		}
		return dateStr;
	}
}
