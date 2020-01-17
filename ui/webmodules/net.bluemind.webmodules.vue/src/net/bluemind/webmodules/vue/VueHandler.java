/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.webmodules.vue;

import java.io.File;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class VueHandler implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(VueHandler.class);

	private static boolean vueDevFileExists = new File("/root/dev-vue").exists();
	private static Buffer vueFileContent = Buffer.buffer(loadVueFile());

	@Override
	public void handle(HttpServerRequest event) {
		logger.debug("VueHandler {}", event.path());

		HttpServerResponse response = event.response();
		response.putHeader(HttpHeaders.CONTENT_TYPE, "application/javascript");
		response.putHeader(HttpHeaders.CONTENT_LENGTH, "" + vueFileContent.length());
		response.write(vueFileContent);
		response.setStatusCode(200);
		response.end();
	}

	private static byte[] loadVueFile() {
		String vueFileName = "vue.min.js";
		if (vueDevFileExists) {
			vueFileName = "vue.js";
		}

		try (InputStream in = VueHandler.class.getClassLoader()
				.getResourceAsStream("web-resources/js/" + vueFileName)) {
			logger.debug("Found file " + vueFileName);
			return ByteStreams.toByteArray(in);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return new byte[0];
	}

}
