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
package net.bluemind.tag.persistence;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class TagRefStore extends AbstractItemValueStore<List<ItemTagRef>> {

	private Container container;

	private JdbcAbstractStore.Creator<List<ItemTagRef>> TAG_REF_CREATOR = new JdbcAbstractStore.Creator<List<ItemTagRef>>() {

		@Override
		public List<ItemTagRef> create(ResultSet con) throws SQLException {
			return new ArrayList<>(5);
		}

	};

	private JdbcAbstractStore.EntityPopulator<List<ItemTagRef>> TAG_REF_POPULATOR = new JdbcAbstractStore.EntityPopulator<List<ItemTagRef>>() {

		@Override
		public int populate(ResultSet rs, int index, List<ItemTagRef> value) throws SQLException {
			String[] c = arrayOfString(rs.getArray(index++));
			String[] it = arrayOfString(rs.getArray(index++));
			for (int i = 0; i < c.length; i++) {
				ItemTagRef ref = new ItemTagRef();
				ref.containerUid = c[i];
				ref.itemUid = it[i];
				value.add(ref);
			}
			return index++;
		}
	};

	public TagRefStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
	}

	@Override
	public void create(final Item item, List<ItemTagRef> value) throws SQLException {
		batchInsert(
				"INSERT INTO t_tagref (tagged_item_uid, tagged_container_uid, tagged_container_type, ref_container_uid, ref_item_uid)"
						+ "VALUES (?, ?, ?, ?, ?) ",
				value, new JdbcAbstractStore.StatementValues<ItemTagRef>() {

					@Override
					public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
							ItemTagRef value) throws SQLException {

						statement.setString(index++, item.uid);
						statement.setString(index++, container.uid);
						statement.setString(index++, container.type);
						statement.setString(index++, value.containerUid);
						statement.setString(index++, value.itemUid);
						return index;
					}

				});
	}

	@Override
	public void update(Item item, List<ItemTagRef> value) throws SQLException {
		delete(item);
		create(item, value);
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_tagref where tagged_item_uid = ? and tagged_container_uid = ?",
				new Object[] { item.uid, container.uid });
	}

	@Override
	public List<ItemTagRef> get(Item item) throws SQLException {
		String selectQuery = "SELECT array_agg(ref_container_uid), array_agg(ref.ref_item_uid) "//
				+ " FROM t_tagref ref "//
				+ " WHERE ref.tagged_item_uid = ? and ref.tagged_container_uid = ? "//
				+ "  GROUP BY ref.tagged_item_uid";
		List<ItemTagRef> tags = unique(selectQuery, TAG_REF_CREATOR, TAG_REF_POPULATOR,
				new Object[] { item.uid, container.uid });
		return null == tags ? Collections.emptyList() : tags;
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_tagref where tagged_container_uid = ?", new Object[] { container.uid });
	}

	@Override
	public List<List<ItemTagRef>> getMultiple(List<Item> items) throws SQLException {
		String query = "SELECT ref.tagged_item_uid, array_agg(ref_container_uid), array_agg(ref.ref_item_uid) "//
				+ " FROM t_tagref ref "//
				+ " WHERE ref.tagged_item_uid = ANY(?) and ref.tagged_container_uid = ? "//
				+ " GROUP BY ref.tagged_item_uid";

		List<ItemTagRefs> values = select(query, (ResultSet con) -> {
			return new ItemTagRefs();
		}, (ResultSet rs, int index, ItemTagRefs refs) -> {
			refs.itemUid = rs.getString(index++);
			refs.value = new ArrayList<>();
			return TAG_REF_POPULATOR.populate(rs, index, refs.value);
		}, new Object[] { items.stream().map(i -> i.uid).toArray(String[]::new), container.uid });

		Map<String, List<ItemTagRef>> map = values.stream().collect(Collectors.toMap(iv -> {
			return iv.itemUid;
		}, iv -> {
			return iv.value;
		}, (v1, v2) -> {
			return v1;
		}));

		return items.stream().map(i -> {
			return map.get(i.uid);
		}).collect(Collectors.toList());
	}

	protected static class ItemTagRefs {
		public String itemUid;
		public List<ItemTagRef> value;
	}

	protected static String[] arrayOfString(Array array) throws SQLException {
		String[] ret = null;
		if (array != null) {
			ret = (String[]) array.getArray();
		} else {
			ret = new String[0];
		}
		return ret;
	}
}
