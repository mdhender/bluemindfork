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
package net.bluemind.core.rest.http.vertx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;

public class HttpRoutes {

	private static final Logger logger = LoggerFactory.getLogger(HttpRoutes.class);

	public static void bindRoutes(Vertx vertx, ExecutorService exec, RouteMatcher matcher) {
		for (HttpRoute r : loadRoutes(vertx, exec)) {
			r.bind(matcher);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<HttpRoute> loadRoutes(Vertx vertx, ExecutorService exec) {
		IExtensionRegistry er = RegistryFactory.getRegistry();

		IExtensionPoint point = er.getExtensionPoint("net.bluemind.core.rest.http.vertx", "httpRoute");
		if (point == null) {
			logger.error("extensionPoint net.bluemind.core.rest.apiEndpoint not found");
			return Collections.emptyList();
		}

		IExtension[] extensions = point.getExtensions();
		List<HttpRoute> ret = new ArrayList<>(extensions.length);
		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {

				Handler<HttpServerRequest> handler = null;
				try {
					handler = (Handler<HttpServerRequest>) e.createExecutableExtension("handler");
				} catch (CoreException ce) {
					logger.error("error during loading extension {} {}", ie.getExtensionPointUniqueIdentifier(), ce);
					continue;
				}
				if (handler instanceof NeedVertx) {
					NeedVertx needsVertx = (NeedVertx) handler;
					needsVertx.setVertx(vertx);
				}
				if (handler instanceof NeedVertxExecutor) {
					NeedVertxExecutor needsVertx = (NeedVertxExecutor) handler;
					needsVertx.setVertxExecutor(vertx, exec);
				}

				Set<String> verbs = new HashSet<>();
				if (e.getChildren() != null) {
					for (IConfigurationElement verbElement : e.getChildren()) {
						verbs.add(verbElement.getAttribute("value"));
					}
				}

				String path = e.getAttribute("path");
				if (verbs.isEmpty()) {
					verbs.addAll(Arrays.asList("POST", "GET", "PUT", "DELETE"));
				}

				HttpRoute route = new HttpRoute(handler, path, verbs);

				ret.add(route);
			}
		}
		return ret;
	}
}
