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
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;

public final class WebModuleRootHandler implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(WebModuleRootHandler.class);

	private RouteMatcher modulesRouter = new RouteMatcher();

	private List<WebModule> modules;

	private List<IWebFilter> filters;

	@SuppressWarnings("unused")
	private Vertx vertx;

	public WebModuleRootHandler(Vertx vertx, List<WebModule> roots, List<IWebFilter> filters) {
		this.vertx = vertx;
		logger.debug("modules {}, filters {}", roots.size(), filters.size());
		modules = new ArrayList<>(roots);
		this.filters = filters;
		// shorter is last
		Collections.sort(modules, new Comparator<WebModule>() {

			@Override
			public int compare(WebModule o1, WebModule o2) {
				return o2.root.length() - o1.root.length();
			}
		});

		for (WebModule module : modules) {
			modulesRouter.allWithRegEx(module.root + ".*", moduleHandler(module));
		}
	}

	private Handler<HttpServerRequest> moduleHandler(final WebModule module) {
		return new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest event) {
				handleModule(event, module);
			}
		};
	}

	protected void handleModule(HttpServerRequest request, WebModule module) {
		String path = request.path();
		String relativeUri = null;
		if (module.root.length() >= path.length()) {
			relativeUri = module.index;
		} else {
			relativeUri = path.substring(module.root.length() + 1);
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
		request.exceptionHandler(new Handler<Throwable>() {

			@Override
			public void handle(Throwable t) {
				onError(request, t);
			}
		});
		String path = request.path();
		logger.debug("handle {} request [{}]", request.method(), path);
		HttpServerRequest fRequest = request;
		for (IWebFilter filter : filters) {
			try {
				fRequest = filter.filter(fRequest);
			} catch (Exception e) {
				onError(request, e);
				return;
			}
			if (fRequest == null) {
				logger.debug("request [{}] ended by filter {}", path, filter);
				return;
			}
		}

		logger.debug("handle {} request [{}] => modules router", request.method(), path);
		try {
			modulesRouter.handle(fRequest);
		} catch (Exception t) {
			onError(request, t);
		}
	}

	private void onError(HttpServerRequest request, Throwable t) {
		logger.error("error during handling request: " + request.path(), t);
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

		WebModuleRootHandler rootHandler = new WebModuleRootHandler(vertx, roots, filters);
		return rootHandler;
	}
}
