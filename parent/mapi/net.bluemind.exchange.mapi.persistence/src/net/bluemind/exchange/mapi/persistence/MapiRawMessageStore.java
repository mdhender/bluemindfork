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
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.LongCreator;
import net.bluemind.exchange.mapi.api.MapiRawMessage;

public class MapiRawMessageStore extends AbstractItemValueStore<MapiRawMessage> {

	private static final Logger logger = LoggerFactory.getLogger(MapiRawMessageStore.class);
	private final Container container;

	public MapiRawMessageStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
		logger.debug("Created on {}.", this.container);
	}

	@Override
	public void create(Item item, MapiRawMessage value) throws SQLException {
		logger.info("Create {} {}", item.id, item.uid);
		String query = "INSERT INTO t_mapi_raw_message (" + MapiRawMessageColumns.cols.names() + ", item_id) VALUES ("
				+ MapiRawMessageColumns.cols.values() + ",?)";
		insert(query, value, MapiRawMessageColumns.values(item.id));
	}

	@Override
	public void update(Item item, MapiRawMessage value) throws SQLException {
		logger.info("Update {} {}", item.id, item.uid);

		String query = "UPDATE t_mapi_raw_message SET (" + MapiRawMessageColumns.cols.names() + ") = ROW("
				+ MapiRawMessageColumns.cols.values() + ") WHERE item_id=?";
		update(query, value, MapiRawMessageColumns.values(item.id));
	}

	@Override
	public void delete(Item item) throws SQLException {
		logger.info("Delete {} {}", item.id, item.uid);

		delete("DELETE FROM t_mapi_raw_message WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public MapiRawMessage get(Item item) throws SQLException {
		String query = "SELECT " + MapiRawMessageColumns.cols.names() + " FROM t_mapi_raw_message WHERE item_id=?";
		return unique(query, rs -> new MapiRawMessage(), (ResultSet rs, int index, MapiRawMessage value) -> {
			value.contentJson = rs.getString(index++);
			return index;
		}, new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_mapi_raw_message where item_id in ( select id from t_container_item where  container_id = ?)",
				new Object[] { container.id });
	}

	public List<Long> sortedIds(SortDescriptor sorted) throws SQLException {
		logger.debug("sorted by {}", sorted);
		String query = "SELECT item.id FROM t_mapi_raw_message rec "
				+ "INNER JOIN t_container_item item ON rec.item_id=item.id " //
				+ "WHERE item.container_id=? " //
				+ "AND (item.flags::bit(32) & 2::bit(32))=0::bit(32) " // not deleted
				+ "ORDER BY item.created DESC";
		// FIXME use sort params
		return select(query, LongCreator.FIRST, Collections.emptyList(), new Object[] { container.id });
	}

}
