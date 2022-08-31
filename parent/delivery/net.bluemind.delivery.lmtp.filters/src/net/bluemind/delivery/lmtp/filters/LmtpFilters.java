/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.delivery.lmtp.filters;

import java.util.List;
import java.util.stream.Collectors;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class LmtpFilters {

	private LmtpFilters() {
	}

	private static final List<IMessageFilter> PLUGINS = load();

	private static List<IMessageFilter> load() {
		RunnableExtensionLoader<ILmtpFilterFactory> rel = new RunnableExtensionLoader<>();
		List<ILmtpFilterFactory> plugins = rel.loadExtensions("net.bluemind.delivery.lmtp.filters", "factory",
				"factory", "impl");

		return plugins.stream().sorted((lf1, lf2) -> Integer.compare(lf2.getPriority(), lf1.getPriority()))
				.map(ILmtpFilterFactory::getEngine).collect(Collectors.toList());

	}

	public static List<IMessageFilter> get() {
		return PLUGINS;
	}

}
