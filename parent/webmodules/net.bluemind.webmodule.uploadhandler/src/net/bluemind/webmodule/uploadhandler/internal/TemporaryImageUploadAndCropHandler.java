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
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.uploadhandler.TemporaryUploadRepository;
import net.bluemind.webmodule.uploadhandler.TemporaryUploadRepository.UniqueFile;

public class TemporaryImageUploadAndCropHandler implements Handler<HttpServerRequest>, NeedVertx {
	Logger logger = LoggerFactory.getLogger(TemporaryImageUploadAndCropHandler.class);

	private Vertx vertx;
	private TemporaryUploadRepository repository;

	@Override
	public void handle(final HttpServerRequest request) {
		request.exceptionHandler(exceptionHandler(request));

		String uuidAsString = request.params().get("uuid");
		UUID parsed = null;
		try {
			parsed = UUID.fromString(uuidAsString);
		} catch (IllegalArgumentException e) {
			request.response().setStatusCode(404).end();
			return;
		}
		Integer xs = Integer.parseInt(request.params().get("x"));
		Integer ys = Integer.parseInt(request.params().get("y"));
		Integer ws = Integer.parseInt(request.params().get("w"));
		Integer hs = Integer.parseInt(request.params().get("h"));

		Integer sws = Integer.parseInt(request.params().get("sw"));
		Integer shs = Integer.parseInt(request.params().get("sh"));
		File f = repository.getTempFile(parsed);

		if (!f.exists()) {
			request.response().setStatusCode(404);
			request.response().end();
			return;
		}

		try (ImageInputStream iis = ImageIO.createImageInputStream(new FileInputStream(f))) {
			BufferedImage img = Subsampling.toBufferredImage(iis);
			BufferedImage dbi = new BufferedImage(sws, sws, img.getType());
			Graphics2D g = dbi.createGraphics();
			double xscale = (double) sws / (double) ws;
			double yscale = (double) shs / (double) hs;
			AffineTransform at = AffineTransform.getTranslateInstance(-xs, -ys);

			at.preConcatenate(AffineTransform.getScaleInstance(xscale, yscale));
			g.drawRenderedImage(img, at);
			ByteArrayOutputStream ret = new ByteArrayOutputStream();
			ImageIO.write(dbi, "png", ret);
			UniqueFile rf = repository.createTempFile();
			vertx.fileSystem().writeFile(rf.file.getAbsolutePath(), Buffer.buffer(ret.toByteArray()), res -> {
				HttpServerResponse resp = request.response();
				resp.headers().add("Content-Type", "text/plain");
				resp.setStatusCode(200).end(rf.uuid.toString());
			});
		} catch (IOException e) {
			sendError("error transforming image", request.response());

		}
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

	private void sendError(String message, HttpServerResponse response) {
		response.setStatusCode(500);
		response.setStatusMessage(message);
		response.end();
	}

}
