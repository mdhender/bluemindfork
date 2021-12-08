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

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.tag.api.Tag;

public class TagStore extends AbstractItemValueStore<Tag> {

	private static final Logger logger = LoggerFactory.getLogger(TagStore.class);

	private static final Creator<Tag> TAG_CREATOR = new Creator<Tag>() {
		@Override
		public Tag create(ResultSet con) throws SQLException {
			return new Tag();
		}
	};

	private Container container;

	public TagStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
	}

	@Override
	public void create(Item item, Tag tag) throws SQLException {
		logger.debug("create tag for item {} ", item.id);

		StringBuilder query = new StringBuilder("INSERT INTO t_tagvalue (");
		TagColumns.COLUMNS.appendNames(null, query);
		query.append(", item_id) VALUES (");
		TagColumns.COLUMNS.appendValues(query);
		query.append(", ?)");
		insert(query.toString(), tag, TagColumns.values(), new Object[] { item.id });
	}

	@Override
	public void update(Item item, Tag value) throws SQLException {
		logger.debug("update tag for item {} ", item.id);
		StringBuilder query = new StringBuilder("UPDATE t_tagvalue SET (");
		TagColumns.COLUMNS.appendNames(null, query);
		query.append(") = (");
		TagColumns.COLUMNS.appendValues(query);
		query.append(")");
		query.append("WHERE item_id = ?");
		update(query.toString(), value, TagColumns.values(), new Object[] { item.id });
	}

	@Override
	public Tag get(Item item) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");
		TagColumns.COLUMNS.appendNames("tag", query);
		query.append(" FROM t_tagvalue tag WHERE item_id = ?");
		return unique(query.toString(), TAG_CREATOR, TagColumns.populator(), new Object[] { item.id });
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_tagvalue WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_tagvalue WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
	}

}
