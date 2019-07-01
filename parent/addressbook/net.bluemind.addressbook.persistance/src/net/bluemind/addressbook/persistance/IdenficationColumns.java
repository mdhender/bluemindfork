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

import java.util.List;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.core.jdbc.Columns;

public class IdenficationColumns {
	public static final Columns COLUMNS = Columns.create() //
			.col("formated_name") //
			.col("formated_name_parameters") //
			.col("familynames") //
			.col("givennames") //
			.col("additionalnames") //
			.col("honorificprefixes") //
			.col("honoricficsuffixes") //
			.col("name_parameters") //
			.col("nickname") //
			.col("nickname_parameters") //
			.col("bday") //
			.col("anniversary") //
			.col("gender") //
			.col("gender_text"); //

	public static VCardStore.StatementValues<VCard> values() {
		return (conn, statement, index, currentRow, value) -> {

			VCard.Identification id = value.identification;
			// formatedName
			statement.setString(index++, id.formatedName.value);
			statement.setString(index++, parametersAsString(id.formatedName.parameters));

			// familynames
			statement.setString(index++, id.name.familyNames);
			// givennames
			statement.setString(index++, id.name.givenNames);
			// additionalnames
			statement.setString(index++, id.name.additionalNames);
			// prefixes
			statement.setString(index++, id.name.prefixes);
			// suffixes
			statement.setString(index++, id.name.suffixes);
			// name parameters
			statement.setString(index++, parametersAsString(id.name.parameters));

			// nickname parameters
			statement.setString(index++, id.nickname.value);
			// nickname parameters
			statement.setString(index++, parametersAsString(id.nickname.parameters));

			// birthday
			statement.setDate(index++, toSqlDate(id.birthday));

			// anniversary
			statement.setDate(index++, toSqlDate(id.anniversary));

			// gender
			statement.setString(index++, id.gender.value);
			statement.setString(index++, id.gender.text);
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
		return (rs, index, value) -> {

			value.identification = new VCard.Identification();

			VCard.Identification id = value.identification;
			// formatedName
			String fn = rs.getString(index++);
			String fnParameters = rs.getString(index++);
			List<Parameter> params = stringAsParameters(fnParameters);
			id.formatedName = VCard.Identification.FormatedName.create(fn, params);

			String familyNames = rs.getString(index++);
			String givenNames = rs.getString(index++);
			String additionalNames = rs.getString(index++);
			String prefixes = rs.getString(index++);
			String suffixes = rs.getString(index++);
			List<Parameter> nameParameters = stringAsParameters(rs.getString(index++));
			id.name = VCard.Identification.Name.create(familyNames, givenNames, additionalNames, prefixes, suffixes,
					nameParameters);

			String nickNames = rs.getString(index++);
			// String nicknamesParams =
			rs.getString(index++);
			id.nickname = VCard.Identification.Nickname.create(nickNames);

			java.sql.Date birthday = rs.getDate(index++);
			id.birthday = toDate(birthday);

			java.sql.Date anniversary = rs.getDate(index++);
			id.anniversary = toDate(anniversary);

			String gender = rs.getString(index++);
			String genderText = rs.getString(index++);
			id.gender = VCard.Identification.Gender.create(gender, genderText);

			return index;
		};

	}

	protected static java.util.Date toDate(java.sql.Date birthday) {
		if (birthday != null) {
			return new java.util.Date(birthday.getTime());
		} else {
			return null;
		}
	}

}