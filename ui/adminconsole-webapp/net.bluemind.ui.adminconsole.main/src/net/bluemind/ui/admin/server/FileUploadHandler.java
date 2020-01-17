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
package net.bluemind.ui.admin.server;

import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

public class FileUploadHandler implements Handler<HttpServerRequest> {
	Logger logger = LoggerFactory.getLogger(FileUploadHandler.class);

	@Override
	public void handle(final HttpServerRequest request) {
		request.setExpectMultipart(true);
		request.uploadHandler(new Handler<HttpServerFileUpload>() {

			@Override
			public void handle(HttpServerFileUpload upload) {
				final Buffer buffer = Buffer.buffer();
				final String filename = upload.filename();

				logger.info("Handling file-upload. filename: {}", filename.toString());

				upload.handler(new Handler<Buffer>() {

					@Override
					public void handle(Buffer buff) {
						buffer.appendBuffer(buff);
					}
				});

				upload.endHandler(new Handler<Void>() {

					@Override
					public void handle(Void arg0) {
						JsonObject fileUploadResponse = new JsonObject();
						fileUploadResponse.put("filename", filename);
						fileUploadResponse.put("data", getBase64Content(buffer));
						logger.info("Sending file-upload response for filename {}", filename);
						HttpServerResponse resp = request.response();
						resp.putHeader("Content-Type", "application/json");
						resp.setStatusCode(200);
						resp.end(fileUploadResponse.toString());
					}

					private String getBase64Content(Buffer buffer) {
						return Base64.getEncoder().encodeToString(buffer.getBytes());
					}

				});
			}
		});
	}

}
