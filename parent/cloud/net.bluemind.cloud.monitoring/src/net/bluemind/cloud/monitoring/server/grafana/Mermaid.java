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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonObject;
import net.bluemind.cloud.monitoring.server.api.model.NodeInfo;
import net.bluemind.system.application.registration.model.ApplicationInfoModel;
import net.bluemind.system.application.registration.model.ApplicationMetric.AppTag;

public class Mermaid {

	private static final String CRLF = "\r\n";
	private static final String BR = "<br>";
	private static final String TAB = "      ";
	private static final String CONNECTION_TEXT = " --";
	private static final String CONNECTION = " --> ";
	private static final String KAFKA = "KAFKA";
	private static final String SUBGRAPH = "    subgraph ";
	private static final String SUBGRAPH_END = "    end";

	private final List<NodeInfo> clusterNodes;
	private Map<String, Long> metrics = new HashMap<>();
	private String topology;
	private Set<NodeInfo> handled = new HashSet<>();
	private Map<String, String> kafkaNameMapping = new HashMap<>();
	private Map<String, Integer> masterNameMapping = new HashMap<>();
	private Map<String, Integer> forkNameMapping = new HashMap<>();

	private int masterIndex = 0;
	private int forkIndex = 0;
	private int tailIndex = 0;
	private int kafkaIndex = 0;
	private boolean evaluated = false;

	public Mermaid(Collection<NodeInfo> clusterNodes) {
		this.clusterNodes = new LinkedList<>(clusterNodes);
	}

	public void evaluate() {
		checkState();
		orderNodes();
		AtomicInteger crpIndex = new AtomicInteger(0);
		StringBuilder sb = new StringBuilder("graph" + CRLF);
		handleCrpNodes(crpIndex, sb);
		handleIsolatedNodes(sb);

		this.topology = cleanAndSetTopology(sb);
	}

	private void orderNodes() {
		Collections.sort(this.clusterNodes, (a1, a2) -> a1.forestId.compareTo(a2.forestId));
	}

	private void handleCrpNodes(AtomicInteger crpIndex, StringBuilder sb) {
		handleNodes(sb, NodeInfo::isCrp, crpIndex::incrementAndGet);
	}

	private void handleIsolatedNodes(StringBuilder sb) {
		clusterNodes.removeAll(handled);
		handleNodes(sb, NodeInfo::isPrimaryNode, () -> -1);
	}

	private void handleNodes(StringBuilder sb, Predicate<NodeInfo> nodeTypeFilter, IntSupplier crpIndex) {
		String forestName = null;
		for (NodeInfo node : clusterNodes) {
			if (!node.forestId.equals(forestName)) {
				if (forestName != null) {
					sb.append(SUBGRAPH_END + CRLF);
				}
				sb.append(SUBGRAPH + node.forestId + CRLF);
				forestName = node.forestId;
			}
			String kafkaName = kafkaNameMapping.computeIfAbsent(node.forestId, forest -> KAFKA + ++kafkaIndex);
			if (nodeTypeFilter.test(node)) {
				handleNode(sb, node, kafkaName, crpIndex.getAsInt());
			}
		}
		if (forestName != null) {
			sb.append(SUBGRAPH_END + CRLF);
		}
	}

	private void checkState() {
		if (evaluated) {
			throw new IllegalStateException("Mermaid can obly be evaluated once");
		}
		evaluated = true;
	}

	private void handleNode(StringBuilder sb, NodeInfo crp, String kafkaName, int crpIndex) {
		boolean isIsolated = true;

		for (NodeInfo node : clusterNodes) {
			if (node.info.product.equals("bm-core") && node.forestId.equals(crp.forestId)) {
				if (node.isPrimaryNode()) {
					isIsolated = false;
					handlePrimaryNode(sb, crp, kafkaName, crpIndex, node);
				} else if (node.isTail() && !handled.contains(node)) {
					handleTail(sb, kafkaName, node);
				}
				handled.add(node);
			}
		}
		if (isIsolated) {
			sb.append(TAB + node(crp, crpIndex) + CRLF);
		}
	}

	private void handleTail(StringBuilder sb, String kafkaName, NodeInfo node) {
		int tIndex = ++tailIndex;
		sb.append(TAB + kafkaName + connection(node, -1, tIndex) + node(node, tIndex) + CRLF);
	}

	private void handlePrimaryNode(StringBuilder sb, NodeInfo node, String kafkaName, int crpIndex, NodeInfo pNode) {
		int mIndex;
		if (pNode.isMaster()) {
			mIndex = masterNameMapping.computeIfAbsent(pNode.forestId, forest -> ++masterIndex);
		} else {
			mIndex = forkNameMapping.computeIfAbsent(pNode.forestId, forest -> ++forkIndex);
		}
		if (crpIndex != -1) { // MASTER or FORK without CRP
			sb.append(TAB + node(node, crpIndex) + CONNECTION + node(pNode, mIndex) + CRLF);
		}
		if (!handled.contains(pNode)) {
			sb.append(TAB + node(pNode, mIndex) + connection(pNode, mIndex, -1) + kafkaName + CRLF);
		}
	}

	private String connection(NodeInfo node, int masterIndex, int tailIndex) {
		String link = null;
		if (node.isPrimaryNode()) {
			if (node.isMaster()) {
				link = "master" + masterIndex;
			} else {
				link = "fork" + masterIndex;
			}
			metrics.put(link, getMetric(node.info, "record-send-rate", AppTag.MASTER));
		} else {
			link = "tail" + tailIndex;
			metrics.put(link, getMetric(node.info, "records-lag-max", AppTag.TAIL));
		}
		return CONNECTION_TEXT + link + CONNECTION;
	}

	private long getMetric(ApplicationInfoModel info, String key, AppTag tag) {
		return info.metrics.stream().filter(m -> key.equals(m.key) && m.tag == tag).findFirst().map(m -> m.value)
				.orElse(0l);
	}

	private String node(NodeInfo node, int index) {
		boolean noInstallId = node.info.installationId == null || node.info.installationId.isEmpty()
				|| node.info.installationId.equals("null");
		String installId = noInstallId ? "" : BR + node.info.installationId.replace("bluemind-", "");
		return node.type.name() + index + "(" + node.type.name() + index + installId + BR + node.info.address + ")";
	}

	public String getTopology() {
		return topology;
	}

	public String getMetrics() {
		JsonObject json = new JsonObject(new HashMap<>(metrics));
		String time = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
		json.put("time", time);
		return json.encode();
	}

	public Map<String, Long> getMetricsAsMap() {
		return metrics;
	}

	private String cleanAndSetTopology(StringBuilder sb) {
		List<String> list = new ArrayList<>(Arrays.asList(sb.toString().split(CRLF)));
		List<Integer> del = new ArrayList<>();

		int index = 0;
		for (Iterator<String> iter = list.iterator(); iter.hasNext();) {
			String line = iter.next();
			if (line.startsWith(SUBGRAPH) && list.get(index + 1).equals(SUBGRAPH_END)) {
				del.add(index);
				del.add(index + 1);
			}
			index++;
		}
		Collections.reverse(del);
		del.forEach(i -> list.remove(i.intValue()));

		return list.stream().collect(Collectors.joining(CRLF));
	}

}
