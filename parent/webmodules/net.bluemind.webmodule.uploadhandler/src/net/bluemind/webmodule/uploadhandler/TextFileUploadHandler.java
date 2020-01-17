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
package net.bluemind.webmodule.uploadhandler;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.core.utils.JsonUtils;

public class TextFileUploadHandler implements Handler<HttpServerRequest> {
	Logger logger = LoggerFactory.getLogger(TextFileUploadHandler.class);

	@Override
	public void handle(final HttpServerRequest request) {
		request.setExpectMultipart(true);
		final List<FileUpload> files = new LinkedList<>();
		request.uploadHandler(new Handler<HttpServerFileUpload>() {

			@Override
			public void handle(HttpServerFileUpload upload) {
				final Buffer buffer = Buffer.buffer();
				final String name = upload.name();
				final String filename = upload.filename();

				logger.info("Handling text file-upload. filename: {} for field {}", filename, name);

				upload.handler(new Handler<Buffer>() {

					@Override
					public void handle(Buffer buff) {
						buffer.appendBuffer(buff);
					}
				});

				upload.endHandler(new Handler<Void>() {

					@Override
					public void handle(Void arg0) {
						// FIXME encoding
						files.add(new FileUpload(name, filename, buffer.toString(), "text"));
					}

				});
			}
		});

		request.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				HttpServerResponse resp = request.response();
				resp.putHeader("Content-Type", "application/json");
				resp.setStatusCode(200);
				resp.end(JsonUtils.asString(files));
			}
		});
	}

}