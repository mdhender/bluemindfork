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
package net.bluemind.backend.cyrus.replication.observers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class ReplicationObservers {

	private ReplicationObservers() {
	}

	public static List<IReplicationObserver> create(Vertx vertx) {
		RunnableExtensionLoader<IReplicationObserverProvider> rel = new RunnableExtensionLoader<>();
		return rel
				.loadExtensionsWithPriority("net.bluemind.backend.cyrus.replication.observers", "provider", "provider",
						"impl")
				.stream().map(prov -> prov.create(vertx)).filter(Objects::nonNull).collect(Collectors.toList());
	}

}
