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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.videoconferencing.starleaf.dto.SLUser;

public class SLUserClient extends SLClient {

	private static final String URL = BASE_URL + "users";

	public SLUserClient(String token) {
		super(token);
	}

	public List<SLUser> list() {

		JsonObject resp = execute(URL, HttpMethod.GET);
		List<SLUser> ret = new ArrayList<>();
		JsonArray users = resp.getJsonArray("users");
		users.forEach(user -> {
			JsonObject u = new JsonObject(user.toString());
			SLUser slUser = new SLUser(u.getString("user_id"), u.getString("email"));
			ret.add(slUser);
		});

		return ret;
	}

}
