package net.bluemind.central.reverse.proxy.vertx;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.streams.ReadStream;

public interface HttpServerRequestContext {

	HttpServerRequest request();

	ReadStream<Buffer> bodyStream();

	Future<Void> withAvalaibleBody();
}
