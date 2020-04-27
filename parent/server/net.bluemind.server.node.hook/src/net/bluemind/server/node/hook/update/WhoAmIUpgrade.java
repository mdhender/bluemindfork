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
package net.bluemind.server.node.hook.update;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.schemaupgrader.UpdateAction;
import net.bluemind.system.schemaupgrader.UpdateResult;
import net.bluemind.system.schemaupgrader.Updater;

public class WhoAmIUpgrade implements Updater {

	private static final Logger logger = LoggerFactory.getLogger(WhoAmIUpgrade.class);

	@Override
	public UpdateResult executeUpdate(IServerTaskMonitor monitor, DataSource pool, Set<UpdateAction> handledActions) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IServer serversApi = prov.instance(IServer.class, InstallationId.getIdentifier());
		List<ItemValue<Server>> servers = serversApi.allComplete();
		for (ItemValue<Server> iv : servers) {
			logger.info("Creating datalocation identity on {}", iv.value.address());
			serversApi.writeFile(iv.uid, "/etc/bm/server.uid", iv.uid.getBytes());
		}
		return UpdateResult.ok();
	}

	@Override
	public Date date() {
		return java.sql.Date.valueOf(LocalDate.of(2020, 4, 28));
	}

	@Override
	public int sequence() {
		return 100;
	}

	@Override
	public boolean afterSchemaUpgrade() {
		return true;
	}

}
