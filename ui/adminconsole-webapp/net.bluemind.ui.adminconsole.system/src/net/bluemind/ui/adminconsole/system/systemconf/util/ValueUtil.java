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
package net.bluemind.ui.adminconsole.system.systemconf.util;

public class ValueUtil {

	public static String removeNonDigitCharacters(String string, int defaultValue) {
		if (null == string) {
			return String.valueOf(defaultValue);
		}
		string = string.trim();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < string.length(); i++) {
			char charAt = string.charAt(i);
			if (Character.isDigit(charAt)) {
				sb.append(charAt);
			}
		}
		if (sb.length() == 0) {
			return String.valueOf(defaultValue);
		}
		return sb.toString();
	}

	public static String readIntValue(String value, int defaultValue) {
		if (value == null) {
			return "";
		}
		return ValueUtil.removeNonDigitCharacters(value, defaultValue);
	}

	public static Boolean readBooleanValue(String value) {
		if (null == value || value.isEmpty() || value.toLowerCase().equals("false") || value.equals("0")) {
			return false;
		}
		return true;
	}

}
