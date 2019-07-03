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
package net.bluemind.backend.mail.replica.service.internal;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

public class VertxInputReadStream implements ReadStream<VertxInputReadStream> {

	private final InputStream strem;
	private final Vertx vertx;
	private Handler<Buffer> dataHandler;
	private Handler<Void> endHandler;
	private boolean paused;
	private Handler<Throwable> exceptionHandler;
	private static final Logger logger = LoggerFactory.getLogger(VertxInputReadStream.class);

	public VertxInputReadStream(Vertx vx, InputStream in) {
		this.strem = in;
		this.vertx = vx;
	}

	@Override
	public VertxInputReadStream dataHandler(Handler<Buffer> handler) {
		dataHandler = handler;
		read();
		return this;
	}

	@Override
	public VertxInputReadStream pause() {
		paused = true;
		return this;
	}

	@Override
	public VertxInputReadStream resume() {
		paused = false;
		read();
		return this;
	}

	@Override
	public VertxInputReadStream exceptionHandler(Handler<Throwable> handler) {
		this.exceptionHandler = handler;
		return this;
	}

	@Override
	public VertxInputReadStream endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
		read();
		return this;
	}

	private byte[] buffer = new byte[8192];
	private boolean ended;

	private void read() {
		if (paused || dataHandler == null || endHandler == null) {
			return;
		}
		try {

			int size = -1;
			while (true) {

				if (ended || paused) {
					break;
				}

				size = strem.read(buffer);
				if (size == -1) {
					break;
				}
				Buffer buff = new Buffer().appendBytes(buffer, 0, size);

				vertx.runOnContext(v -> {
					dataHandler.handle(buff);
				});
			}
			if (!paused && size == -1) {
				end();
			}
		} catch (IOException e) {
			if (exceptionHandler != null) {
				exceptionHandler.handle(e);
			} else {
				logger.error("error during read ", e);
			}
		}

	}

	private void end() {
		ended = true;
		try {
			strem.close();
		} catch (IOException e) {
			logger.warn("Cannot close input stream", e);
		}
		vertx.runOnContext(v -> {
			endHandler.handle(null);
		});
	}
}
