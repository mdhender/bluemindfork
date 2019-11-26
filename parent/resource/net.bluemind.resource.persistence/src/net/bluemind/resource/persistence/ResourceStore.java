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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.StringCreator;
import net.bluemind.resource.api.ResourceDescriptor;

public class ResourceStore extends AbstractItemValueStore<ResourceDescriptor> {

	private static final Logger logger = LoggerFactory.getLogger(ResourceStore.class);
	private Container container;

	public ResourceStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
	}

	@Override
	public void create(Item item, ResourceDescriptor value) throws SQLException {
		logger.debug("create resource {}", item.uid);

		String query = "INSERT INTO t_resource ( " + ResourceColumns.cols.names() + ", item_id)" + " VALUES ( "
				+ ResourceColumns.cols.values() + ", ?)";

		insert(query, value, ResourceColumns.statementValues(item));
	}

	@Override
	public ResourceDescriptor get(Item item) throws SQLException {

		String query = "SELECT  " + ResourceColumns.cols.names() + " from t_resource WHERE  item_id = ? ";

		ResourceDescriptor descriptor = unique(query, ResourceColumns.creator(), ResourceColumns.populator(),
				new Object[] { item.id });

		if (descriptor == null) {
			return null;
		}

		return descriptor;
	}

	@Override
	public void update(Item item, ResourceDescriptor value) throws SQLException {

		String query = "UPDATE t_resource set (" + ResourceColumns.cols.names() + " ) = ("
				+ ResourceColumns.cols.values() + ")" + " WHERE item_id = ? ";
		update(query, value, ResourceColumns.statementValues(item));
	}

	@Override
	public void delete(Item item) throws SQLException {

		delete("DELETE FROM t_resource WHERE " + "item_id = ? ", new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_resource WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
	}

	public List<String> findByType(String typeUid) throws SQLException {
		return select(
				"SELECT ci.uid FROM t_container_item ci, t_resource r WHERE ci.container_id = ? AND r.type_id = ?",
				new StringCreator(1), Arrays.asList(), new Object[] { container.id, typeUid });
	}
}
