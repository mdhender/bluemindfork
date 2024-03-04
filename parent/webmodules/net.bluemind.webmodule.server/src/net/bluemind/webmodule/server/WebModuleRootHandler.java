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
package net.bluemind.webmodule.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.lib.vertx.RouteMatcher;
import net.bluemind.webmodule.server.handlers.MaintenanceHandler;

public final class WebModuleRootHandler implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(WebModuleRootHandler.class);

	private final RouteMatcher modulesRouter;

	private final List<WebModule> modules;
	private final List<IWebFilter> filters;
	private final Supplier<WebserverConfiguration> conf;
	private final MaintenanceHandler maintenanceHandler;

	private final Vertx vertx;

	private WebModuleRootHandler(Vertx vertx, List<WebModule> roots, List<IWebFilter> filters,
			Supplier<WebserverConfiguration> conf) {
		this.vertx = vertx;
		this.modulesRouter = new RouteMatcher(vertx);
		logger.debug("modules {}, filters {}", roots.size(), filters.size());
		modules = new ArrayList<>(roots);
		this.filters = filters;
		// shorter is last
		Collections.sort(modules, (WebModule o1, WebModule o2) -> o2.root.length() - o1.root.length());

		for (WebModule module : modules) {
			modulesRouter.allWithRegEx(module.root + ".*", moduleHandler(module));
		}

		maintenanceHandler = new MaintenanceHandler(roots.stream().filter(r -> r.noMaintenance).map(r -> r.root)
				.map(r -> r.endsWith("/") ? r : r + "/").collect(Collectors.toSet()));

		this.conf = conf;
	}

	private Handler<HttpServerRequest> moduleHandler(final WebModule module) {
		return event -> handleModule(event, module);
	}

	protected void handleModule(HttpServerRequest request, WebModule module) {
		String path = request.path();
		String relativeUri = null;
		if (module.root.length() >= path.length()) {
			relativeUri = module.index;
		} else {
			relativeUri = path.substring(module.root.endsWith("/") ? module.root.length() : module.root.length() + 1);
		}

		logger.debug("handle {} request [{}] => module [{}], relative path [{}]", request.method(), path, module.root,
				relativeUri);

		Handler<HttpServerRequest> handler = module.handlers.get(relativeUri);
		if (handler == null) {
			/*
			 * Wildcard handler. Warning: the wildcard handler will be called on every
			 * single request, the module handler is responsible for handling static files
			 */
			handler = module.handlers.get("*");
		}
		if (handler != null) {
			// dynamic handler
			try {
				handler.handle(request);
			} catch (Exception e) {
				onError(request, e);
			}

		} else {
			// statics
			try {
				module.defaultHandler.handle(request);
			} catch (Exception e) {
				// 404
				logger.error("error during serving request", e);
				notFound(request);
			}
		}
	}

	private void notFound(HttpServerRequest request) {
		request.response().setStatusCode(404);
		request.response().end();
	}

	@Override
	public void handle(final HttpServerRequest request) {
		Handler<Throwable> error = (Throwable t) -> onError(request, t);
		request.exceptionHandler(error);
		vertx.getOrCreateContext().exceptionHandler(error);

		maintenanceHandler.handle(request)
				.orElseGet(() -> searchFilters(request, CompletableFuture.completedFuture(request)))
				.whenComplete((completedRequest, ex) -> {
					if (completedRequest == null) {
						return;
					}
					try {
						modulesRouter.handle(completedRequest);
					} catch (Exception t) {
						onError(completedRequest, t);
					}
				});
	}

	private CompletableFuture<HttpServerRequest> searchFilters(HttpServerRequest request,
			CompletableFuture<HttpServerRequest> root) {
		for (IWebFilter filter : filters) {
			root = root.thenCompose(req -> {
				if (req == null) {
					return CompletableFuture.completedFuture(null);
				}
				return filter.filter(req, conf.get());
			}).exceptionally(e -> {
				onError(request, e);
				return null;
			});
		}

		return root;
	}

	private void onError(HttpServerRequest request, Throwable t) {
		String path = request.path();
		if (request.response().ended()) {
			logger.warn("[{}] Skipping reponse to ended request ({})", path, t.getMessage());
			return;
		}
		logger.error("error during handling request: {}", path, t);

		request.response().setStatusCode(500);
		request.response().setStatusMessage("server error: " + t.getMessage());
		request.response().end();
	}

	public static WebModuleRootHandler build(Vertx vertx) {
		HttpClient httpClient = vertx.createHttpClient();
		List<WebModule> roots = WebModuleResolver.build(vertx, httpClient, WebModuleServerActivator.getModules());

		// load filters
		List<IWebFilter> filters = WebModuleServerActivator.getFilters();

		for (IWebFilter filter : filters) {
			if (filter instanceof NeedVertx f) {
				f.setVertx(vertx);
			}

			if (filter instanceof NeedWebModules f) {
				f.setModules(roots);
			}
		}

		for (WebModule m : roots) {
			for (Handler<HttpServerRequest> handler : m.handlers.values()) {
				if (handler instanceof NeedVertx f) {
					f.setVertx(vertx);
				}

			}
		}

		Supplier<WebserverConfiguration> conf = WebModuleServerActivator.getConf();

		return new WebModuleRootHandler(vertx, roots, filters, conf);
	}
}
