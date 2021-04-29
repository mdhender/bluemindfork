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
package net.bluemind.videoconferencing.starleaf.dto;

import io.vertx.core.json.JsonObject;

/**
 * https://support.starleaf.com/integrating/cloud-api/response-objects/#conf_dia
 */
public class SLConferenceDialInfo {

	public final String confId;

	// The text in a meeting invite that can be customised through the StarLeaf
	// Portal. If no custom text has been added, the default StarLeaf footer is
	// used.
	public final String customInviteFooter;

	// The plain text version of the above (without html links).
	public final String customInvitePlainText;

	public SLConferenceDialInfo(String confId, String customInviteFooter, String customInvitePlainText) {
		this.confId = confId;
		this.customInviteFooter = customInviteFooter;
		this.customInvitePlainText = customInvitePlainText;
	}

	public static SLConferenceDialInfo fromJson(String confId, JsonObject dialInfo) {
		return new SLConferenceDialInfo(confId, dialInfo.getString("custom_invite_footer"),
				dialInfo.getString("custom_invite_plain_text"));
	}
}
