package net.bluemind.central.reverse.proxy.vertx.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

import io.netty.handler.codec.DecoderResult;
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
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

public class TestRequestHelper {

	public static HttpServerRequest createRequest(HttpMethod method, String path, MultiMap formAttributes) {
		return new HttpServerRequest() {
			Handler<Void> endHandler = v -> {
			};

			@Override
			public HttpMethod method() {
				return method;
			}

			@Override
			public String path() {
				return path;
			}

			@Override
			public MultiMap formAttributes() {
				return formAttributes;
			}

			@Override
			public String getFormAttribute(String attributeName) {
				return formAttributes().get(attributeName);
			}

			@Override
			public Map<String, Cookie> cookieMap() {
				return new HashMap<>();
			}

			@Override
			public int cookieCount() {
				return 0;
			}

			@Override
			public HttpServerRequest handler(Handler<Buffer> handler) {
				return null;
			}

			@Override
			public HttpServerRequest endHandler(Handler<Void> endHandler) {
				this.endHandler = endHandler;
				return this;
			}

			@Override
			public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
				return this;
			}

			@Override
			public HttpVersion version() {
				return null;
			}

			@Override
			public String uri() {
				return null;
			}

			@Override
			public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
				return this;
			}

			@Override
			public HttpServerRequest streamPriorityHandler(Handler<StreamPriority> handler) {
				return this;
			}

			@Override
			public SSLSession sslSession() {
				return null;
			}

			@Override
			public HttpServerRequest setExpectMultipart(boolean expect) {
				return this;
			}

			@Override
			public String scheme() {
				return null;
			}

			@Override
			public HttpServerRequest resume() {
				this.endHandler.handle(null);
				return this;
			}

			@Override
			public HttpServerResponse response() {
				return null;
			}

			@Override
			public SocketAddress remoteAddress() {
				return null;
			}

			@Override
			public String query() {
				return null;
			}

			@Override
			public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
				return null;
			}

			@Override
			public HttpServerRequest pause() {
				return this;
			}

			@Override
			public MultiMap params() {
				return MultiMap.caseInsensitiveMultiMap();
			}

			@Override
			public SocketAddress localAddress() {
				return null;
			}

			@Override
			public boolean isSSL() {
				return false;
			}

			@Override
			public boolean isExpectMultipart() {
				return false;
			}

			@Override
			public boolean isEnded() {
				return false;
			}

			@Override
			public String host() {
				return null;
			}

			@Override
			public MultiMap headers() {
				return MultiMap.caseInsensitiveMultiMap();
			}

			@Override
			public String getParam(String paramName) {
				return null;
			}

			@Override
			public String getHeader(CharSequence headerName) {
				return null;
			}

			@Override
			public String getHeader(String headerName) {
				return null;
			}

			@Override
			public Cookie getCookie(String name) {
				return null;
			}

			@Override
			public HttpServerRequest fetch(long amount) {
				return this;
			}

			@Override
			public HttpServerRequest customFrameHandler(Handler<HttpFrame> handler) {
				return this;
			}

			@Override
			public HttpConnection connection() {
				return null;
			}

			@Override
			public long bytesRead() {
				return 0;
			}

			@Override
			public String absoluteURI() {
				return null;
			}

			@Override
			public Future<Buffer> body() {
				return Future.succeededFuture();
			}

			@Override
			public Future<Void> end() {
				return Future.succeededFuture();
			}

			@Override
			public Future<NetSocket> toNetSocket() {
				return null;
			}

			@Override
			public Future<ServerWebSocket> toWebSocket() {
				return null;
			}

			@Override
			public DecoderResult decoderResult() {
				return null;
			}

			@Override
			public Cookie getCookie(String name, String domain, String path) {
				return null;
			}

			@Override
			public Set<Cookie> cookies(String name) {
				return null;
			}

			@Override
			public Set<Cookie> cookies() {
				return null;
			}
		};
	}
}
