package net.bluemind.eas.testhelper.mock;

import java.util.Map;
import java.util.Map.Entry;
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
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.StreamPriority;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

public class RequestObject implements HttpServerRequest {

	private final String base;
	private final String path;
	private final String query;
	private MultiMap params;
	private MultiMap headers;
	private final ResponseObject response;
	private HttpMethod method;
	private Handler<Buffer> dataHandler;
	private Handler<Void> endHandler;

	public static enum HttpMethod {
		GET, POST, OPTIONS
	}

	public RequestObject(HttpMethod method, Map<String, String> reqHeaders, String base, String path,
			Map<String, String> queryParams) {
		this.base = base;
		this.path = path;
		StringBuilder q = new StringBuilder();
		boolean first = true;
		this.params = MultiMap.caseInsensitiveMultiMap();
		this.headers = MultiMap.caseInsensitiveMultiMap();
		for (Entry<String, String> kv : queryParams.entrySet()) {
			String v = kv.getValue();
			String k = kv.getKey();
			params.add(k, v);
			if (!first) {
				q.append('&');
				first = false;
			}
			q.append(k).append('=').append(v);
		}
		this.query = q.toString();
		for (Entry<String, String> kv : reqHeaders.entrySet()) {
			headers.add(kv.getKey(), kv.getValue());
		}
		this.method = method;
		this.response = new ResponseObject();
	}

	public RequestObject(HttpMethod method, Map<String, String> reqHeaders, String base, String path, String query) {
		this.base = base;
		this.path = path;
		this.params = MultiMap.caseInsensitiveMultiMap();
		this.headers = MultiMap.caseInsensitiveMultiMap();
		this.query = query;
		for (Entry<String, String> kv : reqHeaders.entrySet()) {
			headers.add(kv.getKey(), kv.getValue());
		}
		this.method = method;
		this.response = new ResponseObject();
	}

	@Override
	public HttpServerRequest endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
		return this;
	}

	@Override
	public HttpServerRequest handler(Handler<Buffer> handler) {
		this.dataHandler = handler;
		return this;
	}

	@Override
	public HttpServerRequest pause() {
		return this;
	}

	@Override
	public HttpServerRequest resume() {
		return this;
	}

	@Override
	public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public HttpVersion version() {
		return HttpVersion.HTTP_1_1;
	}

	@Override
	public io.vertx.core.http.HttpMethod method() {
		return io.vertx.core.http.HttpMethod.valueOf(method.name());
	}

	@Override
	public String uri() {
		return base + path + "?" + query;
	}

	@Override
	public String path() {
		return path;
	}

	@Override
	public String query() {
		return query;
	}

	@Override
	public HttpServerResponse response() {
		return response;
	}

	@Override
	public MultiMap headers() {
		return headers;
	}

	@Override
	public MultiMap params() {
		return params;
	}

	@Override
	public SocketAddress remoteAddress() {
		return null;
	}

	@Override
	public SocketAddress localAddress() {
		return null;
	}

	@Override
	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return null;
	}

	@Override
	public HttpServerRequest bodyHandler(Handler<Buffer> bodyHandler) {
		this.dataHandler = bodyHandler;
		return this;
	}

	@Override
	public Future<NetSocket> toNetSocket() {
		throw new RuntimeException("Just an un-connected mock object");
	}

	@Override
	public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
		return this;
	}

	@Override
	public MultiMap formAttributes() {
		return MultiMap.caseInsensitiveMultiMap();
	}

	public void trigger(byte[] bs) {
		if (dataHandler != null) {
			dataHandler.handle(Buffer.buffer(bs));
		}
		if (endHandler != null) {
			endHandler.handle(null);
		}
	}

	@Override
	public HttpServerRequest fetch(long amount) {
		return null;
	}

	@Override
	public boolean isSSL() {
		return false;
	}

	@Override
	public String scheme() {
		return null;
	}

	@Override
	public String host() {
		return null;
	}

	@Override
	public long bytesRead() {
		return 0;
	}

	@Override
	public String getHeader(String headerName) {
		return null;
	}

	@Override
	public String getHeader(CharSequence headerName) {
		return null;
	}

	@Override
	public String getParam(String paramName) {
		return null;
	}

	@Override
	public SSLSession sslSession() {
		return null;
	}

	@Override
	public String absoluteURI() {
		return null;
	}

	@Override
	public HttpServerRequest setExpectMultipart(boolean expect) {
		return null;
	}

	@Override
	public boolean isExpectMultipart() {
		return false;
	}

	@Override
	public String getFormAttribute(String attributeName) {
		return null;
	}

	@Override
	public boolean isEnded() {
		return false;
	}

	@Override
	public HttpServerRequest customFrameHandler(Handler<HttpFrame> handler) {
		return null;
	}

	@Override
	public HttpConnection connection() {
		return null;
	}

	@Override
	public HttpServerRequest streamPriorityHandler(Handler<StreamPriority> handler) {
		return null;
	}

	@Override
	public int cookieCount() {
		return 0;
	}

	@Override
	public Future<Buffer> body() {
		return null;
	}

	@Override
	public Future<Void> end() {
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

	@Override
	public Cookie getCookie(String name) {
		return null;
	}

}
