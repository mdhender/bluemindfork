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
package net.bluemind.videoconferencing.zoom.dto;

import io.vertx.core.json.JsonObject;

public class ZoomDialInfo {

	public final String confId;
	public final String weblink;

	public ZoomDialInfo(String confId, String weblink) {
		this.confId = confId;
		this.weblink = weblink;
	}

	public static ZoomDialInfo fromJson(JsonObject response) {
		String confId = response.getString("id");
		String webLink = response.getString("join_url");
		return new ZoomDialInfo(confId, webLink);
	}

}
