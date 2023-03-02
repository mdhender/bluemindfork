/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-20223
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
package net.bluemind.videoconferencing.webex;

import java.io.IOException;
import java.util.Map;

import com.google.common.io.ByteStreams;

import net.bluemind.system.service.RegisteredExternalSystem;

public class WebexSystem extends RegisteredExternalSystem {

	private static final String openIdScope = "spark:kms meeting:schedules_read meeting:participants_read meeting:preferences_read meeting:participants_write meeting:schedules_write";

	public WebexSystem() {
		super(WebexProvider.ID, "WEBEX Video Conferencing", AuthKind.OPEN_ID_PKCE, Map.of("scope", openIdScope));
	}

	@Override
	public byte[] getLogo() {
		try {
			return ByteStreams
					.toByteArray(WebexProvider.class.getClassLoader().getResourceAsStream("resources/logo.png"));
		} catch (IOException e) {
			return new byte[0];
		}
	}

	@Override
	public boolean handles(String userAccountIdentifier) {
		return false;
	}

}
