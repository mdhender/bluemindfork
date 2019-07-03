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
package net.bluemind.customproperties.service.internal;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.customproperties.api.CustomPropertiesRequirements;
import net.bluemind.customproperties.api.ICustomProperties;
import net.bluemind.customproperties.api.ICustomPropertiesRequirements;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class CustomPropertiesService implements ICustomProperties {

	private static final Logger logger = LoggerFactory.getLogger(CustomPropertiesService.class);

	private static final List<ICustomPropertiesRequirements> customProperties = loadCustomProperties();

	private static List<ICustomPropertiesRequirements> loadCustomProperties() {

		RunnableExtensionLoader<ICustomPropertiesRequirements> rel = new RunnableExtensionLoader<ICustomPropertiesRequirements>();
		List<ICustomPropertiesRequirements> props = rel.loadExtensions("net.bluemind", "customproperties",
				"customproperties", "implementation");

		return props;
	}

	@Override
	public CustomPropertiesRequirements get(String objectName) {
		for (ICustomPropertiesRequirements cp : customProperties) {
			if (objectName.equals(cp.support())) {
				CustomPropertiesRequirements cpr = new CustomPropertiesRequirements();
				cpr.support = cp.support();
				cpr.requesterId = cp.getRequesterId();
				cpr.customProperties = cp.getCustomProperties();
				return cpr;
			}
		}

		logger.error("No custom properties for object {}", objectName);
		return null;
	}
}
