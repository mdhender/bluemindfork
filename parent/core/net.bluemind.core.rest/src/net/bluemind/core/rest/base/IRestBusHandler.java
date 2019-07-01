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
package net.bluemind.core.rest.base;

import java.util.function.Function;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.api.fault.ServerFault;

public interface IRestBusHandler {

	void register(RestRequest request, Function<Void, Handler<Message<?>>> msgHandler, Handler<ServerFault> reject);

	void unregisterHandler(String path, Handler<Message<?>> handler);

	void sendEvent(RestRequest request, JsonObject evt);

	void sendEvent(RestRequest request, JsonObject evt, Handler<Message<JsonObject>> handler);
}
