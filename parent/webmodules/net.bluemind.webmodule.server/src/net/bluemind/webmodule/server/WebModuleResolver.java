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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.webmodule.server.handlers.StaticFileHandler;

public class WebModuleResolver {

	private static final Logger logger = LoggerFactory.getLogger(WebModuleResolver.class);

	public List<WebModuleBuilder> loadExtensions() {
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.webmodule");
		if (point == null) {
			logger.error("point net.bluemind.webmodule not found.");
			return Collections.emptyList();
		}

		IExtension[] extensions = point.getExtensions();

		Map<String, WebModuleBuilder> modules = loadWebmodules(extensions);

		loadWebModulesProvider(extensions, modules);

		List<WebModuleBuilder> ret = new ArrayList<>();
		for (WebModuleBuilder b : modules.values()) {
			b.resolveJsBundles();
			ret.add(b);
		}
		return new ArrayList<>(modules.values());
	}

	private void loadWebModulesProvider(IExtension[] extensions, Map<String, WebModuleBuilder> modules) {
		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (!e.getName().equals("web-module-provider")) {
					continue;
				}

				String moduleId = e.getAttribute("module");
				logger.debug("webmoduleprovider {} for {}", ie.getNamespaceIdentifier(), moduleId);

				if ("*".equals(moduleId)) {
					for (WebModuleBuilder module : modules.values()) {
						boolean blacklisted = false;
						for (IConfigurationElement wle : e.getChildren("blacklist")) {
							String blackListRoot = wle.getAttribute("path");
							if (module.root.equals(blackListRoot)) {
								blacklisted = true;
							}
						}
						if (!blacklisted) {
							provide(module, e);
						}
					}
				} else {
					String[] ms = moduleId.split(",");
					for (String mId : ms) {
						WebModuleBuilder module = modules.get(mId);
						if (module == null) {
							logger.error("{} try to provide for webmodule {} but doesnt exists ",
									ie.getContributor().getName(), mId);
							continue;
						}
						provide(module, e);
					}
				}
			}
		}
	}

	private Map<String, WebModuleBuilder> loadWebmodules(IExtension[] extensions) {
		Map<String, WebModuleBuilder> modules = new HashMap<>();
		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (!e.getName().equals("web-module")) {
					continue;
				}

				String root = e.getAttribute("root");
				logger.debug("webmodule {}", root);

				WebModuleBuilder module = new WebModuleBuilder();
				module.root = root;
				if (e.getAttribute("index") != null) {
					module.index = e.getAttribute("index");
				}

				provide(module, e);
				modules.put(module.root, module);
			}
		}
		return modules;
	}

	public void logModules(Collection<WebModuleBuilder> modules) {
		StringJoiner sj = new StringJoiner("\n");
		for (WebModuleBuilder module : modules) {
			sj.add(module.toString());
		}
		logger.info("WebServer modules :\n{}", sj);
	}

	private void provide(WebModuleBuilder module, IConfigurationElement e) {
		List<WebResource> resources = loadWebResources(e);
		module.resources.addAll(resources);

		module.js.addAll(0, loadJs(e));
		module.css.addAll(0, loadCss(e));

		Map<String, HandlerFactory<HttpServerRequest>> handlers = loadHandlers(e);
		module.handlers.putAll(handlers);

		logger.debug("  * provide {} => js files: {}, css files: {}, handlers: {}", module.root, module.js.size(),
				module.css.size(), module.handlers.size());
	}

	private List<String> loadCss(IConfigurationElement e) {
		List<String> ret = new ArrayList<>();
		for (IConfigurationElement cssElement : e.getChildren("css")) {
			ret.add(cssElement.getAttribute("path"));
		}
		return ret;
	}

	private List<JsEntry> loadJs(IConfigurationElement e) {
		List<JsEntry> ret = new ArrayList<>();

		for (IConfigurationElement jsElement : e.getChildren("js")) {
			String path = jsElement.getAttribute("path");
			boolean lifecycle = false;
			boolean translation = false;
			if (jsElement.getAttribute("async-loading") != null) {
				lifecycle = Boolean.parseBoolean(jsElement.getAttribute("async-loading"));
			}

			if (jsElement.getAttribute("translation") != null) {
				translation = Boolean.parseBoolean(jsElement.getAttribute("translation"));
			}
			ret.add(new JsEntry(path, lifecycle, translation));
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	private Map<String, HandlerFactory<HttpServerRequest>> loadHandlers(IConfigurationElement e) {
		Map<String, HandlerFactory<HttpServerRequest>> handlers = new HashMap<>();
		for (final IConfigurationElement handlerConf : e.getChildren("handler")) {
			if (handlerConf.getAttribute("provider") != null) {
				try {
					IHandlerProvider handlerProvider = (IHandlerProvider) handlerConf
							.createExecutableExtension("provider");

					if (logger.isDebugEnabled()) {
						for (Entry<String, HandlerFactory<HttpServerRequest>> hv : handlerProvider.getHandlers()
								.entrySet()) {
							logger.debug("handler {} for path {}", hv.getKey(), hv.getValue());
						}
					}
					handlers.putAll(handlerProvider.getHandlers());
				} catch (InvalidRegistryObjectException | CoreException e1) {
					logger.error("error during handlerProvider {}" + " instantiation", handlerConf.getAttribute("path"),
							e1);
				}
			} else {
				try {

					handlers.put(handlerConf.getAttribute("path"), vertx -> {
						try {
							return (Handler<HttpServerRequest>) handlerConf.createExecutableExtension("class");
						} catch (CoreException e1) {
							logger.error("error during handler {}" + " instantiation for path {}",
									handlerConf.getAttribute("class"), handlerConf.getAttribute("path"), e1);
							return null;
						}
					});
					logger.debug("handler {} for path {}", handlerConf.getAttribute("class"),
							handlerConf.getAttribute("path"));
				} catch (InvalidRegistryObjectException e1) {
					logger.error("error during handler {}" + " instantiation for path {}",
							handlerConf.getAttribute("class"), handlerConf.getAttribute("path"), e1);
				}
			}
		}
		return handlers;
	}

	private List<WebResource> loadWebResources(IConfigurationElement e) {
		List<WebResource> resources = new ArrayList<>();
		for (IConfigurationElement r : e.getChildren("web-resource")) {
			String b = r.getAttribute("bundle");
			Bundle bundle = Platform.getBundle(b);

			if (bundle != null) {
				String preloadAttribute = r.getAttribute("preload");
				boolean preload = true;
				if (null != preloadAttribute && !Boolean.parseBoolean(preloadAttribute)) {
					preload = false;
				}
				WebResource resource = new WebResource(bundle, preload);
				resources.add(resource);
			} else {
				logger.warn("didnt find bundle {}", b);
			}
		}
		return resources;
	}

	public static List<WebModule> build(Vertx vertx, List<WebModuleBuilder> modules) {
		List<WebModule> ret = new ArrayList<>(modules.size());
		for (WebModuleBuilder builder : modules) {
			WebModule m = builder.build(vertx);
			m.defaultHandler = new StaticFileHandler(vertx, m.root, m.index, m.resources, true, true);
			ret.add(m);
		}
		return ret;
	}

}
