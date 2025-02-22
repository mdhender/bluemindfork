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
package net.bluemind.core.container.persistence;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemFlag;

public class ContainersHierarchyNodeStore extends AbstractItemValueStore<ContainerHierarchyNode> {

	private static final Logger logger = LoggerFactory.getLogger(ContainersHierarchyNodeStore.class);
	private final Container container;

	public ContainersHierarchyNodeStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
	}

	@Override
	public void create(Item item, ContainerHierarchyNode value) throws SQLException {
		String query = "INSERT INTO t_container_hierarchy (" + ContainersHierarchyNodeColumns.cols.names()
				+ ", item_id) VALUES (" + ContainersHierarchyNodeColumns.cols.values() + ", ?)";
		if (logger.isDebugEnabled()) {
			logger.debug("Creating with {} {} {}", item, item.id, item.uid);
		}
		insert(query, value, ContainersHierarchyNodeColumns.values(item.id));
	}

	@Override
	public void update(Item item, ContainerHierarchyNode value) throws SQLException {
		String query = "UPDATE t_container_hierarchy SET (" + ContainersHierarchyNodeColumns.cols.names() + ") = ("
				+ ContainersHierarchyNodeColumns.cols.values() + ") WHERE item_id = ?";
		update(query, value, ContainersHierarchyNodeColumns.values(item.id));
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_container_hierarchy WHERE item_id = ?", new Object[] { item.id });
	}

	private static final String SELECT_NODE = "SELECT " + ContainersHierarchyNodeColumns.cols.names()
			+ " FROM t_container_hierarchy WHERE item_id = ?";

	@Override
	public ContainerHierarchyNode get(Item item) throws SQLException {
		return unique(SELECT_NODE, rs -> new ContainerHierarchyNode(), ContainersHierarchyNodeColumns.POPULATOR,
				item.id);
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_container_hierarchy WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
	}

	public void removeExpiredDeletedContainers(int days) throws SQLException {
		String select = "SELECT ci.id " //
				+ "FROM t_container_hierarchy h " //
				+ "JOIN t_container_item ci ON h.item_id = ci.id " //
				+ "WHERE h.container_type = 'mailbox_records' " //
				+ "AND ci.flags::bit(32) & (" //
				+ ItemFlag.Deleted.value + ")::bit(32) = (" + ItemFlag.Deleted.value + ")::bit(32) " //
				+ "AND ci.updated < (now() - interval '" + days + " days')";
		List<Long> selected = select(select, rs -> rs.getLong(1), (rs, index, val) -> index);

		delete("DELETE FROM t_container_hierarchy WHERE item_id = ANY (?)",
				new Object[] { selected.toArray(new Long[selected.size()]) });

		delete("DELETE FROM t_container_item WHERE id = ANY (?)",
				new Object[] { selected.toArray(new Long[selected.size()]) });
	}

}
