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
package net.bluemind.server.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.INodeClientFactory;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.node.client.OkHttpNodeClientFactory;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;

public class CheckServerAvailability extends DefaultServerHook {

	private INodeClientFactory ncf = new OkHttpNodeClientFactory();

	private static final Logger logger = LoggerFactory.getLogger(CheckServerAvailability.class);

	@Override
	public void beforeCreate(BmContext context, String uid, Server server) throws ServerFault {
		checkServerIsAvailable(server);
	}

	@Override
	public void beforeUpdate(BmContext context, String uid, Server server, Server previous) throws ServerFault {
		checkServerIsAvailable(server);
	}

	@Override
	public void onServerDeleted(BmContext context, ItemValue<Server> server) throws ServerFault {
		ncf.delete(server.value.address());
	}

	private void checkServerIsAvailable(Server server) {
		INodeClient nc = ncf.create(NodeActivator.requiresLocalNode(server.address()) ? "127.0.0.1" : server.address());
		nc.ping();
		logger.info("server {} is joinable", server.address());
	}

}
