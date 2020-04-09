/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.system.ldap.export.upgrader;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.ldap.export.conf.SlapdConfig;
import net.bluemind.system.ldap.export.hook.LdapServerHook;
import net.bluemind.system.schemaupgrader.IVersionedUpdater;
import net.bluemind.system.schemaupgrader.UpdateAction;
import net.bluemind.system.schemaupgrader.UpdateResult;

public class UpdateSaslConf implements IVersionedUpdater {
	private static final Logger logger = LoggerFactory.getLogger(UpdateSaslConf.class);

	@Override
	public int major() {
		return 5;
	}

	@Override
	public int buildNumber() {
		return 50011;
	}

	@Override
	public UpdateResult executeUpdate(IServerTaskMonitor monitor, DataSource pool, Set<UpdateAction> handledActions) {
		BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

		List<ItemValue<Server>> directoryServers = new ArrayList<>();
		try {
			for (ItemValue<Server> server : context.provider().instance(IServer.class, "default").allComplete()) {
				if (server.value.tags.contains(LdapServerHook.LDAPTAG)) {
					directoryServers.add(server);
				}
			}
			monitor.progress(1, String.format("Get hosts tagged as %s", LdapServerHook.LDAPTAG));
		} catch (ServerFault e) {
			monitor.end(false, String.format("Fail to get %s hosts: %s", LdapServerHook.LDAPTAG, e.getMessage()), "");
			return UpdateResult.failed();
		}

		IServerTaskMonitor monitorByServer = monitor.subWork(1);

		for (ItemValue<Server> directoryServer : directoryServers) {
			try {
				SlapdConfig.build(directoryServer).updateSasl();
			} catch (ServerFault e) {
				String errorMsg = String.format("Fail to update LDAP SASL2 configuration on host %s: %s",
						directoryServer.value.address(), e.getMessage());
				logger.error(errorMsg, e);
				monitorByServer.end(false, errorMsg, "");

				return UpdateResult.failed();
			}
		}

		return UpdateResult.ok();
	}

	@Override
	public boolean afterSchemaUpgrade() {
		return true;
	}

}
