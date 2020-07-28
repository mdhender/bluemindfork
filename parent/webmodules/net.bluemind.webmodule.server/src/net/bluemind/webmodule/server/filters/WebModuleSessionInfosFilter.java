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

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.api.BMVersion;
import net.bluemind.webmodule.server.IWebFilter;

public class WebModuleSessionInfosFilter implements IWebFilter {
	private static final Logger logger = LoggerFactory.getLogger(WebModuleSessionInfosFilter.class);

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request) {
		if (isSessionInfos(request)) {
			return sessionInfos(request);
		}
		return CompletableFuture.completedFuture(request);
	}

	private boolean isSessionInfos(HttpServerRequest request) {
		String path = request.path();
		return path.endsWith("session-infos.js") || path.endsWith("session-infos");
	}

	private CompletableFuture<HttpServerRequest> sessionInfos(HttpServerRequest request) {
		List<Entry<String, String>> headers = request.headers().entries();
		Map<String, Object> model = loadModel(headers);
		StringWriter stringWriter = new StringWriter();

		model.put("version", BMVersion.getVersion());
		model.put("brandVersion", BMVersion.getVersionName());

		try {
			Configuration configuration = new Configuration(Configuration.VERSION_2_3_30);
			configuration.setClassForTemplateLoading(WebModuleSessionInfosFilter.class, "/templates");
			configuration.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
			Template template = configuration
					.getTemplate(isApplicationJson(request) ? "jsonSessionInfos.ftl" : "jsSessionInfos.ftl");
			template.process(model, stringWriter);
		} catch (TemplateException | IOException e) {
			logger.error("error during template generation", e);
		}

		String contentType = isApplicationJson(request) ? "application/json" : "application/javascript";
		request.response().putHeader("Content-type", contentType + "; charset=utf-8");
		if (!isApplicationJson(request)) {
			request.response().putHeader("X-API-Deprecation-Info", "prefer /session-infos -H accept: application/json");
		}
		request.response().setStatusCode(200).end(stringWriter.toString());
		return CompletableFuture.completedFuture(null);
	}

	private boolean isApplicationJson(HttpServerRequest request) {
		String accept = request.getHeader(HttpHeaders.ACCEPT);
		return accept.toLowerCase().contains("application/json") || request.path().endsWith("session-infos");
	}

	protected Map<String, Object> loadModel(List<Entry<String, String>> entries) {
		Map<String, Object> model = new HashMap<>();
		for (Entry<String, String> entry : entries) {
			if (entry.getKey().equals("BMUserFirstName") || entry.getKey().equals("BMUserLastName")
					|| entry.getKey().equals("BMUserFormatedName")) {
				String value = entry.getValue();
				if (value != null) {
					model.put(entry.getKey(), new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8));
				} else {
					model.put(entry.getKey(), entry.getValue());
				}
			} else {
				model.put(entry.getKey(), entry.getValue());
			}
		}
		return model;
	}

}
