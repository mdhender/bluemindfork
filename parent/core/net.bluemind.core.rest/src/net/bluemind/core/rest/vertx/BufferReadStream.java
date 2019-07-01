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
package net.bluemind.core.rest.vertx;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

public class BufferReadStream implements ReadStream<Void> {

	private Buffer data;
	private boolean finished;
	private Handler<Void> endHandler;
	private boolean running = true;
	private Handler<Buffer> dataHandler;

	public BufferReadStream(Buffer data) {
		this.data = data;
	}

	@Override
	public Void dataHandler(Handler<Buffer> handler) {
		this.dataHandler = handler;
		read();
		return null;
	}

	private void read() {
		if (!running) {
			return;
		}
		dataHandler.handle(data);
		ended();
	}

	private void ended() {
		if (finished) {
			return;

		}

		finished = true;
		if (endHandler != null) {
			endHandler.handle(null);
		}
	}

	@Override
	public Void pause() {
		running = false;
		return null;
	}

	@Override
	public Void resume() {
		running = true;
		if (!finished) {
			read();
		}
		return null;
	}

	@Override
	public Void exceptionHandler(Handler<Throwable> handler) {
		return null;
	}

	@Override
	public Void endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
		return null;
	}

}
