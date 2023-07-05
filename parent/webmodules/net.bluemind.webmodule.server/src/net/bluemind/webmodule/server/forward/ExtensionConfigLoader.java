/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.webmodule.server.forward;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.webmodule.server.WebserverConfiguration;

public class ExtensionConfigLoader implements IConfigLoader {

	private static final Logger logger = LoggerFactory.getLogger(ExtensionConfigLoader.class);

	public void load(WebserverConfiguration conf) {
		logger.info("loading net.bluemind.proxy.http.forward extensions");
		String pluginId = "net.bluemind.proxy.http";
		String pointName = "forward";

		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(pluginId, pointName);
		if (point == null) {
			logger.error("extension point {}.{} not found", pluginId, pointName);
			return;
		}
		IExtension[] extensions = point.getExtensions();
		for (IExtension ie : extensions) {
			ForwardedLocation fl;
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (!"forward".equals(e.getName())) {
					continue;
				}
				fl = new ForwardedLocation(e.getAttribute("path"), e.getAttribute("auth"), e.getAttribute("role"),
						"false");
				conf.getForwardedLocations().add(fl);
				logger.info("adding forward from {} to {} [role:{}]", fl.getPathPrefix(), fl.getRole());
				for (IConfigurationElement wle : e.getChildren("whitelist")) {
					String whiteListUri = wle.getAttribute("uri");
					String whiteListRegex = wle.getAttribute("regex");
					if (whiteListUri != null && !whiteListUri.isEmpty()) {
						fl.whiteList(whiteListUri);
					}
					if (whiteListRegex != null && !whiteListRegex.isEmpty()) {
						fl.whiteListRegex(whiteListRegex);
					}
				}

			}
		}
		logger.info("Loaded {} implementors of {}.{}", extensions.length, pluginId, pointName);
	}

}
