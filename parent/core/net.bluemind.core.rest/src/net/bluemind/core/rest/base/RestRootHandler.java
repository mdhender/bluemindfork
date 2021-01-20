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
package net.bluemind.core.rest.base;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.Endpoints;
import net.bluemind.core.rest.EventBusAccessRules;
import net.bluemind.core.rest.IEventBusAccessRule;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.filter.IRestFilter;
import net.bluemind.core.rest.log.CallLogger;
import net.bluemind.core.rest.model.RestService;
import net.bluemind.core.rest.model.RestServiceApiDescriptor.MethodDescriptor;
import net.bluemind.core.rest.vertx.VertxStream.LocalPathStream;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.BMExecutor;
import net.bluemind.lib.vertx.BMExecutor.BMTask;
import net.bluemind.lib.vertx.BMExecutor.BMTaskMonitor;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class RestRootHandler implements IRestCallHandler, IRestBusHandler {
	private static final Logger logger = LoggerFactory.getLogger(RestRootHandler.class);
	private final Vertx vertx;
	private final List<IEventBusAccessRule> rules;
	private final List<IRestFilter> filters;
	private final boolean directExec;

	private static final BMExecutor executor = ExecutorHolder.get();

	private static class MetricsHolder {
		final Registry registry;
		final IdFactory idFactory;
		final Counter countSuccess;
		final Timer handlingTimer;
		final Counter countFail;

		private MetricsHolder() {
			registry = MetricsRegistry.get();
			idFactory = new IdFactory(MetricsRegistry.get(), RestRootHandler.class);
			this.countSuccess = registry.counter(idFactory.name("callsCount", "status", "success"));
			this.countFail = registry.counter(idFactory.name("callsCount", "status", "failure"));
			this.handlingTimer = registry.timer(idFactory.name("handlingDuration"));
		}
	}

	private static final MetricsHolder metrics = new MetricsHolder();

	public RestRootHandler(Vertx vertx) {
		this(vertx, false);
	}

	public RestRootHandler(Vertx vertx, boolean directExec) {
		this.vertx = vertx;
		this.directExec = directExec;
		this.filters = new RunnableExtensionLoader<IRestFilter>().loadExtensions("net.bluemind.core.rest", "filter",
				"filter", "class");

		rules = EventBusAccessRules.getInstance().getEventBusRules();
		for (RestService service : Endpoints.getEndpoints()) {
			for (MethodDescriptor m : service.descriptor.methods) {
				TreePathNode rootNode = pathsByMethod.get(HttpMethod.valueOf(m.httpMethodName));
				rootNode.insert(m.path, new TreePathLeaf(RestServiceMethodHandler.getInstance(service, m, filters)));
			}

		}

	}

	public ExecutorService executor() {
		return executor.asExecutorService();
	}

	@Override
	public void call(final RestRequest request, AsyncHandler<RestResponse> responseHandler) {

		for (IRestFilter filter : filters) {
			responseHandler = filter.preAuthorization(request, responseHandler);
			if (responseHandler == null) {
				return;
			}
		}

		doCall(request, responseHandler);
	}

	private void doCall(RestRequest request, AsyncHandler<RestResponse> rh) {
		final long start = metrics.registry.clock().monotonicTime();
		TreePathNode rootNode = pathsByMethod.get(request.method);
		final TreePathLeaf leaf = rootNode.leaf(request.path);
		if (leaf == null) {
			rh.failure(new ServerFault("no service registered on path " + request.path, ErrorCode.NOT_FOUND));
			return;
		}
		logger.debug("receive request {}", request);

		AsyncHandler<RestResponse> wrappedHandler = new AsyncHandler<RestResponse>() {

			@Override
			public void success(RestResponse value) {
				rh.success(value);
				long elapsed = metrics.registry.clock().monotonicTime() - start;
				vertx.executeBlocking(prom -> {
					metrics.countSuccess.increment();
					metrics.registry
							.counter(metrics.idFactory.name("callsByRPC", "status", "success", "rpc", leaf.name()))
							.increment();
					metrics.handlingTimer.record(elapsed, TimeUnit.NANOSECONDS);
					prom.complete();
				}, false, ar -> {

				});
			}

			@Override
			public void failure(Throwable e) {
				metrics.countFail.increment();
			}

		};

		RestCallRunnable r = new RestCallRunnable(request, wrappedHandler, leaf);
		if (!directExec) {
			executor.execute(r);
		} else {
			executor.executeDirect(r);
		}
	}

	private final class VertxAwareAsyncHandler implements AsyncHandler<RestResponse> {
		private final AsyncHandler<RestResponse> response;
		private BMTaskMonitor monitor;

		public VertxAwareAsyncHandler(AsyncHandler<RestResponse> responseHandler) {
			this.response = responseHandler;
		}

		public void setMonitor(BMTaskMonitor monitor) {
			this.monitor = monitor;
		}

		@Override
		public void success(RestResponse value) {
			if (vertx != null) {
				vertx.runOnContext((Void event) -> {
					if (value.responseStream != null && monitor != null) {
						value.responseStream = new MonitoredReadStream(value.responseStream, monitor);
					}
					response.success(value);
				});
			} else {
				response.success(value);
			}
		}

		@Override
		public void failure(Throwable e) {
			if (vertx != null) {
				vertx.runOnContext((Void event) -> response.failure(e));
			} else {
				response.failure(e);
			}
		}

	}

	private final class UniqueResponseAsyncHandler implements AsyncHandler<RestResponse> {
		final VertxAwareAsyncHandler response;
		private boolean done = false;

		public UniqueResponseAsyncHandler(VertxAwareAsyncHandler responseHandler) {
			this.response = responseHandler;
		}

		@Override
		public void success(RestResponse value) {
			if (!done) {
				done = true;
				response.success(value);
			} else {
				logger.warn("task is finished but had timeout-ed");
			}
		}

		@Override
		public void failure(Throwable e) {
			if (!done) {
				done = true;
				response.failure(e);
			}
		}

	}

	private final class RestCallRunnable implements BMTask {
		private final RestRequest request;
		private final TreePathLeaf leaf;
		private final UniqueResponseAsyncHandler response;
		private final long creationTime;

		public RestCallRunnable(RestRequest request, AsyncHandler<RestResponse> response, TreePathLeaf leaf) {
			this.request = request;
			this.leaf = leaf;
			this.response = new UniqueResponseAsyncHandler(new VertxAwareAsyncHandler(response));
			this.creationTime = System.currentTimeMillis();
		}

		public void run(BMTaskMonitor monitor) {
			long time = System.currentTimeMillis() - creationTime;
			if (time > 500) {
				CallLogger.logger.warn("{} call {} {} took {}ms to start", "BM-Core", request.method, request.path,
						System.currentTimeMillis() - creationTime);
			} else {
				CallLogger.logger.trace("{} call {} {} took {}ms to start", "BM-Core", request.method, request.path,
						System.currentTimeMillis() - creationTime);
			}
			logger.debug("do call: request {}", request);

			if (request.bodyStream != null) {
				if (request.bodyStream instanceof LocalPathStream) {
					logger.debug("Not wrapping {}", request.bodyStream);
				} else {
					request.bodyStream = new MonitoredReadStream(request.bodyStream, monitor);
				}
			}

			this.response.response.setMonitor(monitor);

			leaf.call(request, response);

		}

		@Override
		public void cancelled() {
			TimeoutException e = new TimeoutException("timeout on request execution");
			logger.error("Error during restcall {}:{}", request, e.getMessage());
			response.failure(e);
		}

		@Override
		public String toString() {
			return "Handling request " + request.toString();

		}
	}

	private Map<HttpMethod, TreePathNode> pathsByMethod = new EnumMap<>(
			new ImmutableMap.Builder<HttpMethod, TreePathNode>().put(HttpMethod.GET, new TreePathNode())//
					.put(HttpMethod.POST, new TreePathNode()) //
					.put(HttpMethod.PUT, new TreePathNode()) //
					.put(HttpMethod.DELETE, new TreePathNode()) //
					.build());

	public static final class TreePathNode {
		public final Map<String, TreePathNode> childrens = new HashMap<>();
		public final Map<String, TreePathLeaf> leaves = new HashMap<>();
		private static final String MAGIC = "/_";

		public TreePathLeaf leaf(String path) {
			if (path.length() > 0 && path.charAt(1) == '/') {
				return leaf(path.substring(1));
			}

			int idx = path.indexOf('/', 1);
			if (idx > 0) {
				String currentPath = path.substring(0, idx);
				TreePathNode child = childrens.get(currentPath);
				if (child == null) {
					// magic path
					child = childrens.get(MAGIC);
				}

				if (child == null) {
					return null;
				} else {
					return child.leaf(path.substring(idx));
				}
			} else {
				TreePathLeaf ret = leaves.get(path);
				if (ret == null) {
					ret = leaves.get(MAGIC);
				}
				return ret;
			}

		}

		public void insert(String path, TreePathLeaf leaf) {
			int idx = path.indexOf('/', 1);
			if (idx > 0) {
				String currentPath = path.substring(0, idx);
				currentPath = magicPath(currentPath);
				TreePathNode child = childrens.computeIfAbsent(currentPath, k -> new TreePathNode());

				child.insert(path.substring(idx), leaf);

			} else {
				String sub = magicPath(path);
				if (leaves.putIfAbsent(sub, leaf) != null && logger.isErrorEnabled()) {
					logger.error("path {} already taken for {}", path, leaf.name());
				}
			}
		}

		private String magicPath(String currentPath) {
			if (currentPath.startsWith("/{")) {
				return MAGIC;
			} else {
				return currentPath;
			}
		}

	}

	public static final class TreePathLeaf implements IRestCallHandler {

		private final IRestCallHandler handler;

		public TreePathLeaf(IRestCallHandler handler) {
			this.handler = handler;
		}

		@Override
		public String name() {
			return handler.name();
		}

		@Override
		public void call(RestRequest request, AsyncHandler<RestResponse> response) {
			handler.call(request, response);
		}

	}

	@Override
	public <T> Future<MessageConsumer<T>> register(RestRequest request, Supplier<Handler<Message<T>>> handlerSupplier,
			Handler<ServerFault> reject) {
		MessageConsumer<T> cons = vertx.eventBus().consumer(request.path);
		return applyRules(request, reject, () -> cons.handler(handlerSupplier.get())).map(succeed -> cons);
	}

	private Future<Boolean> applyRules(RestRequest request, Handler<ServerFault> reject, Runnable toApply) {
		Promise<Boolean> promise = Promise.promise();
		SecurityContext securityContext = null;
		try {
			securityContext = getSecurityContext(request);
		} catch (ServerFault e) {
			reject.handle(e);
			promise.complete(false);
			return promise.future();
		}
		SecurityContext ctx = securityContext;

		Context vertxCtx = Vertx.currentContext();
		for (IEventBusAccessRule rule : rules) {
			if (rule.match(request.path)) {
				vertxCtx.runOnContext(v -> {
					try {
						if (rule.authorize(ServerSideServiceProvider.getProvider(ctx).getContext(), request.path)) {
							toApply.run();
							promise.complete(true);
						} else {
							reject.handle(new ServerFault(String.format("path %s not accessible", request.path),
									ErrorCode.PERMISSION_DENIED));
							promise.complete(false);
						}
					} catch (Exception e) {
						logger.error("error during registring handler", e);
						reject.handle(new ServerFault(
								String.format("error %s accessing path %s", e.getMessage(), request.path),
								ErrorCode.UNKNOWN));
					}
				});
				return promise.future();
			}

		}

		reject.handle(
				new ServerFault(String.format("path %s not accessible", request.path), ErrorCode.PERMISSION_DENIED));

		promise.complete(false);
		return promise.future();
	}

	private SecurityContext getSecurityContext(RestRequest request) {
		SecurityContext securityContext = null;
		String key = request.headers.get("X-BM-ApiKey");
		if (key == null) {
			key = request.params.get("apikey");
		}

		logger.debug("handle request {} from {}) with key {}", request.path, request.remoteAddresses, key);

		if (key == null) {
			securityContext = SecurityContext.ANONYMOUS.from(request.remoteAddresses);
		} else {

			securityContext = Sessions.sessionContext(key);

			if (securityContext == null) {
				throw new ServerFault(String.format("session id %s is not valid", key), ErrorCode.PERMISSION_DENIED);

			}
		}
		return securityContext;
	}

	@Override
	public void sendEvent(RestRequest request, JsonObject evt) {
		logger.debug("send event to {} : {}", request.path, evt);
		applyRules(request, f -> logger.error("cannot send event to {}", request.path),
				() -> vertx.eventBus().send(request.path, evt));
	}

	@Override
	public void sendEvent(RestRequest request, JsonObject evt, Handler<Message<JsonObject>> handler) {
		logger.debug("send event to {} : {}", request.path, evt);
		applyRules(request, f -> {
			logger.error("cannot send event to {}", request.path);
			handler.handle(null);
		}, () -> vertx.eventBus().request(request.path, evt, new DeliveryOptions().setSendTimeout(10000),
				(AsyncResult<Message<JsonObject>> m) -> {
					if (m.failed()) {
						handler.handle(null);
					} else {
						handler.handle(m.result());
					}
				}));
	}

}
