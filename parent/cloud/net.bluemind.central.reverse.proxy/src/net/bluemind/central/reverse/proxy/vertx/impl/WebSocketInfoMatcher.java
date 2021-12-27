package net.bluemind.central.reverse.proxy.vertx.impl;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.ServerWebSocket;
import net.bluemind.central.reverse.proxy.vertx.Auth;
import net.bluemind.central.reverse.proxy.vertx.AuthMatcher;

public class WebSocketInfoMatcher implements AuthMatcher<ServerWebSocket> {

	private final Logger logger = LoggerFactory.getLogger(WebSocketInfoMatcher.class);

	public Future<Optional<Auth>> match(ServerWebSocket websocket) {
		String cookies = websocket.headers().get(HttpHeaders.COOKIE);
		Optional<Auth> auth = ServerCookieDecoder.LAX.decode(cookies).stream()
				.filter(cookie -> "BMCRP".equals(cookie.name())).findFirst().map(Cookie::value).map(Auth::create);
		return Future.succeededFuture(auth);
	}
}
