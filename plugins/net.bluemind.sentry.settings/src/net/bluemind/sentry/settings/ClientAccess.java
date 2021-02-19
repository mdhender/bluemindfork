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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.sentry.settings;

import java.util.Optional;

import io.sentry.SentryClient;

public class ClientAccess {

	private static SentryClient client;
	private static SentryProperties currentSettings;

	private ClientAccess() {
	}

	public static void setClient(SentryClient client) {
		ClientAccess.client = client;
	}

	public static void setSettings(SentryProperties sentryProps) {
		ClientAccess.currentSettings = sentryProps;
	}

	public static Optional<SentryClient> get() {
		return Optional.ofNullable(client);
	}

	public static Optional<SentryProperties> getSettings() {
		return Optional.ofNullable(currentSettings);
	}
}
