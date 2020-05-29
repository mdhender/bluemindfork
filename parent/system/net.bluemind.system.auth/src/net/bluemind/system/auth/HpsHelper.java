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
package net.bluemind.system.auth;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.client.AHCNodeClientFactory;
import net.bluemind.server.api.Server;

public class HpsHelper {
	private Logger logger = LoggerFactory.getLogger(HpsHelper.class);

	public static final String CMD_RESTART_HPS = "service bm-hps restart";

	public static HpsHelper get() {
		return new HpsHelper();
	}

	public void restartHps(Server server) throws ServerFault {
		new AHCNodeClientFactory().create(server.address()).executeCommandNoOut(CMD_RESTART_HPS);
	}

	public void nodeWrite(Server server, String path, String content) throws ServerFault {
		new AHCNodeClientFactory().create(server.address()).writeFile(path,
				new ByteArrayInputStream(content.getBytes()));
	}

	public void nodeWrite(Server server, String path, byte[] content) throws ServerFault {
		new AHCNodeClientFactory().create(server.address()).writeFile(path, new ByteArrayInputStream(content));
	}

	public String nodeRead(Server server, String path) throws ServerFault {
		byte[] content = new AHCNodeClientFactory().create(server.address()).read(path);
		if (content == null) {
			return null;
		} else {
			return new String(content);
		}
	}

	public List<ItemValue<Server>> hpsNodes(BmContext context) throws ServerFault {
		List<ItemValue<Server>> nodes = new ArrayList<>();

		Topology.getIfAvailable().map(t -> nodes.addAll(t.nodes()));

		return nodes;
	}
}
