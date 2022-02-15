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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
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
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;
import net.bluemind.videoconferencing.saas.api.BlueMindVideoRoom;
import net.bluemind.videoconferencing.saas.api.BlueMindVideoTokenResponse;
import net.bluemind.videoconferencing.saas.api.IVideoConferencingSaas;
import net.bluemind.videoconferencing.saas.service.IInCoreVideoConferencingSaas;

public class VideoConferencingSaasService implements IVideoConferencingSaas, IInCoreVideoConferencingSaas {
	private static final Logger logger = LoggerFactory.getLogger(VideoConferencingSaasService.class);
	private final IServiceProvider serviceProvider;
	private final BmContext context;

	private RoomContainerStoreService storeService;

	private static final String YUM_LICENSE_PATH = "/etc/yum.repos.d/bm.repo";
	private static final String APT_LICENSE_PATH = "/etc/apt/sources.list.d/bm.list";

	private static final String BLUEMIND_VIDEO_TOKEN_URL = "https://"
			+ tryReadDomain(Paths.get("/etc/bm", "bluemind.video"), "video.bluemind.net") + "/api/token/get";

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

	private static final class RoomDetails {
		public final boolean moderator;
		public final String title;

		public RoomDetails(boolean moderator, String title) {
			this.title = title;
			this.moderator = moderator;
		}
	}

	public VideoConferencingSaasService(BmContext context) {
		this.context = context;
		serviceProvider = context.getServiceProvider();

		String containerId = InstallationId.getIdentifier();
		ContainerStore containerStore = new ContainerStore(context, context.getDataSource(),
				context.getSecurityContext());
		Container container = null;
		try {
			container = containerStore.get(containerId);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (container == null) {
			container = Container.create(containerId, "videoconferencing_room", "videoconferencing_room",
					context.getSecurityContext().getSubject(), true);
			try {
				containerStore.create(container);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		}

		storeService = new RoomContainerStoreService(context.getDataSource(), context.getSecurityContext(), container);
	}

	@Override
	public BlueMindVideoRoom create(BlueMindVideoRoom room) {
		RBACManager.forContext(context).check("hasFullVideoconferencing", "hasSimpleVideoconferencing");
		storeService.create(UUID.randomUUID().toString(), room.identifier, room);
		return room;
	}

	private String getExternalDomain() {
		SystemConf sysconf = LocalSysconfCache.get();
		return sysconf.stringValue(SysConfKeys.external_url.name());
	}

	private RoomDetails getRoomDetails(Optional<AuthUser> user, String roomName) {
		boolean moderator;
		ItemValue<BlueMindVideoRoom> room = storeService.byIdentifier(roomName);
		if (user.isPresent()) {
			// Instant conference, we have a user, so every logged in user should be
			// considered as moderators. Otherwise, only the organizer is a moderator
			moderator = room == null || user.get().uid.equals(room.value.owner);
		} else {
			moderator = false;
		}
		return new RoomDetails(moderator, room != null ? room.value.title : "");
	}

	@Override
	public BlueMindVideoTokenResponse token(String roomName) {
		Optional<AuthUser> user = Optional.empty();
		// This check is needed, because getCurrentUser will check fir isAnonymous and
		// reject the request
		if (!context.getSecurityContext().isAnonymous()) {
			user = Optional.ofNullable(serviceProvider.instance(IAuthentication.class).getCurrentUser());
		}
		String subscription = tryRead(APT_LICENSE_PATH);

		if (subscription == null) {
			subscription = tryRead(YUM_LICENSE_PATH);
		}
		if (subscription == null) {
			logger.error("Subscription is not readable: can't access BlueMind visio");
			throw new ServerFault("subscription is not readable", ErrorCode.FAILURE);
		}
		RoomDetails roomDetails = getRoomDetails(user, roomName);

		if (!user.isPresent()) {
			BlueMindVideoTokenResponse resp = new BlueMindVideoTokenResponse();
			resp.room = roomName;
			resp.roomTitle = roomDetails.title;
			resp.token = null;
			return resp;
		} else {
			AuthUser u = user.get();

			JsonObject payload = new JsonObject();
			payload.put("external_domain", getExternalDomain());
			payload.put("subscription", subscription);
			payload.put("room", roomName);
			payload.put("moderator", roomDetails.moderator);

			payload.put("name", u.displayName);
			payload.put("email", u.value.defaultEmailAddress());
			if (u.roles.contains("hasFullVideoconferencing")) {
				payload.put("full_visio", true);
			}

			try (AsyncHttpClient client = new DefaultAsyncHttpClient()) {
				CompletableFuture<BlueMindVideoTokenResponse> req = client.preparePost(BLUEMIND_VIDEO_TOKEN_URL) //
						.setHeader("Content-Type", "application/json") //
						.setBody(payload.encode()) //
						.execute() //
						.toCompletableFuture() //
						.exceptionally(t -> {
							throw new ServerFault("Unable to connect to BlueMind visio: " + t.getMessage());
						}).thenApply(httpresp -> {
							boolean success = httpresp.getStatusCode() == 200;
							if (success) {
								try {
									return decodeJsonResponse(httpresp.getResponseBody(), roomDetails);
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
	}

	private BlueMindVideoTokenResponse decodeJsonResponse(String jsonmessage, RoomDetails roomDetails)
			throws IOException {
		BlueMindVideoTokenResponse jresp = new BlueMindVideoTokenResponse();
		jresp.features = new HashMap<>();
		jresp.roomTitle = roomDetails.title;

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
				case "external_domain":
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

	@Override
	public void updateTitle(String roomName, String title) {
		RBACManager.forContext(context).check("hasFullVideoconferencing", "hasSimpleVideoconferencing");
		ItemValue<BlueMindVideoRoom> ivroom = storeService.byIdentifier(roomName);
		if (ivroom != null) {
			ivroom.value.title = title;
			storeService.update(ivroom.uid, ivroom.displayName, ivroom.value);
		}
	}

	@Override
	public BlueMindVideoRoom get(String roomName) {
		ItemValue<BlueMindVideoRoom> room = storeService.byIdentifier(roomName);
		return room != null ? room.value : null;
	}

	@Override
	public void delete(String roomName) {
		RBACManager.forContext(context).check("hasFullVideoconferencing", "hasSimpleVideoconferencing");
		ItemValue<BlueMindVideoRoom> itemRoom = storeService.byIdentifier(roomName);
		storeService.delete(itemRoom.uid);
	}

}
