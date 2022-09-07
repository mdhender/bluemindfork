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
package net.bluemind.sds.proxy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.JsonParser;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.RouteMatcher;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.ExistRequest;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.MgetRequest;
import net.bluemind.sds.dto.MgetRequest.Transfer;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.dto.SdsRequest;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.proxy.dto.ConfigureResponse;
import net.bluemind.sds.proxy.events.DefaultValues;
import net.bluemind.sds.proxy.events.SdsAddresses;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.sds.store.SdsException;
import net.bluemind.sds.store.dummy.DummyBackingStoreFactory;
import net.bluemind.system.api.ArchiveKind;
import net.bluemind.vertx.common.request.Requests;

public abstract class SdsProxyBaseVerticle extends AbstractVerticle {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final Path config = Paths.get("/etc/bm-sds-proxy/config.json");
	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory("http", MetricsRegistry.get(), SdsProxyBaseVerticle.class);

	private static final AtomicReference<ISdsBackingStore> sdsStore = new AtomicReference<>();
	private final Map<ArchiveKind, ISdsBackingStoreFactory> factories;
	private JsonObject storeConfig;

	protected SdsProxyBaseVerticle() {
		this.factories = loadStoreFactories();
		this.storeConfig = loadConfig();
	}

	@Override
	public void start(Promise<Void> startedResult) {

		startBackingStore();

		HttpServer srv = vertx.createHttpServer();
		RouteMatcher router = new RouteMatcher(vertx);
		router.noMatch(req -> {
			logger.warn("Unknown request to {} {}", req.method(), req.absoluteURI());
			req.response().setStatusCode(400).end();
		});

		router.post("/configuration", req -> req.handler(JsonParser.newParser().objectValueMode().handler(js -> vertx
				.executeBlocking(prom -> reConfigure(js.objectValue()).thenAccept(prom::complete).exceptionally(x -> {
					prom.fail(x);
					return null;
				}), false, ar -> {
					if (ar.failed()) {
						logger.error("reconfiguration failed: {}",
								Optional.ofNullable(ar.cause().getMessage()).orElse("NullPointerException"),
								ar.cause());
						req.response()
								.setStatusMessage(
										Optional.ofNullable(ar.cause().getMessage()).orElse("NullPointerException"))
								.setStatusCode(500).end();
					} else {
						req.response().setStatusCode(200).end();
					}
				})

		)));

		router.options("/sds",
				sdsOperation("sds.exists", (ISdsBackingStore store, ExistRequest req) -> store.exists(req),
						js -> ExistRequest.of(js.getString("mailbox"), js.getString("guid")), (httpResp, existResp) -> {
							/* unrecoverable errors are directly handled by sdsOperation */
							if (!httpResp.ended()) {
								httpResp.setStatusCode(existResp.exists ? 200 : 404).end();
							}
						}));

		router.delete("/sds",
				sdsOperation("sds.delete", (ISdsBackingStore store, DeleteRequest req) -> store.delete(req),
						js -> DeleteRequest.of(js.getString("guid")), (httpResp, delResp) -> {
							/* Errors are directly handled by sdsOperation */
							if (!httpResp.ended()) {
								httpResp.setStatusCode(200).end();
							}
						}));

		router.put("/sds", sdsOperation("sds.put", (ISdsBackingStore store, PutRequest req) -> store.upload(req),
				js -> PutRequest.of(js.getString("guid"), js.getString("filename")), (httpResp, putResp) -> {
					/* Errors are directly handled by sdsOperation */
					if (!httpResp.ended()) {
						httpResp.setStatusCode(200).end();
					}
				}));

		router.get("/sds",
				sdsOperation("sds.get", (ISdsBackingStore store, GetRequest req) -> store.download(req),
						js -> GetRequest.of(js.getString("mailbox"), js.getString("guid"), js.getString("filename")),
						(httpResp, getResp) -> {
							/* Errors are directly handled by sdsOperation */
							if (!httpResp.ended()) {
								httpResp.setStatusCode(200).end();
							}
						}));

		router.post("/sds/mget",
				sdsOperation("sds.mget", (ISdsBackingStore store, MgetRequest req) -> store.downloads(req), js -> {
					MgetRequest mr = new MgetRequest();
					JsonArray tx = js.getJsonArray("transfers");
					int len = tx.size();
					List<MgetRequest.Transfer> xfers = new ArrayList<>(len);
					for (int i = 0; i < len; i++) {
						JsonObject one = tx.getJsonObject(i);
						xfers.add(Transfer.of(one.getString("guid"), one.getString("filename")));
					}
					mr.transfers = xfers;
					return mr;
				}, (httpResp, putResp) -> httpResp.setStatusCode(200).end()));

		router.post("/msg_size", validateAppendSize());

		router.post("/mailbox", this::validateMailbox);

		router.put("/sds/mapping", this::putMapping);
		router.delete("/sds/mapping", this::delMapping);
		router.post("/sds/mapping", this::queryMapping);

		srv.requestHandler(router);
		doListen(startedResult, srv);
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
		ArchiveKind archiveKind = ArchiveKind.fromName(storeType);
		if (archiveKind != null && factories.containsKey(archiveKind)) {
			logger.info("Loading store {}", archiveKind);
			return factories.get(archiveKind).create(vertx, storeConfig);
		} else {
			logger.info("Defaulting to dummy store (requested: {})", storeType);
			storeConfig.put("storeType", "dummy");
			return new DummyBackingStoreFactory().create(vertx, storeConfig);
		}
	}

