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
import java.util.Arrays;
import java.util.List;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.tag.api.TagRef;

public class ExplanatoryColumns {
	public static final Columns COLUMNS = Columns.create() //
			.col("urls") //
			.col("urls_parameters") //
			.col("note");

	public static VCardStore.StatementValues<VCard> values() {
		return (Connection conn, PreparedStatement statement, int index, int currentRow, VCard value) -> {

			VCard.Explanatory explanatory = value.explanatory;

			int urlsCount = explanatory.urls.size();
			String[] urlValues = new String[urlsCount];
			String[] urlParameters = new String[urlsCount];

			for (int i = 0; i < urlsCount; i++) {
				urlValues[i] = explanatory.urls.get(i).value;
				urlParameters[i] = ParametersColumns.parametersAsString(explanatory.urls.get(i).parameters);
			}
			statement.setArray(index++, conn.createArrayOf("text", urlValues));

			statement.setArray(index++, conn.createArrayOf("text", urlParameters));

			statement.setString(index++, explanatory.note);
			return index;
		};
	}

	public static VCardStore.EntityPopulator<VCard> populator() {
		return (ResultSet rs, int index, VCard value) -> {

			String[] urlValues = arrayOfString(rs.getArray(index++));
			String[] urlParameters = arrayOfString(rs.getArray(index++));
			String note = rs.getString(index++);

			List<VCard.Explanatory.Url> urls = new ArrayList<>(urlValues.length);
			for (int i = 0; i < urlValues.length; i++) {
				urls.add(VCard.Explanatory.Url.create(urlValues[i],
						ParametersColumns.stringAsParameters(urlParameters[i])));
			}
			value.explanatory = VCard.Explanatory.create(urls, Arrays.<TagRef> asList(), note);
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
