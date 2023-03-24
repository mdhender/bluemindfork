package net.bluemind.webmodules.webapp.webfilters.legacy;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.network.topology.Topology;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.NeedVertx;

public class LegacyAppRedirectFilter implements IWebFilter, NeedVertx {

	private static final Map<String, String> redirect;


	static {
		redirect = new LegacyAppRedirectionLoader().load();
	}

	private HttpClientProvider clientProvider;
	private static final Logger logger = LoggerFactory.getLogger(LegacyAppRedirectFilter.class);

	private static final ILocator locator = (String service, AsyncHandler<String[]> asyncHandler) -> {
		String core = Topology.get().core().value.address();
		String[] resp = new String[] { core };
		asyncHandler.success(resp);
	};

	@Override
	public void setVertx(Vertx vertx) {
		this.clientProvider = new HttpClientProvider(vertx);
	}

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request) {
		CompletableFuture<HttpServerRequest> completableFuture = new CompletableFuture<>();

		if (match(request)) {
			String url = redirect.get(request.path());
			logger.info("Redirect for {} is {} ", request.path(), url);
			new FilterOnRole(url, request).setNext(new FilterOnPref(url, locator, clientProvider)).handle(request,
					completableFuture);
		} else {
			completableFuture.complete(request);
		}

		return completableFuture;
	}

	protected boolean match(HttpServerRequest request) {
		return redirect.containsKey(request.path());
	}









}
