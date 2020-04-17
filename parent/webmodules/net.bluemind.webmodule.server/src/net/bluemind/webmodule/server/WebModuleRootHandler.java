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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.lib.vertx.RouteMatcher;

public final class WebModuleRootHandler implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(WebModuleRootHandler.class);

	private final RouteMatcher modulesRouter;

	private List<WebModule> modules;
	private List<IWebFilter> filters;

	@SuppressWarnings("unused")
	private Vertx vertx;

	public WebModuleRootHandler(Vertx vertx, List<WebModule> roots, List<IWebFilter> filters) {
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
		Handler<Throwable> error = (Throwable t) -> {
			onError(request, t);
		};
		request.exceptionHandler(error);
		vertx.getOrCreateContext().exceptionHandler(error);

		CompletableFuture<HttpServerRequest> root = CompletableFuture.completedFuture(request);

		for (IWebFilter filter : filters) {
			root = root.thenCompose(req -> {
				if (req == null) {
					return CompletableFuture.completedFuture(null);
				}
				return filter.filter(req);
			}).exceptionally(e -> {
				onError(request, e);
				return null;
			});
		}

		root.whenComplete((completedRequest, ex) -> {
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

	private void onError(HttpServerRequest request, Throwable t) {
		String path = request.path();
		logger.error("error during handling request: {} {}", path, t);
		request.response().setStatusCode(500);
		request.response().setStatusMessage("server error: " + t.getMessage());
		request.response().end();
	}

	public static WebModuleRootHandler build(Vertx vertx) {
		List<WebModule> roots = WebModuleResolver.build(vertx, WebModuleServerActivator.getModules());

		// load filters
		List<IWebFilter> filters = WebModuleServerActivator.getFilters();

		for (IWebFilter filter : filters) {
			if (filter instanceof NeedVertx) {
				((NeedVertx) filter).setVertx(vertx);
			}

			if (filter instanceof NeedWebModules) {
				((NeedWebModules) filter).setModules(roots);
			}
		}

		for (WebModule m : roots) {
			for (Handler<HttpServerRequest> handler : m.handlers.values()) {
				if (handler instanceof NeedVertx) {
					((NeedVertx) handler).setVertx(vertx);
				}

			}
		}

		return new WebModuleRootHandler(vertx, roots, filters);
	}
}
