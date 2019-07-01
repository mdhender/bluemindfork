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
package net.bluemind.exchange.mapi.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.AbstractItemValueStore;
import net.bluemind.exchange.mapi.api.MapiFAI;

public class MapiFAIStore extends AbstractItemValueStore<MapiFAI> {

	private static final Logger logger = LoggerFactory.getLogger(MapiFAIStore.class);
	private final Container container;

	public MapiFAIStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
		logger.debug("Created on {}.", this.container);
	}

	@Override
	public void create(Item item, MapiFAI value) throws SQLException {
		String query = "INSERT INTO t_mapi_fai (" + MapiFAIColumns.cols.names() + ", item_id) VALUES ("
				+ MapiFAIColumns.cols.values() + ",?)";
		insert(query, value, MapiFAIColumns.values(item.id));
	}

	@Override
	public void update(Item item, MapiFAI value) throws SQLException {
		String query = "UPDATE t_mapi_fai SET (" + MapiFAIColumns.cols.names() + ") = (" + MapiFAIColumns.cols.values()
				+ ") WHERE item_id=?";
		update(query, value, MapiFAIColumns.values(item.id));
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_mapi_fai WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public MapiFAI get(Item item) throws SQLException {
		String query = "SELECT " + MapiFAIColumns.cols.names() + " FROM t_mapi_fai WHERE item_id=?";
		return unique(query, rs -> new MapiFAI(), (ResultSet rs, int index, MapiFAI value) -> {
			value.folderId = rs.getString(index++);
			value.faiJson = rs.getString(index++);
			return index;
		}, new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_mapi_fai where item_id in ( select id from t_container_item where  container_id = ?)",
				new Object[] { container.id });
	}

	public List<String> byFolder(String folder) throws SQLException {
		String query = "SELECT item.uid FROM t_mapi_fai e "
				+ " INNER JOIN t_container_item item ON e.item_id = item.id "
				+ " WHERE item.container_id=? AND e.folder_id=?";
		return select(query, rs -> rs.getString(1), (ResultSet rs, int index, String value) -> {
			return index;
		}, new Object[] { container.id, folder });
	}

}
