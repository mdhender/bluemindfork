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
package net.bluemind.core.rest.utils;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

public class InputReadStream implements ReadStream<InputReadStream> {

	private InputStream strem;
	private Handler<Buffer> dataHandler;
	private boolean paused = true;
	private Handler<Void> endHandler;
	private Handler<Throwable> exceptionHandler;
	private static final Logger logger = LoggerFactory.getLogger(InputReadStream.class);

	public InputReadStream(InputStream in) {
		this.strem = in;
	}

	@Override
	public InputReadStream dataHandler(Handler<Buffer> handler) {
		dataHandler = handler;
		if (!paused) {
			// starting to stream
			read();
		}
		return this;
	}

	@Override
	public InputReadStream pause() {
		paused = true;
		return this;
	}

	@Override
	public InputReadStream resume() {

		if (dataHandler != null && paused) {
			paused = false;
			read();
		}
		return this;
	}

	@Override
	public InputReadStream exceptionHandler(Handler<Throwable> handler) {
		this.exceptionHandler = handler;
		return this;
	}

	@Override
	public InputReadStream endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
		return this;
	}

	private byte[] buffer = new byte[4096];
	private boolean ended;

	private void read() {
		try {

			int size = -1;
			while (true) {

				if (ended || paused || dataHandler == null) {
					break;
				}

				size = strem.read(buffer);
				if (size == -1) {
					break;
				}
				Buffer buff = new Buffer().appendBytes(buffer, 0, size);

				dataHandler.handle(buff);

			}
			if (!paused && size == -1 && dataHandler != null) {
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
		if (endHandler != null) {
			endHandler.handle(null);
			endHandler = null;
		}
		try {
			strem.close();
		} catch (IOException e) {
			logger.warn("Cannot close input stream", e);
		}
	}
}
