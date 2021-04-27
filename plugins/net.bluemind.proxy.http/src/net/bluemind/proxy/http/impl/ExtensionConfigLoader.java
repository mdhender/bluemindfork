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
package net.bluemind.proxy.http.impl;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.proxy.http.config.ForwardedLocation;
import net.bluemind.proxy.http.config.HPSConfiguration;

public class ExtensionConfigLoader implements IConfigLoader {

	private static final Logger logger = LoggerFactory.getLogger(ExtensionConfigLoader.class);

	public void load(HPSConfiguration conf) {
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
				System.err.println("On " + e);
				if ("forward".equals(e.getName())) {
					fl = new ForwardedLocation(e.getAttribute("path"), e.getAttribute("target"), e.getAttribute("role"),
							"false");
					conf.getForwardedLocations().add(fl);
					logger.info("adding forward from {} to {} [role:{}]", fl.getPathPrefix(), fl.getTargetUrl(),
							fl.getRole());
					if (e.getAttribute("auth_kind") != null)
						fl.setRequiredAuthKind(e.getAttribute("auth_kind"));
					for (IConfigurationElement wle : e.getChildren("whitelist")) {
						fl.whiteList(wle.getAttribute("uri"));
					}
				}
			}
		}
		logger.info("Loaded {} implementors of {}.{}", extensions.length, pluginId, pointName);
	}

}
