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

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.jdbc.Columns;

public class RelatedColumns {
	public static final Columns COLUMNS = Columns.create() //
			.col("spouse") //
			.col("manager") //
			.col("assistant");

	public static VCardStore.StatementValues<VCard> values() {
		return (conn, statement, index, currentRow, value) -> {

			VCard.Related related = value.related;

			// spouse
			statement.setString(index++, related.spouse);

			// manager
			statement.setString(index++, related.manager);

			// assistant
			statement.setString(index++, related.assistant);

			return index;

		};

	}

	public static VCardStore.EntityPopulator<VCard> populator() {
		return (rs, index, value) -> {

			value.related = new VCard.Related();

			VCard.Related related = value.related;

			// spouse
			related.spouse = rs.getString(index++);

			// manager
			related.manager = rs.getString(index++);

			// assistant
			related.assistant = rs.getString(index++);

			return index;
		};

	}
}
