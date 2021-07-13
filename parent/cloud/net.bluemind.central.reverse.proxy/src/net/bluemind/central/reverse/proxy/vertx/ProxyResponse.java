package net.bluemind.central.reverse.proxy.vertx;

import java.util.function.Function;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.streams.ReadStream;

public interface ProxyResponse {

	int statusCode();

	String statusMessage();

	boolean publicCacheControl();

	long maxAge();

	String etag();

	MultiMap headers();

	ProxyResponse bodyFilter(Function<ReadStream<Buffer>, ReadStream<Buffer>> filter);

	ProxyResponse set(HttpClientResponse response);

	void send(Handler<AsyncResult<Void>> completionHandler);

	void cancel();
}
