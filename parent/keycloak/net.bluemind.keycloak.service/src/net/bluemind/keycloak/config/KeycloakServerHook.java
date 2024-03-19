/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.keycloak.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.client.AHCNodeClientFactory;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.server.hook.DefaultServerHook;

public class KeycloakServerHook extends DefaultServerHook {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakServerHook.class);

	private AHCNodeClientFactory ncr = new AHCNodeClientFactory();

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {
		if (!tag.equals(TagDescriptor.bm_keycloak.getTag())) {
			return;
		}

		String serverAddr = server.value.address();
		INodeClient nc = ncr.create(serverAddr);

		TaskRef tr = nc.executeCommand("/usr/share/bm-keycloak/bin/createdb.sh");
		NCUtils.waitFor(nc, tr);
		logger.info("Keycloak database on server {} created", serverAddr);

		nc.executeCommand("rm", "/etc/bm/bm-keycloak.disabled");
		NCUtils.waitFor(nc, tr);

		tr = nc.executeCommand("service", "bm-keycloak", "stop");
		NCUtils.waitFor(nc, tr);

		tr = nc.executeCommand("service", "bm-keycloak", "start");
		NCUtils.waitFor(nc, tr);

		logger.info("Keycloak server started on {}", serverAddr);
	}

	@Override
	public void onServerUntagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {
		if (!tag.equals(TagDescriptor.bm_keycloak.getTag())) {
			return;
		}

		String serverAddr = server.value.address();
		INodeClient nc = ncr.create(serverAddr);

		TaskRef tr = nc.executeCommand("touch", "/etc/bm/bm-keycloak.disabled");
		NCUtils.waitFor(nc, tr);

		tr = nc.executeCommand("service", "bm-keycloak", "stop");
		NCUtils.waitFor(nc, tr);

		logger.info("Keycloak server stopped on {}", serverAddr);

	}

}
