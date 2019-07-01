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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.vertx.java.core.Vertx;

public class DirectClientProxy<S, T> extends BasicClientProxy<S, T> {

	private static final Map<Vertx, RestRootHandler> handlers = new ConcurrentHashMap<>();

	private DirectClientProxy(Vertx vx, Class<S> api, Class<T> asyncApi) {
		super(handlers.computeIfAbsent(vx, v -> new RestRootHandler(v, true)), api, asyncApi);
	}

	public static <S, T> DirectClientProxy<S, T> create(Vertx v, final Class<S> api, Class<T> asyncApi) {
		return new DirectClientProxy<>(v, api, asyncApi);
	}

}
