/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.central.reverse.proxy.vertx.impl;

import java.util.List;
import java.util.function.BiConsumer;

import net.bluemind.central.reverse.proxy.vertx.HttpServerRequestContext;
import net.bluemind.central.reverse.proxy.vertx.ProxyResponse;

public class CompositeResponseHook {

	private CompositeResponseHook() {

	}

	public static BiConsumer<HttpServerRequestContext, ProxyResponse> of(
			List<BiConsumer<HttpServerRequestContext, ProxyResponse>> consumers) {
		return (reqCtx, proxyResp) -> {
			for (BiConsumer<HttpServerRequestContext, ProxyResponse> cons : consumers) {
				cons.accept(reqCtx, proxyResp);
			}
		};

	}

}
