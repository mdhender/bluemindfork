/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.webappdata.persistence;

import java.sql.SQLException;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.webappdata.api.WebAppData;

public class WebAppDataStore extends AbstractItemValueStore<WebAppData> {

	private Container container;

	private static final Creator<WebAppData> WEBAPPDATA_CREATOR = con -> new WebAppData();

	public WebAppDataStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
	}

	@Override
	public void create(Item item, WebAppData webAppData) throws SQLException {
		String query = "INSERT INTO t_webappdata (" + WebAppDataColumns.cols.names() + ", item_id) VALUES ("
				+ WebAppDataColumns.cols.values() + ", ?)";
		insert(query, webAppData, WebAppDataColumns.values(item));
	}

	@Override
	public void update(Item item, WebAppData value) throws SQLException {
		String query = "UPDATE t_webappdata SET (" + WebAppDataColumns.cols.names() + ") = ("
				+ WebAppDataColumns.cols.values() + ") WHERE item_id = ?";
		update(query, value, WebAppDataColumns.values(item));
	}

	@Override
	public WebAppData get(Item item) throws SQLException {
		String query = "SELECT " + WebAppDataColumns.cols.names() + " FROM t_webappdata WHERE item_id = ?";
		return unique(query, WEBAPPDATA_CREATOR, WebAppDataColumns.populator(), new Object[] { item.id });
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_webappdata WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_webappdata WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
	}

	public WebAppData getByKey(String key) throws SQLException {
		String query = "SELECT " + WebAppDataColumns.cols.names()
				+ " FROM t_webappdata JOIN t_container_item ON item_id = id WHERE key = ? AND container_id = ?";
		return unique(query, WEBAPPDATA_CREATOR, WebAppDataColumns.populator(), new Object[] { key, container.id });
	}

}
