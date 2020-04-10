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
package net.bluemind.sds.proxy.events;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.sds.proxy.dto.ConfigureResponse;
import net.bluemind.sds.proxy.dto.DeleteRequest;
import net.bluemind.sds.proxy.dto.ExistRequest;
import net.bluemind.sds.proxy.dto.GetRequest;
import net.bluemind.sds.proxy.dto.JsMapper;
import net.bluemind.sds.proxy.dto.MgetRequest;
import net.bluemind.sds.proxy.dto.PutRequest;
import net.bluemind.sds.proxy.dto.SdsRequest;
import net.bluemind.sds.proxy.dto.SdsResponse;
import net.bluemind.sds.proxy.store.ISdsBackingStore;
import net.bluemind.sds.proxy.store.ISdsBackingStoreFactory;
import net.bluemind.sds.proxy.store.SdsException;
import net.bluemind.sds.proxy.store.dummy.DummyBackingStore;

public class SdsObjectStoreHandlerVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(SdsObjectStoreHandlerVerticle.class);
	private static final Path config = Paths.get("/etc/bm-sds-proxy/config.json");

	public static class SdsObjectStoreFactory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new SdsObjectStoreHandlerVerticle();
		}

	}

	private AtomicReference<ISdsBackingStore> sdsStore = new AtomicReference<>();
	private final Map<String, ISdsBackingStoreFactory> factories;
	private JsonObject storeConfig;

	public SdsObjectStoreHandlerVerticle() {
		this.factories = loadStoreFactories();
		this.storeConfig = loadConfig();
		sdsStore.set(loadStore());
	}

	private JsonObject loadConfig() {

		if (Files.exists(config)) {
			try {
				return new JsonObject(new String(Files.readAllBytes(config)));
			} catch (IOException e) {
				throw new SdsException(e);
			}
		} else {
			logger.info("Configuration {} is missing, using defaults.", config.toFile().getAbsolutePath());
			return new JsonObject();
		}
	}

	private ISdsBackingStore loadStore() {
		String storeType = storeConfig.getString("storeType");
		if (storeType == null || storeType.equals("dummy") || !factories.containsKey(storeType)) {
			logger.info("Defaulting to dummy store (requested: {})", storeType);
			return DummyBackingStore.FACTORY.create(vertx, storeConfig);
		} else {
			logger.info("Loading store {}", storeType);
			return factories.get(storeType).create(vertx, storeConfig);
		}

	}

	private Map<String, ISdsBackingStoreFactory> loadStoreFactories() {
		RunnableExtensionLoader<ISdsBackingStoreFactory> rel = new RunnableExtensionLoader<>();
		List<ISdsBackingStoreFactory> stores = rel.loadExtensions("net.bluemind.sds.proxy", "store", "store",
				"factory");
		logger.info("Found {} backing store(s)", stores.size());
		return stores.stream().collect(Collectors.toMap(ISdsBackingStoreFactory::name, f -> f));
	}

	@Override
	public void start() {

		registerForJsonSdsRequest(SdsAddresses.EXIST, ExistRequest.class, r -> sdsStore.get().exists(r));

		registerForJsonSdsRequest(SdsAddresses.DELETE, DeleteRequest.class, r -> sdsStore.get().delete(r));

		registerForJsonSdsRequest(SdsAddresses.PUT, PutRequest.class, r -> sdsStore.get().upload(r));

		registerForJsonSdsRequest(SdsAddresses.CONFIG, this::reConfigure);

		registerForJsonSdsRequest(SdsAddresses.GET, GetRequest.class, get -> {
			return sdsStore.get().download(get);
		});
		registerForJsonSdsRequest(SdsAddresses.MGET, MgetRequest.class, mget -> sdsStore.get().downloads(mget));

	}

	private CompletableFuture<ConfigureResponse> reConfigure(JsonObject req) {
		logger.info("Apply configuration {}", req);
		storeConfig = req;
		sdsStore.set(loadStore());

		try {
			Files.write(config, req.encode().getBytes(), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			logger.warn("Failed to save configuration to {}", config.toFile().getAbsolutePath());
		}

		// for unit tests
		vertx.eventBus().publish("sds.events.configuration.updated", true);

		return CompletableFuture.completedFuture(new ConfigureResponse());
	}

	private static interface UnsafeFunction<T, R> {
		CompletableFuture<R> apply(T param);
	}

	private <T extends SdsRequest, R extends SdsResponse> void registerForJsonSdsRequest(String address,
			Class<T> reqType, UnsafeFunction<T, R> process) {
		vertx.eventBus().consumer(address, (Message<String> msg) -> {
			String jsonString = msg.body();
			T sdsReq = JsMapper.readValue(jsonString, reqType);
			process.apply(sdsReq).whenComplete((sdsResp, ex) -> {
				if (ex != null) {
					logger.error("{} Error processing payload {}", address, jsonString, ex);
					// let the event bus timeout trigger, an http 500 will be returned
				} else {
					msg.reply(JsMapper.writeValueAsString(sdsResp));
				}
			});
		});
	}

	private <R extends SdsResponse> void registerForJsonSdsRequest(String address,
			UnsafeFunction<JsonObject, R> process) {
		vertx.eventBus().consumer(address, (Message<String> msg) -> {
			process.apply(new JsonObject(msg.body())).whenComplete((sdsResp, ex) -> {
				if (ex != null) {
					logger.error("{} Error processing payload {}", address, msg.body(), ex);
					// let the event bus timeout trigger, an http 500 will be returned
				} else {
					msg.reply(JsMapper.writeValueAsString(sdsResp));
				}
			});
		});
	}

}
