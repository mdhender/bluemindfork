package net.bluemind.central.reverse.proxy.vertx.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.streams.ReadStream;
import net.bluemind.central.reverse.proxy.vertx.HttpServerRequestContext;

public class HttpServerRequestContextImpl implements HttpServerRequestContext {

	private final Logger logger = LoggerFactory.getLogger(HttpServerRequestContextImpl.class);

	private final HttpServerRequest request;
	private final boolean isMultipart;
	private final boolean isUrlEncoded;

	private Buffer body;
	private Future<Void> futureBody;

	public HttpServerRequestContextImpl(HttpServerRequest request) {
		this.request = request;

		String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
		if (contentType == null) {
			isMultipart = false;
			isUrlEncoded = false;
		} else {
			String lowerCaseContentType = contentType.toLowerCase();
			isMultipart = lowerCaseContentType.startsWith(HttpHeaderValues.MULTIPART_FORM_DATA.toString());
			isUrlEncoded = lowerCaseContentType
					.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString());
		}
	}

	@Override
	public HttpServerRequest request() {
		return request;
	}

	@Override
	public ReadStream<Buffer> bodyStream() {
		return body != null ? new BufferedReadStream(body) : request;
	}

	@Override
	public Future<Void> withAvalaibleBody() {
		if (this.futureBody != null) {
			return this.futureBody;
		}

		if (isMultipart || isUrlEncoded) {
			request.setExpectMultipart(true);
		}

		Promise<Void> bodyHandled = Promise.promise();
		this.request.bodyHandler(buffer -> {
			if (!isMultipart /* && !isUrlEncoded */) {
				this.body = buffer;
			}
			this.request.pause();
			request.params().addAll(request.formAttributes());
			bodyHandled.complete();
		});
		futureBody = bodyHandled.future();
		this.request.resume();
		return futureBody;
	}

}
