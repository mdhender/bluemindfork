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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class WebExtensionsResolver {

	private static final Logger logger = LoggerFactory.getLogger(WebExtensionsResolver.class);

	private String lang;

	private String module;

	public WebExtensionsResolver(String lang, String module) {
		this.module = module;
		if (lang == null) {
			lang = "en";

		}
		this.lang = lang;
	}

	public JsonObject loadExtensions() throws IOException {
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
				if (epModule == null || epModule.equals(module)) {
					IExtensionPoint export = Platform.getExtensionRegistry().getExtensionPoint(pointId);
					if (export != null) {
						points.add(export);
					} else {
						logger.warn("export point {} not found", pointId);
					}
				} else {
					logger.debug("extension point {} not activated for module {}", pointId, module);
				}
			}

		}

		JsonObject ret = new JsonObject();

		for (IExtensionPoint export : points) {
			JsonArray jsExtensions = new JsonArray();
			for (IExtension extension : export.getExtensions()) {
				JsonObject jsonExt = new JsonObject();
				jsonExt.putString("bundle", extension.getContributor().getName());
				loadConfiguration(jsonExt, extension.getConfigurationElements());
				jsExtensions.addObject(jsonExt);
			}

			ret.putArray(export.getUniqueIdentifier(), jsExtensions);
		}

		return ret;
	}

	private void loadConfiguration(JsonObject jsonExt, IConfigurationElement[] elts) {
		if (elts.length == 0)
			return;
		for (IConfigurationElement ce : elts) {
			JsonObject jsonCe = new JsonObject();
			for (String att : ce.getAttributeNames()) {
				jsonCe.putString(att, ce.getAttribute(att, lang));
			}
			jsonExt.putObject(ce.getName(), jsonCe);

			if (ce.getChildren() != null) {
				JsonObject child = new JsonObject();

				loadConfiguration(child, ce.getChildren());
				jsonExt.putObject("children", child);
			}
		}

	}
}
