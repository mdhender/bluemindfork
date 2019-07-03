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
package net.bluemind.backend.mail.replica.indexing;

import java.util.List;
import java.util.Optional;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;

public class RecordIndexActivator implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(RecordIndexActivator.class);
	private static Optional<IMailIndexService> indexer = Optional.empty();

	public static class ReloadHook extends DefaultServerHook {

		@Override
		public void onServerTagged(BmContext context, ItemValue<Server> itemValue, String tag) {
			if ("bm/es".equals(tag)) {
				logger.info("Refresh mail indexer for new ES tag.");
				loadIndexer();
			}
		}

	}

	@Override
	public void start(BundleContext context) throws Exception {
		loadIndexer();
	}

	@Override
	public void stop(BundleContext context) throws Exception {

	}

	public static Optional<IMailIndexService> getIndexer() {
		return indexer;
	}

	/**
	 * tests can use this because server hooks are not called by populate helper
	 */
	@VisibleForTesting
	public static void reload() {
		loadIndexer();
	}

	private static void loadIndexer() {
		RunnableExtensionLoader<MailRecordIndexingFactory> epLoader = new RunnableExtensionLoader<>();
		List<MailRecordIndexingFactory> extensions = epLoader
				.loadExtensions("net.bluemind.backend.mail.replica.indexing", "indexer", "indexer", "factory");
		if (!extensions.isEmpty()) {
			indexer = Optional.ofNullable(extensions.get(0).get());
		}
		if (!indexer.isPresent()) {
			logger.warn("Mail replica indexing is not available.");
		}

	}

}
