package net.bluemind.eas.testhelper.mock;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.net.NetSocket;

public class RequestObject implements HttpServerRequest {

	private final String base;
	private final String path;
	private final String query;
	private CaseInsensitiveMultiMap params;
	private CaseInsensitiveMultiMap headers;
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
		this.params = new CaseInsensitiveMultiMap();
		this.headers = new CaseInsensitiveMultiMap();
		for (String k : queryParams.keySet()) {
			String v = queryParams.get(k);
			params.add(k, v);
			if (!first) {
				q.append('&');
				first = false;
			}
			q.append(k).append('=').append(v);
		}
		this.query = q.toString();
		for (String k : reqHeaders.keySet()) {
			String v = reqHeaders.get(k);
			headers.add(k, v);
		}
		this.method = method;
		this.response = new ResponseObject();
	}

	public RequestObject(HttpMethod method, Map<String, String> reqHeaders, String base, String path, String query) {
		this.base = base;
		this.path = path;
		this.params = new CaseInsensitiveMultiMap();
		this.headers = new CaseInsensitiveMultiMap();
		this.query = query;
		for (String k : reqHeaders.keySet()) {
			String v = reqHeaders.get(k);
			headers.add(k, v);
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
	public HttpServerRequest dataHandler(Handler<Buffer> handler) {
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
	public String method() {
		return method.name();
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
	public InetSocketAddress remoteAddress() {
		return null;
	}

	@Override
	public InetSocketAddress localAddress() {
		return null;
	}

	@Override
	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return null;
	}

	@Override
	public URI absoluteURI() {
		return null;
	}

	@Override
	public HttpServerRequest bodyHandler(Handler<Buffer> bodyHandler) {
		this.dataHandler = bodyHandler;
		return this;
	}

	@Override
	public NetSocket netSocket() {
		throw new RuntimeException("Just an un-connected mock object");
	}

	@Override
	public HttpServerRequest expectMultiPart(boolean expect) {
		return this;
	}

	@Override
	public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
		return this;
	}

	@Override
	public MultiMap formAttributes() {
		return new CaseInsensitiveMultiMap();
	}

	public void trigger(byte[] bs) {
		if (dataHandler != null) {
			dataHandler.handle(new Buffer(bs));
		}
		if (endHandler != null) {
			endHandler.handle(null);
		}
	}

}
