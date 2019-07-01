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
package net.bluemind.gwtconsoleapp.base.i18n;

import com.google.gwt.i18n.client.ConstantsWithLookup;

public class I18n {

	public static String translateText(String text, ConstantsWithLookup constants) {
		StringBuilder sb = new StringBuilder();
		int fromIndex = 0;
		int indexOf = 0;
		while ((indexOf = text.indexOf("$$", fromIndex)) != -1) {
			sb.append(text.substring(fromIndex, indexOf));
			int endIndex = text.indexOf("$$", indexOf + 2);
			String key = text.substring(indexOf + 2, endIndex);
			sb.append(constants.getString(key));
			fromIndex = endIndex + 2;
		}

		sb.append(text.substring(fromIndex));

		return sb.toString();
	}

}
