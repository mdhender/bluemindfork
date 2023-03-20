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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class WebexDialInfo {

	public final String confId;
	public final String weblink;
	public final String siteUrl;
	public final String sipAddress;
	public final String telephonyAccessCode;
	public final String telephonyCallInNumbers;

	public WebexDialInfo(String confId, String weblink, String siteUrl, String sipAddress, String telephonyAccessCode,
			String telephonyCallInNumbers) {
		this.confId = confId;
		this.weblink = weblink;
		this.siteUrl = siteUrl;
		this.sipAddress = sipAddress;
		this.telephonyAccessCode = telephonyAccessCode;
		this.telephonyCallInNumbers = telephonyCallInNumbers;
	}

	public static WebexDialInfo fromJson(JsonObject response) {
		String confId = response.getString("id");
		String webLink = response.getString("webLink");
		String siteUrl = response.getString("siteUrl");
		String sipAddress = response.getString("sipAddress");
		String telephonyAccessCode = "";
		if (response.containsKey("telephonyAccessCode")) {
			telephonyAccessCode = response.getString("telephonyAccessCode");
		}
		String telephonyCallInNumbers = "";
		if (response.containsKey("telephony")) {
			JsonObject telephony = response.getJsonObject("telephony");
			if (telephony.containsKey("callInNumbers")) {
				JsonArray numbers = telephony.getJsonArray("callInNumbers");
				String[] numString = new String[numbers.size()];
				for (int i = 0; i < numbers.size(); i++) {
					numString[i] = numbers.getJsonObject(i).getString("callInNumber");
				}
				telephonyCallInNumbers = String.join(",", numString);
			}
		}
		return new WebexDialInfo(confId, webLink, siteUrl, sipAddress, telephonyAccessCode, telephonyCallInNumbers);
	}

}
