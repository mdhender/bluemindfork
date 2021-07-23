/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.videoconferencing.saas.service.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.videoconferencing.saas.api.BlueMindVisioTokenResponse;
import net.bluemind.videoconferencing.saas.api.IVideoConferencingSaas;

public class VideoConferencingSaasService implements IVideoConferencingSaas {
	private static final Logger logger = LoggerFactory.getLogger(VideoConferencingSaasService.class);
	private final IServiceProvider serviceProvider;

	private static final String YUM_LICENSE_PATH = "/etc/yum.repos.d/bm.repo";
	private static final String APT_LICENSE_PATH = "/etc/apt/sources.list.d/bm.list";

	private static final String BLUEMIND_VIDEO_TOKEN_URL = "https://"
			+ tryReadDomain(Paths.get("/etc/bm", "bluemind.video"), "visio.bluemind.net") + "/api/token/get";

	private static String tryReadDomain(Path p, String defaultValue) {
		if (p.toFile().exists()) {
			try {
				return new String(Files.readAllBytes(p)).trim();
			} catch (IOException ie) {
				// sonar: OK
			}
		}
		return defaultValue;

	}

	public VideoConferencingSaasService(BmContext context) {
		serviceProvider = context.getServiceProvider();
	}

	@Override
	public BlueMindVisioTokenResponse token(String roomName) {
		AuthUser user = serviceProvider.instance(IAuthentication.class).getCurrentUser();
		if (user == null) {
			logger.error("current user not found or not usable");
			throw new ServerFault("current user not found or not usable", ErrorCode.INVALID_LOGIN);
		}
		String subscription = tryRead(APT_LICENSE_PATH);

		if (subscription == null) {
			subscription = tryRead(YUM_LICENSE_PATH);
		}
		if (subscription == null) {
			logger.error("Subscription is not readable: can't access BlueMind visio");
			throw new ServerFault("subscription is not readable", ErrorCode.FAILURE);
		}

		JsonObject payload = new JsonObject();
		payload.put("subscription", subscription);
		payload.put("room", roomName);
		payload.put("moderator", true);
		payload.put("name", user.displayName);
		payload.put("email", user.value.defaultEmailAddress());

		try (AsyncHttpClient client = new DefaultAsyncHttpClient()) {
			CompletableFuture<BlueMindVideoTokenResponse> req = client.preparePost(BLUEMIND_VIDEO_TOKEN_URL) //
					.setHeader("Content-Type", "application/json") //
					.setBody(payload.encode()) //
					.execute() //
					.toCompletableFuture() //
					.thenApply(httpresp -> {
						boolean success = httpresp.getStatusCode() == 200;
						if (success) {
							try {
								return decodeJsonResponse(httpresp.getResponseBody());
							} catch (IOException e) {
								logger.error("unable to decode json data", e);
								throw new ServerFault("unable to decode json data: " + e.getMessage());
							}
						} else {
							throw new ServerFault("incorrect bluemind visio saas token service response ("
									+ httpresp.getStatusCode() + "): " + httpresp.getStatusText());
						}
					});
			return req.get(30, TimeUnit.SECONDS);
		} catch (IOException | TimeoutException | ExecutionException e) {
			logger.error("Unable to connect to bluemind visio service", e);
			throw new ServerFault("unable to connect to bluemind visio service: " + e.getMessage());
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			logger.error("Interrupted", ie);
			throw new ServerFault("interrupted: " + ie.getMessage());
		}
	}

	private BlueMindVisioTokenResponse decodeJsonResponse(String jsonmessage) throws IOException {
		BlueMindVisioTokenResponse jresp = new BlueMindVisioTokenResponse();
		jresp.features = new HashMap<>();

		JsonFactory jsonfactory = new JsonFactory();
		try (JsonParser parser = jsonfactory.createParser(jsonmessage)) {
			parser.nextToken();
			if (parser.currentToken() != JsonToken.START_OBJECT) {
				throw new IllegalStateException("Expected an object");
			}

			while (parser.nextToken() != JsonToken.END_OBJECT) {
				String fieldName = parser.getCurrentName();
				if (fieldName == null) {
					continue;
				}
				switch (fieldName) {
				case "error":
					jresp.error = parser.nextTextValue();
					break;
				case "max_duration":
					jresp.maxDuration = parser.nextIntValue(0);
					break;
				case "max_occupants":
					jresp.maxOccupants = parser.nextIntValue(30);
					break;
				case "room":
					jresp.room = parser.nextTextValue();
					break;
				case "features":
					if (parser.nextToken() != JsonToken.START_OBJECT) {
						throw new IllegalStateException("Expected features to be an object");
					}
					while (parser.nextToken() != JsonToken.END_OBJECT) {
						String featureName = parser.getText();
						Boolean featureEnabled = parser.nextBooleanValue();
						if (featureName != null && !featureName.isEmpty()) {
							jresp.features.put(featureName, featureEnabled);
						}
					}
					break;
				case "token":
					jresp.token = parser.nextTextValue();
					break;
				default:
					logger.warn("Unknown field {}", fieldName);
					break;
				}
			}
		}
		return jresp;
	}

	private String tryRead(String path) {
		File file = new File(path);
		if (!file.exists()) {
			return null;
		}
		try {
			return new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			logger.warn("Cannot read subscription file {}:{}", path, e.getMessage());
			return null;
		}
	}
}
