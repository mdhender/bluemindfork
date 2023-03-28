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

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.webmodule.server.handlers.IWebModuleConsumer;
import net.bluemind.webmodule.server.js.BundleDependency;
import net.bluemind.webmodule.server.js.JsDependency;
import net.bluemind.webmodule.server.js.JsEntry;

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

		ret.js = new OrderedJsListBuilder(js).getJsList();

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

	private class OrderedJsListBuilder {
		private Set<String> resolved;
		private List<String> unresolved;
		private List<JsEntry> files;

		public OrderedJsListBuilder(List<JsEntry> js) {
			resolved = new LinkedHashSet<>();
			unresolved = new ArrayList<>();
			this.files = js;
		}

		public List<JsEntry> getJsList() {
			for (JsEntry file : files) {
				resolve(file);
			}
			List<JsEntry> list = new ArrayList<>();
			for (String resolvedBundle : resolved) {
				logger.info("Inject {} JS file", resolvedBundle);

				list.add(getEntryByPath(resolvedBundle));
			}

			return list;
		}
		
		private void resolve(JsEntry js) {
			unresolved.add(js.path);
			for (JsDependency dependency : js.getDependencies()) {
				resolveDependency(dependency, js);
			}

			if (js.getBundle() != null) {
				resolved.add(js.path);
			} else {
				logger.warn("js {} has no bundle", js.path);
			}
		}

		private void resolveDependency(JsDependency dependency, JsEntry js) {
			// should resolve every jsEntry of depBundle
			List<JsEntry> entries = dependency.getEntries(dependency, files);
			if (entries.isEmpty()) {
				throw new RuntimeException(
						"dependency " + dependency + " not found for JsEntry " + js.path + " (" + js.getBundle() + ")");
			}
			for (JsEntry entry : entries) {
				if (!resolved.contains(entry.path)) {
					if (unresolved.contains(entry.path)) {
						throw new RuntimeException("circular dependency " + js.getBundle() + " -> " + dependency);
					}
					resolve(entry);
				}
			}
		}

		private JsEntry getEntryByPath(String path) {
			for (JsEntry j : files) {
				if (j.path != null && j.path.equals(path)) {
					return j;
				}
			}
			throw new RuntimeException("dependency " + path + " not found");
		}
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

				j.setBundle(bundle.getSymbolicName());
				String value = bundle.getHeaders().get("Web-Dependencies");
				if (value != null) {
					logger.debug("bundle {} depencies {}", j.getBundle(), value);
					Arrays.asList(value.split(","))
							.forEach(dependency -> j.addDependency(new BundleDependency(dependency)));
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
		j.setTranslations(translations);
	}

}
