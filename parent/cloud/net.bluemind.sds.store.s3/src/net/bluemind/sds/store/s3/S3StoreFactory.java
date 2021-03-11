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
package net.bluemind.sds.store.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.aws.s3.utils.S3Configuration;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.system.api.ArchiveKind;

public class S3StoreFactory implements ISdsBackingStoreFactory {

	private static final Logger logger = LoggerFactory.getLogger(S3StoreFactory.class);

	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory("s3", MetricsRegistry.get(), S3StoreFactory.class);

	public S3StoreFactory() {
		// ok
	}

	@Override
	public ISdsBackingStore create(Vertx vertx, JsonObject configuration) {
		String type = configuration.getString("storeType");
		if (type == null || !type.equals(kind().toString())) {
			throw new IllegalArgumentException("Configuration is not for an s3 backend: " + configuration.encode());
		}
		logger.info("Configuring with {}", configuration.encode());
		return new S3Store(S3Configuration.from(configuration), registry, idFactory);

	}

	@Override
	public ArchiveKind kind() {
		return ArchiveKind.S3;
	}

}
