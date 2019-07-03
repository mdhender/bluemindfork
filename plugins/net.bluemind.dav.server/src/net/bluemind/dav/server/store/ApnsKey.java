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
package net.bluemind.dav.server.store;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class ApnsKey {
	private static final Logger logger = LoggerFactory.getLogger(ApnsKey.class);
	private static String uuid;

	static {
		try {
			uuid = new String(Files.toByteArray(new File("/etc/bm/mcast.id")));
			logger.info("APNs uuid is {}", uuid);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			uuid = UUID.randomUUID().toString();
		}
	}

	public static final String uuid() {
		return uuid;
	}
}
