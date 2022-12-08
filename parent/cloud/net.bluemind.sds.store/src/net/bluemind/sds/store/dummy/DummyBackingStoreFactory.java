/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.sds.store.dummy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.system.api.ArchiveKind;

public class DummyBackingStoreFactory implements ISdsBackingStoreFactory {
	private static final Logger logger = LoggerFactory.getLogger(DummyBackingStoreFactory.class);

	public DummyBackingStoreFactory() {
	}

	@Override
	public ISdsBackingStore create(Vertx vertx, JsonObject configuration, String dataLocation) {
		String type = configuration.getString("storeType");
		if (type == null || !type.equals(kind().toString())) {
			throw new IllegalArgumentException("Configuration is not for a dummy backend: " + configuration.encode());
		}
		logger.debug("Configuring with {}", configuration.encode());
		return new DummyBackingStore();
	}

	@Override
	public ArchiveKind kind() {
		return ArchiveKind.Dummy;
	}
}