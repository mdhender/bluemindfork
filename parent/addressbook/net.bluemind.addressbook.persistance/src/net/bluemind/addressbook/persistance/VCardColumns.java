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
import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;

public class VCardColumns {

	public static final Columns COLUMNS_MAIN = Columns.create().col("kind").col("source")
			.cols(IdenficationColumns.COLUMNS).cols(DeliveryAddressingColumns.COLUMNS)
			.cols(CommunicationsColumns.COLUMNS).cols(OrganizationalColumns.COLUMNS).cols(ExplanatoryColumns.COLUMNS)
			.cols(RelatedColumns.COLUMNS).cols(SecurityColumns.COLUMNS);

	public static VCardStore.StatementValues<VCard> values(final Item item) {
		return (conn, statement, index, currentRow, value) -> {

			statement.setString(index++, value.kind.name());
			statement.setString(index++, value.source);
			index = IdenficationColumns.values().setValues(conn, statement, index, currentRow, value);
			index = DeliveryAddressingColumns.values().setValues(conn, statement, index, currentRow, value);
			index = CommunicationsColumns.values().setValues(conn, statement, index, currentRow, value);
			index = OrganizationalColumns.values().setValues(conn, statement, index, currentRow, value);
			index = ExplanatoryColumns.values().setValues(conn, statement, index, currentRow, value);
			index = RelatedColumns.values().setValues(conn, statement, index, currentRow, value);
			index = SecurityColumns.values().setValues(conn, statement, index, currentRow, value);

			statement.setLong(index++, item.id);
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
			String kindAsString = rs.getString(index++);
			value.kind = VCard.Kind.valueOf(kindAsString);
			value.source = rs.getString(index++);
			index = IdenficationColumns.populator().populate(rs, index, value);
			index = DeliveryAddressingColumns.populator().populate(rs, index, value);
			index = CommunicationsColumns.populator().populate(rs, index, value);
			index = OrganizationalColumns.populator().populate(rs, index, value);
			index = ExplanatoryColumns.populator().populate(rs, index, value);
			index = RelatedColumns.populator().populate(rs, index, value);
			index = SecurityColumns.populator().populate(rs, index, value);

			return index;
		};

	}

}
