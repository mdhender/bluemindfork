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
package net.bluemind.user.persistence;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;
import javax.validation.constraints.NotNull;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.BooleanCreator;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.container.persistence.StringCreator;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class UserSubscriptionStore extends JdbcAbstractStore {

	private ItemStore itemStore;

	public UserSubscriptionStore(SecurityContext securityContext, DataSource dataSource, Container container) {
		super(dataSource);
		itemStore = new ItemStore(dataSource, container, securityContext);
	}

	public void subscribe(String subject, Container container) throws SQLException {

		Item item = itemStore.get(subject);
		if (item == null) {
			return;
		}

		insert("INSERT INTO t_container_sub ( container_uid, container_type, user_id) values (?, ?, ?)", container,
				(con, statement, index, currentRow, value) -> {
					statement.setString(index++, value.uid);
					statement.setString(index++, value.type);
					statement.setLong(index++, item.id);
					return index;
				});
	}

	public boolean isSubscribed(String subject, Container container) throws SQLException {

		Item item = itemStore.get(subject);
		if (item == null) {
			return false;
		}

		String query = "SELECT EXISTS(SELECT 1 FROM t_container_sub WHERE container_uid = ? and user_id = ?)";

		return unique(query, BooleanCreator.FIRST, Collections.emptyList(), new Object[] { container.uid, item.id });
	}

	public void unsubscribe(String subject, String containerUid) throws SQLException {

		Item item = itemStore.get(subject);
		if (item == null) {
			return;
		}

		delete("DELETE FROM t_container_sub where container_uid = ? and user_id = ?",
				new Object[] { containerUid, item.id });
	}

	public void unsubscribeAll(String subject) throws SQLException {

		Item item = itemStore.get(subject);
		if (item == null) {
			return;
		}

		delete("DELETE FROM t_container_sub where user_id = ?", new Object[] { item.id });
	}

	/**
	 * @param subject user uid
	 * @param type    might be null to search all subscriptions
	 * @return a list of container uid the subject is subscribed to
	 * @throws SQLException
	 */
	public List<String> listSubscriptions(@NotNull String subject, String type) throws SQLException {

		Item item = itemStore.get(subject);
		if (item == null) {
			return Collections.emptyList();
		}

		String query = "select container_uid from t_container_sub where user_id = ?";
		Object[] params = new Object[] { item.id };
		if (type != null) {
			query += " and container_type = ?";
			params = new Object[] { item.id, type };
		}

		return select(query, StringCreator.FIRST, Collections.emptyList(), params);
	}

	public List<String> subscribers(@NotNull String containerUid) throws SQLException {
		String query = "SELECT ci.uid FROM t_container_sub "//
				+ "INNER JOIN t_container_item ci ON ci.id=user_id "//
				+ "WHERE container_uid = ?";
		return select(query, StringCreator.FIRST, Collections.emptyList(), new Object[] { containerUid });
	}

	public void allowSynchronization(String subject, Container container, boolean sync) throws SQLException {

		Item item = itemStore.get(subject);
		if (item == null) {
			return;
		}

		String updateQuery = "update t_container_sub set offline_sync = ? where container_uid = ? and user_id = ?";
		update(updateQuery, null, (con, statement, index, currentRow, value) -> {
			statement.setBoolean(index++, sync);
			statement.setString(index++, container.uid);
			statement.setLong(index++, item.id);
			return index;
		});
	}

	public boolean isSyncAllowed(String subject, Container container) throws SQLException {

		Item item = itemStore.get(subject);
		if (item == null) {
			return false;
		}

		String query = "select offline_sync from t_container_sub where container_uid = ? and user_id = ?";
		Boolean ret = unique(query, BooleanCreator.FIRST, Collections.emptyList(),
				new Object[] { container.uid, item.id });
		if (ret != null) {
			return ret;
		}
		return false;
	}

}
