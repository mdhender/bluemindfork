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
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class ContainerSyncStore extends JdbcAbstractStore {

	private Container container;

	public ContainerSyncStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
		Objects.requireNonNull(container, "non-null container is required");
	}

	public void suspendSync() {
		try {
			delete("DELETE FROM t_container_sync  WHERE container_id = ?", new Object[] { container.id });
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public void initSync() {
		ContainerSyncStatus syncStatus = new ContainerSyncStatus();
		syncStatus.nextSync = System.currentTimeMillis();
		setSyncStatus(syncStatus);
	}

	public void setSyncStatus(final ContainerSyncStatus syncStatus) throws ServerFault {
		try {

			insert("INSERT INTO t_container_sync (container_id, sync_tokens, next_sync, last_sync, errors) VALUES (?,?,?, NOW(), ?) "
					+ "ON CONFLICT (container_id) DO UPDATE SET sync_tokens = ?, next_sync = ?, last_sync = NOW(), errors = ?  ",
					null, (con, statement, index, currentRow, value) -> {
						statement.setLong(index++, container.id);

						if (syncStatus.syncTokens != null) {
							statement.setObject(index++, syncStatus.syncTokens);
						} else {
							statement.setObject(index++, Collections.EMPTY_MAP);
						}

						if (syncStatus.nextSync != null) {
							statement.setTimestamp(index++, new Timestamp(syncStatus.nextSync));
						} else {
							statement.setNull(index++, Types.TIMESTAMP);
						}

						if (syncStatus.errors != null) {
							statement.setInt(index++, syncStatus.errors);
						} else {
							statement.setNull(index++, Types.INTEGER);
						}

						if (syncStatus.syncTokens != null) {
							statement.setObject(index++, syncStatus.syncTokens);
						} else {
							statement.setObject(index++, Collections.EMPTY_MAP);
						}

						if (syncStatus.nextSync != null) {
							statement.setTimestamp(index++, new Timestamp(syncStatus.nextSync));
						} else {
							statement.setNull(index++, Types.TIMESTAMP);
						}

						if (syncStatus.errors != null) {
							statement.setInt(index++, syncStatus.errors);
						} else {
							statement.setNull(index++, Types.INTEGER);
						}

						return index;
					});
		} catch (SQLException e) {
			// if container does not exist then suspend sync status
			if (!this.checkContainerExists()) {
				logger.debug("Update of sync status has failed, removing sync now...");
				this.suspendSync();
			} else {
				throw ServerFault.sqlFault(e);
			}

		}
	}

	/**
	 * @return <code>true</code> if the {@link Container} exists, <code>false</code>
	 *         otherwise
	 */
	private boolean checkContainerExists() {
		try {
			return new ContainerStore(null, datasource, null).get(container.uid) != null;
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public ContainerSyncStatus getSyncStatus() throws ServerFault {
		try {
			@SuppressWarnings("unchecked")
			ContainerSyncStatus status = unique(
					"SELECT sync_tokens, next_sync, last_sync, errors FROM t_container_sync WHERE container_id = ?",
					(Creator<ContainerSyncStatus>) con -> new ContainerSyncStatus(),
					Arrays.<EntityPopulator<ContainerSyncStatus>>asList((rs, index, value) -> {
						value.syncTokens = (Map<String, String>) rs.getObject(index++);
						Timestamp ts = rs.getTimestamp(index++);
						if (ts != null) {
							value.nextSync = ts.getTime();
						}

						ts = rs.getTimestamp(index++);
						if (ts != null) {
							value.lastSync = new Date(ts.getTime());
						}
						
						value.errors = rs.getInt(index++);
						
						return index;
					}), new Object[] { container.id });

			if (status == null) {
				return null;
			} else {
				return status;
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public void delete() throws ServerFault {
		try {
			delete("delete from t_container_sync where container_id = ?", new Object[] { container.id });
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

}