	private Map<ArchiveKind, ISdsBackingStoreFactory> loadStoreFactories() {
		RunnableExtensionLoader<ISdsBackingStoreFactory> rel = new RunnableExtensionLoader<>();
		List<ISdsBackingStoreFactory> stores = rel.loadExtensions("net.bluemind.sds", "store", "store", "factory");
		logger.info("Found {} backing store(s)", stores.size());
		return stores.stream().collect(Collectors.toMap(ISdsBackingStoreFactory::kind, f -> f));
	}

	private void startBackingStore() {
		try {
			ISdsBackingStore store = loadStore();
			sdsStore.set(store);
		} catch (Exception e) {
			logger.warn("error loading sds backing store: {}", e.getMessage(), e);
			vertx.setTimer(1000, tid -> startBackingStore());
		}
	}

	protected abstract void doListen(Promise<Void> startedResult, HttpServer srv);

	private void putMapping(HttpServerRequest r) {
		HttpServerRequest request = Requests.wrap(r);
		Requests.tag(request, "method", SdsAddresses.MAP);

		request.bodyHandler(buf -> {
			logger.debug("MAP {}", buf);
			VertxPlatform.eventBus().request(SdsAddresses.MAP, new JsonObject(buf), ar -> {
				if (ar.succeeded()) {
					request.response().end();
				} else {
					logger.error("mapping error", ar.cause());
					request.response().setStatusCode(500).end();
				}
			});
		});
	}

	private void delMapping(HttpServerRequest r) {
		HttpServerRequest request = Requests.wrap(r);
		request.bodyHandler(buf -> {
			JsonObject reqJs = new JsonObject(buf);
			logger.info("UNMAP {}", reqJs.encodePrettily());
			request.response().end();
		});
	}

	private void queryMapping(HttpServerRequest r) {
		HttpServerRequest request = Requests.wrap(r);
		Requests.tag(request, "method", SdsAddresses.QUERY);
		request.bodyHandler(buf -> {
			logger.debug("QUERY {}", buf);
			VertxPlatform.eventBus().request(SdsAddresses.QUERY, new JsonObject(buf),
					(AsyncResult<Message<JsonObject>> ar) -> {
						if (ar.succeeded()) {
							request.response().end(ar.result().body().encode());
						} else {
							logger.error("mapping query error", ar.cause());
							request.response().setStatusCode(500).end();
						}
					});
		});
	}

	public static interface IStoreOperation<Q extends SdsRequest, R extends SdsResponse> {
		CompletableFuture<R> run(ISdsBackingStore store, Q req);
	}

	public static interface IMapper<Q extends SdsRequest> {
		Q map(JsonObject js);
	}

