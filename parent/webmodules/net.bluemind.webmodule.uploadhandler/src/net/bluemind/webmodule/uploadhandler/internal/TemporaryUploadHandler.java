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
package net.bluemind.webmodule.uploadhandler.internal;

import java.io.File;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.file.AsyncFile;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.streams.Pump;

import net.bluemind.core.utils.JsonUtils;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.uploadhandler.TemporaryUploadRepository;
import net.bluemind.webmodule.uploadhandler.TemporaryUploadRepository.UniqueFile;

public class TemporaryUploadHandler implements Handler<HttpServerRequest>, NeedVertx {
	Logger logger = LoggerFactory.getLogger(TemporaryUploadHandler.class);

	private Vertx vertx;
	private TemporaryUploadRepository repository;

	@Override
	public void handle(final HttpServerRequest request) {
		request.exceptionHandler(exceptionHandler(request));

		if (request.method().equals("GET")) {
			String uuidAsString = request.params().get("uuid");
			UUID parsed = null;
			try {
				parsed = UUID.fromString(uuidAsString);
			} catch (IllegalArgumentException e) {
				request.response().setStatusCode(404).end();
				return;
			}
			File f = repository.getTempFile(parsed);
			if (f.exists()) {
				sendFile(request, f);
			} else {
				request.response().setStatusCode(404).end();
			}

		} else {
			request.expectMultiPart(true);
			request.uploadHandler(upload -> {
				upload.exceptionHandler(exceptionHandler(request));
				upload.pause();
				logger.debug("upload temporay file {}", upload.filename());
				doUpload(request, upload);
			});
		}
	}

	private void sendFile(final HttpServerRequest request, File f) {

		vertx.fileSystem().open(f.getAbsolutePath(), aFile -> {
			final AsyncFile file = aFile.result();
			final Buffer ret = new Buffer();
			file.endHandler(v -> {
				file.close();
				if ("application/json".equals(request.headers().get("Accept"))) {
					request.response().end(JsonUtils.asString(ret.getBytes()));
				} else {
					request.response().end(ret);
				}
			});
			file.dataHandler(buffer -> ret.appendBuffer(buffer));
		});

	}

	private Handler<Throwable> exceptionHandler(final HttpServerRequest request) {
		return e -> {
			logger.error("error during temp upload", e);
			sendError("system error", request.response());
		};
	}

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
		repository = new TemporaryUploadRepository(vertx);

	}

	private void doUpload(final HttpServerRequest request, final HttpServerFileUpload upload) {
		final UniqueFile file = repository.createTempFile();
		if (file == null) {
			sendError("system error", request.response());
			return;
		}

		logger.debug("create temp file {}", file.file.getAbsolutePath());
		vertx.fileSystem().open(repository.getTempFile(file.uuid).getPath(), res -> {
			if (res.failed()) {
				sendError("system error", request.response());
				return;
			}
			upload.endHandler(v -> {
				doResize(request, file.uuid);
				logger.debug("upload succeed, return 200 and uuid {}", file.uuid);
				HttpServerResponse resp = request.response();
				resp.headers().add("Content-Type", "text/plain");
				resp.setStatusCode(200).end(file.uuid.toString());
			});
			Pump.createPump(upload, res.result()).start();
			upload.resume();

		});

	}

	@SuppressWarnings("unused")
	private void doResize(HttpServerRequest request, UUID randUuid) {
		// no need to resize
	}

	private void sendError(String message, HttpServerResponse response) {
		response.setStatusCode(500).setStatusMessage(message).end();
	}

}
