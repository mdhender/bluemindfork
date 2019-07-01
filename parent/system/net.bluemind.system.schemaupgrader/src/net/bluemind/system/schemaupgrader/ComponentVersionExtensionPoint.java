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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.system.schemaupgrader;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import net.bluemind.system.api.InstallationVersion;
import net.bluemind.system.persistance.ComponentVersion;

public class ComponentVersionExtensionPoint {

	private static final Logger logger = LoggerFactory.getLogger(ComponentVersionExtensionPoint.class);

	public static List<ComponentVersion> getComponentsVersion() {

		List<ComponentVersion> ret = new LinkedList<>();
		IExtensionPoint point = Platform.getExtensionRegistry()
				.getExtensionPoint("net.bluemind.system.schemaupgrader.component");

		if (point == null) {
			logger.error("point net.bluemind.system.schemaupgrader.component not found.");
			return ImmutableList.of();
		}

		IExtension[] extensions = point.getExtensions();

		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if ("component-version".equals(e.getName())) {

					try {
						InstallationVersion cv = (InstallationVersion) e.createExecutableExtension("class");
						ret.add(new ComponentVersion(e.getAttribute("id"), cv.softwareVersion));
					} catch (CoreException ce) {
						;
						logger.error(ie.getNamespaceIdentifier() + ": " + ce.getMessage(), ce);
					}

				}
			}
		}
		return ret;

	}
}
