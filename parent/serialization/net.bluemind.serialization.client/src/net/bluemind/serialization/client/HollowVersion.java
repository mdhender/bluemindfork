package net.bluemind.serialization.client;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class HollowVersion extends AbstractVerticle {

	public static final Logger logger = LoggerFactory.getLogger(HollowVersion.class);

	private static Map<String, Long> versions = new HashMap<>();
	private static List<HollowVersionObserver> observers = new ArrayList<>();

	private static MessageConsumer<Object> consumer;

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new HollowVersion();
		}

	}

	@Override
	public void start() {
		consumer = vertx.eventBus().consumer(HollowMessageForwarder.dataSetChanged);
		consumer.handler(message -> {
			JsonObject data = (JsonObject) message.body();
			String dataset = data.getString("dataset");
			String set = dataset.substring(0, dataset.indexOf("/"));
			String subset = dataset.substring(dataset.indexOf("/") + 1);
			long version = data.getLong("version");
			logger.info("Received hollow version update. New version: {}:{}", dataset, version);
			versions.put(dataset, version);
			observers.forEach(o -> o.onUpdate(set, subset, version));
		});
	}

	public static long getVersion(String set, String subset) {
		String key = set + "/" + subset;
		return versions.computeIfAbsent(key, k -> {
			return loadDataSetVersion(set, subset);
		});
	}

	public static long registerObserver(HollowVersionObserver observer, String set, String subset) {
		observers.add(observer);
		return getVersion(set, subset);
	}

	public static boolean isListening() {
		return consumer != null && consumer.isRegistered();
	}

	private static Long loadDataSetVersion(String set, String subset) {
		long version = 0;
		try (BmHollowClient client = new BmHollowClient(BmHollowClient.Type.version, set, subset, 0)) {
			version = client.getVersion();
			notify(set, subset, version);
		}
		return version;
	}

	private static void notify(String set, String subset, long version) {
		observers.forEach(o -> o.onUpdate(set, subset, version));
	}

}
