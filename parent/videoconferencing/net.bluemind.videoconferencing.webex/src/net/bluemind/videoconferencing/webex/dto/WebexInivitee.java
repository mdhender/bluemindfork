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
package net.bluemind.videoconferencing.webex.dto;

import java.util.Optional;

import io.vertx.core.json.JsonObject;

public class WebexInivitee {

	public final String email;
	public final Optional<String> displayName;

	public WebexInivitee(String email, Optional<String> displayName) {
		this.email = email;
		this.displayName = displayName;
	}

	public JsonObject toJson() {
		JsonObject invitee = new JsonObject();
		invitee.put("email", email);
		displayName.ifPresent(name -> invitee.put("displayname", name));
		return invitee;
	}
}
