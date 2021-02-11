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
package net.bluemind.imap.vertx.tests;

import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

public class FakeStream implements ReadStream<Buffer> {

	Handler<Buffer> dh;
	Handler<Void> end;
	private boolean paused;
	private byte[] payload;
	private Vertx vx;
	private int length;
	private AtomicBoolean sent;

	public FakeStream(Vertx vx, byte[] payload) {
		this.payload = payload;
		this.vx = vx;
		this.length = payload.length;
		sent = new AtomicBoolean();
	}

	public int length() {
		return length;
	}

	@Override
	public FakeStream handler(Handler<Buffer> handler) {
		this.dh = handler;
		if (handler != null) {
			gogoIfSet();
		}
		return this;
	}

	private void gogoIfSet() {
		if (paused) {
			return;
		}
		if (dh != null && end != null) {
			boolean payloadSent = sent.getAndSet(true);
			final Handler<Buffer> dhSafe = dh;
			final Handler<Void> endSafe = end;
			if (!payloadSent) {
				vx.runOnContext(gg -> {
					dhSafe.handle(Buffer.buffer(payload));
					endSafe.handle(null);
				});
			}
		}
	}

	@Override
	public FakeStream pause() {
		this.paused = true;
		return this;
	}

	@Override
	public FakeStream resume() {
		this.paused = false;
		gogoIfSet();
		return this;
	}

	@Override
	public FakeStream exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public FakeStream endHandler(Handler<Void> endHandler) {
		this.end = endHandler;
		gogoIfSet();
		return this;
	}

	@Override
	public ReadStream<Buffer> fetch(long amount) {
		return this;
	}

}