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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cloud.monitoring.server.grafana;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.json.JsonObject;
import net.bluemind.cloud.monitoring.server.api.model.NodeInfo;
import net.bluemind.cloud.monitoring.server.api.model.NodeType;

public class Mermaid {

	private static final String CRLF = "\r\n";
	private static final String TAB = "      ";
	private static final String CONNECTION_TEXT = " --";
	private static final String CONNECTION = " --> ";
	private static final String KAFKA = "KAFKA";
	@SuppressWarnings("unused")
	private static final String METRIC_MASTER_BYTE_RATE = "outgoing-byte-rate";
	private static final String METRIC_MASTER_SEND_RATE = "record-send-rate";
	private static final String METRIC_TAIL_MAXLAG = "records-lag-max";

	private final Set<NodeInfo> clusterNodes;
	private Map<String, Long> metrics = new HashMap<>();
	private String topology;

	public Mermaid(Set<NodeInfo> clusterNodes) {
		this.clusterNodes = clusterNodes;
	}

	public void evaluate() {
		AtomicInteger masterIndex = new AtomicInteger(0);
		AtomicInteger tailIndex = new AtomicInteger(0);
		StringBuilder sb = new StringBuilder("graph" + CRLF);
		for (NodeInfo node : clusterNodes) {
			if (node.type == NodeType.CRP) {
				handleCrp(sb, node, masterIndex, tailIndex);
			}
		}

		this.topology = sb.toString();
	}

	private void handleCrp(StringBuilder sb, NodeInfo crp, AtomicInteger masterIndex, AtomicInteger tailIndex) {
		boolean found = false;
		for (NodeInfo node : clusterNodes) {
			if (node.product.equals("bm-core") && node.forestId.equals(crp.forestId)) {
				if (isPrimaryNode(node)) {
					found = true;
					sb.append(TAB + node(crp) + CONNECTION + node(node) + CRLF);
					sb.append(TAB + node(node) + connection(node, masterIndex, tailIndex) + KAFKA + CRLF);
				} else if (node.type == NodeType.TAIL) {
					sb.append(TAB + KAFKA + connection(node, masterIndex, tailIndex) + node(node) + CRLF);
				}
			}
		}
		if (!found) {
			sb.append(TAB + crp.type.name() + CRLF);
		}
	}

	private boolean isPrimaryNode(NodeInfo node) {
		return node.type == NodeType.MASTER || node.type == NodeType.FORK;
	}

	private String connection(NodeInfo node, AtomicInteger masterIndex, AtomicInteger tailIndex) {
		String link = null;
		if (isPrimaryNode(node)) {
			link = "master" + masterIndex.incrementAndGet();
			Long rate = node.metrics.containsKey(METRIC_MASTER_SEND_RATE)
					? Long.valueOf(node.metrics.get(METRIC_MASTER_SEND_RATE))
					: 0l;
			metrics.put(link, rate);
		} else {
			link = "tail" + tailIndex.incrementAndGet();
			Long lag = node.metrics.containsKey(METRIC_TAIL_MAXLAG) ? Long.valueOf(node.metrics.get(METRIC_TAIL_MAXLAG))
					: 0l;
			metrics.put(link, lag);
		}
		return CONNECTION_TEXT + link + CONNECTION;
	}

	private String node(NodeInfo node) {
		return node.type.name() + "(" + node.type.name() + " " + node.address + ")";
	}

	public String getTopology() {
		return topology;
	}

	public String getMetrics() {
		JsonObject json = new JsonObject();
		for (Entry<String, Long> metric : metrics.entrySet()) {
			json.put(metric.getKey(), metric.getValue());
		}
		String time = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
		json.put("time", time);
		return json.encode();
	}
}
