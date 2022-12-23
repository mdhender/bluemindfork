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
package net.bluemind.webmodules.calendar.handlers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.todolist.api.IVTodo;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.server.export.ExportHelper;

public class ExportVTodoHandler implements Handler<HttpServerRequest>, NeedVertx {

	private Vertx vertx;

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public void handle(final HttpServerRequest request) {
		String container = request.params().get("containerUid");
		String sessionId = request.headers().get("BMSessionId");

		ExportHelper.export(vertx, sessionId, request, "vtodo", container,
				client -> client.instance(IVTodo.class, container).exportAll());
	}

}
