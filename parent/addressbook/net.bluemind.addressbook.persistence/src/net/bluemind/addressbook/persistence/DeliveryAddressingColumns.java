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

import static net.bluemind.addressbook.persistence.ParametersColumns.parametersAsString;
import static net.bluemind.addressbook.persistence.ParametersColumns.stringAsParameters;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.jdbc.Columns;

// TODO address parameters
public class DeliveryAddressingColumns {
	public static final Columns COLUMNS = Columns.create() //
			.col("adr_label") //
			.col("postofficebox") //
			.col("extendedaddress") //
			.col("streetaddress") //
			.col("locality") //
			.col("region") //
			.col("postalcode") //
			.col("countryname") //
			.col("adr_parameters");

	public static VCardStore.StatementValues<VCard> values() {
		return (Connection conn, PreparedStatement statement, int index, int currentRow, VCard value) -> {

			List<VCard.DeliveryAddressing> addresses = value.deliveryAddressing;
			int addrCount = addresses.size();
			String[] label = new String[addrCount];
			String[] postOfficeBox = new String[addrCount];
			String[] extentedAddress = new String[addrCount];
			String[] streetAddress = new String[addrCount];
			String[] locality = new String[addrCount];
			String[] region = new String[addrCount];
			String[] postalCode = new String[addrCount];
			String[] countryName = new String[addrCount];
			String[] parameters = new String[addrCount];

			for (int i = 0; i < addrCount; i++) {
				VCard.DeliveryAddressing.Address addr = addresses.get(i).address;
				label[i] = addr.value;
				postOfficeBox[i] = addr.postOfficeBox;
				extentedAddress[i] = addr.extentedAddress;
				streetAddress[i] = addr.streetAddress;
				locality[i] = addr.locality;
				region[i] = addr.region;
				postalCode[i] = addr.postalCode;
				countryName[i] = addr.countryName;
				parameters[i] = parametersAsString(addr.parameters);
			}

			statement.setArray(index++, conn.createArrayOf("text", label));
			statement.setArray(index++, conn.createArrayOf("text", postOfficeBox));
			statement.setArray(index++, conn.createArrayOf("text", extentedAddress));
			statement.setArray(index++, conn.createArrayOf("text", streetAddress));
			statement.setArray(index++, conn.createArrayOf("text", locality));
			statement.setArray(index++, conn.createArrayOf("text", region));
			statement.setArray(index++, conn.createArrayOf("text", postalCode));
			statement.setArray(index++, conn.createArrayOf("text", countryName));
			statement.setArray(index++, conn.createArrayOf("text", parameters));
			return index;
		};
	}

	protected static java.sql.Date toSqlDate(java.util.Date birthday) {
		if (birthday == null) {
			return null;
		} else {
			return new java.sql.Date(birthday.getTime());
		}
	}

	public static VCardStore.EntityPopulator<VCard> populator() {
		return (ResultSet rs, int index, VCard value) -> {

			String[] labels = arrayOfString(rs.getArray(index++));
			String[] postOfficeBox = arrayOfString(rs.getArray(index++));
			String[] extentedAddress = arrayOfString(rs.getArray(index++));
			String[] streetAddress = arrayOfString(rs.getArray(index++));
			String[] locality = arrayOfString(rs.getArray(index++));
			String[] region = arrayOfString(rs.getArray(index++));
			String[] postalCode = arrayOfString(rs.getArray(index++));
			String[] countryName = arrayOfString(rs.getArray(index++));
			String[] parameters = arrayOfString(rs.getArray(index++));

			List<VCard.DeliveryAddressing> addresses = new ArrayList<>(labels.length);

			for (int i = 0; i < labels.length; i++) {
				VCard.DeliveryAddressing.Address addr = VCard.DeliveryAddressing.Address.create(labels[i],
						postOfficeBox[i], extentedAddress[i], streetAddress[i], locality[i], region[i], postalCode[i],
						countryName[i], stringAsParameters(parameters[i]));

				addresses.add(VCard.DeliveryAddressing.create(addr));
			}

			value.deliveryAddressing = addresses;
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

	protected static java.util.Date toDate(java.sql.Date birthday) {
		if (birthday != null) {
			return new java.util.Date(birthday.getTime());
		} else {
			return null;
		}
	}

}