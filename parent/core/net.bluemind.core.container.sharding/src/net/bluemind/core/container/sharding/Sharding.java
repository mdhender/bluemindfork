/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.core.container.sharding;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

public class Sharding {

	private static final Set<String> containerTypes = new HashSet<>();
	private static final Logger logger = LoggerFactory.getLogger(Sharding.class);

	public static Set<String> containerTypes() {
		return ImmutableSet.copyOf(containerTypes);
	}

	static {
		init();
	}

	private static void init() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		Objects.requireNonNull(registry, "OSGi registry is null");
		IExtensionPoint point = registry.getExtensionPoint("net.bluemind.core.container.sharding", "sharded");
		Objects.requireNonNull(point, "Point not found");
		IExtension[] extensions = point.getExtensions();

		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if ("container".equals(e.getName())) {
					String contType = e.getAttribute("type");
					containerTypes.add(contType);
				}
			}
		}
		logger.info("Sharded containers: {}", containerTypes);
	}

}
