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
package net.bluemind.vertx.common.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;

import io.vertx.core.http.HttpServerRequest;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.vertx.common.request.impl.WrappedRequest;

/**
 * Helper to set request attributes that will logged when the response is sent
 * to the device.
 * 
 * Requests wrapped will this helper will record execution times to provide
 * useful logs on completion.
 */
public final class Requests {

	private static final Logger logger = LoggerFactory.getLogger(Requests.class);

	/**
	 * Wraps a request into a {@link WrappedRequest} that will store tags & log them
	 * with execution time at the end.
	 * 
	 * @param impl
	 * @return
	 */
	public static HttpServerRequest wrap(HttpServerRequest impl) {
		return wrap(null, impl);
	}

	public static HttpServerRequest wrap(String metricName, HttpServerRequest impl) {
		Registry registry = MetricsRegistry.get();
		IdFactory idFactory = new IdFactory(metricName, registry, Requests.class);
		return WrappedRequest.create(registry, idFactory, impl);
	}

	/**
	 * Sets a log attribute, displayed when the response is sent.
	 * 
	 * @param sr
	 * @param tag
	 * @param value
	 */
	public static void tag(HttpServerRequest r, String tag, String value) {
		HttpServerRequest sr = Unwrapper.unwrap(r);
		if (sr instanceof WrappedRequest) {
			WrappedRequest wr = (WrappedRequest) sr;
			wr.putLogAttribute(tag, value);
		} else {
			logger.warn("Not a wrapped request {}", sr, new Throwable("call loc"));
		}
	}

	public static void tagAsync(HttpServerRequest sr) {
		tag(sr, "async", "true");
	}

	public static String tag(HttpServerRequest r, String tag) {
		HttpServerRequest sr = Unwrapper.unwrap(r);
		if (sr instanceof WrappedRequest) {
			WrappedRequest wr = (WrappedRequest) sr;
			return wr.logAttribute(tag);
		} else {
			logger.warn("Not a wrapped request {}", sr, new Throwable("call loc"));
			return null;
		}
	}

}
