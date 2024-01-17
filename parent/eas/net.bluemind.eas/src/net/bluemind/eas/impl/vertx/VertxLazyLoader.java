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

import io.vertx.core.Promise;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.LazyLoaded;
import net.bluemind.lib.vertx.VertxPlatform;

public final class VertxLazyLoader {

	private VertxLazyLoader() {
	}

	public static final LazyLoaded<BodyOptions, AirSyncBaseResponse> wrap(
			final LazyLoaded<BodyOptions, AirSyncBaseResponse> toWrap) {
		return new LazyLoaded<BodyOptions, AirSyncBaseResponse>(toWrap.query) {

			@Override
			public void load(final Callback<AirSyncBaseResponse> onLoad) {
				VertxPlatform.getVertx().executeBlocking((Promise<AirSyncBaseResponse> p) -> toWrap.load(p::complete),
						r -> onLoad.onResult(r.result()));
			}
		};
	}

}
