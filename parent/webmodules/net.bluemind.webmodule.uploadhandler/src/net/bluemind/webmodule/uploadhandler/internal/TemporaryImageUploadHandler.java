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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.Pump;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.uploadhandler.TemporaryUploadRepository;
import net.bluemind.webmodule.uploadhandler.TemporaryUploadRepository.UniqueFile;

public class TemporaryImageUploadHandler implements Handler<HttpServerRequest>, NeedVertx {
	private static final int MAX_WIDTH = 800;
	private static final int MAX_HEIGHT = 800;
	Logger logger = LoggerFactory.getLogger(TemporaryImageUploadHandler.class);

	private Vertx vertx;
	private TemporaryUploadRepository repository;

	@Override
	public void handle(final HttpServerRequest request) {
		request.exceptionHandler(exceptionHandler(request));

		if (request.method() == HttpMethod.GET) {
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
				request.response().sendFile(f.getAbsolutePath());

			} else {
				request.response().setStatusCode(404).end();
			}

		} else {
			request.setExpectMultipart(true);
			request.uploadHandler(upload -> {
				upload.exceptionHandler(exceptionHandler(request));
				upload.pause();
				logger.debug("upload temporay file {}", upload.filename());
				doUpload(request, upload);
			});
		}
	}

	private Handler<Throwable> exceptionHandler(final HttpServerRequest request) {
		return new Handler<Throwable>() {

			@Override
			public void handle(Throwable e) {
				logger.error("error during temp upload", e);
				sendError("system error", request.response());
			}
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
		vertx.fileSystem().open(repository.getTempFile(file.uuid).getPath(), new OpenOptions(),
				(AsyncResult<AsyncFile> res) -> {
					if (res.failed()) {
						sendError("system error", request.response());
						return;
					}
					upload.endHandler(new Handler<Void>() {

						@Override
						public void handle(Void arg0) {
							doResize(request, file.uuid);
						}

					});
					Pump.pump(upload, res.result()).start();
					upload.resume();
				});

	}

	private void doResize(final HttpServerRequest request, UUID uuid) {

		int maxWidth = MAX_WIDTH;
		int maxHeight = MAX_HEIGHT;
		if (request.params().contains("width")) {
			try {
				maxWidth = Integer.parseInt(request.params().get("width"));
			} catch (Exception e) {

			}
		}

		if (request.params().contains("height")) {
			try {
				maxHeight = Integer.parseInt(request.params().get("height"));
			} catch (Exception e) {

			}
		}
		try (ImageInputStream iis = ImageIO.createImageInputStream(new FileInputStream(repository.getTempFile(uuid)))) {
			Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
			if (!iter.hasNext()) {
				sendError("error reading image", request.response());
				return;
			}
			ImageReader reader = (ImageReader) iter.next();

			reader.setInput(iis);
			BufferedImage img = reader.read(0);
			if (img.getWidth() < maxWidth && img.getHeight() < maxHeight) {
				logger.debug("upload succeed, return 200 and uuid {}", uuid);
				HttpServerResponse resp = request.response();
				resp.headers().add("Content-Type", "text/plain");
				resp.setStatusCode(200).end(uuid.toString());
				return;
			}

			double tw = img.getWidth();
			double th = img.getHeight();
			double ratio = 1.0;
			if (tw > maxWidth) {
				ratio = (double) maxWidth / (double) tw;
			}

			if (th * ratio > maxHeight) {
				ratio = (double) maxHeight / (double) th;
			}

			double sws = ratio * tw;
			double shs = ratio * th;

			BufferedImage dbi = new BufferedImage((int) sws, (int) shs, img.getType());
			Graphics2D g = dbi.createGraphics();
			AffineTransform at = AffineTransform.getScaleInstance(ratio, ratio);
			g.drawRenderedImage(img, at);
			ByteArrayOutputStream ret = new ByteArrayOutputStream();
			ImageIO.write(dbi, "png", ret);

			UniqueFile rf = repository.createTempFile();
			vertx.fileSystem().writeFile(rf.file.getAbsolutePath(), Buffer.buffer(ret.toByteArray()),
					new Handler<AsyncResult<Void>>() {

						@Override
						public void handle(AsyncResult<Void> event) {
							HttpServerResponse resp = request.response();
							resp.setStatusCode(200);
							resp.end(rf.uuid.toString());
							return;
						}

					});
		} catch (IOException e) {
			sendError("error transforming image", request.response());

		}

	}

	private void sendError(String message, HttpServerResponse response) {
		response.setStatusCode(500);
		response.setStatusMessage(message);
		response.end();
	}

}
