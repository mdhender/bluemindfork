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
package net.bluemind.hsm.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.IUserHook;

public class UserHsmHook implements IUserHook {

	private static Logger logger = LoggerFactory.getLogger(UserHsmHook.class);
	private static final String ROOT = "/var/spool/bm-hsm/snappy/user";

	@Override
	public boolean handleGlobalVirt() {
		return false;
	}

	@Override
	public void beforeCreate(BmContext context, String domainUid, String uid, User user) throws ServerFault {
	}

	@Override
	public void beforeUpdate(BmContext context, String domainUid, String uid, User update, User previous)
			throws ServerFault {

	}

	@Override
	public void beforeDelete(BmContext context, String domainUid, String uid, User previous) throws ServerFault {
	}

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> created) throws ServerFault {
	}

	@Override
	public void onUserUpdated(BmContext context, String domainUid, ItemValue<User> previous, ItemValue<User> current)
			throws ServerFault {
	}

	@Override
	public void onUserDeleted(BmContext context, String domainUid, ItemValue<User> deleted) throws ServerFault {
		if (deleted.uid == null || deleted.uid.isEmpty()) {
			logger.error("fail to remove hsm data. user {}", deleted);
			return;
		}
		String path = ROOT + "/" + domainUid + "/" + deleted.uid;
		logger.info("user {} deleted. remove data from bm-hsm snappy store", deleted.uid, path);
		ItemValue<Server> server = context.su().getServiceProvider().instance(IServer.class, "default")
				.getComplete(deleted.value.dataLocation);
		INodeClient nc = NodeActivator.get(server.value.address());
		NCUtils.execNoOut(nc, "rm -Rf " + path);

	}

	@Override
	public void onAccountTypeUpdated(BmContext context, String domainUid, String uid, AccountType update,
			AccountType previous) throws ServerFault {
	}

}
