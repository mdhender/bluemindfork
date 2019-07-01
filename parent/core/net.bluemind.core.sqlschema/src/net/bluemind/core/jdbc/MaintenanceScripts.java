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
package net.bluemind.core.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

public class MaintenanceScripts {

	public static class MaintenanceScript {
		public final String name;
		public final String script;

		private MaintenanceScript(String name, String script) {
			this.name = name;
			this.script = script;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(MaintenanceScripts.class);

	public static List<MaintenanceScript> getScripts() {

		logger.debug("loading extensionpoint net.bluemind.core.jdbc.maintenance");
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.core.jdbc",
				"maintenance");

		if (point == null) {
			logger.error("point net.bluemind.core.jdbc.maintenance name:maintenance not found");
			throw new RuntimeException("point net.bluemind.core.jdbc.maintenance name:maintenance not found");
		}
		IExtension[] extensions = point.getExtensions();
		List<MaintenanceScript> scripts = new ArrayList<>();
		for (IExtension ie : extensions) {
			Bundle bundle = Platform.getBundle(ie.getContributor().getName());
			logger.debug("loading script from bundle:{}", bundle.getSymbolicName());

			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (e.getName().equals("maintenance-script")) {

					String resource = e.getAttribute("script");
					URL url = bundle.getResource(resource);
					if (url == null) {
						logger.error("bundle [{}] resource {} not found", bundle.getSymbolicName(), resource);
						continue;
					}

					try (InputStream in = url.openStream()) {
						String script = new String(ByteStreams.toByteArray(in));
						scripts.add(new MaintenanceScript(e.getAttribute("name"), script));
					} catch (IOException t) {
						logger.error("error reading {} ", url, t);
						continue;
					}

				}
			}

		}

		return scripts;
	}
}
