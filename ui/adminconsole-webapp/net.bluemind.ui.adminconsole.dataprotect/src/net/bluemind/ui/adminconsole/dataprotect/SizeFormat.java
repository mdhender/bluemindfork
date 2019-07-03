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
package net.bluemind.ui.adminconsole.dataprotect;

import com.google.gwt.i18n.client.NumberFormat;

public class SizeFormat {

	public static String readableFileSize(long size) {
		if (size <= 0)
			return "0";
		final String[] units = new String[] { "MB", "GB", "TB" };
		int digitGroups = Math.min(units.length - 1, (int) ((Math.log10(size) / Math.log10(1024))));
		return NumberFormat.getFormat("####.##").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
}
