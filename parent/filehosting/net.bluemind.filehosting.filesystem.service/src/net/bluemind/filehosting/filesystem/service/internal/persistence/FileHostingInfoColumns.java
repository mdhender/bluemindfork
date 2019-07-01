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

import net.bluemind.core.jdbc.Columns;

public class FileHostingInfoColumns {

	public static final Columns cols = Columns.create() //
			.col("path") //
			.col("created") //
			.col("owner"); //

	/**
	 * @return
	 */
	public static FileHostingStore.StatementValues<FileHostingEntityInfo> statementValues() {
		return (con, statement, index, currentRow, u) -> {
			statement.setString(index++, u.path);
			statement.setTimestamp(index++, new Timestamp(u.created.getTime()));
			statement.setString(index++, u.owner);
			return index;
		};
	}

	public static FileHostingStore.EntityPopulator<FileHostingEntityInfo> populator() {
		return (rs, index, value) -> {
			value.path = rs.getString(index++);
			value.created = new Date(rs.getTimestamp(index++).getTime());
			value.owner = rs.getString(index++);
			return index;
		};
	}
}
