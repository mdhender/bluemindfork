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
package net.bluemind.directory.persistence;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import com.google.common.base.Strings;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;

public class DirItemStore extends ItemStore {

	private Kind kind;

	public DirItemStore(DataSource pool, Container container, SecurityContext contextHolder, DirEntry.Kind kind) {
		super(pool, container, contextHolder);
		this.kind = kind;
	}

	@Override
	public Item get(String uid) throws SQLException {
		String selectQuery = "SELECT " + COLUMNS.names("item") + " FROM t_container_item item, t_directory_entry dir "
				+ " WHERE item.uid = ? and item.container_id = ? AND dir.item_id = item.id AND dir.kind = ?";
		return unique(selectQuery, (rs) -> new Item(), ITEM_POPULATORS,
				new Object[] { uid, container.id, kind.name() });

	}

	public Item getByEmail(String email) throws SQLException {
		if (Strings.isNullOrEmpty(email)) {
			return null;
		}
		String[] splitted = email.split("@");
		if (splitted.length != 2) {
			return null;
		}
		String leftPart = splitted[0];
		String domain = splitted[1];

		String selectQuery = "SELECT " + COLUMNS.names("item") + " FROM t_container_item item " //
				+ "  JOIN t_directory_entry dir ON dir.item_id = item.id " //
				+ "  JOIN t_mailbox_email e ON e.item_id = item.id " //
				+ "  JOIN t_domain dom ON dom.name = ? " //
				+ "  WHERE (e.left_address || '@' || e.right_address = ? "
				+ "OR ( (e.all_aliases = true AND e.left_address = ?) AND"
				+ " (dom.name = ? OR ? = ANY(dom.aliases)))) " //
				+ "  AND  item.container_id = ? AND dir.kind = ? ";

		return unique(selectQuery, (rs) -> new Item(), ITEM_POPULATORS,
				new Object[] { container.uid, email, leftPart, domain, domain, container.id, kind.name() });
	}

	@Override
	public Item getByExtId(String extId) throws SQLException {
		String selectQuery = "SELECT " + COLUMNS.names("item") + " FROM t_container_item item, t_directory_entry dir "
				+ " WHERE item.external_id = ? and item.container_id = ? AND dir.item_id = item.id AND dir.kind = ?";
		return unique(selectQuery, (rs) -> new Item(), ITEM_POPULATORS,
				new Object[] { extId, container.id, kind.name() });

	}

	@Override
	public Item getById(long id) throws SQLException {
		String selectQuery = "SELECT " + COLUMNS.names("item") + " FROM t_container_item item, t_directory_entry dir "
				+ " WHERE id = ? and container_id = ? AND dir.item_id = item.id AND dir.kind = ?";
		return unique(selectQuery, (rs) -> new Item(), ITEM_POPULATORS, new Object[] { id, container.id, kind.name() });

	}

	@Override
	public List<Item> getMultiple(List<String> uids) throws SQLException {
		String selectQuery = "SELECT " + COLUMNS.names("item") + " FROM t_container_item item, t_directory_entry dir "
				+ " WHERE item.container_id = ? and item.uid = ANY (?) AND dir.item_id = item.id AND dir.kind = ?";

		String[] array = uids.toArray(new String[0]);
		return select(selectQuery, (rs) -> new Item(), ITEM_POPULATORS,
				new Object[] { container.id, array, kind.name() });

	}

	@Override
	public List<Item> getMultipleById(List<Long> uids) throws SQLException {
		StringBuilder selectQuery = new StringBuilder( //
				"SELECT " + COLUMNS.names("item") + " FROM t_container_item item, t_directory_entry dir " //
						+ " WHERE item.container_id = ? and item.id IN (0");
		for (long l : uids) {
			selectQuery.append(",").append(l);
		}
		selectQuery.append(")");
		selectQuery.append(" AND dir.item_id = item.id AND dir.kind = ?");
		return select(selectQuery.toString(), (rs) -> new Item(), ITEM_POPULATORS,
				new Object[] { container.id, kind.name() });
	}

	@Override
	public List<Item> all() throws SQLException {
		String selectQuery = "SELECT " + COLUMNS.names("item") //
				+ " FROM t_container_item item, t_directory_entry dir " //
				+ " WHERE container_id = ? AND dir.item_id = item.id AND dir.kind = ? ";

		return select(selectQuery, (rs) -> new Item(), ITEM_POPULATORS, new Object[] { container.id, kind.name() });

	}

	@Override
	public List<String> allItemUids() throws SQLException {
		String query = "SELECT item.uid FROM t_container_item item, t_directory_entry dir " //
				+ " WHERE item.container_id = ? AND dir.item_id = item.id AND dir.kind = ?";
		return select(query, rs -> rs.getString(1), Arrays.<EntityPopulator<String>>asList(),
				new Object[] { container.id, kind.name() });

	}

}
