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

import static net.bluemind.addressbook.persistance.ParametersColumns.parametersAsString;
import static net.bluemind.addressbook.persistance.ParametersColumns.stringAsParameters;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.jdbc.Columns;

public class CommunicationsColumns {
	public static final Columns COLUMNS = Columns.create() //
			.col("tel") //
			.col("tel_parameters") //
			.col("email") //
			.col("email_parameters") //
			.col("impp") //
			.col("impp_parameters") //
			.col("lang") //
			.col("lang_parameters"); //

	public static VCardStore.StatementValues<VCard> values() {
		return (Connection conn, PreparedStatement statement, int index, int currentRow, VCard value) -> {

			VCard.Communications communications = value.communications;
			int telsCount = communications.tels.size();
			String[] telValues = new String[telsCount];
			String[] telExts = new String[telsCount];
			String[] telParameters = new String[telsCount];

			for (int i = 0; i < telsCount; i++) {
				VCard.Communications.Tel tel = communications.tels.get(i);
				telValues[i] = tel.value;
				telExts[i] = tel.ext;
				telParameters[i] = parametersAsString(tel.parameters);
			}

			int mailsCount = communications.emails.size();
			String[] mailValues = new String[mailsCount];
			String[] mailParameters = new String[mailsCount];

			for (int i = 0; i < mailsCount; i++) {
				VCard.Communications.Email email = communications.emails.get(i);
				mailValues[i] = email.value;
				mailParameters[i] = parametersAsString(email.parameters);
			}

			int imppsCount = communications.impps.size();
			String[] imppValues = new String[imppsCount];
			String[] imppParameters = new String[imppsCount];

			for (int i = 0; i < imppsCount; i++) {
				VCard.Communications.Impp impp = communications.impps.get(i);
				imppValues[i] = impp.value;
				imppParameters[i] = parametersAsString(impp.parameters);
			}

			int langsCount = communications.langs.size();
			String[] langValues = new String[langsCount];
			String[] langParameters = new String[langsCount];

			for (int i = 0; i < langsCount; i++) {
				VCard.Communications.Lang lang = communications.langs.get(i);
				langValues[i] = lang.value;
				langParameters[i] = parametersAsString(lang.parameters);
			}

			statement.setArray(index++, conn.createArrayOf("text", telValues));
			statement.setArray(index++, conn.createArrayOf("text", telParameters));
			statement.setArray(index++, conn.createArrayOf("text", mailValues));
			statement.setArray(index++, conn.createArrayOf("text", mailParameters));
			statement.setArray(index++, conn.createArrayOf("text", imppValues));
			statement.setArray(index++, conn.createArrayOf("text", imppParameters));

			statement.setArray(index++, conn.createArrayOf("text", langValues));
			statement.setArray(index++, conn.createArrayOf("text", langParameters));

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

			String[] telValues = arrayOfString(rs.getArray(index++));
			String[] telParameters = arrayOfString(rs.getArray(index++));
			String[] mailValues = arrayOfString(rs.getArray(index++));
			String[] mailParameters = arrayOfString(rs.getArray(index++));
			String[] imppValues = arrayOfString(rs.getArray(index++));
			String[] imppParameters = arrayOfString(rs.getArray(index++));
			String[] langValues = arrayOfString(rs.getArray(index++));
			String[] langParameters = arrayOfString(rs.getArray(index++));

			List<VCard.Communications.Tel> tels = new ArrayList<>(telValues.length);
			for (int i = 0; i < telValues.length; i++) {

				VCard.Communications.Tel tel = VCard.Communications.Tel.create(telValues[i],
						stringAsParameters(telParameters[i]));
				tels.add(tel);
			}

			List<VCard.Communications.Email> mails = new ArrayList<>(mailValues.length);
			for (int i = 0; i < mailValues.length; i++) {

				VCard.Communications.Email email = VCard.Communications.Email.create(mailValues[i],
						stringAsParameters(mailParameters[i]));
				mails.add(email);
			}

			List<VCard.Communications.Impp> impss = new ArrayList<>(imppValues.length);
			for (int i = 0; i < imppValues.length; i++) {
				VCard.Communications.Impp impp = VCard.Communications.Impp.create(imppValues[i],
						stringAsParameters(imppParameters[i]));
				impss.add(impp);
			}

			List<VCard.Communications.Lang> langs = new ArrayList<>(langValues.length);
			for (int i = 0; i < langValues.length; i++) {
				VCard.Communications.Lang lang = VCard.Communications.Lang.create(langValues[i],
						stringAsParameters(langParameters[i]));
				langs.add(lang);
			}

			value.communications.tels = tels;
			value.communications.emails = mails;
			value.communications.impps = impss;
			value.communications.langs = langs;
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