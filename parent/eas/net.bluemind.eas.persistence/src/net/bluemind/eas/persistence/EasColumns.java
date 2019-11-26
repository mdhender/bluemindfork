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
package net.bluemind.eas.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.eas.api.Heartbeat;
import net.bluemind.eas.api.SentItem;

public class EasColumns {

	// Heartbeat stuff
	public static final Columns t_eas_heartbeat = Columns.create() //
			.col("device_uid") //
			.col("heartbeat");

	public static EasStore.EntityPopulator<Heartbeat> heartbeatPopulator() {

		return new EasStore.EntityPopulator<Heartbeat>() {

			@Override
			public int populate(ResultSet rs, int index, Heartbeat heartbeat) throws SQLException {
				heartbeat.deviceUid = rs.getString(index++);
				heartbeat.value = rs.getLong(index++);
				return index;
			}

		};

	}

	public static EasStore.StatementValues<Heartbeat> heartbeatValues() {
		return (con, statement, index, currentRow, heartbeat) -> {
			statement.setString(index++, heartbeat.deviceUid);
			statement.setLong(index++, heartbeat.value);
			return index;
		};
	}

	// Reset
	public static final Columns t_eas_pending_reset = Columns.create() //
			.col("account") //
			.col("device");

	public static EasStore.StatementValues<ResetStatus> resetStatusValue() {
		return (con, statement, index, currentRow, resetStatus) -> {
			statement.setString(index++, resetStatus.account);
			statement.setString(index++, resetStatus.device);
			return index;
		};
	}

	// Sent item
	public static final Columns t_eas_sent_item = Columns.create() //
			.col("device") //
			.col("folder")//
			.col("item");

	public static EasStore.StatementValues<SentItem> sentItemValues() {
		return (con, statement, index, currentRow, sentItem) -> {
			statement.setString(index++, sentItem.device);
			statement.setInt(index++, sentItem.folder);
			statement.setString(index++, sentItem.item);
			return index;
		};
	}

	public static EasStore.EntityPopulator<SentItem> sentItemPopulator() {

		return (rs, index, sentItem) -> {
			sentItem.device = rs.getString(index++);
			sentItem.folder = rs.getInt(index++);
			sentItem.item = rs.getString(index++);
			return index;
		};

	}

	// FolderSync
	public static final Columns t_eas_folder_sync = Columns.create() //
			.col("account") //
			.col("device")//
			.col("versions");

	@SuppressWarnings("unchecked")
	public static EasStore.EntityPopulator<Map<String, String>> folderSyncPopulator() {

		return (rs, index, versions) -> {
			Object properties = rs.getObject(index++);
			if (properties != null) {
				versions.putAll((Map<String, String>) properties);
			}
			return index;
		};

	}

}
