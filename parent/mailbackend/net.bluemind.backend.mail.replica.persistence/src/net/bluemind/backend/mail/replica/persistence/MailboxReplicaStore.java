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
package net.bluemind.backend.mail.replica.persistence;

import java.sql.SQLException;
import java.util.Collections;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.StringCreator;

public class MailboxReplicaStore extends AbstractItemValueStore<MailboxReplica> {

	private static final Logger logger = LoggerFactory.getLogger(MailboxReplicaStore.class);
	private static final Creator<MailboxReplica> MB_CREATOR = con -> new MailboxReplica();
	private final Container container;
	public final String partition;

	public MailboxReplicaStore(DataSource pool, Container container, String partition) {
		super(pool);
		this.container = container;
		this.partition = partition;
		logger.debug("Created for {}", this.partition);
	}

	@Override
	public void create(Item item, MailboxReplica value) throws SQLException {
		String query = "INSERT INTO t_mailbox_replica ( " + MailboxReplicaColumns.COLUMNS.names()
				+ ", item_id) VALUES (" + MailboxReplicaColumns.COLUMNS.values() + ", ? )";
		insert(query, value, MailboxReplicaColumns.values(item));
	}

	@Override
	public void update(Item item, MailboxReplica value) throws SQLException {
		String query = "UPDATE t_mailbox_replica SET ( " + MailboxReplicaColumns.COLUMNS.names() + ") = ("
				+ MailboxReplicaColumns.COLUMNS.values() + " )" + " WHERE item_id = ? ";

		update(query, value, MailboxReplicaColumns.values(item));
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_mailbox_replica WHERE item_id = ?", new Object[] { item.id });
	}

	private static final String GET_QUERY = "SELECT " + MailboxReplicaColumns.COLUMNS.names()
			+ " FROM t_mailbox_replica WHERE item_id = ?";

	@Override
	public MailboxReplica get(Item item) throws SQLException {
		return unique(GET_QUERY, MB_CREATOR, MailboxReplicaColumns.populator(), new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_mailbox_replica WHERE item_id IN ( SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
	}

	public String byName(String name) throws SQLException {
		String query = "SELECT item.uid FROM t_mailbox_replica mr INNER JOIN t_container_item item ON mr.item_id = item.id"
				+ " WHERE item.container_id = ? and mr.name = ?";
		String ret = unique(query, StringCreator.FIRST, Collections.emptyList(), new Object[] { container.id, name });
		if (logger.isDebugEnabled()) {
			logger.debug("byName({}) in container {} => {}", name, container.id, ret);
		}
		return ret;
	}

}
