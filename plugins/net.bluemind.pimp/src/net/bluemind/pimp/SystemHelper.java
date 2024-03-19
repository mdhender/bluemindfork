/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.pimp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemHelper {
	private static final Logger logger = LoggerFactory.getLogger(SystemHelper.class);

	public static int cmd(String... argv) throws IOException {
		return cmd(Collections.emptyMap(), argv);
	}

	public static int cmd(Map<String, String> customEnv, String... argv) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(argv);
		pb.redirectErrorStream(true);

		if (customEnv != null) {
			pb.environment().putAll(customEnv);
		}

		Process pid = pb.start();
		InputStream in = pid.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		String line = null;
		do {
			line = br.readLine();
			logger.info("{}", line != null ? line : "---");
		} while (line != null);

		int exit = 1;
		try {
			exit = pid.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error("cmd '{}' interrupted", List.of(argv).stream().collect(Collectors.joining(" ")));
		}

		return exit;
	}
}
