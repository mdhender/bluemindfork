/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.hps.auth.oauth;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.proxy.http.auth.api.AuthRequirements;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer;

public class OAuthEnforcer implements IAuthEnforcer {
	private Boolean enabled;

	public OAuthEnforcer() {
		File ini = new File("/etc/bm/oauth.ini");
		enabled = ini.exists();
	}

	@Override
	public AuthRequirements enforce(ISessionStore checker, HttpServerRequest req) {
		if (!enabled.booleanValue()) {
			return AuthRequirements.notHandle();
		}

		// make /login/native happy ( and login public ressources available )
		if (req.path().startsWith("/login/") && !req.path().equals("/login/index.html")) {
			return AuthRequirements.notHandle();
		}

		if (io.vertx.core.http.HttpMethod.GET == req.method()) {
			return AuthRequirements.needSession(getProtocol());
		} else {
			return AuthRequirements.notHandle();
		}
	}

	@Override
	public IAuthProtocol getProtocol() {
		OAuthIni oAuthIni = new OAuthIni("/etc/bm/oauth.ini");
		String host = oAuthIni.getProperty("host");
		String clientId = oAuthIni.getProperty("client-id");
		String clientSecret = oAuthIni.getProperty("client-secret");
		try {
			Builder requestBuilder = HttpRequest.newBuilder(new URI(host));
			requestBuilder.GET().build();
			HttpRequest request = requestBuilder.build();
			HttpClient cli = HttpClient.newHttpClient();
			HttpResponse<String> resp = cli.send(request, BodyHandlers.ofString());
			JsonObject conf = new JsonObject(resp.body());
			OAuthConf oAuthConf = new OAuthConf(conf, clientId, clientSecret);
			return new OAuthProtocol(oAuthConf);
		} catch (Exception e) {
			throw new ServerFault(e.getMessage());
		}
	}

}