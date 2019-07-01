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
package net.bluemind.directory.service;

import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

import net.bluemind.directory.service.internal.DirectoryDecorator;
import net.bluemind.directory.service.internal.DirectoryService;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;

public class DirectoryActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {

		MQ.init(new MQ.IMQConnectHandler() {

			@Override
			public void connected() {
				MQ.registerProducer(Topic.DIRECTORY_NOTIFICATIONS);
			}
		});

		DirectoryService.decorators = loadDecorators();
		LoggerFactory.getLogger(DirectoryActivator.class).info("Loaded {} directory decorators",
				DirectoryService.decorators.size());

	}

	private List<DirectoryDecorator> loadDecorators() {
		RunnableExtensionLoader<DirectoryDecorator> loader = new RunnableExtensionLoader<>();
		return loader.loadExtensions("net.bluemind.directory", "decorator", "decorator", "impl");
	}

	@Override
	public void stop(BundleContext context) throws Exception {

	}

}
