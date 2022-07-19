/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.sds.store.cyrusspool;

import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.network.topology.Topology;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ArchiveKind;

public class SpoolStoreFactory implements ISdsBackingStoreFactory {

	private ServerSideServiceProvider prov;
	private List<ItemValue<Server>> backends;

	public SpoolStoreFactory() {
		this.prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		this.backends = Topology.get().nodes().stream().filter(s -> s.value.tags.contains("mail/imap"))
				.collect(Collectors.toList());
	}

	@Override
	public ArchiveKind kind() {
		return ArchiveKind.Cyrus;
	}

	@Override
	public ISdsBackingStore create(Vertx vertx, JsonObject configuration) {
		return new SpoolBackingStore(vertx, prov, backends);
	}

}
