package net.bluemind.central.reverse.proxy.vertx;

import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.SocketAddress;
import net.bluemind.central.reverse.proxy.vertx.impl.WebSocketProxyImpl;

public interface WebSocketProxy extends Handler<ServerWebSocket> {

	static WebSocketProxy reverseProxy(HttpClient client) {
		return new WebSocketProxyImpl(client);
	}

	void handle(ServerWebSocket event);

	WebSocketProxy target(SocketAddress address);

	WebSocketProxy target(int port, String host);

	WebSocketProxy originSelector(Function<ServerWebSocket, Future<SocketAddress>> selector);

}
