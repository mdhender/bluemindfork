package net.bluemind.central.reverse.proxy.vertx;

import java.util.function.Function;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

public interface ProxyRequest {
	MultiMap headers();

	ProxyRequest bodyFilter(Function<ReadStream<Buffer>, ReadStream<Buffer>> filter);

	void proxy(Handler<AsyncResult<Void>> completionHandler);

	void send(Handler<AsyncResult<ProxyResponse>> completionHandler);
}
