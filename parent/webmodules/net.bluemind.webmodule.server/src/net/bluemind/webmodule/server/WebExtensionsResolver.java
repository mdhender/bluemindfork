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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class WebExtensionsResolver {

	private static final Logger logger = LoggerFactory.getLogger(WebExtensionsResolver.class);

	private final String lang;

	private final String module;

	private static final Map<String, PreEncodedObject> extensions = new ConcurrentHashMap<>();

	public WebExtensionsResolver(String lang, String module) {
		this.module = module;
		this.lang = Optional.ofNullable(lang).orElse("en");
	}

	public PreEncodedObject loadExtensions() {
		String cacheKey = lang + ":" + module;
		logger.debug("loadExt for {}", cacheKey);
		return extensions.computeIfAbsent(cacheKey, k -> new PreEncodedObject(loadExtensionsImpl(lang, module)));
	}

	private static JsonObject loadExtensionsImpl(String l, String m) {
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.extensions");
		if (point == null) {
			return new JsonObject();
		}
		IExtension[] extensions = point.getExtensions();

		List<IExtensionPoint> points = new ArrayList<>();
		for (IExtension ie : extensions) {
			for (IConfigurationElement ce : ie.getConfigurationElements()) {
				String pointId = ce.getAttribute("id");
				String epModule = ce.getAttribute("module");
				if (epModule == null || epModule.equals(m)) {
					IExtensionPoint export = Platform.getExtensionRegistry().getExtensionPoint(pointId);
					if (export != null) {
						points.add(export);
					} else {
						logger.warn("export point {} not found", pointId);
					}
				} else {
					logger.debug("extension point {} not activated for module {}", pointId, m);
				}
			}

		}

		JsonObject ret = new JsonObject();

		for (IExtensionPoint export : points) {
			JsonArray jsExtensions = new JsonArray();
			for (IExtension extension : export.getExtensions()) {
				JsonObject jsonExt = new JsonObject();
				jsonExt.put("bundle", extension.getContributor().getName());
				loadConfiguration(l, jsonExt, extension.getConfigurationElements());
				jsExtensions.add(jsonExt);
			}

			ret.put(export.getUniqueIdentifier(), jsExtensions);
		}

		return ret;
	}

	private static void loadConfiguration(String lang, JsonObject jsonExt, IConfigurationElement[] elts) {
		if (elts.length == 0)
			return;
		for (IConfigurationElement ce : elts) {
			JsonObject jsonCe = new JsonObject();
			for (String att : ce.getAttributeNames()) {
				jsonCe.put(att, ce.getAttribute(att, lang));
			}
			if (ce.getValue() != null) {
				jsonCe.put("body", ce.getValue());
			}

			if (ce.getChildren() != null) {
				JsonObject child = new JsonObject();
				loadConfiguration(lang, child, ce.getChildren());
				jsonCe.put("children", child);
			}
			jsonExt.put(ce.getName(), jsonCe);
		}

	}
}
