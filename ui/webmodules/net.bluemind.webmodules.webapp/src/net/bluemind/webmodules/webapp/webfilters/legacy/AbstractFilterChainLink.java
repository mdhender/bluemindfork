package net.bluemind.webmodules.webapp.webfilters.legacy;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpServerRequest;
import net.bluemind.webmodule.server.handlers.TemporaryRedirectHandler;

abstract class AbstractFilterChainLink implements IFilterChainLink {
	private IFilterChainLink next;
    private final String url;
	private static final Logger logger = LoggerFactory.getLogger(AbstractFilterChainLink.class);

	public AbstractFilterChainLink(String webappUrl) {
        this.url = webappUrl;
	}

	public IFilterChainLink setNext(IFilterChainLink next) {
		if (this.next != null) {
			this.next.setNext(next);
		} else {
			this.next = next;
		}
		return this;
	}

	public void handle(HttpServerRequest request, CompletableFuture<HttpServerRequest> response) {
		if (this.isResponsible(request)) {
			this.redirect(request, response);
		} else if (this.next != null) {
			this.next.handle(request, response);
		}
	}

	protected abstract void redirect(HttpServerRequest request, CompletableFuture<HttpServerRequest> response);

	protected void goToApp(HttpServerRequest request, CompletableFuture<HttpServerRequest> response) {
		response.complete(request);
	}

	protected void goToWebApp(HttpServerRequest request, CompletableFuture<HttpServerRequest> response) {
		String userUid = request.headers().get("BMUserId");
		String domainUid = request.headers().get("BMUserDomainId");
		logger.info("Redirecting from {} to {} for user {} on domain {}", request.path(),
				url, userUid, domainUid);
		new TemporaryRedirectHandler(url).handle(request);
		response.complete(null);
	}
}