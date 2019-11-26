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
package net.bluemind.document.persistence;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.document.api.DocumentMetadata;

public class DocumentMetadataColumns {

	public static final Columns COLUMNS = Columns.create() //
			.col("uid")// uid
			.col("filename") // filename
			.col("name") // name
			.col("description") // description
			.col("mime"); // mime

	public static DocumentMetadataStore.StatementValues<DocumentMetadata> values(final Item item) {
		return (conn, statement, index, currentRow, value) -> {

			statement.setString(index++, value.uid);
			statement.setString(index++, value.filename);
			statement.setString(index++, value.name);
			statement.setString(index++, value.description);
			statement.setString(index++, value.mime);

			statement.setLong(index++, item.id);

			return index;

		};

	}

	public static DocumentMetadataStore.EntityPopulator<DocumentMetadata> populator() {
		return (rs, index, value) -> {

			value.uid = rs.getString(index++);
			value.filename = rs.getString(index++);
			value.name = rs.getString(index++);
			value.description = rs.getString(index++);
			value.mime = rs.getString(index++);

			return index;
		};

	}
}
