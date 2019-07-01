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
package net.bluemind.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class InstallationId {

	private static final Logger logger = LoggerFactory.getLogger(InstallationId.class);

	protected static String identifier;

	private static final String ID_PREFIX = "bluemind-";

	private static final String NO_ID = ID_PREFIX + "noid";

	public static String getIdentifier() {
		return identifier;
	}

	public static void reload() {
		init();
	}

	protected static void init() {
		File mcastIdFile = new File("/etc/bm/mcast.id");
		if (mcastIdFile.exists()) {
			try {
				identifier = "bluemind-" + Files.toString(mcastIdFile, Charset.defaultCharset());
			} catch (IOException e) {
				logger.error("error during reading mcast.id {}", e.getMessage(), e);
			}
		} else {
			logger.warn("/etc/bm/mcast.id doesnt exists. mcastid will be {}", NO_ID);
			identifier = NO_ID;
		}
	}
}
