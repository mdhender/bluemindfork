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
package net.bluemind.calendar.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;

public class VEventOccurrenceStore extends AbstractItemValueStore<List<VEventOccurrence>> {

	private final Container container;

	public VEventOccurrenceStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
	}

	@Override
	public void create(Item item, List<VEventOccurrence> value) throws SQLException {
		String query = "INSERT INTO t_calendar_vevent (" + VEventOccurrenceColumns.ALL.names() + ", item_id) VALUES ("
				+ VEventOccurrenceColumns.ALL.values() + ", ?)";

		batchInsert(query, value, VEventOccurrenceColumns.values(item.id));
	}

	@Override
	public void update(Item item, List<VEventOccurrence> value) throws SQLException {
		throw new UnsupportedOperationException("Updating an occurrence is not supported");
	}

	@Override
	public void delete(Item item) throws SQLException {
		throw new UnsupportedOperationException("Deleting a single recurrent item is not supported");
	}

	@Override
	public List<VEventOccurrence> get(Item item) throws SQLException {
		String query = "SELECT " + VEventOccurrenceColumns.ALL.names()
				+ " FROM t_calendar_vevent v JOIN t_container_item ci ON ci.id = v.item_id WHERE ci.id = ? AND NOT v.recurid_timestamp IS NULL";
		return select(query, (ResultSet con) -> {
			return new VEventOccurrence();
		}, (ResultSet rs, int index, VEventOccurrence event) -> {
			return VEventOccurrenceColumns.populator().populate(rs, index, event);
		}, new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_calendar_vevent WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?) AND NOT recurid_timestamp IS NULL",
				new Object[] { container.id });
	}

	@Override
	public List<List<VEventOccurrence>> getMultiple(List<Item> items) throws SQLException {
		List<ItemV<List<VEventOccurrence>>> values = new ArrayList<>();
		for (Item item : items) {
			String query = "SELECT item_id, " + VEventOccurrenceColumns.ALL.names()
					+ " FROM t_calendar_vevent WHERE item_id = ANY(?::int4[]) AND NOT recurid_timestamp IS NULL ORDER BY item_id ASC";
			List<ItemV<VEventOccurrence>> value = select(query, (ResultSet con) -> {
				return new ItemV<>();
			}, (ResultSet rs, int index, ItemV<VEventOccurrence> event) -> {
				event.itemId = rs.getLong(index++);
				event.value = new VEventOccurrence();
				return VEventOccurrenceColumns.populator().populate(rs, index, event.value);

			}, new Object[] { item.id });
			ItemV<List<VEventOccurrence>> itemV = new ItemV<>();
			itemV.itemId = item.id;
			itemV.value = value.stream().map(val -> val.value).collect(Collectors.toList());
			values.add(itemV);
		}

		return join(items, values);
	}

}
