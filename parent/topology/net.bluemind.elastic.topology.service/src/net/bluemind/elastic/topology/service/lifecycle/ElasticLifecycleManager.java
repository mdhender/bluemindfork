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

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.server.api.Server;

public abstract class ElasticLifecycleManager {

	protected final ItemValue<Server> node;

	protected ElasticLifecycleManager(ItemValue<Server> node) {
		this.node = node;
	}

	public abstract void stop();

	public abstract void start();

	public void restart() {
		stop();
		start();
	}

	public static ElasticLifecycleManager defaultManager(ItemValue<Server> node) {
		return new SystemdLifecycleManager(node);
	}

}
