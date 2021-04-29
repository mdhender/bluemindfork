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
package net.bluemind.videoconferencing.starleaf.client;

import java.util.ArrayList;
import java.util.List;

import io.netty.handler.codec.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import net.bluemind.videoconferencing.starleaf.dto.SLConference;
import net.bluemind.videoconferencing.starleaf.dto.SLConferenceDialInfo;

public class SLConferenceClient extends SLClient {

	private static final String URL = BASE_URL + "conferences";

	public SLConferenceClient(String token) {
		super(token);
	}

	/**
	 * https://support.starleaf.com/integrating/cloud-api/org-admin-level-requests/#List_conf
	 */
	public List<String> list() {

		JsonObject resp = execute(URL, HttpMethod.GET);

		List<String> ret = new ArrayList<>();
		resp.getJsonArray("conf_ids").forEach(confId -> {
			ret.add(confId.toString());
		});

		return ret;
	}

	/**
	 * https://support.starleaf.com/integrating/cloud-api/org-admin-level-requests/#Create_conf
	 */
	public SLConferenceDialInfo create(SLConference conf) {

		JsonObject body = new JsonObject();
		body.put("settings", conf.asJson());
		body.put("owner_id", conf.ownerId);

		JsonObject resp = execute(URL, HttpMethod.POST, body);

		return SLConferenceDialInfo.fromJson(resp.getString("conf_id"), resp.getJsonObject("dial_info"));
	}

	/**
	 * https://support.starleaf.com/integrating/cloud-api/org-admin-level-requests/#Delete_conf
	 */
	public void delete(String confId) {
		execute(URL + "/" + confId, HttpMethod.DELETE);
	}

	/**
	 * https://support.starleaf.com/integrating/cloud-api/org-admin-level-requests/#Update_conf
	 */
	public SLConferenceDialInfo update(String confId, SLConference conf) {

		JsonObject body = new JsonObject();
		body.put("settings", conf.asJson());
		body.put("owner_id", conf.ownerId);

		JsonObject resp = execute(URL + "/" + confId, HttpMethod.PUT, body);

		return SLConferenceDialInfo.fromJson(confId, resp);
	}

	/**
	 * https://support.starleaf.com/integrating/cloud-api/org-admin-level-requests/#find_conf_details
	 */
	public SLConference get(String confId) {
		JsonObject resp = execute(URL + "/" + confId, HttpMethod.GET);
		return SLConference.fromJson(resp);
	}

}
