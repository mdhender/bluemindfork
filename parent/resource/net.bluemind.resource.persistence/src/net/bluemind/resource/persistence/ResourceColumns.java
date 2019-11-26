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
package net.bluemind.resource.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.Creator;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.ResourceReservationMode;

public class ResourceColumns {

	public static Columns cols = Columns.create() //
			.col("type_id") //
			.col("label")//
			.col("description")//
			.col("mailbox_location")//
			.col("values") //
			.col("reservation_mode", "resource_reservation_mode_type");

	public static ResourceStore.StatementValues<ResourceDescriptor> statementValues(final Item item) {
		return new ResourceTypeStore.StatementValues<ResourceDescriptor>() {
			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					ResourceDescriptor desc) throws SQLException {

				statement.setString(index++, desc.typeIdentifier);
				statement.setString(index++, desc.label);
				statement.setString(index++, desc.description);
				statement.setString(index++, desc.dataLocation);
				Map<String, String> values = new HashMap<>();
				for (ResourceDescriptor.PropertyValue prop : desc.properties) {
					values.put(prop.propertyId, prop.value);
				}
				statement.setObject(index++, values);
				statement.setString(index++, desc.reservationMode.name());
				statement.setLong(index++, item.id);
				return index;
			}
		};
	}

	public static EntityPopulator<ResourceDescriptor> populator() {
		return new EntityPopulator<ResourceDescriptor>() {

			@Override
			public int populate(ResultSet rs, int index, ResourceDescriptor value) throws SQLException {
				value.typeIdentifier = rs.getString(index++);
				value.label = rs.getString(index++);
				value.description = rs.getString(index++);
				value.dataLocation = rs.getString(index++);
				@SuppressWarnings("unchecked")
				Map<String, String> values = (Map<String, String>) rs.getObject(index++);

				List<ResourceDescriptor.PropertyValue> properties = new ArrayList<>(values.size());
				for (Map.Entry<String, String> entry : values.entrySet()) {
					properties.add(ResourceDescriptor.PropertyValue.create(entry.getKey(), entry.getValue()));
				}

				value.properties = properties;
				value.reservationMode = ResourceReservationMode.valueOf(rs.getString(index++));
				return index;
			}

		};
	}

	public static Creator<ResourceDescriptor> creator() {
		return rs -> new ResourceDescriptor();
	}
}
