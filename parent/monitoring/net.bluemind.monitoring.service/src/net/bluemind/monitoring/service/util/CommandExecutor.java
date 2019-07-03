/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
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
package net.bluemind.monitoring.service.util;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.monitoring.api.Command;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public abstract class CommandExecutor {

	public static int execCmdOnServer(Server s, Command c) {
		ExitList result;

		try {
			result = NCUtils.exec(NodeActivator.get(s.ip), c.commandLine);
		} catch (ServerFault sf) {
			throw new ServerFault(String.format("Unable to run command '%s' on server %s", c.commandLine, s.address()));
		}

		c.rawData = String.join("\n", result);
		c.dataList = Formatter.parseRawResultInFetchedDataList(c.rawData);

		return result.getExitCode();
	}

	public static List<Server> getAllServers() {

		IServer server = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		List<ItemValue<Server>> servers = server.allComplete();
		List<Server> realServers = new ArrayList<Server>();

		for (ItemValue<Server> s : servers) {
			realServers.add(s.value);
		}

		return realServers;
	}

	public static Server getServerByIp(String ip) {
		Server server = null;

		for (Server s : CommandExecutor.getAllServers()) {
			if (s.ip.equals(ip)) {
				server = s;
			}
		}

		return server;
	}
}
