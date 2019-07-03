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
package net.bluemind.index;

import java.util.List;

import org.elasticsearch.client.Client;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.index.mail.MailIndexService;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.mailindex.hook.IIndexSelectionPolicy;

public class MailIndexActivator implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(MailIndexActivator.class);

	private static BundleContext context;
	private static IIndexSelectionPolicy mailIndexHook;

	static BundleContext getContext() {
		return context;
	}

	private static MailIndexService mailIndexService;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		MailIndexActivator.context = bundleContext;
		init();

		RunnableExtensionLoader<IIndexSelectionPolicy> rel = new RunnableExtensionLoader<>();

		List<IIndexSelectionPolicy> hooks = rel.loadExtensionsWithPriority("net.bluemind.mailindex", "hook", "hook",
				"impl");
		if (!hooks.isEmpty()) {
			MailIndexActivator.mailIndexHook = hooks.get(0);
		}

	}

	private static void init() {
		MailIndexActivator.mailIndexService = new MailIndexService();
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		MailIndexActivator.context = null;
		MailIndexActivator.mailIndexService = null;
		MailIndexActivator.mailIndexHook = null;
	}

	public static IMailIndexService getService() {
		if (mailIndexService == null) {
			init();
		}
		try {
			Client client = ESearchActivator.getClient();
			if (client == null) {
				logger.error("Failed to obtain an elasticsearch client.");
				return null;
			}

			return mailIndexService;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}

	}

	public static IIndexSelectionPolicy getMailIndexHook() {
		return mailIndexHook;
	}
}
