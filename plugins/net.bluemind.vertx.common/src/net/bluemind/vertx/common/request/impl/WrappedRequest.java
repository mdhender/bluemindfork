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
package net.bluemind.vertx.common.request.impl;

import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

import com.netflix.spectator.api.Registry;

import io.netty.handler.codec.DecoderResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.StreamPriority;
import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.WriteStream;
import net.bluemind.metrics.registry.IdFactory;

public class WrappedRequest implements HttpServerRequestInternal {

	private final HttpServerRequestInternal impl;
	private final WrappedResponse response;

	private WrappedRequest(Registry registry, IdFactory idfactory, HttpServerRequest impl) {
		this.impl = (HttpServerRequestInternal) impl;
		this.response = new WrappedResponse(registry, idfactory, impl.response());
	}

	public String logAttribute(String k) {
		return response.getLogAttribute(k);
	}

	public void putLogAttribute(String k, String v) {
		response.putLogAttribute(k, v);
	}

	public static HttpServerRequest create(Registry registry, IdFactory idfactory, HttpServerRequest req) {
		return new WrappedRequest(registry, idfactory, req);
	}

	public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
		impl.exceptionHandler(handler);
		return this;
	}

	public HttpServerRequest pause() {
		impl.pause();
		return this;
	}

	public HttpServerRequest endHandler(Handler<Void> endHandler) {
		impl.endHandler(endHandler);
		return this;
	}

	public HttpServerRequest resume() {
		impl.resume();
		return this;
	}

	public HttpVersion version() {
		return impl.version();
	}

	public String uri() {
		return impl.uri();
	}

	public String path() {
		return impl.path();
	}

	public String query() {
		return impl.query();
	}

	public HttpServerResponse response() {
		return response;
	}

	public MultiMap headers() {
		return impl.headers();
	}

	public MultiMap params() {
		return impl.params();
	}

	@Override
	public SocketAddress remoteAddress() {
		return impl.remoteAddress();
	}

	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return impl.peerCertificateChain();
	}

	@Override
	public HttpServerRequest bodyHandler(Handler<Buffer> bodyHandler) {
		impl.bodyHandler(bodyHandler);
		return this;
	}

	public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
		impl.uploadHandler(uploadHandler);
		return this;
	}

	public MultiMap formAttributes() {
		return impl.formAttributes();
	}

	public HttpServerRequest handler(Handler<Buffer> handler) {
		impl.handler(handler);
		return this;
	}

	public HttpServerRequest fetch(long amount) {
		impl.fetch(amount);
		return this;
	}

	public HttpMethod method() {
		return impl.method();
	}

	@Override
	public boolean isSSL() {
		return impl.isSSL();
	}

	public String scheme() {
		return impl.scheme();
	}

	public String host() {
		return impl.host();
	}

	public long bytesRead() {
		return impl.bytesRead();
	}

	@Override
	public String getHeader(String headerName) {
		return impl.getHeader(headerName);
	}

	@Override
	public String getHeader(CharSequence headerName) {
		return impl.getHeader(headerName);
	}

	@Override
	public Pipe<Buffer> pipe() {
		return impl.pipe();
	}

	@Override
	public String getParam(String paramName) {
		return impl.getParam(paramName);
	}

	@Override
	public Future<Void> pipeTo(WriteStream<Buffer> dst) {
		return impl.pipeTo(dst);
	}

	@Override
	public void pipeTo(WriteStream<Buffer> dst, Handler<AsyncResult<Void>> handler) {
		impl.pipeTo(dst, handler);
	}

	@Override
	public SocketAddress localAddress() {
		return impl.localAddress();
	}

	@Override
	public SSLSession sslSession() {
		return impl.sslSession();
	}

	public String absoluteURI() {
		return impl.absoluteURI();
	}

	public HttpServerRequest setExpectMultipart(boolean expect) {
		impl.setExpectMultipart(expect);
		return this;
	}

	public boolean isExpectMultipart() {
		return impl.isExpectMultipart();
	}

	public String getFormAttribute(String attributeName) {
		return impl.getFormAttribute(attributeName);
	}

	public Future<ServerWebSocket> upgrade() {
		return impl.toWebSocket();
	}

	public boolean isEnded() {
		return impl.isEnded();
	}

	public HttpServerRequest customFrameHandler(Handler<HttpFrame> handler) {
		impl.customFrameHandler(handler);
		return this;
	}

	public HttpConnection connection() {
		return impl.connection();
	}

	@Override
	public StreamPriority streamPriority() {
		return impl.streamPriority();
	}

	public HttpServerRequest streamPriorityHandler(Handler<StreamPriority> handler) {
		impl.streamPriorityHandler(handler);
		return this;
	}

	public Cookie getCookie(String name) {
		return impl.getCookie(name);
	}

	@Override
	public int cookieCount() {
		return impl.cookieCount();
	}

	@Override
	@Deprecated
	public Map<String, Cookie> cookieMap() {
		return impl.cookieMap();
	}

	@Override
	public Future<Buffer> body() {
		return impl.body();
	}

	@Override
	public Future<Void> end() {
		return impl.end();
	}

	@Override
	public Future<NetSocket> toNetSocket() {
		return impl.toNetSocket();
	}

	@Override
	public Future<ServerWebSocket> toWebSocket() {
		return impl.toWebSocket();
	}

	@Override
	public DecoderResult decoderResult() {
		return impl.decoderResult();
	}

	@Override
	public Cookie getCookie(String name, String domain, String path) {
		return impl.getCookie(name, domain, path);
	}

	@Override
	public Set<Cookie> cookies(String name) {
		return impl.cookies(name);
	}

	@Override
	public Set<Cookie> cookies() {
		return impl.cookies();
	}

	@Override
	public Object metric() {
		return impl.metric();
	}

	@Override
	public Context context() {
		return impl.context();
	}

	@Override
	public HttpServerRequest setParamsCharset(String charset) {
		impl.setParamsCharset(charset);
		return this;
	}

	@Override
	public String getParamsCharset() {
		return impl.getParamsCharset();
	}
}
