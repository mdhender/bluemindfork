/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class CustomStyleHandler implements Handler<HttpServerRequest> {
	private static final Logger logger = LoggerFactory.getLogger(CustomStyleHandler.class);

	private static byte[] customCss = loadCustomLogoCss();

	@Override
	public void handle(HttpServerRequest event) {
		HttpServerResponse response = event.response();
		response.putHeader(HttpHeaders.CONTENT_TYPE, "text/css");

		if (!LogoManager.hasCustomLogo()) {
			response.setStatusCode(200);
			response.end();
			return;
		}

		String version = Activator.getContext().getBundle().getVersion().toString();

		response.putHeader(HttpHeaders.ETAG, version);

		String ifNoneMatch = event.headers().get(HttpHeaders.IF_NONE_MATCH);
		if (version.equals(ifNoneMatch)) {
			response.setStatusCode(304);
			response.end();
			return;
		}

		response.setStatusCode(200);
		// FIXME make buffer static
		response.end(Buffer.buffer(customCss));
	}

	private static byte[] loadCustomLogoCss() {
		try (InputStream in = LogoHandler.class.getClassLoader().getResourceAsStream("web-resources/customlogo.css")) {
			return ByteStreams.toByteArray(in);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return new byte[0];
	}

}