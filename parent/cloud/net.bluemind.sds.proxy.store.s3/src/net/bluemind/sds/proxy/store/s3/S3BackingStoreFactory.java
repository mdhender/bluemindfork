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
package net.bluemind.sds.proxy.store.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import com.netflix.spectator.api.Registry;

import net.bluemind.aws.s3.utils.S3Configuration;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.sds.proxy.store.ISdsBackingStore;
import net.bluemind.sds.proxy.store.ISdsBackingStoreFactory;

public class S3BackingStoreFactory implements ISdsBackingStoreFactory {

	private static final Logger logger = LoggerFactory.getLogger(S3BackingStore.class);
	public static final String NAME = "s3";

	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory("s3", MetricsRegistry.get(), S3BackingStoreFactory.class);

	public S3BackingStoreFactory() {
	}

	@Override
	public ISdsBackingStore create(Vertx vertx, JsonObject configuration) {
		String type = configuration.getString("storeType");
		if (!type.equals(NAME)) {
			throw new IllegalArgumentException("Configuration is not for an s3 backend: " + configuration.encode());
		}
		logger.info("Configuring with {}", configuration.encode());
		return new S3BackingStore(S3Configuration.from(configuration), registry, idFactory);

	}

	@Override
	public String name() {
		return NAME;
	}

}
