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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.system.application.registration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.utils.JsonUtils;

public class ApplicationRegistration extends AbstractVerticle {

	public static final String APPLICATION_REGISTRATION = "bm.application.registration";
	public static final Logger logger = LoggerFactory.getLogger(ApplicationRegistration.class);
	private final Store store;

	public ApplicationRegistration(Store store) {
		this.store = store;
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {

		vertx.eventBus().consumer(APPLICATION_REGISTRATION, event -> {
			ApplicationInfo info = JsonUtils.read(((JsonObject) event.body()).encode(), ApplicationInfo.class);
			Publisher applicationPublisher = store.getPublisher(new DefaultTopicDescriptor("bluemind_cluster",
					"__nodes__", "system", "application-registration", info.product));
			applicationPublisher.store(info.product, info.machineId.getBytes(), info.toJson().getBytes());
		});

		startPromise.complete();
	}

}
