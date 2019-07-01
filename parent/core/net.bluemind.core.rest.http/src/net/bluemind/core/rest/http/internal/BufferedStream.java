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
package net.bluemind.core.rest.http.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

public class BufferedStream implements ReadStream<BufferedStream> {
	private static final Logger logger = LoggerFactory.getLogger(BufferedStream.class);
	private Handler<Void> endHandler;
	private boolean pause;
	private Handler<Buffer> dataHandler;
	private Buffer buffer = new Buffer();
	private boolean end;

	@Override
	public BufferedStream dataHandler(Handler<Buffer> handler) {
		this.dataHandler = handler;
		logger.debug("datahadler drain");
		drain();
		return this;
	}

	@Override
	public BufferedStream pause() {
		logger.debug("pause");
		this.pause = true;
		return this;
	}

	@Override
	public BufferedStream resume() {
		if (pause) {
			logger.debug("resume");
			pause = false;
			logger.debug("resume drain");
			drain();
		}
		return this;
	}

	private synchronized void drain() {
		if (!pause && dataHandler != null && buffer.length() > 0) {
			Buffer oldBuffer = buffer;
			buffer = new Buffer();
			dataHandler.handle(oldBuffer);
		}

		if (!pause && endHandler != null && buffer.length() == 0 && end) {
			endHandler.handle(null);
		}
	}

	@Override
	public BufferedStream exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public BufferedStream endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
		return this;
	}

	public void write(Buffer data) {
		this.buffer.appendBuffer(data);
		logger.debug("writecall drain");
		drain();
	}

	public void end() {
		end = true;
		logger.debug("endcall drain");
		drain();
	}

}
