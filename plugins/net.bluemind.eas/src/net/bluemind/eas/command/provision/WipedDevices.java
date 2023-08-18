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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.http.AuthenticatedEASQuery;
import net.bluemind.eas.impl.Backends;

public final class WipedDevices {

	private static final Logger logger = LoggerFactory.getLogger(WipedDevices.class);
	private static final Map<String, String> wipedDevicesIdentifiers = new ConcurrentHashMap<>();

	private WipedDevices() {

	}

	/**
	 * Called from EasActivator
	 */
	public static void init() {
		Backends.internalStorage().getWipedDevices();
	}

	public static boolean isWiped(AuthenticatedEASQuery query) {
		String devId = query.deviceIdentifier();
		if (devId == null) {
			return false;
		} else {
			return wipedDevicesIdentifiers.containsKey(devId);
		}
	}

	public static String getWipeMode(String identifier) {
		return wipedDevicesIdentifiers.get(identifier);
	}

	public static void wipe(String identifier, String mode) {
		logger.info("WIPE notification for device {}", identifier);
		wipedDevicesIdentifiers.put(identifier, mode);
	}

	public static void unwipe(String identifier) {
		logger.info("Un-WIPE notification for device {}", identifier);
		wipedDevicesIdentifiers.remove(identifier);
	}

}
