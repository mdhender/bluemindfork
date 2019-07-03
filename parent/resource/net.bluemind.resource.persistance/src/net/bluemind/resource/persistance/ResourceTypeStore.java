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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.resource.api.type.ResourceType;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;

public class ResourceTypeStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(ResourceTypeStore.class);
	private Container container;

	public ResourceTypeStore(DataSource dataSource, Container resourcesContainer) {
		super(dataSource);
		this.container = resourcesContainer;
	}

	public void create(String identifier, ResourceTypeDescriptor descriptor) throws ServerFault {
		logger.debug("create resourcetype {}", identifier);

		doOrFail(() -> {
			String query = "INSERT INTO t_resource_type ( " + ResourceTypeColumns.cols.names()
					+ ", resource_container_id)" + " VALUES ( " + ResourceTypeColumns.cols.values() + ", ?)";

			insert(query, descriptor, ResourceTypeColumns.statementValues(identifier, container));

			String pQuery = " INSERT INTO t_resource_type_prop (" + ResourceTypeColumns.propCols.names()
					+ ", type_id, resource_container_id) VALUES (" + ResourceTypeColumns.propCols.values() + ", ?, ? )";
			batchInsert(pQuery, descriptor.properties, ResourceTypeColumns.propStatementValues(identifier, container));
			return null;
		});
	}

	public ResourceTypeDescriptor get(String identifier) throws SQLException {

		String query = "SELECT  " + ResourceTypeColumns.cols.names()
				+ " from t_resource_type WHERE  resource_container_id = ? " + " AND id = ?";

		String propQuery = " SELECT " + ResourceTypeColumns.propCols.names()
				+ " from t_resource_type_prop WHERE resource_container_id = ? " + " AND type_id = ?";

		ResourceTypeDescriptor descriptor = unique(query, ResourceTypeColumns.creator(),
				ResourceTypeColumns.populator(), new Object[] { container.id, identifier });

		if (descriptor == null) {
			return null;
		}
		List<ResourceTypeDescriptor.Property> props = select(propQuery, ResourceTypeColumns.propCreator(),
				ResourceTypeColumns.propPopulator(), new Object[] { container.id, identifier });

		descriptor.properties = props;

		return descriptor;
	}

	public void update(String identifier, ResourceTypeDescriptor descriptor) throws ServerFault {
		doOrFail(() -> {

			String query = "UPDATE t_resource_type set label = ? WHERE id = ? and resource_container_id = ?";

			update(query, descriptor, ResourceTypeColumns.statementValues(identifier, container));

			delete("DELETE FROM t_resource_type_prop WHERE " + "type_id = ? and resource_container_id = ?",
					new Object[] { identifier, container.id });

			String pQuery = " INSERT INTO t_resource_type_prop (" + ResourceTypeColumns.propCols.names()
					+ ", type_id, resource_container_id) VALUES (" + ResourceTypeColumns.propCols.values() + ", ?, ? )";
			batchInsert(pQuery, descriptor.properties, ResourceTypeColumns.propStatementValues(identifier, container));
			return null;
		});
	}

	public void delete(String identifier) throws ServerFault {
		doOrFail(() -> {

			delete("DELETE FROM t_resource_type_prop WHERE " + "type_id = ? and resource_container_id = ?",
					new Object[] { identifier, container.id });

			delete("DELETE FROM t_resource_type WHERE " + "id = ? and resource_container_id = ?",
					new Object[] { identifier, container.id });
			return null;
		});
	}

	public List<ResourceType> getTypes() throws SQLException {
		return select("SELECT id, label from t_resource_type WHERE resource_container_id = ?", rs -> new ResourceType(),
				new EntityPopulator<ResourceType>() {

					@Override
					public int populate(ResultSet rs, int index, ResourceType value) throws SQLException {

						value.identifier = rs.getString(index++);
						value.label = rs.getString(index++);
						return index;
					}
				}, new Object[] { container.id });
	}
}
