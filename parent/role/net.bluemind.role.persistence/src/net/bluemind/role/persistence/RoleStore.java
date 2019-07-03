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
package net.bluemind.role.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.StringCreator;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class RoleStore extends JdbcAbstractStore {

	private Container container;

	public RoleStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;

	}

	public void set(Item item, Set<String> roles) throws SQLException {
		delete("DELETE from t_domain_role WHERE item_id = ? ", new Object[] { item.id });

		if (roles.isEmpty()) {
			return;
		}

		batchInsert("INSERT INTO t_domain_role (role, item_id) values (?, ?)", roles, statementValues(item));
	}

	private StatementValues<String> statementValues(final Item item) {
		return new StatementValues<String>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow, String role)
					throws SQLException {
				statement.setString(index++, role);
				statement.setLong(index++, item.id);
				return index;
			}
		};
	}

	public Set<String> get(Item item) throws SQLException {
		List<String> list = select("SELECT role FROM t_domain_role WHERE item_id = ?", StringCreator.FIRST,
				Collections.emptyList(), new Object[] { item.id });
		return new HashSet<>(list);
	}

	public void deleteAll() throws SQLException {
		delete("DELETE from t_domain_role WHERE item_id in (SELECT id from t_container_item where container_id = ?",
				new Object[] { container.id });

	}

	public Set<String> getItemsWithRoles(List<String> roles) throws SQLException {
		List<String> list = select(
				"select c.uid from t_container_item c INNER JOIN t_domain_role d ON c.id = d.item_id where c.container_id = ? AND d.role = ANY (?)",
				StringCreator.FIRST, Collections.emptyList(),
				new Object[] { container.id, roles.toArray(new String[0]) });
		return new HashSet<>(list);
	}

}
