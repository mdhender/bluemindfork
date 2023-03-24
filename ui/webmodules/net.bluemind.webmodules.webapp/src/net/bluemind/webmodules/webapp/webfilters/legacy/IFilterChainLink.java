package net.bluemind.webmodules.webapp.webfilters.legacy;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.http.HttpServerRequest;

interface IFilterChainLink {

    boolean isResponsible(HttpServerRequest request);

	IFilterChainLink setNext(IFilterChainLink next);

	void handle(HttpServerRequest request, CompletableFuture<HttpServerRequest> response);
}