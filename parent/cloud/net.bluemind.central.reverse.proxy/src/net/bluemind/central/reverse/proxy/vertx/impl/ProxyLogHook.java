package net.bluemind.central.reverse.proxy.vertx.impl;

import java.util.Optional;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.central.reverse.proxy.vertx.Auth;
import net.bluemind.central.reverse.proxy.vertx.DispatchInfoMatcher;
import net.bluemind.central.reverse.proxy.vertx.HttpServerRequestContext;
import net.bluemind.central.reverse.proxy.vertx.ProxyResponse;

public class ProxyLogHook implements BiConsumer<HttpServerRequestContext, ProxyResponse> {

	private final Logger logger = LoggerFactory.getLogger(ProxyLogHook.class);

	private final DispatchInfoMatcher<HttpServerRequestContext, Optional<Auth>> infoMatcher;

	public ProxyLogHook(DispatchInfoMatcher<HttpServerRequestContext, Optional<Auth>> infoMatcher) {
		this.infoMatcher = infoMatcher;
	}

	@Override
	public void accept(HttpServerRequestContext context, ProxyResponse response) {
		infoMatcher.match(context).onSuccess(maybeAuth -> maybeAuth.ifPresent(auth -> {
			if (logger.isInfoEnabled()) {
				ProxyResponseImpl respImpl = (ProxyResponseImpl) response;

				logger.info("[{}] {} - {} {} => {} {}", auth.login, respImpl.targetAddress(),
						context.request().method(), context.request().path(), response.getStatusCode(),
						response.getStatusMessage());
			}
		}));
	}

}
