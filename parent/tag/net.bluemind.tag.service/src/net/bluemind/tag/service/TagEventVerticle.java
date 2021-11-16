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
package net.bluemind.tag.service;

import java.util.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class TagEventVerticle extends AbstractVerticle {

	private List<ITagEventConsumer> consumers;

	@Override
	public void start() {
		RunnableExtensionLoader<ITagEventConsumer> loader = new RunnableExtensionLoader<>();
		consumers = loader.loadExtensions("net.bluemind.tag", "eventConsumer", "consumer", "class");
		EventBus eventBus = vertx.eventBus();
		eventBus.consumer("tags.changed",
				(Message<JsonObject> event) -> vertx.executeBlocking(
						prom -> tagChanged(event.body().getString("containerUid"), event.body().getString("itemUid")),
						false));
	}

	protected void tagChanged(String tagContainerUid, String tagUid) {
		for (ITagEventConsumer consumer : consumers) {
			consumer.tagChanged(tagContainerUid, tagUid);
		}
	}
}
