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

	private Template jsTemplate;
	private Template jsonTemplate;

	public WebModuleSessionInfosFilter() {

		Configuration freemarkerCfg = new Configuration();
		freemarkerCfg.setClassForTemplateLoading(WebModuleSessionInfosFilter.class, "/templates");
		freemarkerCfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);

		try {
			jsTemplate = freemarkerCfg.getTemplate("jsSessionInfos.ftl");
			jsonTemplate = freemarkerCfg.getTemplate("jsonSessionInfos.ftl");

		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request) {
		String path = request.path();
		String accept = request.getHeader(HttpHeaders.ACCEPT);
		CompletableFuture<HttpServerRequest> response;
		if (!path.endsWith("session-infos.js")) {
			response = CompletableFuture.completedFuture(request);
		} else if (accept.toLowerCase().contains("application/json")) {
			response = responseWithSessionInfosJson(request);
		} else {
			response = responseWithSessionInfosJs(request);
		}
		return response;
	}

	private CompletableFuture<HttpServerRequest> responseWithSessionInfosJs(HttpServerRequest request) {
		return responseWithSessionInfos(request, jsTemplate, "application/javascript");
	}

	private CompletableFuture<HttpServerRequest> responseWithSessionInfosJson(HttpServerRequest request) {
		return responseWithSessionInfos(request, jsonTemplate, "application/json");
	}

	private CompletableFuture<HttpServerRequest> responseWithSessionInfos(HttpServerRequest request, Template template,
			String type) {

		Map<String, Object> model = new HashMap<>();

		loadModel(model, request);

		StringWriter sw = new StringWriter();

		model.put("version", BMVersion.getVersion());
		model.put("brandVersion", BMVersion.getVersionName());
		try {
			template.process(model, sw);
		} catch (TemplateException | IOException e) {
			logger.error("error during js generation", e);
		}

		request.response().putHeader("Content-type", type + "; charset=utf-8");
		request.response().setStatusCode(200).end(sw.toString());
		return CompletableFuture.completedFuture(null);

	}

	protected void loadModel(Map<String, Object> model, HttpServerRequest request) {

		for (Entry<String, String> entry : request.headers().entries()) {
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
	}

}
