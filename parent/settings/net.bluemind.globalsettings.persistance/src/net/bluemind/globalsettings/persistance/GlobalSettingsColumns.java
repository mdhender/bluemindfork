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
package net.bluemind.globalsettings.persistance;

import java.util.Map;

import net.bluemind.core.jdbc.Columns;

public class GlobalSettingsColumns {

	public static final Columns COLUMNS = Columns.create().col("settings");

	/**
	 * @return
	 */
	public static GlobalSettingsStore.StatementValues<Map<String, String>> statementValues() {
		return (con, statement, index, currentRow, settings) -> {
			statement.setObject(index++, settings);
			return index;
		};
	}

	@SuppressWarnings("unchecked")
	public static GlobalSettingsStore.EntityPopulator<Map<String, String>> populator() {
		return (rs, index, value) -> {
			value.putAll((Map<String, String>) rs.getObject(index++));

			return index;
		};
	}

}
