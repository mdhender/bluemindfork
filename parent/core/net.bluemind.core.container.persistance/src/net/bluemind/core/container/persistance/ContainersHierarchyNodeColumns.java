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
package net.bluemind.core.container.persistance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;

public class ContainersHierarchyNodeColumns {

	public static final Columns cols = Columns.create()//
			.col("name")//
			.col("container_type")//
			.col("container_uid")//
	;

	static final EntityPopulator<ContainerHierarchyNode> POPULATOR = (ResultSet rs, int index,
			ContainerHierarchyNode value) -> {
		value.name = rs.getString(index++);
		value.containerType = rs.getString(index++);
		value.containerUid = rs.getString(index++);
		return index;
	};

	public static StatementValues<ContainerHierarchyNode> values(long id) {
		return new StatementValues<ContainerHierarchyNode>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					ContainerHierarchyNode value) throws SQLException {
				statement.setString(index++, value.name);
				statement.setString(index++, value.containerType);
				statement.setString(index++, value.containerUid);
				statement.setLong(index++, id);
				return index;
			}
		};
	}

}
