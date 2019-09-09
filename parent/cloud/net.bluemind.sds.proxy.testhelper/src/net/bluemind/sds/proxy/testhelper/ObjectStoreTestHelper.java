/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.sds.proxy.testhelper;

import java.io.IOException;
import java.io.InputStream;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;

public class ObjectStoreTestHelper {

	private ObjectStoreTestHelper() {

	}

	public static void setup(CyrusService cs, boolean restartCyrus) {
		ItemValue<Server> srv = cs.server();
		INodeClient nodeClient = NodeActivator.get(srv.value.address());
		try (InputStream in = ObjectStoreTestHelper.class.getClassLoader().getResourceAsStream("config/cyrus-hsm")) {
			nodeClient.writeFile("/etc/cyrus-hsm", in);
		} catch (IOException e) {
			throw new ServerFault(e);
		}
		if (restartCyrus) {
			cs.reload();
		}
	}

}
