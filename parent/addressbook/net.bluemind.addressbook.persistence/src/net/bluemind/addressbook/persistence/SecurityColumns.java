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
package net.bluemind.addressbook.persistence;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.jdbc.Columns;

public class SecurityColumns {
	public static final Columns COLUMNS = Columns.create() //
			.col("certs") //
			.col("cert_parameters");

	public static VCardStore.StatementValues<VCard> values() {
		return (Connection conn, PreparedStatement statement, int index, int currentRow, VCard value) -> {

			VCard.Security security = value.security;

			String[] certs = new String[security.keys.size()];
			String[] certParameters = new String[security.keys.size()];

			for (int i = 0; i < security.keys.size(); i++) {
				certs[i] = security.keys.get(i).value;
				certParameters[i] = ParametersColumns.parametersAsString(security.keys.get(i).parameters);
			}
			statement.setArray(index++, conn.createArrayOf("text", certs));
			statement.setArray(index++, conn.createArrayOf("text", certParameters));

			return index;
		};
	}

	public static VCardStore.EntityPopulator<VCard> populator() {
		return (ResultSet rs, int index, VCard value) -> {

			String[] certs = arrayOfString(rs.getArray(index++));
			String[] certParameters = arrayOfString(rs.getArray(index++));

			List<VCard.Security.Key> keys = new ArrayList<>(certs.length);
			for (int i = 0; i < certs.length; i++) {
				keys.add(VCard.Security.Key.create(certs[i], ParametersColumns.stringAsParameters(certParameters[i])));
			}

			value.security = VCard.Security.create(keys);
			return index;
		};
	}

	protected static String[] arrayOfString(Array array) throws SQLException {
		String[] ret = null;
		if (array != null) {
			ret = (String[]) array.getArray();
		} else {
			ret = new String[0];
		}
		return ret;
	}

}
