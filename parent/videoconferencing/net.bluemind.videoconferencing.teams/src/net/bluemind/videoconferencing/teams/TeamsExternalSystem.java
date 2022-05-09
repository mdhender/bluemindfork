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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.videoconferencing.teams;

import java.io.InputStream;

import com.google.common.io.ByteStreams;

import net.bluemind.system.api.ConnectionTestStatus;
import net.bluemind.system.service.RegisteredExternalSystem;
import net.bluemind.user.api.UserAccount;

public class TeamsExternalSystem extends RegisteredExternalSystem {

	public TeamsExternalSystem() {
		super(TeamsProvider.PROVIDER_NAME, "Video Conferencing, Meetings, Calling", AuthKind.NONE);
	}

	@Override
	public byte[] getLogo() {
		try (InputStream in = getClass().getClassLoader()
				.getResourceAsStream("/resources/external-account-teams-logo.png")) {
			return ByteStreams.toByteArray(in);
		} catch (Exception e) {
			return new byte[0];
		}
	}

	@Override
	public boolean handles(String userAccountIdentifier) {
		return userAccountIdentifier.startsWith(super.identifier);
	}

	@Override
	public ConnectionTestStatus testConnection(String domain, UserAccount account) {
		return ConnectionTestStatus.NOT_SUPPORTED;
	}

}
