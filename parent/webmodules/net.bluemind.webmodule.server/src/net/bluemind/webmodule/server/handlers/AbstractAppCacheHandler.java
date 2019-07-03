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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import com.netflix.spectator.api.Registry;

import freemarker.template.Configuration;
import freemarker.template.Template;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.webmodule.server.JsEntry;
import net.bluemind.webmodule.server.LogoVersion;
import net.bluemind.webmodule.server.WebModule;
import net.bluemind.webmodule.server.WebResource;

public abstract class AbstractAppCacheHandler implements Handler<HttpServerRequest>, IWebModuleConsumer {

	private static final Logger logger = LoggerFactory.getLogger(AbstractAppCacheHandler.class);
	private Template mainTemplate;
	private WebModule module;

	private final Registry registry = MetricsRegistry.get();
	private final IdFactory idFactory = new IdFactory("appCache", registry, AbstractAppCacheHandler.class);

	public AbstractAppCacheHandler() {
		Configuration freemarkerCfg = new Configuration();
		freemarkerCfg.setClassForTemplateLoading(this.getClass(), "/templates");
		freemarkerCfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);

		try {
			mainTemplate = freemarkerCfg.getTemplate(getTemplateName());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

	}

	@Override
	public void handle(HttpServerRequest request) {
		registry.counter(idFactory.name("requests")).increment();
		long start = registry.clock().monotonicTime();
		StringWriter sw = new StringWriter();

		Set<String> files = new HashSet<>();
		for (WebResource resource : module.resources) {
			files.addAll(filterJs(resource, module, request));
		}

		// FEATLOGO-27
		files.add(LogoVersion.getUri());

		// FEATLOGO-35 banner bluemind logo
		files.add("style/customstyle.css");
		files.add("images/bluemind-credits-deplie.png");
		files.add("images/bluemind-credits-deplie@2x.png");

		Map<String, Object> model = new HashMap<>();
		model.put("files", files);
		model.put("version", getVersion() + "." + LogoVersion.getVersion(request));
		model.put("lang", getLang(request));
		for (Entry<String, String> entry : request.headers().entries()) {
			model.put(entry.getKey(), entry.getValue());
		}
		try {
			mainTemplate.process(model, sw);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		HttpServerResponse resp = request.response();
		String body = sw.toString();
		byte[] data = body.getBytes();
		resp.putHeader("Content-Length", "" + data.length);
		resp.putHeader("ContentType", "text/cache-manifest").putHeader("Cache-Control", "no-cache")
				.putHeader("Pragma", "no-cache").putHeader("Expires", "0");
		resp.write(new Buffer(data));
		resp.setStatusCode(200);
		resp.end();
		registry.timer(idFactory.name("requestTime")).record(registry.clock().monotonicTime() - start,
				TimeUnit.NANOSECONDS);

	}

	/**
	 * 
	 * Remove translated js from appcache (because retrieving toto.js by a
	 * french user will fetch toto_fr.js)
	 * 
	 * @param resource
	 * @param module
	 * @param request
	 * @return
	 */
	private Collection<? extends String> filterJs(WebResource resource, WebModule module, HttpServerRequest request) {
		String lang = getLang(request);
		ArrayList<String> ret = new ArrayList<>(resource.getResources().size());

		Set<String> toRemove = new HashSet<>();
		for (JsEntry js : module.js) {
			if (js.hasTranslation()) {
				ret.add(js.getTranslation(lang).getPath());
				toRemove.add(js.getPath().substring(0, js.getPath().length() - ".js".length()));
			}
		}

		for (String file : resource.getResources()) {

			boolean addFile = true;
			if (file.endsWith("js.map")) {
				addFile = false;
			} else if (file.endsWith(".js.gz")) {
				addFile = false;
			} else if (file.endsWith(".css.gz")) {
				addFile = false;
			} else if (file.endsWith(".symbolMap")) {
				addFile = false;
			} else if (file.endsWith(".js")) {
				for (String t : toRemove) {
					if (file.startsWith(t)) {
						addFile = false;
						break;
					}
				}
			}

			if (addFile) {
				ret.add(file);
			}
		}
		return ret;

	}

	public void setModule(WebModule module) {
		this.module = module;
	}

	protected abstract String[] getDirectories();

	protected abstract String getTemplateName();

	protected abstract String getVersion();

	protected String getLang(HttpServerRequest request) {
		return request.headers().get("BMLang");
	}
}