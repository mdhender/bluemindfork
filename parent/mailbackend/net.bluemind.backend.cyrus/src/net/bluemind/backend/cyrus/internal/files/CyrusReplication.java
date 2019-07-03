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
package net.bluemind.backend.cyrus.internal.files;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import freemarker.template.Template;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.node.api.INodeClient;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

/**
 * /etc/cyrus-annotations handler
 * 
 * 
 */
public class CyrusReplication extends AbstractConfFile {

	private INodeClient node;

	public CyrusReplication(INodeClient nc) throws ServerFault {
		super(null, null);
		this.node = nc;
	}

	public CyrusReplication(IServer service, String serverUid) throws ServerFault {
		super(service, serverUid);
	}

	@Override
	public void write() throws ServerFault {
		Template cyrusConf = openTemplate("backend.replication.conf");
		Map<String, Object> data = new HashMap<String, Object>();
		Optional<ItemValue<Server>> coreServer = service.allComplete().stream()
				.filter(srv -> srv.value.tags.contains("bm/core")).findFirst();
		data.put("adminToken", Token.admin0());
		data.put("coreAddress", coreServer.isPresent() ? coreServer.get().value.address() : "127.0.0.1");
		byte[] rendered = render(cyrusConf, data);
		if (node != null) {
			node.writeFile("/etc/cyrus-replication", new ByteArrayInputStream(rendered));
		} else {
			service.writeFile(serverUid, "/etc/cyrus-replication", rendered);
		}
	}

}
