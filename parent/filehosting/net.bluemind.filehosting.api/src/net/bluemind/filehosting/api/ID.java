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
package net.bluemind.filehosting.api;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ID {

	private static final String regex = "bm-[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

	public static String generate() {
		return "bm-".concat(UUID.randomUUID().toString());
	}

	public static boolean isUUID(String string) {
		return string.matches(regex);
	}

	public static String extract(String absoluteURI) {
		Pattern pattern = Pattern.compile("(" + regex + ")");
		Matcher matcher = pattern.matcher(absoluteURI);
		matcher.find();
		return matcher.group(0);
	}

}
