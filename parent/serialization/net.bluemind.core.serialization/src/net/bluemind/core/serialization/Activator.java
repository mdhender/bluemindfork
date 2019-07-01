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
package net.bluemind.core.serialization;

import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;

public class Activator implements BundleActivator {

	public static List<DataSerializationFactory> serializers;

	@Override
	public void start(BundleContext context) throws Exception {
		MQ.init(() -> MQ.registerProducer(Topic.DATA_SERIALIZATION_NOTIFICATIONS));

		loadSerializerFactories();
	}

	private static void loadSerializerFactories() {
		RunnableExtensionLoader<DataSerializationFactory> epLoader = new RunnableExtensionLoader<>();
		List<DataSerializationFactory> extensions = epLoader.loadExtensions("net.bluemind.core.data", "serializer",
				"serializer", "impl");
		Activator.serializers = extensions;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		Activator.serializers = null;
	}

}
