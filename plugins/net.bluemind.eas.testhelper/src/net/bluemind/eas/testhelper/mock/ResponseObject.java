package net.bluemind.eas.testhelper.mock;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;

public class ResponseObject implements HttpServerResponse {

	private static final Logger logger = LoggerFactory.getLogger(ResponseObject.class);

	private int statusCode;
	private String statusMessage;
	private final MultiMap headers;
	private final MultiMap trailers;
	public final Buffer content;

	private final CountDownLatch latch;

	public ResponseObject() {
		this.headers = MultiMap.caseInsensitiveMultiMap();
		this.trailers = MultiMap.caseInsensitiveMultiMap();
		this.content = Buffer.buffer();
		this.statusCode = 200;
		this.statusMessage = "OK";
		this.latch = new CountDownLatch(1);
	}

	@Override
	public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public HttpServerResponse setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return false;
	}

	@Override
	public HttpServerResponse drainHandler(Handler<Void> handler) {
		return this;
	}

	@Override
	public int getStatusCode() {
		return statusCode;
	}

	@Override
	public HttpServerResponse setStatusCode(int statusCode) {
		this.statusCode = statusCode;
		return this;
	}

	@Override
	public String getStatusMessage() {
		return statusMessage;
	}

	@Override
	public HttpServerResponse setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
		return this;
	}

	@Override
	public HttpServerResponse setChunked(boolean chunked) {
		return this;
	}

	@Override
	public boolean isChunked() {
		return false;
	}

	@Override
	public MultiMap headers() {
		return headers;
	}

	@Override
	public HttpServerResponse putHeader(String name, String value) {
		headers.add(name, value);
		return this;
	}

	@Override
	public HttpServerResponse putHeader(CharSequence name, CharSequence value) {
		headers.add(name, value);
		return this;
	}

	@Override
	public HttpServerResponse putHeader(String name, Iterable<String> values) {
		headers.add(name, values);
		return this;
	}

	@Override
	public HttpServerResponse putHeader(CharSequence name, Iterable<CharSequence> values) {
		headers.add(name, values);
		return this;
	}

	@Override
	public MultiMap trailers() {
		return trailers;
	}

	@Override
	public HttpServerResponse putTrailer(String name, String value) {
		return this;
	}

	@Override
	public HttpServerResponse putTrailer(CharSequence name, CharSequence value) {
		return this;
	}

	@Override
	public HttpServerResponse putTrailer(String name, Iterable<String> values) {
		return this;
	}

	@Override
	public HttpServerResponse putTrailer(CharSequence name, Iterable<CharSequence> value) {
		return this;
	}

	@Override
	public HttpServerResponse closeHandler(Handler<Void> handler) {
		return this;
	}

	@Override
	public Future<Void> write(Buffer chunk) {
		content.appendBuffer(chunk);
		return Future.succeededFuture();
	}

	@Override
	public Future<Void> write(String chunk, String enc) {
		content.appendString(chunk, enc);
		return Future.succeededFuture();
	}

	@Override
	public Future<Void> write(String chunk) {
		content.appendString(chunk);
		return Future.succeededFuture();
	}

	private void endImpl() {
		logger.info("Request ended.");
		latch.countDown();
	}

	public Buffer waitForIt(long timeout, TimeUnit unit) {
		try {
			if (latch.await(timeout, unit)) {
				return content;
			} else {
				throw new TimeoutException("Response not received in time");
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Future<Void> end(String chunk) {
		content.appendString(chunk);
		endImpl();
		return Future.succeededFuture();
	}

	@Override
	public Future<Void> end(String chunk, String enc) {
		content.appendString(chunk, enc);
		endImpl();
		return Future.succeededFuture();
	}

	@Override
	public Future<Void> end(Buffer chunk) {
		content.appendBuffer(chunk);
		endImpl();
		return Future.succeededFuture();
	}

	@Override
	public Future<Void> end() {
		endImpl();
		return Future.succeededFuture();
	}

	@Override
	public Future<Void> sendFile(String filename) {
		return Future.failedFuture(new RuntimeException("Not implemented."));
	}

	@Override
	public void close() {
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		endImpl();
		handler.handle(Future.succeededFuture());
	}

	@Override
	public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
		content.appendBuffer(data);
		handler.handle(Future.succeededFuture());
	}

	@Override
	public long bytesWritten() {
		return 0;
	}

	@Override
	public int streamId() {
		return 0;
	}

	@Override
	public HttpServerResponse endHandler(Handler<Void> handler) {
		return null;
	}

	@Override
	public void write(String chunk, String enc, Handler<AsyncResult<Void>> handler) {

	}

	@Override
	public void write(String chunk, Handler<AsyncResult<Void>> handler) {
	}

	@Override
	public HttpServerResponse writeContinue() {
		return null;
	}

	@Override
	public void end(String chunk, Handler<AsyncResult<Void>> handler) {
	}

	@Override
	public void end(String chunk, String enc, Handler<AsyncResult<Void>> handler) {
	}

	@Override
	public void end(Buffer chunk, Handler<AsyncResult<Void>> handler) {
	}

	@Override
	public Future<Void> sendFile(String filename, long offset, long length) {
		return null;
	}

	@Override
	public HttpServerResponse sendFile(String filename, long offset, long length,
			Handler<AsyncResult<Void>> resultHandler) {
		return null;
	}

	@Override
	public boolean ended() {
		return false;
	}

	@Override
	public boolean closed() {
		return false;
	}

	@Override
	public boolean headWritten() {
		return false;
	}

	@Override
	public HttpServerResponse headersEndHandler(Handler<Void> handler) {
		return null;
	}

	@Override
	public HttpServerResponse bodyEndHandler(Handler<Void> handler) {
		return null;
	}

	@Override
	public Future<HttpServerResponse> push(HttpMethod method, String host, String path, MultiMap headers) {
		return null;
	}

	@Override
	public boolean reset(long code) {
		return false;
	}

	@Override
	public HttpServerResponse writeCustomFrame(int type, int flags, Buffer payload) {
		return null;
	}

	@Override
	public HttpServerResponse addCookie(Cookie cookie) {
		return null;
	}

	@Override
	public Cookie removeCookie(String name, boolean invalidate) {
		return null;
	}

	@Override
	public Set<Cookie> removeCookies(String name, boolean invalidate) {
		return null;
	}

	@Override
	public Cookie removeCookie(String name, String domain, String path, boolean invalidate) {
		return null;
	}
}
