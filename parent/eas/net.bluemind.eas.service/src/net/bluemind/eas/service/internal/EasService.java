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
package net.bluemind.eas.service.internal;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.eas.api.Account;
import net.bluemind.eas.api.FolderSyncVersions;
import net.bluemind.eas.api.Heartbeat;
import net.bluemind.eas.api.IEas;
import net.bluemind.eas.api.SentItem;
import net.bluemind.eas.persistence.EasStore;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public class EasService implements IEas {

	private EasStore store;
	private BmContext context;

	public EasService(BmContext context) {
		this.context = context;
		store = new EasStore(context.getDataSource());

	}

	@Override
	public Heartbeat getHeartbeat(String deviceUid) throws ServerFault {
		checkAccess();
		try {
			return store.getHeartbeat(deviceUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void setHeartbeat(Heartbeat heartbeat) throws ServerFault {
		checkAccess();
		store.setHeartbeat(heartbeat);
	}

	@Override
	public Boolean needReset(Account account) throws ServerFault {
		checkAccess();
		try {
			return store.needReset(account);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void deletePendingReset(Account account) throws ServerFault {
		checkAccess();
		try {
			store.deletePendingReset(account);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void insertPendingReset(Account account) throws ServerFault {
		checkAccess();
		store.insertPendingReset(account);
	}

	@Override
	public List<SentItem> getSentItems(String folderId, Account account) throws ServerFault {
		checkAccess();
		try {
			return store.getSentItems(account, Integer.parseInt(folderId));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void insertSentItems(List<SentItem> sentItems) throws ServerFault {
		checkAccess();
		store.insertSentItems(sentItems);
	}

	@Override
	public void resetSentItems(String folderId, Account account) throws ServerFault {
		checkAccess();
		try {
			store.resetSentItems(account, Integer.parseInt(folderId));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void insertClientId(String clientId) throws ServerFault {
		checkAccess();
		try {
			store.insertClientId(clientId);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public Boolean isKnownClientId(String clientId) throws ServerFault {
		checkAccess();
		boolean ret = true;
		try {
			ret = store.isKnownClientId(clientId);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		return ret;
	}

	@Override
	public void setFolderSyncVersions(FolderSyncVersions versions) throws ServerFault {
		checkAccess();
		store.setFolderSyncVersions(versions.account, versions.versions);
	}

	@Override
	public Map<String, String> getFolderSyncVersions(Account account) throws ServerFault {
		checkAccess();
		try {
			return store.getFolderSyncVersions(account);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public Map<String, String> getConfiguration() throws ServerFault {
		if (context.getSecurityContext().isAnonymous()) {
			throw new ServerFault("Permission denied", ErrorCode.PERMISSION_DENIED);
		}
		ISystemConfiguration service = context.su().provider().instance(ISystemConfiguration.class);

		SystemConf systemConf = service.getValues();

		HashMap<String, String> ret = new HashMap<String, String>();
		ret.put(SysConfKeys.eas_sync_unknown.name(), systemConf.stringValue(SysConfKeys.eas_sync_unknown.name()));
		ret.put(SysConfKeys.eas_min_heartbeat.name(), systemConf.stringValue(SysConfKeys.eas_min_heartbeat.name()));
		ret.put(SysConfKeys.eas_max_heartbeat.name(), systemConf.stringValue(SysConfKeys.eas_max_heartbeat.name()));

		return ret;
	}

	private void checkAccess() throws ServerFault {
		if (!context.getSecurityContext().isDomainAdmin(context.getSecurityContext().getContainerUid())) {
			throw new ServerFault("Permission denied", ErrorCode.PERMISSION_DENIED);
		}
	}
}
