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

import java.security.cert.Certificate;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.WriteStream;

public class FakeNetSocket implements NetSocket {

	private Handler<Void> close;
	private final String id;
	private Handler<Void> end;
	private Handler<Buffer> data;
	private Handler<Throwable> ex;
	private Vertx vx;
	private WriteStream<Buffer> writeHandler;

	public FakeNetSocket(Vertx vx, WriteStream<Buffer> writeHandler) {
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
	public NetSocket handler(Handler<Buffer> handler) {
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
	public Future<Void> write(Buffer data) {
		writeHandler.write(data);
		return Future.succeededFuture();
	}

	@Override
	public Future<Void> write(String str) {
		return write(Buffer.buffer(str));
	}

	@Override
	public Future<Void> write(String str, String enc) {
		return write(Buffer.buffer(str, enc));
	}

	@Override
	public Future<Void> sendFile(String filename) {
		return Future.succeededFuture();
	}

	@Override
	public NetSocket sendFile(String filename, Handler<AsyncResult<Void>> resultHandler) {
		resultHandler.handle(FakeResult.ok(null));
		return this;
	}

	@Override
	public SocketAddress remoteAddress() {
		return SocketAddress.inetSocketAddress(42, "127.0.0.1");
	}

	@Override
	public SocketAddress localAddress() {
		return SocketAddress.inetSocketAddress(4200, "127.0.0.1");
	}

	@Override
	public Future<Void> close() {
		return Future.future(h -> {
			if (close != null) {
				vx.runOnContext(v -> close.handle(null));
			}
		});
	}

	@Override
	public NetSocket closeHandler(Handler<Void> handler) {
		this.close = handler;
		return this;
	}

	@Override
	public boolean isSsl() {
		return false;
	}

	@Override
	public NetSocket fetch(long amount) {
		return this;
	}

	@Override
	public void write(String str, Handler<AsyncResult<Void>> handler) {
		write(Buffer.buffer(str), handler);
	}

	@Override
	public void write(String str, String enc, Handler<AsyncResult<Void>> handler) {
		write(Buffer.buffer(str, enc), handler);
	}

	@Override
	public void write(Buffer message, Handler<AsyncResult<Void>> handler) {
		write(message);
		handler.handle(FakeResult.ok(null));
	}

	@Override
	public Future<Void> sendFile(String filename, long offset, long length) {
		return Future.succeededFuture();
	}

	@Override
	public NetSocket sendFile(String filename, long offset, long length, Handler<AsyncResult<Void>> resultHandler) {
		return this;
	}

	@Override
	public Future<Void> end() {
		return Future.succeededFuture();
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		handler.handle(FakeResult.ok(null));
	}

	@Override
	public void close(Handler<AsyncResult<Void>> handler) {
		handler.handle(FakeResult.ok(null));
	}

	@Override
	public SSLSession sslSession() {
		return null;
	}

	@Override
	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return new X509Certificate[0];
	}

	@Override
	public String indicatedServerName() {
		return null;
	}

	@Override
	public NetSocket upgradeToSsl(Handler<AsyncResult<Void>> handler) {
		return null;
	}

	@Override
	public Future<Void> upgradeToSsl() {
		return null;
	}

	@Override
	public NetSocket upgradeToSsl(String serverName, Handler<AsyncResult<Void>> handler) {
		return null;
	}

	@Override
	public Future<Void> upgradeToSsl(String serverName) {
		return null;
	}

	@Override
	public List<Certificate> peerCertificates() throws SSLPeerUnverifiedException {
		return null;
	}

	@Override
	public String applicationLayerProtocol() {
		return null;
	}

	@Override
	public SocketAddress remoteAddress(boolean real) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SocketAddress localAddress(boolean real) {
		// TODO Auto-generated method stub
		return null;
	}

}
