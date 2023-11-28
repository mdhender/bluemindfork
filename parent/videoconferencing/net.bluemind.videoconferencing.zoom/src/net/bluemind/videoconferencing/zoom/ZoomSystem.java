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
package net.bluemind.videoconferencing.zoom;

import java.io.IOException;
import java.util.Map;

import com.google.common.io.ByteStreams;

import net.bluemind.system.service.RegisteredExternalSystem;

public class ZoomSystem extends RegisteredExternalSystem {

	private static final String openIdScope = "meeting:write";

	public ZoomSystem() {
		super(ZoomProvider.ID, "Zoom", AuthKind.OPEN_ID_PKCE, Map.of("scope", openIdScope, "name", "Zoom"));
	}

	@Override
	public byte[] getLogo() {
		try {
			return ByteStreams
					.toByteArray(ZoomProvider.class.getClassLoader().getResourceAsStream("resources/logo.png"));
		} catch (IOException e) {
			return new byte[0];
		}
	}

	@Override
	public boolean handles(String userAccountIdentifier) {
		return false;
	}

}
