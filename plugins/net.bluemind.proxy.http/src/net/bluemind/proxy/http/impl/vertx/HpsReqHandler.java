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
package net.bluemind.proxy.http.impl.vertx;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.lib.vertx.RouteMatcher;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer;
import net.bluemind.proxy.http.auth.impl.Enforcers;
import net.bluemind.proxy.http.config.ForwardedLocation;
import net.bluemind.proxy.http.config.HPSConfiguration;
import net.bluemind.proxy.http.impl.SessionStore;

public final class HpsReqHandler implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(HpsReqHandler.class);
	private final RouteMatcher rm;
	private static final NoMatch noMatch = new NoMatch();
	private static final FaviconHandler favicon = new FaviconHandler();
	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory(MetricsRegistry.get(), HpsReqHandler.class);

	public HpsReqHandler(Vertx vertx, HPSConfiguration conf, SessionStore ss, CoreState coreState) {
		rm = new RouteMatcher(vertx);
		List<IAuthEnforcer> enforcers = Enforcers.enforcers(vertx);
		for (ForwardedLocation fl : conf.getForwardedLocations()) {
			ProtectedLocationHandler plh = new ProtectedLocationHandler(vertx, enforcers, fl, ss, coreState);
			rm.regex(fl.getPathPrefix(), plh);
			if (!fl.getPathPrefix().endsWith("/")) {
				rm.regex(fl.getPathPrefix() + "/.*", plh);
			} else if (fl.getPathPrefix().equals("/")) {
				rm.regex("/[^/]+", plh);
			}
		}
		rm.regex("/maintenance/.*", new MaintenanceRequestHandler(coreState));
		rm.noMatch(noMatch);
	}

	@Override
	public void handle(HttpServerRequest event) {
		event.exceptionHandler(e -> {
			registry.counter(idFactory.name("requestsCount", "status", "500"));
			logger.error("unhandled exception for request " + event.uri(), e);
			event.response().setStatusCode(500);
			event.response().end();
		});
		String uri = event.uri();
		if (uri.endsWith("/favicon.ico")) {
			favicon.handle(event);
		} else {
			registry.counter(idFactory.name("requestsCount", "status", "all"));
			logger.debug("handle {}", uri);
			rm.handle(event);
		}
	}

}
