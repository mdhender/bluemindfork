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
package net.bluemind.backend.postfix.internal.cf;

import java.util.HashMap;
import java.util.Map;

import freemarker.template.Template;
import net.bluemind.backend.postfix.internal.PostfixPaths;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class RelayPassword extends AbstractConfFile {
	private final ItemValue<Server> server;
	private final String relayHost;

	public RelayPassword(IServer service, ItemValue<Server> server, String relayHost) throws ServerFault {
		super(service, server.uid);
		this.server = server;
		this.relayHost = relayHost;
	}

	@Override
	public void write() throws ServerFault {
		Template mcf = openTemplate("shard-relay-pwd");
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("relayHost", relayHost);
		data.put("pwd", Token.admin0());
		service.writeFile(serverUid, PostfixPaths.RELAY_PASSWORD, render(mcf, data));

		enable();
	}

	private void enable() {
		INodeClient nc = NodeActivator.get(server.value.address());
		TaskRef tr = nc.executeCommand("chmod +x " + PostfixPaths.RELAY_PASSWORD);
		NCUtils.waitFor(nc, tr);

		tr = nc.executeCommand("postmap " + PostfixPaths.RELAY_PASSWORD);
		NCUtils.waitFor(nc, tr);
	}
}
