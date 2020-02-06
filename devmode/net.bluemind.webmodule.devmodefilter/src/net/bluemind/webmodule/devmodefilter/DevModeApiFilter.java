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
package net.bluemind.webmodule.devmodefilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.NeedVertx;

/**
 * format du fichier /root/dev-filters.properties redirection http:
 * https-forward=/adminconsole/net.bluemind.ui.adminconsole.main:192.168.0.1:8995:/net.bluemind.ui.adminconsole.main
 * port-forward=SRC_PORT:DST_HOST
 *
 */
public class DevModeApiFilter implements IWebFilter, NeedVertx {
	private static final Logger logger = LoggerFactory.getLogger(DevModeApiFilter.class);

	private Vertx vertx;

	public DevModeApiFilter() {

	}

	@Override
	public HttpServerRequest filter(HttpServerRequest request) {
		if (!request.uri().endsWith("reload-devmode")) {
			return request;
		}

		logger.info("reload devmode state");
		vertx.eventBus().request("devmode.state:reload", true, (AsyncResult<Message<Boolean>> m) -> {
			if (m.succeeded() && m.result().body()) {
				request.response().setStatusCode(200).setStatusMessage("reloaded").end();
			} else {
				request.response().setStatusCode(500).setStatusMessage("reload failed").end();
			}
		});
		return null;
	}

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

}
