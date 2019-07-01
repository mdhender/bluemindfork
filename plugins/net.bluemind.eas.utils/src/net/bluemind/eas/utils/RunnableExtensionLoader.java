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
package net.bluemind.eas.utils;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads plugins based on some interface
 * 
 * 
 * @param <T>
 */
public class RunnableExtensionLoader<T> {

	private Logger logger;

	public RunnableExtensionLoader() {
		logger = LoggerFactory.getLogger(getClass());
	}

	/**
	 * Loads plugins declaring an executable extension
	 * 
	 * @param pluginId
	 * @param pointName
	 * @param element
	 * @param attribute
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<T> loadExtensions(String pluginId, String pointName,
			String element, String attribute) {

		List<T> factories = new LinkedList<T>();
		IExtensionPoint point = Platform.getExtensionRegistry()
				.getExtensionPoint(pluginId, pointName);
		if (point == null) {
			logger.error("point " + pluginId + "." + pointName + " [" + element
					+ " " + attribute + "=XXX] not found.");
			return factories;
		}
		IExtension[] extensions = point.getExtensions();

		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (element.equals(e.getName())) {
					try {
						T factory = (T) e.createExecutableExtension(attribute);
						factories.add(factory);
						logger.info(factory.getClass().getSimpleName()
								+ " loaded.");
					} catch (CoreException ce) {
						ce.printStackTrace();
					}

				}
			}
		}
		logger.info("Loaded " + factories.size() + " implementors of "
				+ pluginId + "." + pointName);
		return factories;
	}

}
