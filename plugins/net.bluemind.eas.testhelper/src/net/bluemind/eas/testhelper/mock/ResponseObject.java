package net.bluemind.eas.testhelper.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;

public class ResponseObject implements HttpServerResponse {

	private static final Logger logger = LoggerFactory.getLogger(ResponseObject.class);

	private int statusCode;
	private String statusMessage;
	private final CaseInsensitiveHeaders headers;
	private final CaseInsensitiveHeaders trailers;
	public final Buffer content;

	private final CountDownLatch latch;

	public ResponseObject() {
		this.headers = new CaseInsensitiveHeaders();
		this.trailers = new CaseInsensitiveHeaders();
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
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public HttpServerResponse write(Buffer chunk) {
		content.appendBuffer(chunk);
		return this;
	}

	@Override
	public HttpServerResponse write(String chunk, String enc) {
		content.appendString(chunk, enc);
		return this;
	}

	@Override
	public HttpServerResponse write(String chunk) {
		content.appendString(chunk);
		return this;
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
	public void end(String chunk) {
		content.appendString(chunk);
		endImpl();
	}

	@Override
	public void end(String chunk, String enc) {
		content.appendString(chunk, enc);
		endImpl();
	}

	@Override
	public void end(Buffer chunk) {
		content.appendBuffer(chunk);
		endImpl();
	}

	@Override
	public void end() {
		endImpl();
	}

	@Override
	public HttpServerResponse sendFile(String filename) {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void close() {
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public HttpServerResponse write(Buffer data, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse endHandler(Handler<Void> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse write(String chunk, String enc, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse write(String chunk, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse writeContinue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void end(String chunk, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void end(String chunk, String enc, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void end(Buffer chunk, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public HttpServerResponse sendFile(String filename, long offset, long length) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse sendFile(String filename, long offset, long length,
			Handler<AsyncResult<Void>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean ended() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean closed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean headWritten() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public HttpServerResponse headersEndHandler(Handler<Void> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse bodyEndHandler(Handler<Void> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long bytesWritten() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int streamId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String host, String path,
			Handler<AsyncResult<HttpServerResponse>> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String path, MultiMap headers,
			Handler<AsyncResult<HttpServerResponse>> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String path, Handler<AsyncResult<HttpServerResponse>> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String host, String path, MultiMap headers,
			Handler<AsyncResult<HttpServerResponse>> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reset(long code) {
		// TODO Auto-generated method stub

	}

	@Override
	public HttpServerResponse writeCustomFrame(int type, int flags, Buffer payload) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse addCookie(Cookie cookie) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Cookie removeCookie(String name, boolean invalidate) {
		// TODO Auto-generated method stub
		return null;
	}

}
