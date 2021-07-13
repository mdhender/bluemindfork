package net.bluemind.central.reverse.proxy.vertx.impl;

import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import net.bluemind.central.reverse.proxy.model.ProxyInfoStoreClient;
import net.bluemind.central.reverse.proxy.vertx.AuthMatcher;

public class DownstreamSelector<T> implements Function<T, Future<SocketAddress>> {

	private final Vertx vertx;
	private final AuthMatcher<T> infoMatcher;
	private final ProxyInfoStoreClient storeClient;

	public DownstreamSelector(Vertx vertx, AuthMatcher<T> infoMatcher, ProxyInfoStoreClient storeClient) {
		this.vertx = vertx;
		this.infoMatcher = infoMatcher;
		this.storeClient = storeClient;
	}

	@Override
	public Future<SocketAddress> apply(T context) {
		return infoMatcher.match(context)
				.flatMap(
						maybeAuth -> maybeAuth.map(auth -> storeClient.ip(auth.login).recover(t -> storeClient.anyIp()))
								.orElseGet(storeClient::anyIp))
				.map(ip -> new SocketAddressImpl(443, ip));
	}

}
