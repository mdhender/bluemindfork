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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public final class Token {

	private static final Logger logger = LoggerFactory.getLogger(Token.class);
	private static final String NO_TOKEN = "NO_TOKEN";
	private static String admin0Token;
	public static final Path TOKEN_PATH = Paths.get("/etc/bm/bm-core.tok");

	private static WatchService watchService = null;

	public static void startWatcher(WatchService watchService) {
		Token.watchService = watchService;
	}

	public static void stopWatcher() {
		if (watchService != null) {
			try {
				watchService.close();
			} catch (IOException ignored) {
				// ignored
			}
		}
	}

	private static final String loadToken() {
		File tokenFile = TOKEN_PATH.toFile();
		if (tokenFile.exists()) {
			try {
				return Files.asCharSource(tokenFile, StandardCharsets.UTF_8).read().trim().replace("\r\n", "");
			} catch (IOException e) {
				throw new RuntimeException(e);
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
		if (watchService != null) {
			WatchKey key;
			while ((key = watchService.poll()) != null) {
				for (WatchEvent<?> event : key.pollEvents()) {
					if (event.context().equals(TOKEN_PATH.getFileName().toString())) {
						logger.info("token {} changed: reloading", TOKEN_PATH);
						forceReload();
					}
				}
				key.reset();
			}
		}
		return admin0Token;
	}

	public static boolean exists() {
		String token = admin0();
		return (null != token && !token.equals(NO_TOKEN));
	}
}
