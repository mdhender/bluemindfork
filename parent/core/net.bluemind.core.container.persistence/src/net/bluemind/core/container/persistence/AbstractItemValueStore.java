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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public abstract class AbstractItemValueStore<T> extends JdbcAbstractStore implements IItemValueStore<T> {

	public AbstractItemValueStore(DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public List<T> getMultiple(List<Item> items) throws SQLException {
		List<T> ret = new ArrayList<>();

		for (Item i : items) {
			ret.add(get(i));
		}

		return ret;
	}

	public static class ItemV<T> {
		public T value;
		public long itemId;
	}

	protected List<T> join(List<Item> items, List<ItemV<T>> values) throws SQLException {

		Map<Long, T> map = values.stream().collect(Collectors.toMap(iv -> {
			return iv.itemId;
		}, iv -> {
			return iv.value;
		}, (v1, v2) -> {
			return v1;
		}));

		return items.stream().map(i -> {
			return map.get(i.id);
		}).collect(Collectors.toList());
	}

}
