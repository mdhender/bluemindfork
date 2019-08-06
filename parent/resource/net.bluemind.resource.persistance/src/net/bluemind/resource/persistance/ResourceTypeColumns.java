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
package net.bluemind.resource.persistance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.Creator;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property;

public class ResourceTypeColumns {

	public static Columns cols = Columns.create() //
			.col("label")//
			.col("templates")//
			.col("id");//

	public static Columns propCols = Columns.create() //
			.col("id")//
			.col("label")//
			.col("type", "enum_resource_type_prop_type");

	public static ResourceTypeStore.StatementValues<ResourceTypeDescriptor> statementValues(final String identifier,
			final Container container) {
		return new ResourceTypeStore.StatementValues<ResourceTypeDescriptor>() {
			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					ResourceTypeDescriptor desc) throws SQLException {

				statement.setString(index++, desc.label);
				statement.setObject(index++, desc.templates);
				statement.setString(index++, identifier);
				statement.setLong(index++, container.id);

				return index;
			}
		};
	}

	public static ResourceTypeStore.StatementValues<ResourceTypeDescriptor.Property> propStatementValues(
			final String identifier, final Container container) {
		return new ResourceTypeStore.StatementValues<ResourceTypeDescriptor.Property>() {
			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					ResourceTypeDescriptor.Property prop) throws SQLException {

				statement.setString(index++, prop.id);
				statement.setString(index++, prop.label);
				statement.setString(index++, prop.type.name());
				statement.setString(index++, identifier);
				statement.setLong(index++, container.id);
				return index;
			}
		};
	}

	public static EntityPopulator<ResourceTypeDescriptor> populator() {
		return new EntityPopulator<ResourceTypeDescriptor>() {

			@Override
			public int populate(ResultSet rs, int index, ResourceTypeDescriptor value) throws SQLException {
				value.label = rs.getString(index++);
				value.templates = new HashMap<>();
				@SuppressWarnings("unchecked")
				final Map<String, String> optionals = (Map<String, String>) rs.getObject(index++);
				if (optionals != null) {
					value.templates.putAll(optionals);
				}

				// skip id
				index++;

				return index;
			}

		};
	}

	public static Creator<ResourceTypeDescriptor> creator() {
		return rs -> new ResourceTypeDescriptor();
	}

	public static EntityPopulator<Property> propPopulator() {
		return new EntityPopulator<ResourceTypeDescriptor.Property>() {

			@Override
			public int populate(ResultSet rs, int index, Property value) throws SQLException {

				value.id = rs.getString(index++);
				value.label = rs.getString(index++);
				value.type = ResourceTypeDescriptor.Property.Type.valueOf(rs.getString(index++));
				return index;
			}
		};
	}

	public static Creator<Property> propCreator() {
		return rs -> new Property();
	}
}
