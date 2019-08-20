/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.resource.helper;

import java.util.List;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public final class ResourceTemplateHelpers {
	private static final RunnableExtensionLoader<IResourceTemplateHelper> RUNNABLE_EXTENSION_LOADER = new RunnableExtensionLoader<IResourceTemplateHelper>();
	private static final IResourceTemplateHelper INSTANCE = loadFirst();

	public static IResourceTemplateHelper getInstance() {
		return INSTANCE;
	}

	public static IResourceTemplateHelper loadFirst() {
		final List<IResourceTemplateHelper> resourceTemplateHelpers = load();
		if (!resourceTemplateHelpers.isEmpty()) {
			return resourceTemplateHelpers.get(0);
		}
		return null;
	}

	public static List<IResourceTemplateHelper> load() {
		return RUNNABLE_EXTENSION_LOADER.loadExtensions("net.bluemind.resource", "helper", "templatehelper", "impl");
	}
}
