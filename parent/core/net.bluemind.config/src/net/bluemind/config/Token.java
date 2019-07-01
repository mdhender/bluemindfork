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

import com.google.common.base.Throwables;
import com.google.common.io.Files;

public final class Token {

	private static final Logger logger = LoggerFactory.getLogger(Token.class);
	private static final String NO_TOKEN = "NO_TOKEN";
	private static String admin0Token;

	private static final String loadToken() {
		File tokenFile = new File("/etc/bm/bm-core.tok");
		if (tokenFile.exists()) {
			try {
				String ret = Files.toString(new File("/etc/bm/bm-core.tok"), Charset.defaultCharset()).trim()
						.replaceAll("\r\n", "");
				return ret;
			} catch (IOException e) {
				throw Throwables.propagate(e);
			}
		} else {
			logger.debug("token file ({}) doesnt exists", tokenFile);
			return NO_TOKEN;
		}
	}

	public static void forceReload() {
		admin0Token = loadToken();
	}

	public static final String admin0() {
		if (null == admin0Token || admin0Token.equals(NO_TOKEN)) {
			admin0Token = loadToken();
		}
		return admin0Token;
	}

	public static boolean exists() {
		String token = admin0();
		return (null != token && !token.equals(NO_TOKEN));
	}
}
