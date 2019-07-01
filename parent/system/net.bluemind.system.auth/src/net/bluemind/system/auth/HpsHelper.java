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
import java.util.List;
import java.util.stream.Collectors;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.node.client.AHCNodeClientFactory;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class HpsHelper {

	protected AHCNodeClientFactory nodeClientFactory = new AHCNodeClientFactory();

	public static final String CMD_RESTART_HPS = "service bm-hps restart";

	protected void restartHps(Server server) throws ServerFault {
		nodeClientFactory.create(server.address()).executeCommandNoOut(CMD_RESTART_HPS);
	}

	protected void reloadHps(String origin) {
		Producer prod = MQ.getProducer(Topic.SERVICE_HPS_RELOAD);
		if (prod != null) {
			OOPMessage cm = MQ.newMessage();
			cm.putStringProperty("origin", origin);
			prod.send(cm);
		}

	}

	protected void nodeWrite(Server server, String path, String content) throws ServerFault {
		nodeClientFactory.create(server.address()).writeFile(path, new ByteArrayInputStream(content.getBytes()));
	}

	protected void nodeWrite(Server server, String path, byte[] content) throws ServerFault {
		nodeClientFactory.create(server.address()).writeFile(path, new ByteArrayInputStream(content));
	}

	protected String nodeRead(Server server, String path) throws ServerFault {
		byte[] content = nodeClientFactory.create(server.address()).read(path);
		if (content == null) {
			return null;
		} else {
			return new String(content);
		}
	}

	protected List<ItemValue<Server>> hpsNodes(BmContext context) throws ServerFault {
		return context.provider().instance(IServer.class, "default").allComplete().stream().filter(s -> {
			return s.value.tags.contains("bm/hps");
		}).collect(Collectors.toList());
	}
}
