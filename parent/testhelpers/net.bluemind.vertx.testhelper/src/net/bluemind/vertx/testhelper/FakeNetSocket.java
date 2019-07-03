/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.vertx.testhelper;

import java.net.InetSocketAddress;
import java.util.UUID;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.streams.WriteStream;

public class FakeNetSocket implements NetSocket {

	private Handler<Void> close;
	private final String id;
	private Handler<Void> end;
	private Handler<Buffer> data;
	private Handler<Throwable> ex;
	private Vertx vx;
	private WriteStream<?> writeHandler;

	public FakeNetSocket(Vertx vx, WriteStream<?> writeHandler) {
		this.vx = vx;
		this.id = UUID.randomUUID().toString();
		this.writeHandler = writeHandler;
	}

	@Override
	public NetSocket endHandler(Handler<Void> endHandler) {
		this.end = endHandler;
		return this;
	}

	@Override
	public NetSocket dataHandler(Handler<Buffer> handler) {
		this.data = handler;
		return this;
	}

	@Override
	public NetSocket pause() {
		return this;
	}

	@Override
	public NetSocket resume() {
		return this;
	}

	@Override
	public NetSocket exceptionHandler(Handler<Throwable> handler) {
		this.ex = handler;
		return this;
	}

	@Override
	public NetSocket setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return false;
	}

	@Override
	public NetSocket drainHandler(Handler<Void> handler) {
		if (handler != null) {
			vx.runOnContext(v -> handler.handle(null));
		}
		return this;
	}

	@Override
	public String writeHandlerID() {
		return id;
	}

	@Override
	public NetSocket write(Buffer data) {
		writeHandler.write(data);
		return this;
	}

	@Override
	public NetSocket write(String str) {
		return write(new Buffer(str));
	}

	@Override
	public NetSocket write(String str, String enc) {
		return write(new Buffer(str, enc));
	}

	@Override
	public NetSocket sendFile(String filename) {
		return this;
	}

	@Override
	public NetSocket sendFile(String filename, Handler<AsyncResult<Void>> resultHandler) {
		resultHandler.handle(FakeResult.ok(null));
		return this;
	}

	@Override
	public InetSocketAddress remoteAddress() {
		return InetSocketAddress.createUnresolved("127.0.0.1", 42);
	}

	@Override
	public InetSocketAddress localAddress() {
		return InetSocketAddress.createUnresolved("127.0.0.1", 4200);
	}

	@Override
	public void close() {
		if (close != null) {
			vx.runOnContext(v -> close.handle(null));
		}
	}

	@Override
	public NetSocket closeHandler(Handler<Void> handler) {
		this.close = handler;
		return this;
	}

	@Override
	public NetSocket ssl(Handler<Void> handler) {
		return this;
	}

	@Override
	public boolean isSsl() {
		return false;
	}

}
