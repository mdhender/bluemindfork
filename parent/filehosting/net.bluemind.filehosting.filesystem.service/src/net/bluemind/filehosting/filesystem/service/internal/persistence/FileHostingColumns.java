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
package net.bluemind.filehosting.filesystem.service.internal.persistence;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import net.bluemind.core.jdbc.Columns;

public class FileHostingColumns {

	public static final Columns cols = Columns.create() //
			.col("uid") //
			.col("owner") //
			.col("path") //
			.col("metadata") //
			.col("download_limit") //
			.col("expiration_date") //
			.col("access_count") //
			.col("last_access");

	/**
	 * @return
	 */
	public static FileHostingStore.StatementValues<FileHostingEntity> statementValues() {
		return (con, statement, index, currentRow, u) -> {
			statement.setString(index++, u.uid);
			statement.setString(index++, u.owner);
			statement.setString(index++, u.path);
			statement.setObject(index++, u.metadata);
			statement.setInt(index++, u.downloadLimit);
			if (null == u.expirationDate) {
				statement.setTimestamp(index++, null);
			} else {
				statement.setTimestamp(index++, Timestamp.from(u.expirationDate.toInstant()));
			}
			statement.setInt(index++, u.accessCount);
			if (null == u.lastAccess) {
				u.lastAccess = new Date();
			}
			statement.setTimestamp(index++, Timestamp.from(u.lastAccess.toInstant()));
			return index;
		};
	}

	@SuppressWarnings("unchecked")
	public static FileHostingStore.EntityPopulator<FileHostingEntity> populator() {
		return (rs, index, value) -> {
			value.uid = rs.getString(index++);
			value.owner = rs.getString(index++);
			value.path = rs.getString(index++);
			value.metadata.putAll((Map<String, String>) rs.getObject(index++));
			value.downloadLimit = rs.getInt(index++);
			Timestamp timestamp = rs.getTimestamp(index++);
			if (null != timestamp) {
				value.expirationDate = Date.from(timestamp.toInstant());
			}
			value.accessCount = rs.getInt(index++);
			value.lastAccess = Date.from(rs.getTimestamp(index++).toInstant());
			return index;
		};
	}
}
