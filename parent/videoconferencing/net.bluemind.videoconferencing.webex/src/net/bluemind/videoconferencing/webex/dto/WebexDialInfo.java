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

import io.vertx.core.json.JsonObject;

public class WebexDialInfo {

	public final String confId;
	public final String weblink;
	public final String siteUrl;
	public final String sipAddress;
	public final String meetingNumber;
	public final String password;
	public final String phoneAndVideoSystemPassword;

	public WebexDialInfo(String confId, String weblink, String siteUrl, String sipAddress, String meetingNumber,
			String password, String phoneAndVideoSystemPassword) {
		this.confId = confId;
		this.weblink = weblink;
		this.siteUrl = siteUrl;
		this.sipAddress = sipAddress;
		this.meetingNumber = meetingNumber;
		this.password = password;
		this.phoneAndVideoSystemPassword = phoneAndVideoSystemPassword;
	}

	public static WebexDialInfo fromJson(JsonObject response) {
		String confId = response.getString("id");
		String webLink = response.getString("webLink");
		String siteUrl = response.getString("siteUrl");
		String sipAddress = response.getString("sipAddress");
		String meetingNumber = response.getString("meetingNumber");
		String password = response.getString("password");
		String phoneAndVideoSystemPassword = response.getString("phoneAndVideoSystemPassword");

		return new WebexDialInfo(confId, webLink, siteUrl, sipAddress, meetingNumber, password,
				phoneAndVideoSystemPassword);
	}

}
