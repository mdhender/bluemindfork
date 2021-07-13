package net.bluemind.central.reverse.proxy.vertx.impl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import net.bluemind.central.reverse.proxy.vertx.Auth;
import net.bluemind.central.reverse.proxy.vertx.AuthMatcher;
import net.bluemind.central.reverse.proxy.vertx.HttpServerRequestContext;

public class RequestInfoMatcher implements AuthMatcher<HttpServerRequestContext> {

	private final Logger logger = LoggerFactory.getLogger(RequestInfoMatcher.class);

	public Future<Optional<Auth>> match(HttpServerRequestContext context) {
		Future<Auth> futureAuth = Future.succeededFuture(null);
		if (context.request().cookieMap().containsKey("BMCRP")) {
			futureAuth = loginFromCookie(context);

		} else if (getAuthorization(context) != null && getAuthorization(context).toLowerCase().startsWith("basic")) {
			futureAuth = loginFromBasicAuth(context);

		} else if (context.request().path().startsWith("/login")
				&& context.request().method().equals(HttpMethod.POST)) {
			futureAuth = loginFromLoginRoute(context);
		}
		return futureAuth.map(Optional::ofNullable);
	}

	private Future<Auth> loginFromCookie(HttpServerRequestContext context) {
		String cookies = context.request().headers().get(HttpHeaders.COOKIE);
		String login = ServerCookieDecoder.LAX.decode(cookies).stream().filter(cookie -> "BMCRP".equals(cookie.name()))
				.findFirst().map(Cookie::value).orElse(null);
		return Future.succeededFuture(Auth.create(login));
	}

	private Future<Auth> loginFromBasicAuth(HttpServerRequestContext context) {
		String base64Credentials = getAuthorization(context).substring("Basic".length()).trim();
		byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
		String credentials = new String(credDecoded, StandardCharsets.UTF_8);
		String[] loginPassword = credentials.split(":", 2);
		return Future.succeededFuture(Auth.create(loginPassword[0], loginPassword[1]));
	}

	private Future<Auth> loginFromLoginRoute(HttpServerRequestContext context) {
		return context.withAvalaibleBody().map(v -> Auth.create(context.request().getFormAttribute("login")));
	}

	private String getAuthorization(HttpServerRequestContext context) {
		return context.request().headers().get(HttpHeaders.AUTHORIZATION);
	}

}
