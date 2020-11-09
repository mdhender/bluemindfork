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
package net.bluemind.webmodule.server.filters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.JsEntry;
import net.bluemind.webmodule.server.NeedWebModules;
import net.bluemind.webmodule.server.PreEncodedObject;
import net.bluemind.webmodule.server.WebModule;

public class WebModuleResourcesFilter implements IWebFilter, NeedWebModules {
	private Map<String, WebModule> modules;

	private static final Logger logger = LoggerFactory.getLogger(WebModuleResourcesFilter.class);
	private static final Map<String, PreEncodedObject> resCache = new ConcurrentHashMap<>();

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request) {
		String path = request.path();
		if (!path.endsWith("module-webresources")) {
			return CompletableFuture.completedFuture(request);
		}

		String lang = request.headers().get("BMLang");
		String moduleName = request.params().get("module");
		WebModule module = modules.get(moduleName);
		if (module == null) {
			request.response().setStatusCode(404).end("module " + moduleName + " not found");
			return CompletableFuture.completedFuture(null);
		}
		String cacheKey = lang + ":" + moduleName;
		PreEncodedObject modJs = resCache.computeIfAbsent(cacheKey, k -> {
			JsonObject ret = new JsonObject();
			ret.put("css", new JsonArray(module.css));

			JsonArray js = new JsonArray();
			for (JsEntry jEntry : module.js) {
				JsonObject entry = new JsonObject();
				entry.put("bundle", jEntry.getBundle());
				entry.put("path", jEntry.getTranslation(lang).path);
				entry.put("lifecycle", jEntry.lifecycle);
				js.add(entry);
			}
			ret.put("js", js);
			return new PreEncodedObject(ret);
		});
		logger.debug("resFilter for {}", cacheKey);
		request.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON).setStatusCode(200)
				.end(modJs.buffer());
		return CompletableFuture.completedFuture(null);

	}

	@Override
	public void setModules(List<WebModule> webm) {
		modules = new HashMap<>();
		resCache.clear();
		for (WebModule module : webm) {
			modules.put(module.root, module);
		}
	}
}