	private <Q extends SdsRequest, R extends SdsResponse> Handler<HttpServerRequest> sdsOperation(String name,
			IStoreOperation<Q, R> op, IMapper<Q> map, BiConsumer<HttpServerResponse, R> cons) {

		Id okTimerId = idFactory.name("requestTime")//
				.withTag("method", name)//
				.withTag("status", "OK");
		Timer okTimer = registry.timer(okTimerId);
		Id failedTimerId = idFactory.name("requestTime")//
				.withTag("method", name)//
				.withTag("status", "FAILED");
		Timer failedTimer = registry.timer(failedTimerId);

		return httpReq -> {

			long start = registry.clock().monotonicTime();
			HttpServerRequest req = Requests.wrap(httpReq);
			Requests.tag(req, "method", name);

			req.handler(JsonParser.newParser().objectValueMode().handler(jsonEvent -> {
				JsonObject obj = jsonEvent.objectValue();
				Q request = map.map(obj);
				vertx.executeBlocking((Promise<R> prom) -> op.run(sdsStore.get(), request).thenAccept(prom::complete)
						.exceptionally(x -> {
							prom.fail(x);
							return null;
						}), false, (AsyncResult<R> ar) -> {
							if (ar.failed()) {
								logger.error("{} failed.", name, ar.cause());
								failedTimer.record(registry.clock().monotonicTime() - start, TimeUnit.NANOSECONDS);
								req.response().setStatusCode(500).end();
							} else {
								R resp = ar.result();
								resp.tags().forEach((k, v) -> Requests.tag(req, k, v));
								okTimer.record(registry.clock().monotonicTime() - start, TimeUnit.NANOSECONDS);

								if (!resp.succeeded()) {
									req.response().setStatusMessage(resp.error.message).setStatusCode(500).end();
								}
								cons.accept(req.response(), resp);
							}
						});
			}));
		};
	}

	private CompletableFuture<ConfigureResponse> reConfigure(JsonObject req) {
		if (storeConfig != null && req.encode().equals(storeConfig.encode())) {
			logger.info("Sysconf changed, but nothing changed, ignoring");
			return CompletableFuture.completedFuture(new ConfigureResponse());
		}
		logger.info("Apply configuration {}", req);
		storeConfig = req;
		ISdsBackingStore oldStore = sdsStore.getAndSet(loadStore());
		if (oldStore != null) {
			oldStore.close();
		}

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

	private void validateMailbox(HttpServerRequest request) {
		HttpServerRequest req = Requests.wrap(request);
		req.bodyHandler(payload ->

		vertx.eventBus().request(SdsAddresses.VALIDATION, payload, new DeliveryOptions().setSendTimeout(3000),
				(AsyncResult<Message<Boolean>> result) -> {
					if (result.failed()) {
						logger.info("Unable to get a result ({}), accept by default", result.cause().getMessage());
						req.response().setStatusCode(200).end();
					} else {
						boolean isAccepted = result.result().body();
						if (isAccepted) {
							req.response().setStatusCode(200).end();
						} else {
							logger.warn("Refusing for {}", payload);
							req.response().setStatusCode(403).end();
						}
					}

				}));
	}

	private Handler<HttpServerRequest> validateAppendSize() {
		DeliveryOptions opts = new DeliveryOptions().setSendTimeout(3000);

		return request -> {
			HttpServerRequest req = Requests.wrap(request);
			Requests.tag(req, "method", "append.size");
			req.bodyHandler(payload -> {
				Long tmpSize = DefaultValues.MAX_SIZE;
				try {
					tmpSize = new JsonObject(payload).getLong("size", DefaultValues.MAX_SIZE);
				} catch (Exception e) {
					logger.error("Failed to handle '{}'", payload);
				}
				final Long reqSize = tmpSize;
				Requests.tag(req, "size", Long.toString(reqSize));
				vertx.eventBus().request(SdsAddresses.SIZE_VALIDATION, reqSize, opts,
						(AsyncResult<Message<Boolean>> result) -> {
							String msg = null;
							boolean accept = true;
							if (result.failed()) {
								logger.warn(
										"Unable to get a size-check result for {}byte(s) ({}), accepting if less than {}MB.",
										reqSize, result.cause().getMessage(), DefaultValues.MAX_SIZE_MB);
								accept = reqSize > 0 && reqSize <= DefaultValues.MAX_SIZE;
							} else {
								accept = result.result().body();
								if (!accept) {
									logger.warn("Refusing for {} MB ({} bytes)", reqSize / 1024 / 1024, reqSize);
									msg = "append of " + reqSize + "byte(s) rejected, too big";
								}
							}
							HttpServerResponse resp = req.response();
							if (msg != null) {
								resp.setStatusMessage(msg);
							}
							if (accept) {
								resp.setStatusCode(200).end();
							} else {
								// cyrus-sds-dispatch curl call has a 5sec timeout
								Requests.tagAsync(req);
								vertx.setTimer(2500, tid -> {
									resp.setStatusCode(400).end();
								});
							}

						});
			});

		};

	}

}
