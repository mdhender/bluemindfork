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
package net.bluemind.content.analysis;

import java.util.List;
import java.util.Optional;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class ContentAnalyzerFactory implements BundleActivator {

	private static ContentAnalyzer analyzer;
	private static final Logger logger = LoggerFactory.getLogger(ContentAnalyzerFactory.class);

	public static Optional<ContentAnalyzer> get() {
		return Optional.ofNullable(analyzer);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		RunnableExtensionLoader<ContentAnalyzer> epLoader = new RunnableExtensionLoader<>();
		List<ContentAnalyzer> extensions = epLoader.loadExtensionsWithPriority("net.bluemind.content.analysis",
				"analyzer", "analyzer", "impl");
		logger.info("Loaded {} content analyzers", extensions.size());
		if (!extensions.isEmpty()) {
			ContentAnalyzerFactory.analyzer = extensions.get(0);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {

	}

}
