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
package net.bluemind.addressbook.persistance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.addressbook.api.VCard.Security.Key;
import net.bluemind.core.jdbc.Columns;

public class SecurityColumns {
	public static final Columns COLUMNS = Columns.create() //
			.col("pem") //
			.col("pem_parameters");

	public static VCardStore.StatementValues<VCard> values() {
		return (Connection conn, PreparedStatement statement, int index, int currentRow, VCard value) -> {

			VCard.Security security = value.security;

			statement.setString(index++, security.key.value);

			statement.setString(index++, ParametersColumns.parametersAsString(security.key.parameters));

			return index;
		};
	}

	public static VCardStore.EntityPopulator<VCard> populator() {
		return (ResultSet rs, int index, VCard value) -> {

			String keyValue = rs.getString(index++);
			List<Parameter> keyParameters = ParametersColumns.stringAsParameters(rs.getString(index++));
			Key key = Key.create(keyValue, keyParameters);

			value.security = VCard.Security.create(key);
			return index;
		};
	}

}
