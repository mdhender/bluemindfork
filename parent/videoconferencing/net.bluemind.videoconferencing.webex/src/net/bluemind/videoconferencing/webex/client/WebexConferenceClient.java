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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.videoconferencing.webex.client;

import io.netty.handler.codec.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.incore.IInCoreUserAccessToken;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.videoconferencing.utils.ApiHttpHelper;
import net.bluemind.videoconferencing.webex.WebexProvider;
import net.bluemind.videoconferencing.webex.dto.WebexConference;
import net.bluemind.videoconferencing.webex.dto.WebexDialInfo;

public class WebexConferenceClient {

	private static final String apiEndpoint = "https://webexapis.com/v1/";
	private static final String meetingsPath = "meetings";
	private final ApiHttpHelper http;

	public WebexConferenceClient(BmContext context) {
		String bearerToken = ServerSideServiceProvider.getProvider(context).instance(IInCoreUserAccessToken.class).get(
				context.getSecurityContext().getContainerUid(), context.getSecurityContext().getSubject(),
				WebexProvider.ID).token;
		this.http = new ApiHttpHelper(bearerToken);
	}

	public WebexDialInfo create(WebexConference conference) {
		String url = apiEndpoint + meetingsPath;
		JsonObject response = http.execute(url, HttpMethod.POST, conference.toJson());
		return WebexDialInfo.fromJson(response);
	}

	public WebexDialInfo update(String conferenceId, WebexConference conference) {
		String url = apiEndpoint + meetingsPath + "/" + conferenceId;
		JsonObject response = http.execute(url, HttpMethod.PATCH, conference.toJson());
		return WebexDialInfo.fromJson(response);
	}

	public void delete(String conferenceId) {
		String url = apiEndpoint + meetingsPath + "/" + conferenceId;
		http.execute(url, HttpMethod.DELETE, null);
	}

}
