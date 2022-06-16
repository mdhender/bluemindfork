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
package net.bluemind.central.reverse.proxy.model.impl;

import com.typesafe.config.Config;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import net.bluemind.central.reverse.proxy.common.config.CrpConfig;
import net.bluemind.central.reverse.proxy.model.ProxyInfoStore;
import net.bluemind.central.reverse.proxy.model.RecordHandler;
import net.bluemind.central.reverse.proxy.model.client.ProxyInfoStoreClient;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.VertxPlatform;

public class ProxyInfoVerticleFactory implements IVerticleFactory, IUniqueVerticleFactory {

	private static final Config config = CrpConfig.get("Model", ProxyInfoVerticleFactory.class.getClassLoader());

	@Override
	public boolean isWorker() {
		return false;
	}

	@Override
	public Verticle newInstance() {
		Vertx vertx = VertxPlatform.getVertx();
		ProxyInfoStore store = ProxyInfoStore.create(vertx);
		ProxyInfoStoreClient storeClient = ProxyInfoStoreClient.create(vertx);
		RecordHandler<byte[], byte[]> recordHandler = RecordHandler.createByteHandler(storeClient, vertx);
		return new ProxyInfoVerticle(config, store, recordHandler);
	}

}
