/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.system.application.registration.metrics;

import java.util.Arrays;
import java.util.List;

public class NodeMetrics {

	private enum NodeType {
		MASTER, TAIL;
	}

	public enum Metric {
		OUTGOING_BYTE_RATE("outgoing-byte-rate", "0", NodeType.MASTER), //
		RECORD_SEND_RATE("record-send-rate", "0", NodeType.MASTER), //
		RECORDS_LAG_MAX("records-lag-max", "0", NodeType.TAIL);

		private String name;
		private String defaultValue;
		private NodeType type;

		private Metric(String name, String defaultValue, NodeType type) {
			this.name = name;
			this.defaultValue = defaultValue;
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public static List<Metric> getByType(NodeType type) {
			return Arrays.asList(Metric.values()).stream().filter(m -> m.type == type).toList();
		}
	}

	public static List<Metric> getMetrics(boolean isTailOrMaster) {
		return isTailOrMaster ? Metric.getByType(NodeType.TAIL) : Metric.getByType(NodeType.MASTER);
	}

}
