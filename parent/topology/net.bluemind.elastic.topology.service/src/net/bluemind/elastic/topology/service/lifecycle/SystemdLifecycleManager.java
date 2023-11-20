/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.elastic.topology.service.lifecycle;

import java.util.concurrent.TimeUnit;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;

public class SystemdLifecycleManager extends ElasticLifecycleManager {

	public SystemdLifecycleManager(ItemValue<Server> node) {
		super(node);
	}

	@Override
	public void stop() {
		INodeClient nc = NodeActivator.get(node.value.address());
		NCUtils.exec(nc, "systemctl stop bm-elasticsearch", 90, TimeUnit.SECONDS);
	}

	@Override
	public void start() {
		INodeClient nc = NodeActivator.get(node.value.address());
		NCUtils.exec(nc, "systemctl start bm-elasticsearch", 90, TimeUnit.SECONDS);
	}

}
