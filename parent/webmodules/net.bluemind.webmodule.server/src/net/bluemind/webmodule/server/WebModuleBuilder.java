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
package net.bluemind.webmodule.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;

import net.bluemind.webmodule.server.handlers.IWebModuleConsumer;

public class WebModuleBuilder {

	private static final Logger logger = LoggerFactory.getLogger(WebModuleBuilder.class);

	public String root;
	public String rootFile;
	public String index = "index.html";

	public Map<String, HandlerFactory<HttpServerRequest>> handlers = new HashMap<>();

	public List<JsEntry> js = new ArrayList<JsEntry>();
	public List<String> css = new ArrayList<String>();
	public List<WebResource> resources = new ArrayList<WebResource>();

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("%s\n", root));
		sb.append(String.format("    index: %s\n", index));

		StringJoiner sj = new StringJoiner(",", "[", "]");
		for (WebResource r : resources) {
			sj.add(r.getBundleName());
		}
		sb.append(String.format("    resources: %s \n", sj.toString()));

		sj = new StringJoiner(",", "[", "]");
		for (String handlerPath : handlers.keySet()) {
			sj.add(handlerPath);
		}
		sb.append(String.format("    handlers: %s \n", sj.toString()));

		sj = new StringJoiner(",", "[", "]");
		for (JsEntry jsPath : js) {
			sj.add(jsPath.path);
		}
		sb.append(String.format("    js: %s \n", sj.toString()));

		sj = new StringJoiner(",", "[", "]");
		for (String cssPath : css) {
			sj.add(cssPath);
		}
		sb.append(String.format("    css: %s\n", sj.toString()));

		return sb.toString();

	}

	public WebModule build(Vertx vertx) {
		WebModule ret = new WebModule();
		ret.css = new ArrayList<>(new HashSet<>(this.css));
		ret.handlers = new HashMap<>();
		ret.index = this.index;
		ret.resources = this.resources;
		ret.root = root;
		ret.rootFile = rootFile;
		ret.js = orderedJs();

		for (Entry<String, HandlerFactory<HttpServerRequest>> handlerEntry : this.handlers.entrySet()) {
			Handler<HttpServerRequest> handler = handlerEntry.getValue().create(vertx);
			if (handler instanceof IWebModuleConsumer) {
				((IWebModuleConsumer) handler).setModule(ret);
			}
			if (handler instanceof NeedVertx) {
				((NeedVertx) handler).setVertx(vertx);
			}
			ret.handlers.put(handlerEntry.getKey(), handler);
		}
		return ret;
	}

	private List<JsEntry> orderedJs() {

		Set<String> resolved = new LinkedHashSet<>();
		List<String> unresolved = new ArrayList<>();
		for (JsEntry j : js) {
			resolve(j, resolved, unresolved);
		}

		List<JsEntry> ret = new ArrayList<>();
		for (String r : resolved) {
			ret.add(entry(r));
		}

		return ret;
	}

	private void resolve(JsEntry j, Set<String> resolved, List<String> unresolved) {
		unresolved.add(j.bundle);
		for (String dep : j.dependencies) {
			logger.debug("resolve depencency {} for {}", dep, j.bundle);
			if (!resolved.contains(dep)) {
				if (unresolved.contains(dep)) {
					throw new RuntimeException("circular depencency " + j.bundle + " -> " + dep);
				}
				resolve(entry(dep), resolved, unresolved);
			}
		}

		if (j.bundle != null) {
			resolved.add(j.bundle);
		} else {
			logger.warn("js {} has no bundle", j.path);
		}
	}

	private JsEntry entry(String dep) {
		for (JsEntry j : js) {

			if (j.bundle != null && j.bundle.equals(dep)) {
				return j;
			}
		}
		throw new RuntimeException("depenency " + dep + " not found");
	}

	private WebResource findResourceBundle(String path) {

		for (WebResource r : this.resources) {
			if (r.getResource(path) != null) {
				return r;
			}
		}
		return null;
	}

	public void resolveJsBundles() {
		// resolve js
		for (JsEntry j : this.js) {
			WebResource webResourceBundle = findResourceBundle(j.path);
			if (webResourceBundle != null) {
				Bundle bundle = webResourceBundle.getBundle();

				j.bundle = bundle.getSymbolicName();
				String value = bundle.getHeaders().get("Web-Dependencies");
				if (value != null) {
					logger.debug("bundle {} depencies {}", j.bundle, value);
					j.dependencies = Arrays.asList(value.split(","));
				}
				if (j.hasTranslation()) {
					loadTranslations(j, webResourceBundle);
				}
			} else {
				logger.error("Could not find bundle for {}", j.path);
			}
		}

	}

	private void loadTranslations(JsEntry j, WebResource webResourceBundle) {
		String ret = j.path;
		String path = ret.substring(0, ret.lastIndexOf(".js"));
		path += "_";
		Map<String, String> translations = new HashMap<>(10);
		logger.debug("looking for {}", path);
		for (String r : webResourceBundle.getResources()) {
			if (r.endsWith(".js") && r.startsWith(path)) {
				String lang = r.substring(path.length(), (path.length() - 1) + ".js".length());
				logger.debug("found translation for {} {}", lang, r);
				translations.put(lang, r);
			}
		}

		j.translations = translations;
	}

}
