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
package net.bluemind.eas.impl.vertx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.LazyLoaded;
import net.bluemind.vertx.common.LocalJsonObject;

public class WorkerLazyLoader extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(WorkerLazyLoader.class);

	public void start() {

		vertx.eventBus().consumer("eas.backend.lazyloader",
				new Handler<Message<LocalJsonObject<LazyLoaded<BodyOptions, AirSyncBaseResponse>>>>() {

					@Override
					public void handle(
							final Message<LocalJsonObject<LazyLoaded<BodyOptions, AirSyncBaseResponse>>> event) {
						LazyLoaded<BodyOptions, AirSyncBaseResponse> toLoad = event.body().getValue();
						logger.debug("Lazy loading {}", toLoad);
						try {
							toLoad.load(new Callback<AirSyncBaseResponse>() {

								@Override
								public void onResult(AirSyncBaseResponse data) {
									LocalJsonObject<AirSyncBaseResponse> resp = new LocalJsonObject<>(data);
									event.reply(resp);
								}
							});
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
							LocalJsonObject<AirSyncBaseResponse> resp = new LocalJsonObject<>(null);
							event.reply(resp);
						}
					}
				});
	}

}
