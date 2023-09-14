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
package net.bluemind.eas.config.global;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalConfig {
	private static final Logger logger = LoggerFactory.getLogger(GlobalConfig.class);

	public static final String ROOT = "/etc/bm-eas/";

	public static int EAS_PORT = 8082;

	public static String DATA_IN_LOG_FILENAME = "data.in.logs";

	public static boolean DISABLE_POLICIES = false;
	public static boolean DATA_IN_LOGS = false;
	public static boolean FAIL_ON_INVALID_REQUESTS = false;
	public static Set<String> USERS_WATCHED = new HashSet<>();

	public static void configUpdate() throws IOException {
		disablePolicies();
		failOnInvalidRequests();
		dataInLogs();
		updateUsersListToWatch();
		if (DATA_IN_LOGS) {
			boolean all = USERS_WATCHED.isEmpty();
			logger.info("Debug content logs activated for {}",
					all ? "all eas users"
							: String.format("users : [%s]", USERS_WATCHED.stream()
									.map(u -> u.substring(0, u.indexOf("_at_"))).collect(Collectors.joining(","))));
		}
	}

	public static boolean logDataForUser(String userLogin) {
		return DATA_IN_LOGS
				&& (USERS_WATCHED.isEmpty() || USERS_WATCHED.stream().anyMatch(u -> u.equalsIgnoreCase(userLogin)));
	}

	private static void updateUsersListToWatch() throws IOException {
		if (!DATA_IN_LOGS) {
			USERS_WATCHED.clear();
			return;
		}

		Path path = Paths.get(ROOT, DATA_IN_LOG_FILENAME);
		List<String> lines = Files.readAllLines(path);
		lines.removeIf(l -> l.isBlank() || l.isEmpty());
		USERS_WATCHED.clear();
		if (lines.isEmpty()) {
			return;
		}
		USERS_WATCHED.addAll(lines);
		USERS_WATCHED = new HashSet<>(USERS_WATCHED);
		logger.debug("data.in.logs watch list [{}]", USERS_WATCHED.stream().collect(Collectors.joining(",")));
	}

	private static void disablePolicies() {
		DISABLE_POLICIES = new File(ROOT, "disable.policies").exists();
	}

	private static void dataInLogs() throws IOException {
		DATA_IN_LOGS = new File(ROOT, DATA_IN_LOG_FILENAME).exists();
	}

	private static void failOnInvalidRequests() {
		FAIL_ON_INVALID_REQUESTS = new File(ROOT, "validate.requests").exists();
	}

}
