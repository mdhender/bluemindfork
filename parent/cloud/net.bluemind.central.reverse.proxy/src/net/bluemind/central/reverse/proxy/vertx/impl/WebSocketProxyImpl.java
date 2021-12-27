package net.bluemind.central.reverse.proxy.vertx.impl;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import net.bluemind.central.reverse.proxy.vertx.WebSocketProxy;

public class WebSocketProxyImpl implements WebSocketProxy {

	private final Logger logger = LoggerFactory.getLogger(WebSocketProxyImpl.class);

	private final HttpClient httpClient;
	private Function<ServerWebSocket, Future<SocketAddress>> targetSelector = context -> Future
			.failedFuture("No target available");

	public WebSocketProxyImpl(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Override
	public WebSocketProxy target(SocketAddress address) {
		targetSelector = context -> Future.succeededFuture(address);
		return this;
	}

	@Override
	public WebSocketProxy target(int port, String host) {
		return target(new SocketAddressImpl(port, host));
	}

	@Override
	public WebSocketProxy originSelector(Function<ServerWebSocket, Future<SocketAddress>> selector) {
		targetSelector = selector;
		return this;
	}

	@Override
	public void handle(ServerWebSocket upstreamWebSocket) {

		targetSelector.apply(upstreamWebSocket).onComplete(ar -> {
			if (ar.succeeded()) {
				SocketAddress address = ar.result();
				WebSocketConnectOptions options = new WebSocketConnectOptions() //
						.setHost(address.host()).setPort(address.port()) //
						.setURI(upstreamWebSocket.path()) //
						.addHeader(HttpHeaders.SET_COOKIE.toString(),
								upstreamWebSocket.headers().get(HttpHeaders.COOKIE));
				httpClient.webSocket(options, ar2 -> {
					if (ar2.succeeded()) {
						WebSocket downstreamWebSocket = ar2.result();
						upstreamWebSocket.frameHandler(downstreamWebSocket::writeFrame);

						downstreamWebSocket.frameHandler(upstreamWebSocket::writeFrame);

						upstreamWebSocket.endHandler(v -> {
							upstreamWebSocket.close();
							downstreamWebSocket.close();
						});
						downstreamWebSocket.endHandler(v -> {
							upstreamWebSocket.close();
							downstreamWebSocket.close();
						});
					} else {
						upstreamWebSocket.close();
					}
				});
			} else {
				upstreamWebSocket.close();
			}
		});

	}

}
