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
package net.bluemind.eas.http.internal;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.EasUrls;
import net.bluemind.eas.http.IEasRequestEndpoint;
import net.bluemind.lib.vertx.RouteMatcher;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.vertx.common.http.BasicAuthHandler;
import net.bluemind.vertx.common.request.Requests;

public final class EASRouter implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(EASRouter.class);

	private final RouteMatcher rm;

	private static final ConcurrentHashMap<String, IEasRequestEndpoint> endpoints;
	private static final AtomicLong requestId = new AtomicLong();

	static {
		List<IEasRequestEndpoint> points = Endpoints.get();
		endpoints = new ConcurrentHashMap<>();
		for (IEasRequestEndpoint point : points) {
			if (point.acceptsVersion(12.1)) {
				for (String cmd : point.supportedCommands()) {
					logger.info("Registering eas v12 {} endpoint {}", cmd, point);
					endpoints.put("12." + cmd, point);
				}
			}
			if (point.acceptsVersion(14.1)) {
				for (String cmd : point.supportedCommands()) {
					logger.info("Registering eas v14 {} endpoint {}", cmd, point);
					endpoints.put("14." + cmd, point);
				}
			}
			if (point.acceptsVersion(16.1)) {
				for (String cmd : point.supportedCommands()) {
					logger.info("Registering eas v16 {} endpoint {}", cmd, point);
					endpoints.put("16." + cmd, point);
				}
			}
		}
	}

	public EASRouter(Vertx vertx) {
		rm = new RouteMatcher(vertx);
		rm.noMatch(new NoMatch());
		rm.get(EasUrls.ROOT, new BrokenGet());
		Handler<HttpServerRequest> optionsChain = nonValidatingQueryHandler(vertx, new OptionsHandler());
		Handler<HttpServerRequest> postChain = validatingQueryHandler(vertx, postHandler());
		rm.options(EasUrls.ROOT, optionsChain);
		rm.post(EasUrls.ROOT, postChain);
		rm.options(EasUrls.ROOT + "/", optionsChain);
		rm.post(EasUrls.ROOT + "/", postChain);
	}

	private Handler<AuthorizedDeviceQuery> postHandler() {
		return event -> {
			Requests.tagUserLogin(event.request(), event.loginAtDomain());
			final String pointKey = "" + ((int) event.protocolVersion()) + "." + event.command();
			IEasRequestEndpoint ep = endpoints.get(pointKey);
			if (ep != null) {
				try {
					ep.handle(event);
				} catch (Exception t) {
					handlerException(event.request(), t);
				}
			} else {
				event.request().endHandler(v -> {
					logger.warn("Missing endpoint for point key: {}", pointKey);
					HttpServerResponse resp = event.request().response();
					resp.setStatusCode(500).setStatusMessage("Not implemented").end();
				});
			}
		};
	}

	@Override
	public void handle(final HttpServerRequest event) {
		if (logger.isDebugEnabled()) {
			logger.debug("request {}\n{}", event.absoluteURI(), event.headers());
		}
		final HttpServerRequest wrapped = Requests.wrap(event);
		Requests.tag(wrapped, "m", event.method().name());
		Requests.tag(wrapped, "rid", Long.toString(requestId.incrementAndGet()));
		Requests.tag(wrapped, "ua", event.headers().get("User-Agent"));
		if (!event.headers().contains("Authorization") && event.method() == HttpMethod.POST) {
			// iOS sends command with no Authorization header
			String cmd = event.getParam("Cmd");
			if (!Strings.isNullOrEmpty(cmd)) {
				Requests.tag(wrapped, "cmd", cmd);
			}
			Requests.tag(wrapped, "auth", "none");
		}

		wrapped.exceptionHandler(t -> handlerException(event, t));
		try {
			rm.handle(wrapped);
		} catch (Exception t) {
			handlerException(event, t);
		}
	}

	private void handlerException(final HttpServerRequest event, Throwable t) {
		logger.error("******** {}", t.getMessage(), t);
		HttpServerResponse resp = event.response();
		resp.setStatusCode(500).setStatusMessage(t.getMessage() != null ? t.getMessage() : "null").end();
	}

	private Handler<HttpServerRequest> validatingQueryHandler(Vertx vertx, Handler<AuthorizedDeviceQuery> next) {
		return new BasicAuthHandler(vertx, "bm-eas", BasicRoles.ROLE_EAS, new EASQueryDecoder(new ApplyFiltersHandler(
				new DeviceValidationHandler(vertx, new AuthorizedDevicesFiltersHandler(next)))));
	}

	private Handler<HttpServerRequest> nonValidatingQueryHandler(Vertx vertx,
			final Handler<AuthorizedDeviceQuery> next) {
		return new BasicAuthHandler(vertx, "bm-eas", BasicRoles.ROLE_EAS,
				new EASQueryDecoder(new ApplyFiltersHandler(event -> {
					if (logger.isDebugEnabled()) {
						logger.debug("[{}] no validation required.", event.loginAtDomain());
					}
					next.handle(new AuthorizedDeviceQuery(vertx, event, null));
				})));
	}

}
