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
package net.bluemind.core.rest.vertx;

import org.vertx.java.platform.Verticle;

import net.bluemind.core.rest.base.RestRootHandler;

public class RestBusVerticle extends Verticle {

	@Override
	public void start() {
		getVertx().eventBus().registerHandler("bm-core",
				new RestVertxRootHandler(vertx, new RestRootHandler(getVertx())));
		getVertx().eventBus().registerHandler("bm-core-json",
				new RestJsonVertxRootHandler(vertx, new RestRootHandler(vertx)));

	}

}
