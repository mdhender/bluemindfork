package net.bluemind.central.reverse.proxy.vertx.impl;

import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.net.impl.SocketAddressImpl;
import net.bluemind.central.reverse.proxy.model.client.ProxyInfoStoreClient;
import net.bluemind.central.reverse.proxy.vertx.AuthMatcher;
import net.bluemind.central.reverse.proxy.vertx.SessionManager;

public class DownstreamSelector<T> implements Function<T, Future<CloseableSession>> {

	private final AuthMatcher<T> infoMatcher;
	private final ProxyInfoStoreClient storeClient;
	private final SessionManager sessionManager;

	public DownstreamSelector(AuthMatcher<T> infoMatcher, ProxyInfoStoreClient storeClient,
			SessionManager sessionManager) {
		this.infoMatcher = infoMatcher;
		this.storeClient = storeClient;
		this.sessionManager = sessionManager;
	}

	@Override
	public Future<CloseableSession> apply(T context) {
		return infoMatcher //
				.match(context) //
				.flatMap(maybeAuth -> maybeAuth //
						.map(auth -> storeClient.ip(auth.login).recover(t -> storeClient.anyIp())) //
						.orElseGet(storeClient::anyIp)) //
				.map(ip -> {
					CloseableSession session = new CloseableSession(new SocketAddressImpl(443, ip));
					sessionManager.add(ip, session);
					return session;
				});
	}

}
