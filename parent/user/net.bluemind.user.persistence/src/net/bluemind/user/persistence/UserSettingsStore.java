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
package net.bluemind.user.persistence;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.user.api.UserSettings;

public class UserSettingsStore extends AbstractItemValueStore<UserSettings> {
	private static final Logger logger = LoggerFactory.getLogger(UserSettingsStore.class);

	private final Container container;

	private static final Creator<Map<String, String>> SETTINGS_CREATOR = con -> new HashMap<>();

	public UserSettingsStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
		logger.debug("created {}", this.container);
	}

	@Override
	public void create(Item item, UserSettings value) throws SQLException {
		StringBuilder query = new StringBuilder("INSERT INTO t_settings_user (item_id, ");
		UserSettingsColumns.cols.appendNames(null, query);
		query.append(") VALUES (" + item.id + ", ");
		UserSettingsColumns.cols.appendValues(query);
		query.append(")");

		insert(query.toString(), value.values, UserSettingsColumns.statementValues());
	}

	@Override
	public void update(Item item, UserSettings value) throws SQLException {
		delete("DELETE FROM t_settings_user WHERE item_id = ?", new Object[] { item.id });

		StringBuilder query = new StringBuilder("INSERT INTO t_settings_user (item_id, ");
		UserSettingsColumns.cols.appendNames(null, query);
		query.append(") VALUES (" + item.id + ", ");
		UserSettingsColumns.cols.appendValues(query);
		query.append(")");

		insert(query.toString(), value.values, UserSettingsColumns.statementValues());
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_settings_user WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public UserSettings get(Item item) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");
		UserSettingsColumns.cols.appendNames(null, query);
		query.append(" FROM t_settings_user WHERE item_id = ?");
		Map<String, String> settings = unique(query.toString(), SETTINGS_CREATOR, UserSettingsColumns.populator(),
				new Object[] { item.id });
		return settings == null ? null : UserSettings.of(settings);
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_settings_user WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
	}
}
