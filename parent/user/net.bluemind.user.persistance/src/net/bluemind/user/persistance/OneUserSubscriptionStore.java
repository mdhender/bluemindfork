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
package net.bluemind.user.persistance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.BooleanCreator;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class OneUserSubscriptionStore extends JdbcAbstractStore {

	private ItemStore itemStore;
	private final long userId;

	public OneUserSubscriptionStore(SecurityContext securityContext, DataSource dataSource, Container container,
			String subject) {
		super(dataSource);
		itemStore = new ItemStore(dataSource, container, securityContext);
		Item item;
		try {
			item = itemStore.get(subject);
			if (item == null) {
				throw ServerFault.notFound("subject " + subject + " not found");
			} else {
				this.userId = item.id;
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public void subscribe(Container container) throws SQLException {

		insert("INSERT INTO t_container_sub ( container_uid, container_type, user_id) values (?, ?, ?)", container,
				(con, statement, index, currentRow, value) -> {
					statement.setString(index++, value.uid);
					statement.setString(index++, value.type);
					statement.setLong(index++, userId);
					return index;
				});
	}

	public boolean isSubscribed(Container container) throws SQLException {

		String query = "SELECT EXISTS(SELECT 1 FROM t_container_sub WHERE container_uid = ? and user_id = ?)";

		return unique(query, BooleanCreator.FIRST, Collections.emptyList(), new Object[] { container.uid, userId });
	}

	public void unsubscribe(Container container) throws SQLException {

		delete("DELETE FROM t_container_sub where container_uid = ? and user_id = ?",
				new Object[] { container.uid, userId });
	}

	public void unsubscribeAll(String subject) throws SQLException {

		Item item = itemStore.get(subject);
		if (item == null) {
			return;
		}

		delete("DELETE FROM t_container_sub where user_id = ?", new Object[] { item.id });
	}

	/**
	 * @param type might be null to search all subscriptions
	 * @return a list of container uid the subject is subscribed to
	 * @throws SQLException
	 */
	public List<ContainerSubscription> listSubscriptions(String type) throws SQLException {

		String query = "select container_uid, offline_sync from t_container_sub where user_id = ?";
		Object[] params = new Object[] { userId };
		if (type != null) {
			query += " and container_type = ?";
			params = new Object[] { userId, type };
		}
		Creator<ContainerSubscription> cm = (rs) -> new ContainerSubscription();
		EntityPopulator<ContainerSubscription> pop = new EntityPopulator<ContainerSubscription>() {

			@Override
			public int populate(ResultSet rs, int index, ContainerSubscription value) throws SQLException {
				value.containerUid = rs.getString(index++);
				value.offlineSync = rs.getBoolean(index++);
				return index;
			}
		};

		return select(query, cm, Arrays.asList(pop), params);
	}

	public void allowSynchronization(Container container, boolean sync) throws SQLException {

		String updateQuery = "update t_container_sub set offline_sync = ? where container_uid = ? and user_id = ?";
		update(updateQuery, null, (con, statement, index, currentRow, value) -> {
			statement.setBoolean(index++, sync);
			statement.setString(index++, container.uid);
			statement.setLong(index++, userId);
			return index;
		});
	}

	public boolean isSyncAllowed(Container container) throws SQLException {

		String query = "select offline_sync from t_container_sub where container_uid = ? and user_id = ?";
		Boolean ret = unique(query, BooleanCreator.FIRST, Collections.emptyList(),
				new Object[] { container.uid, userId });
		if (ret != null) {
			return ret;
		}
		return false;
	}

}
