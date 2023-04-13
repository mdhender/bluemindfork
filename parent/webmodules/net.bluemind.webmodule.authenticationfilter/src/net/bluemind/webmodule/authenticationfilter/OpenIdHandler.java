/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.webmodule.authenticationfilter;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Strings;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.openid.api.OpenIdProperties;
import net.bluemind.webmodule.authenticationfilter.internal.ExternalCreds;

public class OpenIdHandler extends AbstractAuthHandler implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(OpenIdHandler.class);
	private static final Decoder b64UrlDecoder = Base64.getUrlDecoder();

	@Override
	public void handle(HttpServerRequest event) {
		if (!Strings.isNullOrEmpty(event.params().get("code"))) {
			List<String> forwadedFor = new ArrayList<>(event.headers().getAll("X-Forwarded-For"));
			forwadedFor.add(event.remoteAddress().host());

			String code = event.params().get("code");
			String state = event.params().get("state");

			JsonObject jsonState = new JsonObject(new String(b64UrlDecoder.decode(state.getBytes())));
			String key = jsonState.getString("codeVerifierKey");
			String codeVerifier = AuthenticationFilter.verify(key);
			if (Strings.isNullOrEmpty(codeVerifier)) {
				error(event, new Throwable("Failed to fetch codeVerifier"));
				return;
			}

			String domainUid = jsonState.getString("domain_uid");
			Map<String, String> domainSettings = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
					.get(domainUid);

			try {
				String endpoint = domainSettings.get(OpenIdProperties.OPENID_TOKEN_ENDPOINT.name());
				URI uri = new URI(endpoint);
				HttpClient client = initHttpClient(uri);

				client.request(HttpMethod.POST, uri.getPath(), reqHandler -> {
					if (reqHandler.succeeded()) {
						HttpClientRequest r = reqHandler.result();
						r.response(respHandler -> {
							if (respHandler.succeeded()) {
								HttpClientResponse resp = respHandler.result();
								resp.body(body -> {
									JsonObject token = new JsonObject(new String(body.result().getBytes()));
									String redirectTo = jsonState.getString("path");
									validateToken(event, forwadedFor, token, redirectTo, domainUid);
								});
							} else {
								error(event, respHandler.cause());
							}
						});

						MultiMap headers = r.headers();
						headers.add(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
						headers.add(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
						String params = "grant_type=authorization_code";
						params += "&client_id=" + encode(domainSettings.get(OpenIdProperties.OPENID_CLIENT_ID.name()));
						params += "&client_secret="
								+ encode(domainSettings.get(OpenIdProperties.OPENID_CLIENT_SECRET.name()));
						params += "&code=" + encode(code);
						params += "&code_verifier=" + encode(codeVerifier);
						params += "&redirect_uri=" + encode("https://" + event.host() + "/auth/openid");
						params += "&scope=openid";

						byte[] postData = params.getBytes(StandardCharsets.UTF_8);
						headers.add(HttpHeaders.CONTENT_LENGTH, Integer.toString(postData.length));
						r.write(Buffer.buffer(postData));
						r.end();
					} else {
						error(event, reqHandler.cause());
					}
				});

				return;
			} catch (Exception e) {
				error(event, e);
				logger.error(e.getMessage(), e);
			}
		}

		event.response().end();
	}

	private String encode(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	private void validateToken(HttpServerRequest request, List<String> forwadedFor, JsonObject token, String redirectTo,
			String domainUid) {
		DecodedJWT accessToken = null;
		try {
			accessToken = JWT.decode(token.getString("access_token"));
		} catch (Throwable t) {
			logger.error("Unexpected token endpoint response : " + token);
			throw t;
		}

		Claim email = accessToken.getClaim("email");
		if (email.isMissing() || email.isNull()) {
			error(request, new Throwable("Failed to validate id_token: no email"));
			return;
		}

		AuthProvider prov = new AuthProvider(vertx);

		ExternalCreds creds = new ExternalCreds();
		creds.setLoginAtDomain(email.asString());
		createSession(request, prov, forwadedFor, creds, redirectTo, domainUid, token);

	}

}
