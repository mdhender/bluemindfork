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

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;

public class OwnerSubscriptionStore extends AbstractItemValueStore<ContainerSubscriptionModel> {

	private static final Logger logger = LoggerFactory.getLogger(OwnerSubscriptionStore.class);
	private final Container container;

	public OwnerSubscriptionStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
	}

	@Override
	public void create(Item item, ContainerSubscriptionModel value) throws SQLException {
		String query = "INSERT INTO t_owner_subscription (" + OwnerSubscriptionColumns.cols.names()
				+ ", item_id) VALUES (" + OwnerSubscriptionColumns.cols.values() + ",?)";
		if (logger.isDebugEnabled()) {
			logger.debug("Creating with {} {} {}", item, item.id, item.uid);
		}
		insert(query, value, OwnerSubscriptionColumns.values(item.id));
	}

	@Override
	public void update(Item item, ContainerSubscriptionModel value) throws SQLException {
		String query = "UPDATE t_owner_subscription SET (" + OwnerSubscriptionColumns.cols.names() + ") = ("
				+ OwnerSubscriptionColumns.cols.values() + ") WHERE item_id=?";
		update(query, value, OwnerSubscriptionColumns.values(item.id));
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_owner_subscription WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public ContainerSubscriptionModel get(Item item) throws SQLException {
		String query = "SELECT " + OwnerSubscriptionColumns.cols.names() + " FROM t_owner_subscription WHERE item_id=?";
		return unique(query, rs -> new ContainerSubscriptionModel(), OwnerSubscriptionColumns.POPULATOR,
				new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("delete from t_owner_subscription where item_id in ( select id from t_container_item where  container_id = ?)",
				new Object[] { container.id });
	}

}
