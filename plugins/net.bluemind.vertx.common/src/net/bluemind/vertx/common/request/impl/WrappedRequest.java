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

import java.net.InetSocketAddress;
import java.net.URI;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.net.NetSocket;

import com.netflix.spectator.api.Registry;

import net.bluemind.metrics.registry.IdFactory;

public class WrappedRequest implements HttpServerRequest {

	private final HttpServerRequest impl;
	private final WrappedResponse response;

	private WrappedRequest(Registry registry, IdFactory idfactory, HttpServerRequest impl) {
		this.impl = impl;
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

	public HttpServerRequest dataHandler(Handler<Buffer> handler) {
		impl.dataHandler(handler);
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

	public String method() {
		return impl.method();
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

	public InetSocketAddress remoteAddress() {
		return impl.remoteAddress();
	}

	public InetSocketAddress localAddress() {
		return impl.localAddress();
	}

	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return impl.peerCertificateChain();
	}

	public URI absoluteURI() {
		return impl.absoluteURI();
	}

	public HttpServerRequest bodyHandler(Handler<Buffer> bodyHandler) {
		impl.bodyHandler(bodyHandler);
		return this;
	}

	public NetSocket netSocket() {
		return impl.netSocket();
	}

	public HttpServerRequest expectMultiPart(boolean expect) {
		impl.expectMultiPart(expect);
		return this;
	}

	public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
		impl.uploadHandler(uploadHandler);
		return this;
	}

	public MultiMap formAttributes() {
		return impl.formAttributes();
	}

}
