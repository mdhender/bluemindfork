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

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;

import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.LazyLoaded;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.vertx.common.LocalJsonObject;

public final class VertxLazyLoader {

	public static final LazyLoaded<BodyOptions, AirSyncBaseResponse> wrap(
			final LazyLoaded<BodyOptions, AirSyncBaseResponse> toWrap) {
		LazyLoaded<BodyOptions, AirSyncBaseResponse> ret = new LazyLoaded<BodyOptions, AirSyncBaseResponse>(
				toWrap.query) {

			@Override
			public void load(final Callback<AirSyncBaseResponse> onLoad) {
				LocalJsonObject<LazyLoaded<BodyOptions, AirSyncBaseResponse>> jso = new LocalJsonObject<>(toWrap);
				VertxPlatform.eventBus().send("eas.backend.lazyloader", jso,
						new Handler<Message<LocalJsonObject<AirSyncBaseResponse>>>() {

							@Override
							public void handle(Message<LocalJsonObject<AirSyncBaseResponse>> event) {
								onLoad.onResult(event.body().getValue());
							}
						});
			}
		};
		return ret;
	}

}
