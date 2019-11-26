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

package net.bluemind.server.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.Server;

public final class ServerStore extends AbstractItemValueStore<Server> {

	private static final Logger logger = LoggerFactory.getLogger(ServerStore.class);
	private static final Creator<Server> SERVER_CREATOR = new Creator<Server>() {

		@Override
		public Server create(ResultSet con) throws SQLException {
			return new Server();
		}
	};

	private final Container installation;

	/**
	 * @param dataSource
	 */
	public ServerStore(DataSource dataSource, Container installation) {
		super(dataSource);
		this.installation = installation;
		logger.debug("new store for {}", this.installation);
	}

	@Override
	public void create(Item item, Server value) throws SQLException {
		StringBuilder query = new StringBuilder("INSERT INTO t_server (item_id, ");
		ServerColumns.cols.appendNames(null, query);
		query.append(") VALUES (" + item.id + ", ");
		ServerColumns.cols.appendValues(query);
		query.append(")");
		insert(query.toString(), value, ServerColumns.statementValues());
		logger.debug("insert complete: {}", value);
	}

	@Override
	public void update(Item item, Server value) throws SQLException {
		StringBuilder query = new StringBuilder("UPDATE t_server SET (");

		ServerColumns.cols.appendNames(null, query);
		query.append(") = (");
		ServerColumns.cols.appendValues(query);
		query.append(") WHERE item_id = ").append(item.id);

		update(query.toString(), value, ServerColumns.statementValues());
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_server WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public Server get(Item item) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");

		ServerColumns.cols.appendNames("s", query);

		query.append(", item.uid FROM t_server s");
		query.append(" INNER JOIN t_container_item item ON s.item_id = item.id");
		query.append(" WHERE item_id = ").append(item.id);
		Server s = unique(query.toString(), SERVER_CREATOR, ServerColumns.populator());

		return s;
	}

	public void assign(final String serverUid, final String domainUid, final String tag) throws SQLException {
		update("INSERT INTO t_server_assignment (server_uid, domain_uid, tag) VALUES (?, ?, ?)", null,
				new StatementValues<Void>() {

					@Override
					public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
							Void value) throws SQLException {
						statement.setString(index++, serverUid);
						statement.setString(index++, domainUid);
						statement.setString(index++, tag);
						return index;
					}
				});
	}

	public void unassign(final String serverUid, final String domainUid, final String tag) throws SQLException {
		update("DELETE FROM t_server_assignment WHERE server_uid=? AND domain_uid=? AND tag=?", null,
				new StatementValues<Void>() {

					@Override
					public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
							Void value) throws SQLException {
						statement.setString(index++, serverUid);
						statement.setString(index++, domainUid);
						statement.setString(index++, tag);
						return index;
					}
				});
	}

	public void unassignFromDomain(final String domainUid) throws SQLException {
		update("DELETE FROM t_server_assignment WHERE domain_uid=?", null, new StatementValues<Void>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow, Void value)
					throws SQLException {
				statement.setString(index++, domainUid);
				return index;
			}
		});
	}

	@Override
	public void deleteAll() throws SQLException {
		// FIXME should we throw UnsupportedOperationException?
	}

	public List<Assignment> getAssignments(final String domainUid) throws SQLException {
		String q = "SELECT server_uid, tag FROM t_server_assignment WHERE domain_uid=?";
		return select(q, new Creator<Assignment>() {

			@Override
			public Assignment create(ResultSet con) throws SQLException {
				return new Assignment();
			}
		}, new EntityPopulator<Assignment>() {

			@Override
			public int populate(ResultSet rs, int index, Assignment value) throws SQLException {
				value.domainUid = domainUid;
				value.serverUid = rs.getString(index++);
				value.tag = rs.getString(index++);
				return index;
			}
		}, new Object[] { domainUid });
	}

	public List<Assignment> getServerAssignments(final String uid) throws SQLException {
		String q = "SELECT domain_uid, tag FROM t_server_assignment WHERE server_uid=?";
		return select(q, new Creator<Assignment>() {

			@Override
			public Assignment create(ResultSet con) throws SQLException {
				return new Assignment();
			}
		}, new EntityPopulator<Assignment>() {

			@Override
			public int populate(ResultSet rs, int index, Assignment value) throws SQLException {
				value.serverUid = uid;
				value.domainUid = rs.getString(index++);
				value.tag = rs.getString(index++);
				return index;
			}
		}, new Object[] { uid });
	}

}
