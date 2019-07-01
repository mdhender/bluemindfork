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
package net.bluemind.directory.persistance.internal;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.directory.api.OrgUnit;

public class OrgUnitColumns {

	public static final Columns COLUMNS = Columns.create() //
			.col("name")//
			.col("parent_item_id");

	private OrgUnitColumns() {
	}

	public static EntityPopulator<OrgUnit> populator() {
		return (rs, index, value) -> {
			value.name = rs.getString(index++);
			value.parentUid = rs.getString(index++);
			return index;
		};
	}
}
