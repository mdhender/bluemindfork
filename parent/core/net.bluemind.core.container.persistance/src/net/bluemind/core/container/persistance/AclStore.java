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
package net.bluemind.core.container.persistance;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;

public class AclStore extends JdbcAbstractStore {

	private AclCache cache;

	private JdbcAbstractStore.StatementValues<AccessControlEntry> statementValues(final int position,
			final Container container) {

		return (con, statement, index, currentRow, value) -> {
			statement.setLong(index++, container.id);
			statement.setString(index++, value.subject);
			statement.setString(index++, value.verb.name());
			statement.setInt(index++, position + currentRow);
			return index;
		};
	}

	/**
	 * please prefer the {@link AclStore#AclStore(BmContext, DataSource)} variant
	 * that enables caching.
	 * 
	 * @param pool
	 */
	@Deprecated
	public AclStore(DataSource pool) {
		this(null, pool);
	}

	public AclStore(BmContext ctx, DataSource pool) {
		super(pool);
		this.cache = AclCache.get(ctx);
	}

	public void store(final Container container, final List<AccessControlEntry> entries)
			throws SQLException, ServerFault {
		doOrFail(() -> {
			delete("DELETE FROM t_container_acl where container_id = ?", new Object[] { container.id });
			batchInsert("INSERT INTO t_container_acl ( container_id, subject, verb, position) values (?, ?, ?, ?)",
					new ArrayList<>(new HashSet<>(entries)), statementValues(0, container));
			cache.put(container.uid, entries);
			return null;
		});
	}

	public void add(final Container container, final List<AccessControlEntry> entries) throws SQLException {
		List<AccessControlEntry> previous = get(container);
		batchInsert("INSERT INTO t_container_acl (container_id, subject, verb, position) values (?, ?, ?, ?)", entries,
				statementValues(previous.size(), container));
		cache.invalidate(container.uid);
	}

	private static final String GET_QUERY = "select subject, verb from t_container_acl where container_id = ?  order by position";

	public List<AccessControlEntry> get(final Container container) throws SQLException {
		List<AccessControlEntry> cached = cache.getIfPresent(container.uid);
		if (cached != null) {
			return cached;
		}
		List<AccessControlEntry> acls = new ArrayList<>(select(GET_QUERY, (rs) -> new AccessControlEntry(),
				Arrays.<EntityPopulator<AccessControlEntry>>asList((rs, index, value) -> {

					String subject = rs.getString(index++);
					value.subject = subject;
					value.verb = Verb.valueOf(rs.getString(index++));
					return index;
				}), new Object[] { container.id }));
		cache.put(container.uid, acls);
		return acls;
	}

	public void deleteAll(Container container) throws SQLException {
		delete("delete from t_container_acl where container_id  = ? ", new Object[] { container.id });
		cache.invalidate(container.uid);
	}

	public DataSource getDataSource() {
		return datasource;
	}

	public List<AccessControlEntry> retrieveAndStore(Container container, List<AccessControlEntry> entries)
			throws ServerFault {
		return doOrFail(() -> {
			List<AccessControlEntry> acl = get(container);
			store(container, entries);
			return acl;
		});
	}
}
