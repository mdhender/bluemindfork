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
package net.bluemind.elasticsearch.initializer;

import java.util.Arrays;
import java.util.List;

import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;

public class ElasticSearchServerHook extends DefaultServerHook {
	private static final Logger logger = LoggerFactory.getLogger(ElasticSearchServerHook.class);
	private static final String TAGS = "bm/es";
	private static final List<ISchemaInitializer> initializers = init();

	private static List<ISchemaInitializer> init() {
		RunnableExtensionLoader<ISchemaInitializer> extensionLoader = new RunnableExtensionLoader<ISchemaInitializer>();
		return extensionLoader.loadExtensions("net.bluemind.elasticsearch", "initializer", "initializer",
				"implementation");
	}

	public ElasticSearchServerHook() {
	}

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (!TAGS.equals(tag)) {
			return;
		}

		Client client = null;
		for (ISchemaInitializer initializer : initializers) {
			if (tag.equals(initializer.getTag())) {
				if (client == null) {
					client = ESearchActivator.createClient(Arrays.asList(itemValue.value.address()));
				}
				initializer.initializeSchema(client);
			}
		}
	}

	@Override
	public void onServerAssigned(BmContext context, ItemValue<Server> server, ItemValue<Domain> assignedDomain,
			String tag) throws ServerFault {
		if (!TAGS.equals(tag)) {
			return;
		}

		logger.info("server {}:{} assigned to domain {} as {}", server.uid, server.value.address(), assignedDomain,
				tag);

		ESearchActivator.initClasspath();
	}

	@Override
	public void onServerUnassigned(BmContext context, ItemValue<Server> server, ItemValue<Domain> assignedDomain,
			String tag) throws ServerFault {
		if (!TAGS.equals(tag)) {
			return;
		}

		logger.info("server {}:{} unassigned from domain {} as {}", server.uid, server.value.address(), assignedDomain,
				tag);

		ESearchActivator.initClasspath();
	}

}
