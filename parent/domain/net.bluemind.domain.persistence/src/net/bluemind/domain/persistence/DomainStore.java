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
package net.bluemind.domain.persistence;

import java.sql.SQLException;
import java.util.Collections;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.StringCreator;
import net.bluemind.domain.api.Domain;

public class DomainStore extends AbstractItemValueStore<Domain> {

	private static Logger logger = LoggerFactory.getLogger(DomainStore.class);

	public DomainStore(DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public void create(Item item, Domain value) throws SQLException {
		logger.debug("create domain for item {} ", item.id);

		String query = "INSERT INTO t_domain ( " + DomainColumns.cols.names() + ", item_id)" + " VALUES ( "
				+ DomainColumns.cols.values() + ", ?)";

		insert(query.toString(), value, DomainColumns.statementValues(item.id));
	}

	@Override
	public void update(Item item, Domain value) throws SQLException {
		logger.debug("update domain for item {} ", item.id);

		String query = "UPDATE t_domain SET (  " + DomainColumns.cols.names() + " " + ") = ( "
				+ DomainColumns.cols.values() + ") WHERE item_id = ?";

		insert(query.toString(), value, DomainColumns.statementValues(item.id));

	}

	@Override
	public void delete(Item item) throws SQLException {
		logger.debug("delete domain for item {} ", item.id);

		String query = "DELETE FROM t_domain WHERE item_id = ?";

		delete(query, new Object[] { item.id });
	}

	@Override
	public Domain get(Item item) throws SQLException {
		String query = "SELECT " + DomainColumns.cols.names() + " FROM t_domain WHERE item_id = ?";

		return unique(query, DomainColumns.creator(), DomainColumns.populator(), new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		// FIXME don't know if i need to implement this
		throw new RuntimeException("Not implemented");
	}

	public String findByNameOrAliases(String name) throws SQLException {
		String query = "SELECT item.uid FROM t_container_item item , t_domain domain WHERE domain.item_id = item.id AND "
				+ " ( domain.name = ? or ?::text = ANY (domain.aliases )) ";

		return unique(query, StringCreator.FIRST, Collections.emptyList(), new Object[] { name, name });
	}
}
