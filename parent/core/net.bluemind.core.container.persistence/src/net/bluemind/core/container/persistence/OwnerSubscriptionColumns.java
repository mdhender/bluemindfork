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
package net.bluemind.core.container.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;

public class OwnerSubscriptionColumns {

	public static final Columns cols = Columns.create()//
			.col("container_type")//
			.col("container_uid")//
			.col("offline_sync")//
			.col("owner")//
			.col("default_container")//
			.col("name")//
	;

	static final EntityPopulator<ContainerSubscriptionModel> POPULATOR = (ResultSet rs, int index,
			ContainerSubscriptionModel value) -> {
		value.containerType = rs.getString(index++);
		value.containerUid = rs.getString(index++);
		value.offlineSync = rs.getBoolean(index++);
		value.owner = rs.getString(index++);
		value.defaultContainer = rs.getBoolean(index++);
		value.name = rs.getString(index++);
		return index;
	};

	public static StatementValues<ContainerSubscriptionModel> values(long id) {
		return new StatementValues<ContainerSubscriptionModel>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					ContainerSubscriptionModel value) throws SQLException {
				statement.setString(index++, value.containerType);
				statement.setString(index++, value.containerUid);
				statement.setBoolean(index++, value.offlineSync);
				statement.setString(index++, value.owner);
				statement.setBoolean(index++, value.defaultContainer);
				statement.setString(index++, value.name);
				statement.setLong(index++, id);
				return index;
			}
		};
	}

}
