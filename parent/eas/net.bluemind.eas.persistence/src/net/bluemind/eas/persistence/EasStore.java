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
package net.bluemind.eas.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.eas.api.Account;
import net.bluemind.eas.api.Heartbeat;
import net.bluemind.eas.api.SentItem;

public class EasStore extends JdbcAbstractStore {

	private static final Creator<Boolean> existCreator = new Creator<Boolean>() {

		@Override
		public Boolean create(ResultSet con) throws SQLException {
			return Boolean.TRUE;
		}
	};

	private static final EntityPopulator<Boolean> existPopulator = new EntityPopulator<Boolean>() {

		@Override
		public int populate(ResultSet rs, int index, Boolean value) throws SQLException {
			return 0;
		}
	};

	public EasStore(DataSource dataSource) {
		super(dataSource);
	}

	public static final String SELECT_HB = "SELECT " //
			+ EasColumns.t_eas_heartbeat.names() //
			+ " FROM t_eas_heartbeat WHERE device_uid = ?";

	// Heartbeat stuff
	public Heartbeat getHeartbeat(String deviceUid) throws SQLException {
		return unique(SELECT_HB, new Creator<Heartbeat>() {
			@Override
			public Heartbeat create(ResultSet con) throws SQLException {
				return new Heartbeat();
			}
		}, Arrays.<EntityPopulator<Heartbeat>>asList(EasColumns.heartbeatPopulator()), new Object[] { deviceUid });
	}

	public static final String UPSERT_HB = "INSERT INTO t_eas_heartbeat (" + EasColumns.t_eas_heartbeat.names()
			+ ") VALUES (" + EasColumns.t_eas_heartbeat.values() + ") "
			+ "ON CONFLICT (device_uid) DO update set heartbeat = EXCLUDED.heartbeat";

	public void setHeartbeat(Heartbeat heartbeat) throws ServerFault {
		doOrFail(() -> {
			insert(UPSERT_HB, heartbeat, EasColumns.heartbeatValues());
			return null;
		});
	}

	// Reset
	public void insertPendingReset(Account account) throws ServerFault {
		doOrFail(() -> {
			delete("DELETE FROM t_eas_pending_reset WHERE account = ? AND device = ?",
					new Object[] { account.userUid, account.device });

			StringBuilder query = new StringBuilder();
			query.append("INSERT INTO t_eas_pending_reset (");
			EasColumns.t_eas_pending_reset.appendNames(null, query);
			query.append(") VALUES (");
			EasColumns.t_eas_pending_reset.appendValues(query);
			query.append(")");

			insert(query.toString(), ResetStatus.create(account.userUid, account.device),
					EasColumns.resetStatusValue());
			return null;
		});
	}

	public Boolean needReset(Account account) throws SQLException {
		String query = "SELECT 1 FROM t_eas_pending_reset WHERE account = ? AND device = ?";

		List<Boolean> res = select(query, existCreator, existPopulator,
				new Object[] { account.userUid, account.device });

		return !res.isEmpty();
	}

	public void deletePendingReset(Account account) throws SQLException {
		delete("DELETE FROM t_eas_pending_reset WHERE account = ? AND device = ?",
				new Object[] { account.userUid, account.device });
	}

	// SentItem
	private static final Creator<SentItem> SENTITEM_CREATOR = new Creator<SentItem>() {
		@Override
		public SentItem create(ResultSet con) throws SQLException {
			return new SentItem();
		}
	};

	public void insertSentItems(List<SentItem> items) throws ServerFault {
		doOrFail(() -> {
			StringBuilder query = new StringBuilder();
			query.append("INSERT INTO t_eas_sent_item (");
			EasColumns.t_eas_sent_item.appendNames(null, query);
			query.append(") VALUES (");
			EasColumns.t_eas_sent_item.appendValues(query);
			query.append(")");

			// FIXME use batch
			for (SentItem item : items) {
				delete("DELETE FROM t_eas_sent_item WHERE device = ? AND folder = ? AND item = ?",
						new Object[] { item.device, item.folder, item.item });
				insert(query.toString(), new Object[] { item.device, item.folder, item.item });
			}
			return null;
		});
	}

	public List<SentItem> getSentItems(Account account, int folderId) throws SQLException {
		String query = "SELECT device, folder, item FROM t_eas_sent_item WHERE device = ? AND folder = ?";
		return select(query, SENTITEM_CREATOR, EasColumns.sentItemPopulator(),
				new Object[] { account.device, folderId });
	}

	public void resetSentItems(Account account, int folderId) throws SQLException {
		delete("DELETE FROM t_eas_sent_item WHERE device = ? AND folder = ?",
				new Object[] { account.device, folderId });
	}

	public void resetSentItems(String device) throws SQLException {
		delete("DELETE FROM t_eas_sent_item WHERE device = ?", new Object[] { device });
	}

	// Client ID
	public boolean isKnownClientId(String clientId) throws SQLException {
		String query = "SELECT 1 FROM t_eas_client_id WHERE client_id=?";
		List<Boolean> found = select(query, existCreator, existPopulator, new Object[] { clientId });
		return !found.isEmpty();
	}

	public void insertClientId(String clientId) throws SQLException {
		insert("INSERT INTO t_eas_client_id (client_id) VALUES (?)", new Object[] { clientId });
	}

	// Folder Sync
	private static final Creator<Map<String, String>> MAP_CREATOR = new Creator<Map<String, String>>() {
		@Override
		public Map<String, String> create(ResultSet con) throws SQLException {
			return new HashMap<String, String>();
		}
	};

	public void setFolderSyncVersions(Account account, Map<String, String> versions) throws ServerFault {
		doOrFail(() -> {
			// delete old versions first
			delete("DELETE FROM t_eas_folder_sync WHERE account = ? AND device = ?",
					new Object[] { account.userUid, account.device });

			StringBuilder query = new StringBuilder();
			query.append("INSERT INTO t_eas_folder_sync (");
			EasColumns.t_eas_folder_sync.appendNames(null, query);
			query.append(") VALUES (");
			EasColumns.t_eas_folder_sync.appendValues(query);
			query.append(")");
			insert(query.toString(), new Object[] { account.userUid, account.device, versions });
			return null;
		});
	}

	public Map<String, String> getFolderSyncVersions(Account account) throws SQLException {
		String query = "SELECT versions FROM t_eas_folder_sync WHERE account = ? AND device = ?";
		return unique(query, MAP_CREATOR, EasColumns.folderSyncPopulator(),
				new Object[] { account.userUid, account.device });
	}

}
