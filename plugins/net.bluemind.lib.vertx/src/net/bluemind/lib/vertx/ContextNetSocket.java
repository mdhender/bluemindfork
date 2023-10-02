/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.lib.vertx;

import java.security.cert.Certificate;
import java.util.List;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

/*
 * Vert.x NetSocket created by NetServers are all using the eventLoop context
 * used to listen on the server socket.
 * 
 * This class will wrap all future/async result in a runOnContext,
 * ensuring to keep the execution on the correct thread and with the
 * correct context. (DuplicatedContext generally)
 */
public class ContextNetSocket implements NetSocket {
	private ContextInternal context;
	private NetSocket ns;

	public ContextNetSocket(Context context, NetSocket ns) {
		this.context = (ContextInternal) context;
		this.ns = ns;
	}

	@Override
	public Future<Void> write(Buffer data) {
		Promise<Void> prom = context.promise();
		ns.write(data).onSuccess(v -> prom.complete()).onFailure(prom::fail);
		return prom.future();
	}

	@Override
	public boolean writeQueueFull() {
		return ns.writeQueueFull();
	}

	@Override
	public NetSocket exceptionHandler(Handler<Throwable> handler) {
		ns.exceptionHandler(t -> context.runOnContext(v -> handler.handle(t)));
		return this;
	}

	@Override
	public NetSocket handler(Handler<Buffer> handler) {
		ns.handler(ar -> context.runOnContext(v -> handler.handle(ar)));
		return this;
	}

	@Override
	public NetSocket pause() {
		ns.pause();
		return this;
	}

	@Override
	public NetSocket resume() {
		ns.resume();
		return this;
	}

	@Override
	public NetSocket fetch(long amount) {
		ns.fetch(amount);
		return this;
	}

	@Override
	public NetSocket endHandler(Handler<Void> endHandler) {
		ns.endHandler(ar -> context.runOnContext(v -> endHandler.handle(ar)));
		return this;
	}

	@Override
	public NetSocket setWriteQueueMaxSize(int maxSize) {
		ns.setWriteQueueMaxSize(maxSize);
		return this;
	}

	@Override
	public NetSocket drainHandler(Handler<Void> handler) {
		ns.drainHandler(ar -> context.runOnContext(v -> handler.handle(ar)));
		return this;
	}

	@Override
	public String writeHandlerID() {
		return ns.writeHandlerID();
	}

	@Override
	public void write(String str, Handler<AsyncResult<Void>> handler) {
		ns.write(str, ar -> context.runOnContext(v -> handler.handle(ar)));
	}

	@Override
	public Future<Void> write(String str) {
		Promise<Void> prom = context.promise();
		ns.write(str).onSuccess(v -> prom.complete()).onFailure(prom::fail);
		return prom.future();
	}

	@Override
	public void write(String str, String enc, Handler<AsyncResult<Void>> handler) {
		ns.write(str, enc, ar -> context.runOnContext(v -> handler.handle(ar)));
	}

	@Override
	public Future<Void> write(String str, String enc) {
		Promise<Void> prom = context.promise();
		ns.write(str, enc).onSuccess(v -> prom.complete()).onFailure(prom::fail);
		return prom.future();
	}

	@Override
	public void write(Buffer message, Handler<AsyncResult<Void>> handler) {
		ns.write(message, ar -> context.runOnContext(v -> handler.handle(ar)));
	}

	@Override
	public Future<Void> sendFile(String filename, long offset, long length) {
		Promise<Void> prom = context.promise();
		ns.sendFile(filename, offset, length).onSuccess(v -> prom.complete()).onFailure(prom::fail);
		return prom.future();
	}

	@Override
	public NetSocket sendFile(String filename, long offset, long length, Handler<AsyncResult<Void>> resultHandler) {
		ns.sendFile(filename, offset, length, ar -> context.runOnContext(v -> resultHandler.handle(ar)));
		return this;
	}

	@Override
	public SocketAddress remoteAddress() {
		return ns.remoteAddress();
	}

	@Override
	public SocketAddress remoteAddress(boolean real) {
		return ns.remoteAddress(real);
	}

	@Override
	public SocketAddress localAddress() {
		return ns.localAddress();
	}

	@Override
	public SocketAddress localAddress(boolean real) {
		return ns.localAddress(real);
	}

	@Override
	public Future<Void> end() {
		Promise<Void> prom = context.promise();
		ns.end().onSuccess(v -> prom.complete()).onFailure(prom::fail);
		return prom.future();
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		ns.end(ar -> context.runOnContext(v -> handler.handle(ar)));
	}

	@Override
	public Future<Void> close() {
		Promise<Void> prom = context.promise();
		ns.close().onSuccess(v -> prom.complete()).onFailure(prom::fail);
		return prom.future();
	}

	@Override
	public void close(Handler<AsyncResult<Void>> handler) {
		ns.close(ar -> context.runOnContext(v -> handler.handle(ar)));
	}

	@Override
	public NetSocket closeHandler(Handler<Void> handler) {
		ns.closeHandler(ar -> context.runOnContext(v -> handler.handle(ar)));
		return this;
	}

	@Override
	public NetSocket upgradeToSsl(Handler<AsyncResult<Void>> handler) {
		ns.upgradeToSsl(ar -> context.runOnContext(v -> handler.handle(ar)));
		return this;
	}

	@Override
	public Future<Void> upgradeToSsl() {
		Promise<Void> prom = context.promise();
		ns.upgradeToSsl().onSuccess(v -> prom.complete()).onFailure(prom::fail);
		return prom.future();
	}

	@Override
	public NetSocket upgradeToSsl(String serverName, Handler<AsyncResult<Void>> handler) {
		ns.upgradeToSsl(serverName, ar -> context.runOnContext(v -> handler.handle(ar)));
		return this;
	}

	@Override
	public Future<Void> upgradeToSsl(String serverName) {
		Promise<Void> prom = context.promise();
		ns.upgradeToSsl(serverName).onSuccess(v -> prom.complete()).onFailure(prom::fail);
		return prom.future();
	}

	@Override
	public boolean isSsl() {
		return ns.isSsl();
	}

	@Override
	public SSLSession sslSession() {
		return ns.sslSession();
	}

	@Override
	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return ns.peerCertificateChain();
	}

	@Override
	public List<Certificate> peerCertificates() throws SSLPeerUnverifiedException {
		return ns.peerCertificates();
	}

	@Override
	public String indicatedServerName() {
		return ns.indicatedServerName();
	}

	@Override
	public String applicationLayerProtocol() {
		return ns.applicationLayerProtocol();
	}

}
