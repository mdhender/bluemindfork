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
package net.bluemind.webmodule.server.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import net.bluemind.common.freemarker.EquinoxTemplateLoader;
import net.bluemind.core.api.BMVersion;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.webmodule.server.JsEntry;
import net.bluemind.webmodule.server.WebExtensionsResolver;
import net.bluemind.webmodule.server.WebModule;

public abstract class AbstractIndexHandler implements IWebModuleConsumer, Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractIndexHandler.class);

	private final Registry registry = MetricsRegistry.get();
	private final IdFactory idFactory = new IdFactory("ftlTemplates", registry, AbstractIndexHandler.class);

	private WebModule module;

	private String cssLibs;

	private Template jsRuntimeTemplate;
	private Template jsLibTemplate;

	private Template mainTemplate;

	public static final String JS_RUNTIME_LIB;

	static {
		String js = "";

		try (InputStream in = AbstractIndexHandler.class
				.getResourceAsStream("/web-resources/js/compile/net.bluemind.webmodule.server.js")) {
			js = new String(com.google.common.io.ByteStreams.toByteArray(in));
		} catch (Exception e) {
			logger.error("error during loading script ", e);
		}

		JS_RUNTIME_LIB = js;
	}

	private void init() {
		Configuration freemarkerCfg = new Configuration();
		freemarkerCfg.setTemplateLoader(new EquinoxTemplateLoader(this.getClass().getClassLoader(), "/templates/"));
		freemarkerCfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);

		try {
			mainTemplate = freemarkerCfg.getTemplate(getTemplateName());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		Configuration privateFreemarkerCfg = new Configuration();
		privateFreemarkerCfg.setClassForTemplateLoading(AbstractIndexHandler.class, "/templates");
		privateFreemarkerCfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
		try {
			jsRuntimeTemplate = privateFreemarkerCfg.getTemplate("jsRuntime.ftl");
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		try {
			jsLibTemplate = privateFreemarkerCfg.getTemplate("jsLibs.ftl");
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		try {
			generateCssRuntime(privateFreemarkerCfg);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	private void generateCssRuntime(Configuration cfg) throws Exception {
		Template template = cfg.getTemplate("cssLibs.ftl");
		Map<String, Object> model = new HashMap<>();
		model.put("cssLinks", module.css);

		StringWriter sw = new StringWriter();
		template.process(model, sw);

		cssLibs = sw.toString();

	}

	private String generateJsRuntime(String lang) throws Exception {
		Map<String, Object> model = new HashMap<>();

		List<JsEntry> js = new ArrayList<>(module.js.size());

		for (JsEntry one : module.js) {
			if (one.getBundle() == null) {
				logger.error("no bundle found for {}", one.path);
				continue;
			}
			if (one.hasTranslation()) {
				logger.debug("load translation for {}", one.path);
				js.add(getTranslation(lang, one));
			} else {
				js.add(one);
			}
		}

		model.put("runtime", JS_RUNTIME_LIB);
		model.put("jsLinks", js);

		StringWriter sw = new StringWriter();
		jsLibTemplate.process(model, sw);

		return sw.toString();
	}

	private JsEntry getTranslation(String lang, JsEntry one) {
		return one.getTranslation(lang);
	}

	@Override
	public void handle(HttpServerRequest request) {
		registry.counter(idFactory.name("requests")).increment();
		StringWriter sw = new StringWriter();

		Map<String, Object> model = new HashMap<>();
		loadHeaders(request, model);
		loadPageModel(request, model);

		buildJsRuntime(request, model);
		model.put("cssRuntime", cssLibs);

		try {
			mainTemplate.process(model, sw);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		HttpServerResponse resp = request.response();
		String body = sw.toString();
		byte[] data = body.getBytes();
		if (resp.headers().contains("WWW-Authenticate")) {
			resp.setStatusCode(401);
		} else {
			resp.setStatusCode(200);
		}
		resp.headers().add(HttpHeaders.CONTENT_TYPE, "text/html");
		resp.end(Buffer.buffer(data));
	}

	private void buildJsRuntime(HttpServerRequest request, Map<String, Object> model) {

		try {
			String lang = getLang(request);
			JsonObject exts = new WebExtensionsResolver(lang, module.root).loadExtensions();
			model.put("extensions", exts.encodePrettily());
			model.put("jsLibs", generateJsRuntime(lang));
			model.put("version", BMVersion.getVersion());
			model.put("brandVersion", BMVersion.getVersionName());
			StringWriter sw = new StringWriter();
			jsRuntimeTemplate.process(model, sw);
			model.put("jsRuntime", sw.toString());
		} catch (Exception e1) {
			logger.error("error during extensions loading ", e1);
		}

	}

	protected String getLang(HttpServerRequest request) {
		return request.headers().get("BMLang");
	}

	private void loadHeaders(HttpServerRequest request, Map<String, Object> model) {

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

	public void setModule(WebModule module) {
		this.module = module;
		init();
	}

	protected abstract String getTemplateName();

	protected void loadPageModel(HttpServerRequest request, Map<String, Object> model) {
		// nothing to do. Override it if you want to enrich the page model
	}
}