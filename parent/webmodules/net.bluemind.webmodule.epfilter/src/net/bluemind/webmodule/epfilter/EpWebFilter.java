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
package net.bluemind.webmodule.epfilter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.WebExtensionsResolver;

public class EpWebFilter implements IWebFilter {

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request) {
		String path = request.path();
		if (path.endsWith("uiextension")) {
			String lang = request.headers().get("BMLang");
			String module = request.params().get("module");
			try {
				JsonObject eps = new WebExtensionsResolver(lang, module).loadExtensions();
				request.response().putHeader("Content-type", "application/json; charset=utf-8");
				request.response().setStatusCode(200).end(eps.encode());
			} catch (IOException e) {
				request.response().setStatusCode(500).end();
			}

			return CompletableFuture.completedFuture(null);
		} else {
			return CompletableFuture.completedFuture(request);
		}
	}
}
