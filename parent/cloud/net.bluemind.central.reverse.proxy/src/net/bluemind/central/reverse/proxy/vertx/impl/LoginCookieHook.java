package net.bluemind.central.reverse.proxy.vertx.impl;

import java.util.Optional;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import net.bluemind.central.reverse.proxy.vertx.Auth;
import net.bluemind.central.reverse.proxy.vertx.DispatchInfoMatcher;
import net.bluemind.central.reverse.proxy.vertx.HttpServerRequestContext;
import net.bluemind.central.reverse.proxy.vertx.ProxyResponse;

public class LoginCookieHook implements BiConsumer<HttpServerRequestContext, ProxyResponse> {

	private final Logger logger = LoggerFactory.getLogger(LoginCookieHook.class);

	private final DispatchInfoMatcher<HttpServerRequestContext, Optional<Auth>> infoMatcher;

	public LoginCookieHook(DispatchInfoMatcher<HttpServerRequestContext, Optional<Auth>> infoMatcher) {
		this.infoMatcher = infoMatcher;
	}

	@Override
	public void accept(HttpServerRequestContext context, ProxyResponse response) {
		infoMatcher.match(context).onSuccess(maybeAuth -> maybeAuth.ifPresent(auth -> {
			Cookie cookie = new DefaultCookie("BMCRP", auth.login);
			cookie.setPath("/");
			response.headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(cookie));
		}));
	}

}
