/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.smime.cacerts.persistence;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.LongCreator;
import net.bluemind.smime.cacerts.api.SmimeCacert;

public class SmimeCacertStore extends AbstractItemValueStore<SmimeCacert> {

	private static final Creator<SmimeCacert> SMIME_CACERT_CREATOR = con -> new SmimeCacert();

	private Container container;

	public SmimeCacertStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
	}

	@Override
	public void create(Item item, SmimeCacert value) throws SQLException {
		String query = "INSERT INTO t_smime_cacerts (" + SmimeCacertColumns.cols.names() + ", item_id) VALUES ("
				+ SmimeCacertColumns.cols.values() + ", ?)";
		insert(query, value, SmimeCacertColumns.values(item));
	}

	@Override
	public void update(Item item, SmimeCacert value) throws SQLException {
		String query = "UPDATE t_smime_cacerts SET cert = ? WHERE item_id = ?";
		update(query, new Object[] { value.cert, item.id });
	}

	@Override
	public SmimeCacert get(Item item) throws SQLException {
		String query = "SELECT " + SmimeCacertColumns.cols.names() + " FROM t_smime_cacerts WHERE item_id = ?";
		return unique(query, SMIME_CACERT_CREATOR, SmimeCacertColumns.populator(), new Object[] { item.id });
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_smime_cacerts WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_smime_cacerts WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
	}

	public List<Long> sortedIds(SortDescriptor sorted) throws SQLException {
		String query = "SELECT item.id FROM t_smime_cacerts "
				+ "INNER JOIN t_container_item AS item ON t_smime_cacerts.item_id = item.id " //
				+ "WHERE item.container_id = ? " //
				+ "AND (item.flags::bit(32) & 2::bit(32)) = 0::bit(32) " // not deleted
				+ "ORDER BY item.created DESC";
		// FIXME use sort params
		return select(query, LongCreator.FIRST, Collections.emptyList(), new Object[] { container.id });
	}

}
