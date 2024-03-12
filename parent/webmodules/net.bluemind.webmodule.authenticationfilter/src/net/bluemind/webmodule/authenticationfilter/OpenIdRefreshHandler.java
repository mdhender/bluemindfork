/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.webmodule.authenticationfilter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import net.bluemind.common.cache.persistence.CacheBackingStore;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.keycloak.utils.endpoints.KeycloakEndpoints;
import net.bluemind.webmodule.authenticationfilter.internal.SessionData;
import net.bluemind.webmodule.authenticationfilter.internal.SessionsCache;

public class OpenIdRefreshHandler implements Handler<Long> {
	private static final Logger logger = LoggerFactory.getLogger(OpenIdRefreshHandler.class);

	private class SessionRefresh {
		private final long timerId;
		private final SessionData sessionData;

		public SessionRefresh(long timerId, SessionData sessionData) {
			this.timerId = timerId;
			this.sessionData = sessionData;
		}

		private void refresh() {
			httpClient.request(new RequestOptions().setMethod(HttpMethod.POST)
					.setAbsoluteURI(KeycloakEndpoints.tokenEndpoint(sessionData.realm)), this::decorateRequest);
		}

		private void decorateRequest(AsyncResult<HttpClientRequest> request) {
			if (!request.succeeded()) {
				logger.error("Unable to refresh session {}", sid, request.cause());
				return;
			}

			HttpClientRequest r = request.result();
			r.response(this::refreshResponse);

			byte[] postData = new StringBuilder().append("client_id=").append(IKeycloakUids.clientId(sessionData.realm)) //
					.append("&client_secret=").append(sessionData.openIdClientSecret)
					.append("&grant_type=refresh_token&refresh_token=")
					.append(sessionData.jwtToken.getString("refresh_token")).toString()
					.getBytes(StandardCharsets.UTF_8);

			MultiMap headers = r.headers();
			headers.add(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
			headers.add(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
			headers.add(HttpHeaders.CONTENT_LENGTH, Integer.toString(postData.length));
			r.write(Buffer.buffer(postData));
			r.end();
		}

		private void refreshResponse(AsyncResult<HttpClientResponse> response) {
			if (!response.succeeded()) {
				logger.error("Fail to refresh session {}", sid, response.cause());
				return;
			}

			response.result().body(body -> {
				if (response.result().statusCode() != 200) {
					logger.error("Fail to refresh session {}, response status {}, content {}", sid,
							response.result().statusCode(), new String(body.result().getBytes()));
					return;
				}

				JsonObject jwtToken = new JsonObject(new String(body.result().getBytes()));

				CacheBackingStore<SessionData> cache = SessionsCache.get();
				synchronized (cache) {
					cache.put(sid, sessionData.setOpenId(jwtToken, sessionData.realm, sessionData.openIdClientSecret,
							timerId));
				}

				logger.debug("Session {} refreshed successfully", sid);
			});
		}
	}

	private static final long TIMER_PERIOD = TimeUnit.MINUTES.toMinutes(30);
	private static final long REFRESH_BEFORE_EXPIRATION = TimeUnit.MINUTES.toMinutes(35);

	private final Vertx vertx;
	private final HttpClient httpClient;
	private final String sid;

	public OpenIdRefreshHandler(Vertx vertx, HttpClient httpClient, String sid) {
		this.vertx = vertx;
		this.httpClient = httpClient;
		this.sid = sid;
	}

	public long setRefreshTimer() {
		long refreshTimerId = vertx.setPeriodic(TimeUnit.MINUTES.toMillis(TIMER_PERIOD), this);
		logger.debug("Refresh timer {} set for {}", refreshTimerId, sid);
		return refreshTimerId;
	}

	@Override
	public void handle(Long timerId) {
		try {
			logger.debug("Starting refresh timer {} for session {}", timerId, sid);
			SessionData sessionData;
			CacheBackingStore<SessionData> cache = SessionsCache.get();
			synchronized (cache) {
				sessionData = cache.getIfPresent(sid);
			}

			if (sessionData == null) {
				logger.debug("Session {} not found, cancel refresh timer {}", sid, timerId);
				vertx.cancelTimer(timerId);
				return;
			}

			if (timerId != sessionData.refreshTimerId) {
				// Remove refresh timer if not the official refresh timer for current session
				logger.warn(
						"Current timer {} for session {} is not the registered session refresh timer {}, cancel refresh timer {}",
						timerId, sid, sessionData.refreshTimerId, timerId);
				vertx.cancelTimer(timerId);
				return;
			}

			DecodedJWT accessToken = JWT.decode(sessionData.jwtToken.getString("access_token"));
			long expireIn = Duration.between(Instant.now(), accessToken.getExpiresAtAsInstant()).toMinutes();
			if (expireIn > REFRESH_BEFORE_EXPIRATION) {
				// No refresh needed
				logger.debug(
						"No refresh needed for session {}, token expire in {}mn - refresh only if expiration is less than {}mn",
						sid, expireIn, REFRESH_BEFORE_EXPIRATION);
				return;
			}

			logger.debug("Refreshing session {} as token expire in {}mn - refresh only if expiration is less than {}mn",
					sid, expireIn, REFRESH_BEFORE_EXPIRATION);
			new SessionRefresh(timerId, sessionData).refresh();
		} catch (RuntimeException re) {
			// Unsupported/unkonwn error, session will expire as no more refresh done...
			// Maybe need to unset session refreshTimerId. Next acess to session in cache
			// will set a new refresh timer ?
			logger.error("Error on periodic OpenId token refresh for session {}, cancel timer {}", sid, timerId, re);
			vertx.cancelTimer(timerId);
		}
	}
}
