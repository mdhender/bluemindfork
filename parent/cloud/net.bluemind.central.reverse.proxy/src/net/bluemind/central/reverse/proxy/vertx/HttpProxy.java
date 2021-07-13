package net.bluemind.central.reverse.proxy.vertx;

import java.util.function.BiConsumer;
import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import net.bluemind.central.reverse.proxy.vertx.impl.HttpProxyImpl;

public interface HttpProxy extends Handler<HttpServerRequest> {

	static HttpProxy reverseProxy(HttpClient client) {
		return new HttpProxyImpl(client);
	}

	HttpProxy target(SocketAddress address);

	HttpProxy target(int port, String host);

	HttpProxy selector(Function<HttpServerRequestContext, Future<SocketAddress>> selector);

	HttpProxy responseHook(BiConsumer<HttpServerRequestContext, ProxyResponse> responder);

	void handle(HttpServerRequest request);

	ProxyRequest proxy(HttpServerRequest request, SocketAddress target);

}
