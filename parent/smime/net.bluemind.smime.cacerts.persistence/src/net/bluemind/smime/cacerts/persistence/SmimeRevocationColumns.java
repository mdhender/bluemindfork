/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.smime.cacerts.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.smime.cacerts.api.SmimeRevocation;

public class SmimeRevocationColumns {
	public static final Columns cols = Columns.create() //
			.col("serial_number") //
			.col("revocation_date") //
			.col("revocation_reason") //
			.col("url") //
			.col("last_update") //
			.col("next_update") //
			.col("issuer") //
			.col("ca_item_id");

	public static StatementValues<SmimeRevocation> values(ItemValue<SmimeCacert> cacert) {
		return new StatementValues<SmimeRevocation>() {

			@Override
			public int setValues(Connection conn, PreparedStatement statement, int index, int currentRow,
					SmimeRevocation value) throws SQLException {

				statement.setString(index++, value.serialNumber);
				statement.setTimestamp(index++, Timestamp.from(value.revocationDate.toInstant()));
				statement.setString(index++, value.revocationReason);

				statement.setString(index++, value.url);
				statement.setTimestamp(index++,
						value.lastUpdate != null ? Timestamp.from(value.lastUpdate.toInstant()) : null);
				statement.setTimestamp(index++,
						value.nextUpdate != null ? Timestamp.from(value.nextUpdate.toInstant()) : null);
				statement.setString(index++, value.issuer);
				statement.setLong(index++, cacert.item().id);

				return index;
			}
		};
	}

	public static EntityPopulator<SmimeRevocation> populator(ItemValue<SmimeCacert> cacert) {
		return new EntityPopulator<SmimeRevocation>() {

			@Override
			public int populate(ResultSet rs, int index, SmimeRevocation value) throws SQLException {
				value.serialNumber = rs.getString(index++);
				value.revocationDate = Date.from(rs.getTimestamp(index++).toInstant());
				value.revocationReason = rs.getString(index++);

				value.url = rs.getString(index++);
				Timestamp lastUpdate = rs.getTimestamp(index++);
				if (lastUpdate != null) {
					value.lastUpdate = Date.from(lastUpdate.toInstant());
				}
				Timestamp nextUpdate = rs.getTimestamp(index++);
				if (lastUpdate != null) {
					value.nextUpdate = Date.from(nextUpdate.toInstant());
				}
				value.issuer = rs.getString(index++);
				value.cacertItemUid = cacert.uid;

				return index;
			}
		};
	}

	public static EntityPopulator<SmimeRevocation> populator() {
		return new EntityPopulator<SmimeRevocation>() {

			@Override
			public int populate(ResultSet rs, int index, SmimeRevocation value) throws SQLException {
				value.serialNumber = rs.getString(index++);
				value.revocationDate = Date.from(rs.getTimestamp(index++).toInstant());
				value.revocationReason = rs.getString(index++);

				value.url = rs.getString(index++);
				Timestamp lastUpdate = rs.getTimestamp(index++);
				if (lastUpdate != null) {
					value.lastUpdate = Date.from(lastUpdate.toInstant());
				}
				Timestamp nextUpdate = rs.getTimestamp(index++);
				if (lastUpdate != null) {
					value.nextUpdate = Date.from(nextUpdate.toInstant());
				}
				value.issuer = rs.getString(index++);
				index++; // ca_item_id
				value.cacertItemUid = rs.getString(index++);

				return index;
			}
		};
	}
}
