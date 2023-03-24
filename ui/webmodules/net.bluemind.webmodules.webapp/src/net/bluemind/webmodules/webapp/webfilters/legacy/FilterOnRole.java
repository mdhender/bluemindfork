package net.bluemind.webmodules.webapp.webfilters.legacy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.http.HttpServerRequest;

class FilterOnRole extends AbstractFilterChainLink {
    private final Set<String> roles;
	private static final String ROLE_MAIL_WEBAPP = "hasMailWebapp";
	private static final String ROLE_WEBMAIL = "hasWebmail";

	public FilterOnRole(String webappUrl, HttpServerRequest request) {
		super(webappUrl);
		roles = request.headers().get("BMRoles") != null ? //
				new HashSet<>(Arrays.asList(request.headers().get("BMRoles").split(","))) //
				: Collections.<String>emptySet();
	}

	public boolean isResponsible(HttpServerRequest request) {
		return !roles.contains(ROLE_MAIL_WEBAPP) || !roles.contains(ROLE_WEBMAIL);
	}

	@Override
	protected void redirect(HttpServerRequest request, CompletableFuture<HttpServerRequest> response) {
		if (roles.contains(ROLE_MAIL_WEBAPP)) {
			goToWebApp(request, response);
		} else {
			goToApp(request, response);
		}

	}
}