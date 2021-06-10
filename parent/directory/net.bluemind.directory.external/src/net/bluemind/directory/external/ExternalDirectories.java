/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.directory.external;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class ExternalDirectories {

	private static final Logger logger = LoggerFactory.getLogger(ExternalDirectories.class);

	private static final List<IExternalDirectoryProvider> allProviders = load();

	private static List<IExternalDirectoryProvider> load() {
		RunnableExtensionLoader<IExternalDirectoryProvider> rel = new RunnableExtensionLoader<>();
		List<IExternalDirectoryProvider> result = rel.loadExtensions("net.bluemind.directory", "external", "provider",
				"factory");
		logger.info("External dirs: {}", result.size());
		return result;
	}

	private final List<IExternalDirectory> domList;

	public ExternalDirectories(String domainUid) {
		this.domList = allProviders.stream().flatMap(prov -> prov.getAvailable().stream())
				.filter(dir -> dir.manages(domainUid)).collect(Collectors.toList());
	}

	public List<IExternalDirectory> dirs() {
		return domList;
	}

}
