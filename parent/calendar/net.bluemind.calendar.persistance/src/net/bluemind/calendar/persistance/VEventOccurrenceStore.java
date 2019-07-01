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
package net.bluemind.calendar.persistance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.AbstractItemValueStore;

public class VEventOccurrenceStore extends AbstractItemValueStore<List<VEventOccurrence>> {

	private final Container container;

	public VEventOccurrenceStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
	}

	@Override
	public void create(Item item, List<VEventOccurrence> value) throws SQLException {
		String query = "INSERT INTO t_calendar_vevent (" + VEventOccurrenceColumns.ALL.names() + ", item_id ) values ("
				+ VEventOccurrenceColumns.ALL.values() + ", ? )";

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
		String query = "select " + VEventOccurrenceColumns.ALL.names()
				+ " from t_calendar_vevent v join t_container_item ci on ci.id = v.item_id where ci.id = ? and not v.recurid_timestamp is null";
		List<VEventOccurrence> values = select(query, (ResultSet con) -> {
			return new VEventOccurrence();
		}, (ResultSet rs, int index, VEventOccurrence event) -> {
			return VEventOccurrenceColumns.populator().populate(rs, index, event);
		}, new Object[] { item.id });

		return values;
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("delete from t_calendar_vevent where item_id in ( select id from t_container_item where container_id = ?) and not recurid_timestamp is null",
				new Object[] { container.id });
	}

	@Override
	public List<List<VEventOccurrence>> getMultiple(List<Item> items) throws SQLException {
		List<ItemV<List<VEventOccurrence>>> values = new ArrayList<>();
		for (Item item : items) {
			String query = "select item_id, " + VEventOccurrenceColumns.ALL.names()
					+ " from t_calendar_vevent where item_id = ANY(?::int4[]) and not recurid_timestamp is null order by item_id asc";
			List<ItemV<VEventOccurrence>> value = select(query, (ResultSet con) -> {
				return new ItemV<VEventOccurrence>();
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
