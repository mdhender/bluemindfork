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
package net.bluemind.backend.cyrus.replication.testhelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

public class InputStreamWrapper implements ReadStream<Buffer> {

	private static final Logger logger = LoggerFactory.getLogger(InputStreamWrapper.class);
	private Handler<Buffer> data;
	private InputStream input;
	private Vertx vertx;
	private Handler<Void> end;
	private Handler<Throwable> error;
	private boolean paused;

	public InputStreamWrapper(Vertx vertx, InputStream input) {
		Objects.requireNonNull(vertx, "Vertx must not be null");
		// Objects.requireNonNull(vertx.currentContext(), "Vertx context must
		// not be null");
		this.vertx = vertx;
		this.input = input;
	}

	@Override
	public InputStreamWrapper handler(Handler<Buffer> handler) {
		logger.debug("Setting dataHandler with {}", handler);
		this.data = handler;
		checkReadable();
		return this;
	}

	@Override
	public InputStreamWrapper exceptionHandler(Handler<Throwable> handler) {
		this.error = handler;
		return this;
	}

	@Override
	public InputStreamWrapper endHandler(Handler<Void> endHandler) {
		logger.debug("Setting endHandler with {}", endHandler);
		this.end = endHandler;
		checkReadable();
		return this;
	}

	@Override
	public InputStreamWrapper pause() {
		this.paused = true;
		return this;
	}

	@Override
	public InputStreamWrapper resume() {
		this.paused = false;
		checkReadable();
		return this;
	}

	private void checkReadable() {
		if (!paused && data != null && end != null) {
			doReadLoop();
		}
	}

	private void doReadLoop() {
		if (Vertx.currentContext() != null) {
			vertx.runOnContext(xxx -> {
				loop();
			});
		} else {
			vertx.setTimer(1, xxx -> {
				loop();
			});
		}

	}

	private void loop() {
		byte[] buf = new byte[8192];
		try {
			int read = input.read(buf, 0, buf.length);
			if (read == -1) {
				end.handle(null);
			} else {
				byte[] validBytes = new byte[read];
				System.arraycopy(buf, 0, validBytes, 0, read);
				data.handle(Buffer.buffer(validBytes));
				checkReadable();
			}
		} catch (IOException e) {
			error.handle(e);
		}
	}

	@Override
	public ReadStream<Buffer> fetch(long amount) {
		return this;
	}

}
