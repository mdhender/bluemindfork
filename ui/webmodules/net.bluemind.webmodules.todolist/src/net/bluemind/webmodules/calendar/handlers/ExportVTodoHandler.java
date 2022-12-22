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
import net.bluemind.core.container.api.IContainersPromise;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.todolist.api.IVTodoPromise;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.server.export.ExportHelper;

public class ExportVTodoHandler implements Handler<HttpServerRequest>, NeedVertx {

	private HttpClientProvider prov;

	@Override
	public void setVertx(Vertx vertx) {
		prov = new HttpClientProvider(vertx);
	}

	@Override
	public void handle(final HttpServerRequest request) {
		String container = request.params().get("containerUid");
		String sessionId = request.headers().get("BMSessionId");

		final VertxPromiseServiceProvider clientProvider = new VertxPromiseServiceProvider(prov, ExportHelper.locator(),
				sessionId);

		IContainersPromise icp = clientProvider.instance(IContainersPromise.class);
		IVTodoPromise ivep = clientProvider.instance(IVTodoPromise.class, container);

		icp.get(container).thenCombine(ivep.exportAll(),
				(containerDescriptor, ics) -> ExportHelper.setResponse(request, "vtodo", containerDescriptor, ics))
				.exceptionally(e -> {
					return ExportHelper.error(request.response(), container, e);
				});
	}

}
