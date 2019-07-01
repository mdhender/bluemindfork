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
package net.bluemind.ui.im.client;

public class MessageFormatter {

	public static String convert(String plain) {

		String urlRegexp = "\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:\'\".,<>???“”‘’]))";
		StringBuilder sb = new StringBuilder();
		String escaped = plain;
		escaped = escaped.replace("\r\n", "\n");
		escaped = escaped.replace("\n", "<br/>");
		escaped = escaped.replaceAll(urlRegexp, "<a target=\"_blank\" href=\"$1\">$1</a>");
		escaped = escaped.replaceAll("<a target=\"_blank\" href=\"www", "<a target=\"_blank\" href=\"http://www");
		sb.append(escaped);
		String ret = sb.toString();
		return ret;
	}
}
