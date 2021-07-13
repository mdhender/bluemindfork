package net.bluemind.central.reverse.proxy.vertx;

import java.util.Optional;

import io.vertx.core.http.ServerWebSocket;
import net.bluemind.central.reverse.proxy.vertx.impl.RequestInfoMatcher;
import net.bluemind.central.reverse.proxy.vertx.impl.WebSocketInfoMatcher;

public interface AuthMatcher<T> extends DispatchInfoMatcher<T, Optional<Auth>> {

	static AuthMatcher<HttpServerRequestContext> requestMatcher() {
		return new RequestInfoMatcher();
	}

	static AuthMatcher<ServerWebSocket> webSocketMatcher() {
		return new WebSocketInfoMatcher();
	}

}
