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
package net.bluemind.device.persistance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.device.api.Device;

public class DeviceColumns {
	public static final Columns cols = Columns.create() //
			.col("identifier") //
			.col("owner") //
			.col("type") //
			.col("wipe_date") //
			.col("wipe_user") //
			.col("unwipe_date") //
			.col("unwipe_user") //
			.col("wipe")//
			.col("partnership")//
			.col("policy")//
			.col("last_sync");

	public static DeviceStore.StatementValues<Device> values(Item it) {
		return (final Connection con, final PreparedStatement statement, int index, final int currentRow,
				final Device value) -> {

			statement.setString(index++, value.identifier);
			statement.setString(index++, value.owner);
			statement.setString(index++, value.type);

			statement.setTimestamp(index++, getTimeStamp(value.wipeDate));
			statement.setString(index++, value.wipeBy);

			statement.setTimestamp(index++, getTimeStamp(value.unwipeDate));
			statement.setString(index++, value.unwipeBy);

			statement.setBoolean(index++, value.isWipe);

			statement.setBoolean(index++, value.hasPartnership);
			statement.setInt(index++, value.policy);

			statement.setTimestamp(index++, getTimeStamp(value.lastSync));
			statement.setLong(index++, it.id);

			return index;
		};
	}

	private static Timestamp getTimeStamp(Date dt) {
		if (null == dt) {
			return null;
		}
		return new Timestamp(dt.getTime());
	}

	public static DeviceStore.EntityPopulator<Device> populator() {
		return (ResultSet rs, int index, Device value) -> {

			value.identifier = rs.getString(index++);
			value.owner = rs.getString(index++);
			value.type = rs.getString(index++);

			value.wipeDate = fromTimeStamp(rs.getTimestamp(index++));
			value.wipeBy = rs.getString(index++);

			value.unwipeDate = fromTimeStamp(rs.getTimestamp(index++));
			value.unwipeBy = rs.getString(index++);

			value.isWipe = rs.getBoolean(index++);

			value.hasPartnership = rs.getBoolean(index++);
			value.policy = rs.getInt(index++);

			value.lastSync = fromTimeStamp(rs.getTimestamp(index++));

			return index;
		};
	}

	private static Date fromTimeStamp(Timestamp ts) {
		if (null == ts) {
			return null;
		}
		return new Date(ts.getTime());
	}

}
