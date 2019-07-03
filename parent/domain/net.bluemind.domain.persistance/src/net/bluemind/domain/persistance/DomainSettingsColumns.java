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
package net.bluemind.domain.persistance;

import java.util.Map;

public enum DomainSettingsColumns {

	settings(null, false); //

	private final String enumType;
	private final boolean fetchOnly;

	private DomainSettingsColumns(String enumType, boolean fetchOnly) {
		this.enumType = enumType;
		this.fetchOnly = fetchOnly;
	}

	public static void appendNames(String prefix, StringBuilder query) {
		boolean first = true;
		DomainSettingsColumns[] vals = DomainSettingsColumns.values();
		for (int i = 0; i < vals.length; i++) {
			if (vals[i].fetchOnly) {
				continue;
			}
			if (!first) {
				query.append(", ");
			}

			if (prefix != null) {
				query.append(prefix).append(".");
			}
			query.append(vals[i].name());
			first = false;
		}

	}

	public static void appendValues(StringBuilder query) {
		boolean first = true;
		DomainSettingsColumns[] vals = DomainSettingsColumns.values();
		for (int i = 0; i < vals.length; i++) {
			if (vals[i].fetchOnly) {
				continue;
			}
			if (!first) {
				query.append(',');
			}

			query.append("?");
			DomainSettingsColumns uc = vals[i];
			if (uc.enumType != null) {
				query.append("::").append(uc.enumType);
			}
			first = false;
		}
	}

	/**
	 * @return
	 */
	public static DomainSettingsStore.StatementValues<Map<String, String>> statementValues() {
		return (con, statement, index, currentRow, settings) -> {
			statement.setObject(index++, settings);
			return index;
		};
	}

	@SuppressWarnings("unchecked")
	public static DomainSettingsStore.EntityPopulator<Map<String, String>> populator() {
		return (rs, index, value) -> {
			value.putAll((Map<String, String>) rs.getObject(index++));

			return index;
		};
	}
}
