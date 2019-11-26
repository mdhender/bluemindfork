/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.container.service.internal;

import java.sql.SQLException;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;

public class DummyStore extends AbstractItemValueStore<Dummy> {

	private Container container;

	public DummyStore(Container c, DataSource dataSource) {
		super(dataSource);
		this.container = c;
	}

	@Override
	public void create(Item item, Dummy value) throws SQLException {
		String q = "insert into t_dummy_value (" + Dummy.DUMMY_COLUMNS.names() + ", item_id) values ("
				+ Dummy.DUMMY_COLUMNS.values() + ", ?)";
		insert(q, value, Dummy.value(item));
	}

	@Override
	public void update(Item item, Dummy value) throws SQLException {
		String q = "update t_dummy_value set (" + Dummy.DUMMY_COLUMNS.names() + ") = (" + Dummy.DUMMY_COLUMNS.values()
				+ ") WHERE item_id = ?";
		update(q, value, Dummy.value(item));
	}

	@Override
	public Dummy get(Item item) throws SQLException {
		String query = "SELECT " + Dummy.DUMMY_COLUMNS.names() + " FROM t_dummy_value WHERE item_id = ?";
		return unique(query, Dummy.CREATOR, Dummy.POPUL, new Object[] { item.id });
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_dummy_value WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_dummy_value WHERE item_id IN ( SELECT id FROM t_container_item WHERE  container_id = ?)",
				new Object[] { container.id });
	}

}
