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
package net.bluemind.user.service.internal;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

public class UserDefaultImage {

	private static final Logger logger = LoggerFactory.getLogger(UserDefaultImage.class);
	public static byte[] DEFAULT_INDIV_ICON = null;

	static {
		load();
	}

	public static void load() {
		try (InputStream in = UserDefaultImage.class.getResourceAsStream("/data/user.png")) {
			DEFAULT_INDIV_ICON = ByteStreams.toByteArray(in);
		} catch (IOException e) {
			logger.error("error during icon loading", e);
		}
	}

}
