package net.bluemind.eas.testhelper.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.http.HttpServerResponse;

public class ResponseObject implements HttpServerResponse {

	private static final Logger logger = LoggerFactory.getLogger(ResponseObject.class);

	private int statusCode;
	private String statusMessage;
	private final CaseInsensitiveMultiMap headers;
	private final CaseInsensitiveMultiMap trailers;
	public final Buffer content;

	private final CountDownLatch latch;

	public ResponseObject() {
		this.headers = new CaseInsensitiveMultiMap();
		this.trailers = new CaseInsensitiveMultiMap();
		this.content = new Buffer();
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
	public HttpServerResponse sendFile(String filename, String notFoundFile) {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public HttpServerResponse sendFile(String filename, Handler<AsyncResult<Void>> resultHandler) {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public HttpServerResponse sendFile(String filename, String notFoundFile, Handler<AsyncResult<Void>> resultHandler) {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void close() {
	}

}
