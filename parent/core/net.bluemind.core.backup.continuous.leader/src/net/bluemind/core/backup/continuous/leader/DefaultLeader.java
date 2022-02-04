/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.leader;

import java.util.concurrent.ConcurrentHashMap;

import net.bluemind.core.backup.continuous.api.InstallationWriteLeader;

public class DefaultLeader {

	private DefaultLeader() {

	}

	private static final ConcurrentHashMap<String, InstallationWriteLeader> cached = new ConcurrentHashMap<>();

	public static InstallationWriteLeader leader() {
		return cached.computeIfAbsent("ZK", k -> new ZkWriteLeader());
	}

	public static void reset() {
		cached.clear();
	}

}
