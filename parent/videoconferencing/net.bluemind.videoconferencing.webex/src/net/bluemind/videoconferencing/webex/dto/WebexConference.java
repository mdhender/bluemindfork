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

import java.util.List;
import java.util.Optional;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class WebexConference {

	public final boolean adhoc = false;
	public final boolean sendEmail = false;
	public final String title;
	public final String start;
	public final String end;
	public final String timezone;
	public final Optional<String> recurrence;
	public final List<WebexInivitee> invitees;

	public WebexConference(String title, String start, String end, String timezone, Optional<String> recurrence,
			List<WebexInivitee> invitees) {
		this.title = title;
		this.start = start;
		this.end = end;
		this.timezone = timezone;
		this.recurrence = recurrence;
		this.invitees = invitees;
	}

	public String toJson() {
		JsonObject json = new JsonObject();
		json.put("adhoc", false);
		json.put("title", title);
		json.put("start", start);
		json.put("end", end);
		json.put("timezone", timezone);
		recurrence.ifPresent(rec -> json.put("recurrence", rec));
		if (!invitees.isEmpty()) {
			JsonArray inviteesArray = new JsonArray();
			for (WebexInivitee invitee : invitees) {
				inviteesArray.add(invitee.toJson());
			}
			json.put("invitees", inviteesArray);
		}
		json.put("sendEmail", false);
		return json.encode();
	}

}
