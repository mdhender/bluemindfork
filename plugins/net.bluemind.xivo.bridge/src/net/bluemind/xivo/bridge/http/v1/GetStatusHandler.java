/*BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012
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
 * END LICENSE
 */

package net.bluemind.xivo.bridge.http.v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;

import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.xivo.common.PhoneStatus;

public class GetStatusHandler implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(GetStatusHandler.class);

	@Override
	public void handle(final HttpServerRequest event) {
		event.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void v) {
				MultiMap p = event.params();
				String latd = new StringBuilder()//
						.append(p.get("login")) //
						.append('@') //
						.append(p.get("domain")) //
						.toString();
				ConcurrentSharedMap<String, Integer> sharedStatus = VertxPlatform.getVertx().sharedData()
						.getMap("phone_status");
				Integer statusCode = sharedStatus.get(latd);
				JsonObject status = new JsonObject();

				if (statusCode == null) {
					logger.warn("Unknown status for '{}'", latd);
					status.putString("status", "UNKNOWN");
				} else {
					status.putString("status", PhoneStatus.fromCode(statusCode).name());
				}
				logger.info("Fetch status of '{}' => {}", latd, status.encodePrettily());
				HttpServerResponse resp = event.response();
				resp.end(status.encode());
			}
		});
	}
}
