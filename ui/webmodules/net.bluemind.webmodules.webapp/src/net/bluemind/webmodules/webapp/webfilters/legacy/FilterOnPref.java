package net.bluemind.webmodules.webapp.webfilters.legacy;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxServiceProvider;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.user.api.IUserSettingsAsync;

class FilterOnPref extends AbstractFilterChainLink {
	private final ILocator locator;
	private final HttpClientProvider clientProvider;

	public FilterOnPref(String webappUrl, ILocator locator, HttpClientProvider clientProvider) {
		super(webappUrl);
		this.locator = locator;
		this.clientProvider = clientProvider;
	}

	public boolean isResponsible(HttpServerRequest request) {
		return true;
	}

	protected void redirect(HttpServerRequest request, CompletableFuture<HttpServerRequest> response) {
		String userUid = request.headers().get("BMUserId");
		String domainUid = request.headers().get("BMUserDomainId");
		String apiKey = request.headers().get("BMSessionId");
		VertxServiceProvider provider = new VertxServiceProvider(clientProvider, locator, apiKey).from(request);
		provider.instance(TagDescriptor.bm_core.getTag(), IUserSettingsAsync.class, domainUid).getOne(userUid,
				"mail-application", new AsyncHandler<String>() {

					@Override
					public void success(String mailApplication) {
						if (!mailApplication.equals("mail-webapp")) {
							goToApp(request, response);
						} else {
							goToWebApp(request, response);
						}
					}

					@Override
					public void failure(Throwable e) {
						goToApp(request, response);
					}
				});
	}
}