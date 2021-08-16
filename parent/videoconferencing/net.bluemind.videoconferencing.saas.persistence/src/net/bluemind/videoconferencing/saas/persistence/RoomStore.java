/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.videoconferencing.saas.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.StringCreator;
import net.bluemind.videoconferencing.saas.api.BlueMindVideoRoom;

public class RoomStore extends AbstractItemValueStore<BlueMindVideoRoom> {
	private Container container;

	private static final Creator<BlueMindVideoRoom> ROOM_CREATOR = new Creator<BlueMindVideoRoom>() {
		@Override
		public BlueMindVideoRoom create(ResultSet con) throws SQLException {
			return new BlueMindVideoRoom();
		}
	};

	public RoomStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
	}

	@Override
	public void create(Item item, BlueMindVideoRoom value) throws SQLException {
		StringBuilder query = new StringBuilder("INSERT INTO t_videoconferencing_room (");
		RoomColumns.cols.appendNames(null, query);
		query.append(", item_id) VALUES (");
		RoomColumns.cols.appendValues(query);
		query.append(",?)");
		insert(query.toString(), value, RoomColumns.values(item));
	}

	private static String updateRequest() {
		StringBuilder query = new StringBuilder("UPDATE t_videoconferencing_room SET (");
		RoomColumns.cols.appendNames(null, query);
		query.append(") = (");
		RoomColumns.cols.appendValues(query);
		query.append(")");
		query.append("WHERE item_id = ?");
		return query.toString();
	}

	private static final String UPDATE_REQ = updateRequest();

	@Override
	public void update(Item item, BlueMindVideoRoom value) throws SQLException {
		update(UPDATE_REQ, value, RoomColumns.values(item));
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_videoconferencing_room WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public BlueMindVideoRoom get(Item item) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");
		RoomColumns.cols.appendNames("room", query);
		query.append(" FROM t_videoconferencing_room room WHERE item_id = ?");
		return unique(query.toString(), ROOM_CREATOR, Arrays.<EntityPopulator<BlueMindVideoRoom>>asList(RoomColumns.populator()),
				new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_videoconferencing_room WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
	}

	public String byIdentifier(String identifier) throws SQLException {
		String q = "SELECT uid FROM t_container_item INNER JOIN t_videoconferencing_room ON item_id = t_container_item.id WHERE identifier = ? AND container_id = ?";
		return unique(q, StringCreator.FIRST, Collections.emptyList(), new Object[] { identifier, container.id });
	}
}
