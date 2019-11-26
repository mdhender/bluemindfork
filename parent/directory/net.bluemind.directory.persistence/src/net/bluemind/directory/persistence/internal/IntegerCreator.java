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
package net.bluemind.directory.persistence.internal;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.jdbc.JdbcAbstractStore.Creator;

public class IntegerCreator implements Creator<Integer> {

	private int columnIndex;

	public IntegerCreator(int columnIndex) {
		this.columnIndex = columnIndex;
	}

	@Override
	public Integer create(ResultSet con) throws SQLException {
		return con.getInt(columnIndex);
	}

}
