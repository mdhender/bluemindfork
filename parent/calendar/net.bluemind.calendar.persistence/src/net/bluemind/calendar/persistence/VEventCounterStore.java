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

import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;

public class VEventCounterStore extends AbstractItemValueStore<List<VEventCounter>> {

	private final Container container;

	public VEventCounterStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
	}

	@Override
	public void create(Item item, List<VEventCounter> value) throws SQLException {
		if (value == null || value.isEmpty()) {
			return;
		}
		String query = "INSERT INTO t_calendar_vevent_counter (" + VEventCounterColumns.ALL.names()
				+ ", vevent.item_id ) values (" + VEventCounterColumns.ALL.values() + ", ? )";

		batchInsert(query, value, VEventCounterColumns.values(item.id));
	}

	@Override
	public void update(Item item, List<VEventCounter> value) throws SQLException {
		throw new UnsupportedOperationException("Updating an counter is not supported");
	}

	@Override
	public void delete(Item item) throws SQLException {
		throw new UnsupportedOperationException("Deleting a single counter is not supported");
	}

	@Override
	public List<VEventCounter> get(Item item) throws SQLException {
		String query = "select " + VEventCounterColumns.SELECT_ALL.names()
				+ " from t_calendar_vevent_counter v join t_container_item ci on ci.id = (v.vevent).item_id where ci.id = ?";
		List<VEventCounter> values = select(query, (ResultSet con) -> {
			return new VEventCounter();
		}, (ResultSet rs, int index, VEventCounter event) -> {
			return VEventCounterColumns.populator().populate(rs, index, event);
		}, new Object[] { item.id });

		return values;
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("delete from t_calendar_vevent_counter where (vevent).item_id in ( select id from t_container_item where container_id = ?)",
				new Object[] { container.id });
	}

	@Override
	public List<List<VEventCounter>> getMultiple(List<Item> items) throws SQLException {
		List<ItemV<List<VEventCounter>>> values = new ArrayList<>();
		for (Item item : items) {
			String query = "select (vevent).item_id, " + VEventCounterColumns.SELECT_ALL.names()
					+ " from t_calendar_vevent_counter where item_id = ANY(?::int4[]) order by item_id asc";
			List<ItemV<VEventCounter>> value = select(query, (ResultSet con) -> {
				return new ItemV<VEventCounter>();
			}, (ResultSet rs, int index, ItemV<VEventCounter> event) -> {
				event.itemId = rs.getLong(index++);
				event.value = new VEventCounter();
				return VEventCounterColumns.populator().populate(rs, index, event.value);

			}, new Object[] { item.id });
			ItemV<List<VEventCounter>> itemV = new ItemV<>();
			itemV.itemId = item.id;
			itemV.value = value.stream().map(val -> val.value).collect(Collectors.toList());
			values.add(itemV);
		}

		return join(items, values);
	}

}
