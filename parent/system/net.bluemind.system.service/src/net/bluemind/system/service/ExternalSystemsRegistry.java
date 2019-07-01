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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.system.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import net.bluemind.core.utils.ImageUtils;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.system.api.ExternalSystem;

public class ExternalSystemsRegistry {

	private static List<RegisteredExternalSystem> externalSystems;
	private static final Logger logger = LoggerFactory.getLogger(ExternalSystemsRegistry.class);

	static {
		loadExternalSystems();
	}

	private static void loadExternalSystems() {
		externalSystems = new ArrayList<>();
		logger.info("loading net.bluemind.system.external_systems extensions");
		String pluginId = "net.bluemind.system";
		String pointName = "external_systems";
		String element = "system";
		String attribute = "impl";

		RunnableExtensionLoader<RegisteredExternalSystem> epLoader = new RunnableExtensionLoader<>();
		externalSystems = ImmutableList.<RegisteredExternalSystem>builder()
				.addAll(epLoader.loadExtensionsWithPriority(pluginId, pointName, element, attribute)).build();

		logger.debug("Registered {} external systems", externalSystems.size());
		for (RegisteredExternalSystem system : externalSystems) {
			logger.debug("Registered external system: {}", system.identifier);
		}

	}

	public static List<ExternalSystem> getExternalSystems() {
		return externalSystems.stream().map(ExternalSystemsRegistry::toExternalSystem).collect(Collectors.toList());
	}

	public static byte[] getLogo(String systemIdentifier) {
		try {
			byte[] logo = getSystem(systemIdentifier).getLogo();
			byte[] sanitized = ImageUtils.checkAndSanitize(logo);
			byte[] content = ImageUtils.resize(sanitized, 140, 40);
			return content;
		} catch (Exception e) {
			logger.warn("Cannot load logo of external system {}", systemIdentifier);
			return null;
		}
	}

	public static ExternalSystem getExternalSystem(String systemIdentifier) {
		return toExternalSystem(getSystem(systemIdentifier));
	}

	private static ExternalSystem toExternalSystem(RegisteredExternalSystem system) {
		return new ExternalSystem(system.identifier, system.description, system.authKind);
	}

	private static RegisteredExternalSystem getSystem(String systemIdentifier) {
		return externalSystems.stream().filter(s -> s.identifier.equals(systemIdentifier)).findFirst().orElse(null);
	}

}
