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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.dynresources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.system.api.CustomLogo;
import net.bluemind.webmodule.server.WebModule;
import net.bluemind.webmodule.server.handlers.IWebModuleConsumer;

public class LogoHandler implements Handler<HttpServerRequest>, IWebModuleConsumer {

	private static final Logger logger = LoggerFactory.getLogger(LogoHandler.class);
	private WebModule module;

	@Override
	public void handle(HttpServerRequest event) {
		logger.debug("LogoHandler {}", event.path());
		CustomLogo logo = LogoManager.getLogo();
		if (logo != null) {
			HttpServerResponse response = event.response();
			response.putHeader("Content-Length", "" + logo.content.length);
			response.putHeader("ContentType", "image/png");
			response.write(Buffer.buffer(logo.content));
			response.setStatusCode(200);
			response.end();
		} else {
			module.defaultHandler.handle(event);
		}
	}

	@Override
	public void setModule(WebModule module) {
		this.module = module;
	}

}
