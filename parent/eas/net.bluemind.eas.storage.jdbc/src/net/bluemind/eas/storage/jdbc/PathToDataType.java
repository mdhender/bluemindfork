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
package net.bluemind.eas.storage.jdbc;

import net.bluemind.eas.dto.type.ItemDataType;

public class PathToDataType {

	public static ItemDataType fromCollectionPath(String path) {
		ItemDataType dataType = null;
		if (path.contains("\\calendar\\")) {
			dataType = ItemDataType.CALENDAR;
		} else if (path.endsWith("\\contacts")) {
			dataType = ItemDataType.CONTACTS;
		} else if (path.contains("\\tasks")) {
			dataType = ItemDataType.TASKS;
		} else if (path.contains("\\email")) {
			dataType = ItemDataType.EMAIL;
		} else if (path.contains("\\notes")) {
			dataType = ItemDataType.NOTES;
		}
		return dataType;
	}

}
