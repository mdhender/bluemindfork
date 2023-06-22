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
package net.bluemind.cloud.monitoring.server.grafana;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.cloud.monitoring.server.api.model.DataState;

public class Topology implements Handler<HttpServerRequest> {

	public Topology() {
	}

	@Override
	public void handle(HttpServerRequest request) {
		request.response().headers().add("Access-Control-Allow-Origin", "*");
		request.response().setStatusCode(200);
		request.response().end(DataState.getTopology());

	}

}
