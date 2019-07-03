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
package net.bluemind.eclipse.common;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load plugins based on some interface
 * 
 * @param <T>
 */
public class RunnableExtensionLoader<T> {

	private static final Logger logger = LoggerFactory.getLogger(RunnableExtensionLoader.class);

	public static class Builder {

		private String pluginId;
		private String pointName;
		private String element;
		private String implAttr;
		private boolean withPriority;

		private Builder() {

		}

		public Builder pluginId(String pluginId) {
			this.pluginId = pluginId;
			return this;
		}

		public Builder pointName(String pointName) {
			this.pointName = pointName;
			return this;
		}

		public Builder element(String elem) {
			this.element = elem;
			return this;
		}

		public Builder implAttribute(String implAttr) {
			this.implAttr = implAttr;
			return this;
		}

		public Builder withPriority(boolean b) {
			this.withPriority = b;
			return this;
		}

		public <T> List<T> load() {
			RunnableExtensionLoader<T> rel = new RunnableExtensionLoader<T>();
			if (withPriority) {
				return rel.loadExtensionsWithPriority(pluginId, pointName, element, implAttr);
			} else {
				return rel.loadExtensions(pluginId, pointName, element, implAttr);
			}
		}

	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Load plugins declaring an executable extension
	 * 
	 * @param pluginId
	 * @param pointName
	 * @param element
	 * @param attribute
	 * @return
	 */
	public List<T> loadExtensions(String pluginId, String pointName, String element, String attribute) {
		List<T> factories = new LinkedList<T>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		Objects.requireNonNull(registry, "OSGi registry is null");
		IExtensionPoint point = registry.getExtensionPoint(pluginId, pointName);
		if (point == null) {
			logger.error("point " + pluginId + "." + pointName + " [" + element + " " + attribute + "=XXX] not found.");
			return factories;
		}
		IExtension[] extensions = point.getExtensions();

		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (element.equals(e.getName())) {

					try {
						@SuppressWarnings("unchecked")
						T factory = (T) e.createExecutableExtension(attribute);
						factories.add(factory);
						logger.debug(factory.getClass().getName() + " loaded.");
					} catch (CoreException ce) {
						;
						logger.error(ie.getNamespaceIdentifier() + ": " + ce.getMessage(), ce);
					}

				}
			}
		}
		logger.debug("Loaded " + factories.size() + " implementors of " + pluginId + "." + pointName);
		return factories;
	}

	private static class FactoryExtension<T> implements Comparable<FactoryExtension<T>> {
		public T factory;
		public int priority;

		@Override
		public int compareTo(FactoryExtension<T> o) {
			return o.priority - priority;
		}
	}

	/**
	 * Load plugins declaring an executable extension
	 * 
	 * @param pluginId
	 * @param pointName
	 * @param element
	 * @param attribute
	 * @return
	 */
	public List<T> loadExtensionsWithPriority(String pluginId, String pointName, String element, String attribute) {
		List<FactoryExtension<T>> factories = new LinkedList<>();
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(pluginId, pointName);
		if (point == null) {
			logger.error("point " + pluginId + "." + pointName + " [" + element + " " + attribute + "=XXX] not found.");
			return Collections.emptyList();
		}
		IExtension[] extensions = point.getExtensions();

		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (element.equals(e.getName())) {

					try {
						@SuppressWarnings("unchecked")
						T factory = (T) e.createExecutableExtension(attribute);
						int priority = 0;
						if (e.getAttribute("priority") != null) {
							String value = e.getAttribute("priority");
							try {
								priority = Integer.parseInt(value);
							} catch (Exception ex) {
								// ignore priority
							}
						}

						if (factory instanceof IHasPriority) {
							priority = ((IHasPriority) factory).priority();
						}
						FactoryExtension<T> fe = new FactoryExtension<>();
						fe.factory = factory;
						fe.priority = priority;
						factories.add(fe);
						logger.debug(factory.getClass().getName() + " loaded.");
					} catch (CoreException ce) {
						logger.error(ie.getNamespaceIdentifier() + ": " + ce.getMessage(), ce);
					} catch (Exception ex) {
						logger.error(ex.getMessage(), ex);
					}

				}
			}
		}

		logger.info("Loaded " + factories.size() + " implementors of " + pluginId + "." + pointName);

		return factories.stream().sorted(FactoryExtension::compareTo).map(fe -> {
			return fe.factory;
		}).collect(Collectors.toList());
	}

}
