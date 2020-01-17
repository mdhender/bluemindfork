/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.eas.command.provision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.impl.ConcurrentHashSet;
import net.bluemind.eas.http.AuthenticatedEASQuery;
import net.bluemind.eas.impl.Backends;

public final class WipedDevices {

	private static final Logger logger = LoggerFactory.getLogger(WipedDevices.class);
	private static final ConcurrentHashSet<String> wipedDevicesIdentifiers = new ConcurrentHashSet<>();

	/**
	 * Called from EasActivator
	 */
	public static void init() {
		wipedDevicesIdentifiers.addAll(Backends.internalStorage().getWipedDevices());
	}

	public static boolean isWiped(AuthenticatedEASQuery query) {
		String devId = query.deviceIdentifier();
		if (devId == null) {
			return false;
		} else {
			return wipedDevicesIdentifiers.contains(query.deviceIdentifier());
		}
	}

	public static void wipe(String id) {
		logger.info("WIPE notification for device {}", id);
		wipedDevicesIdentifiers.add(id);
	}

	public static void unwipe(String id) {
		logger.info("Un-WIPE notification for device {}", id);
		wipedDevicesIdentifiers.remove(id);
	}
}
