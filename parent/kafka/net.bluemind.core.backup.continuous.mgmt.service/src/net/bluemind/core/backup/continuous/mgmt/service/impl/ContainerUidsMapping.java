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
package net.bluemind.core.backup.continuous.mgmt.service.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.bluemind.core.task.service.IServerTaskMonitor;

public class ContainerUidsMapping {

	private ContainerUidsMapping() {
	}

	private static final Map<String, String> dbUidToKafkaUid = new ConcurrentHashMap<>();

	public static void map(IServerTaskMonitor mon, String db, String kafka) {
		mon.log("Register mappping between db " + db + " and " + kafka + " in kafka.");
		dbUidToKafkaUid.put(db, kafka);
	}

	public static String alias(String dbUid) {
		return dbUidToKafkaUid.getOrDefault(dbUid, dbUid);
	}

}
