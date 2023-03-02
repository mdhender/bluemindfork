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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.videoconferencing.zoom.client;

import io.netty.handler.codec.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.incore.IInCoreUserAccessToken;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.videoconferencing.utils.ApiHttpHelper;
import net.bluemind.videoconferencing.zoom.ZoomProvider;
import net.bluemind.videoconferencing.zoom.dto.ZoomConference;
import net.bluemind.videoconferencing.zoom.dto.ZoomDialInfo;

public class ZoomConferenceClient {

	private static final String apiEndpoint = "https://api.zoom.us/v2";
	private ApiHttpHelper http;

	public ZoomConferenceClient(BmContext context) {
		String bearerToken = ServerSideServiceProvider.getProvider(context).instance(IInCoreUserAccessToken.class).get(
				context.getSecurityContext().getContainerUid(), context.getSecurityContext().getSubject(),
				ZoomProvider.ID).token;
		this.http = new ApiHttpHelper(bearerToken);
	}

	public ZoomDialInfo create(ZoomConference conference) {
		String url = apiEndpoint + "/users/me/meetings";
		JsonObject response = http.execute(url, HttpMethod.POST, conference.toJson());
		return ZoomDialInfo.fromJson(response);
	}

	public ZoomDialInfo update(String conferenceId, ZoomConference conference) {
		String url = apiEndpoint + "/meetings/" + conferenceId;
		JsonObject response = http.execute(url, HttpMethod.PATCH, conference.toJson());
		return ZoomDialInfo.fromJson(response);
	}

	public void delete(String conferenceId) {
		String url = apiEndpoint + "/meetings/" + conferenceId;
		http.execute(url, HttpMethod.DELETE, null);
	}

}
