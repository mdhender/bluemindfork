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
package net.bluemind.cloud.monitoring.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import net.bluemind.cloud.monitoring.server.api.model.NodeInfo;
import net.bluemind.cloud.monitoring.server.api.model.NodeType;
import net.bluemind.cloud.monitoring.server.grafana.Mermaid;
import net.bluemind.system.application.registration.model.ApplicationInfoModel;
import net.bluemind.system.application.registration.model.ApplicationMetric;
import net.bluemind.system.application.registration.model.ApplicationMetric.AppTag;

public class MermaidTest {

	private static final String DEFAULT_FOREST = "default-forest";

	@Test
	public void test_1CRP() {

		Set<NodeInfo> nodes = new HashSet<>();
		nodes.add(crp(DEFAULT_FOREST, "1.2.3.4"));

		Mermaid mermaid = new Mermaid(nodes);
		mermaid.evaluate();

		String topology = mermaid.getTopology();
		assertGraph(topology);
		assertIsolatedNode(topology, "CRP1(CRP1<br>1.2.3.4)");
		assertTrue(mermaid.getMetricsAsMap().isEmpty());
		assertForest(topology, DEFAULT_FOREST);
	}

	@Test
	public void test_1CRP_1MASTER() {

		Set<NodeInfo> nodes = new HashSet<>();
		nodes.add(crp(DEFAULT_FOREST, "1.2.3.4"));
		nodes.add(master(DEFAULT_FOREST, "2.3.4.5", "400"));

		Mermaid mermaid = new Mermaid(nodes);
		mermaid.evaluate();

		String topology = mermaid.getTopology();
		assertGraph(topology);
		assertConnection(topology, "CRP1(CRP1<br>1.2.3.4)", "MASTER1(MASTER1<br>2.3.4.5)");
		assertConnection(topology, "MASTER1(MASTER1<br>2.3.4.5)", "--master1", "KAFKA1");

		Map<String, Long> metricsAsMap = mermaid.getMetricsAsMap();
		assertEquals(1, metricsAsMap.size());
		assertEquals(400l, metricsAsMap.get("master1").longValue());
		assertForest(topology, DEFAULT_FOREST);
	}

	@Test
	public void test_1CRP_1MASTER_1TAIL() {

		Set<NodeInfo> nodes = new HashSet<>();
		nodes.add(crp(DEFAULT_FOREST, "1.2.3.4"));
		nodes.add(master(DEFAULT_FOREST, "2.3.4.5", "400"));
		nodes.add(tail(DEFAULT_FOREST, "3.4.5.6", "600"));

		Mermaid mermaid = new Mermaid(nodes);
		mermaid.evaluate();

		String topology = mermaid.getTopology();
		assertGraph(topology);
		assertConnection(topology, "CRP1(CRP1<br>1.2.3.4)", "MASTER1(MASTER1<br>2.3.4.5)");
		assertConnection(topology, "MASTER1(MASTER1<br>2.3.4.5)", "--master1", "KAFKA1");
		assertConnection(topology, "KAFKA1", "--tail1", "TAIL1(TAIL1<br>3.4.5.6)");

		Map<String, Long> metricsAsMap = mermaid.getMetricsAsMap();
		assertEquals(2, metricsAsMap.size());
		assertEquals(400l, metricsAsMap.get("master1").longValue());
		assertTrue(metricsAsMap.values().contains(600l));
		assertTrue(metricsAsMap.keySet().contains("tail1"));
		assertForest(topology, DEFAULT_FOREST);
	}

	@Test
	public void test_1CRP_1MASTER_2TAIL() {
		Set<NodeInfo> nodes = new HashSet<>();
		nodes.add(crp(DEFAULT_FOREST, "1.2.3.4"));
		nodes.add(master(DEFAULT_FOREST, "2.3.4.5", "400"));
		nodes.add(tail(DEFAULT_FOREST, "x.x.x.x", "600"));
		nodes.add(tail(DEFAULT_FOREST, "x.x.x.x", "800"));

		Mermaid mermaid = new Mermaid(nodes);
		mermaid.evaluate();

		String topology = mermaid.getTopology();
		assertGraph(topology);
		assertConnection(topology, "CRP1(CRP1<br>1.2.3.4)", "MASTER1(MASTER1<br>2.3.4.5)");
		assertConnection(topology, "MASTER1(MASTER1<br>2.3.4.5)", "--master1", "KAFKA1");
		assertConnection(topology, "KAFKA1", "--tail1", "TAIL1(TAIL1<br>x.x.x.x)");
		assertConnection(topology, "KAFKA1", "--tail2", "TAIL2(TAIL2<br>x.x.x.x)");

		Map<String, Long> metricsAsMap = mermaid.getMetricsAsMap();
		assertEquals(3, metricsAsMap.size());
		assertEquals(400l, metricsAsMap.get("master1").longValue());
		assertTrue(metricsAsMap.values().contains(600l));
		assertTrue(metricsAsMap.values().contains(800l));
		assertTrue(metricsAsMap.keySet().contains("tail1"));
		assertTrue(metricsAsMap.keySet().contains("tail2"));
		assertForest(topology, DEFAULT_FOREST);
	}

	@Test
	public void test_2CRP_1MASTER_2TAIL() {
		Set<NodeInfo> nodes = new LinkedHashSet<>();
		nodes.add(crp(DEFAULT_FOREST, "1.2.3.4"));
		nodes.add(crp(DEFAULT_FOREST, "2.3.4.5"));
		nodes.add(master(DEFAULT_FOREST, "2.3.4.5", "400"));
		nodes.add(tail(DEFAULT_FOREST, "x.x.x.x", "600"));
		nodes.add(tail(DEFAULT_FOREST, "x.x.x.x", "800"));

		Mermaid mermaid = new Mermaid(nodes);
		mermaid.evaluate();

		String topology = mermaid.getTopology();
		assertGraph(topology);
		assertConnection(topology, "CRP1(CRP1<br>1.2.3.4)", "MASTER1(MASTER1<br>2.3.4.5)");
		assertConnection(topology, "CRP2(CRP2<br>2.3.4.5)", "MASTER1(MASTER1<br>2.3.4.5)");
		assertConnection(topology, "MASTER1(MASTER1<br>2.3.4.5)", "--master1", "KAFKA1");
		assertConnection(topology, "KAFKA1", "--tail1", "TAIL1(TAIL1<br>x.x.x.x)");
		assertConnection(topology, "KAFKA1", "--tail2", "TAIL2(TAIL2<br>x.x.x.x)");

		Map<String, Long> metricsAsMap = mermaid.getMetricsAsMap();
		assertEquals(3, metricsAsMap.size());
		assertEquals(400l, metricsAsMap.get("master1").longValue());
		assertTrue(metricsAsMap.values().contains(600l));
		assertTrue(metricsAsMap.values().contains(800l));
		assertTrue(metricsAsMap.keySet().contains("tail1"));
		assertTrue(metricsAsMap.keySet().contains("tail2"));
		assertForest(topology, DEFAULT_FOREST);
	}

	@Test
	public void test_2FORESTS_1CRP_1MASTER_1TAIL() {
		Set<NodeInfo> nodes = new LinkedHashSet<>();
		nodes.add(crp(DEFAULT_FOREST, "1.2.3.4"));
		nodes.add(master(DEFAULT_FOREST, "2.3.4.5", "400"));
		nodes.add(tail(DEFAULT_FOREST, "3.4.5.6", "600"));

		nodes.add(crp("forest2", "4.3.2.1"));
		nodes.add(master("forest2", "5.4.3.2", "400"));
		nodes.add(tail("forest2", "6.5.4.3", "600"));

		Mermaid mermaid = new Mermaid(nodes);
		mermaid.evaluate();

		String topology = mermaid.getTopology();

		assertGraph(topology);
		assertConnection(topology, "CRP1(CRP1<br>1.2.3.4)", "MASTER1(MASTER1<br>2.3.4.5)");
		assertConnection(topology, "MASTER1(MASTER1<br>2.3.4.5)", "--master1", "KAFKA1");
		assertConnection(topology, "KAFKA1", "--tail1", "TAIL1(TAIL1<br>3.4.5.6)");

		assertConnection(topology, "CRP2(CRP2<br>4.3.2.1)", "MASTER2(MASTER2<br>5.4.3.2)");
		assertConnection(topology, "MASTER2(MASTER2<br>5.4.3.2)", "--master2", "KAFKA2");
		assertConnection(topology, "KAFKA2", "--tail2", "TAIL2(TAIL2<br>6.5.4.3)");

		Map<String, Long> metricsAsMap = mermaid.getMetricsAsMap();
		assertEquals(4, metricsAsMap.size());
		assertEquals(400l, metricsAsMap.get("master1").longValue());
		assertEquals(400l, metricsAsMap.get("master2").longValue());
		assertTrue(metricsAsMap.values().contains(600l));
		assertTrue(metricsAsMap.keySet().contains("tail1"));
		assertTrue(metricsAsMap.keySet().contains("tail2"));
		assertForest(topology, DEFAULT_FOREST);
		assertForest(topology, "forest");
	}

	@Test
	public void test_2FORESTS_2CRP_1MASTER_2TAIL() {
		Set<NodeInfo> nodes = new LinkedHashSet<>();
		nodes.add(crp(DEFAULT_FOREST, "1.2.3.4"));
		nodes.add(crp(DEFAULT_FOREST, "1.1.3.4"));
		nodes.add(master(DEFAULT_FOREST, "2.3.4.5", "400"));
		nodes.add(tail(DEFAULT_FOREST, "3.4.5.6", "600"));
		nodes.add(tail(DEFAULT_FOREST, "3.3.5.6", "600"));

		nodes.add(crp("forest2", "4.3.2.1"));
		nodes.add(crp("forest2", "4.4.2.1"));
		nodes.add(master("forest2", "5.4.3.2", "400"));
		nodes.add(tail("forest2", "6.5.4.3", "600"));
		nodes.add(tail("forest2", "6.6.4.3", "600"));

		Mermaid mermaid = new Mermaid(nodes);
		mermaid.evaluate();

		String topology = mermaid.getTopology();

		assertGraph(topology);
		assertConnection(topology, "CRP1(CRP1<br>1.2.3.4)", "MASTER1(MASTER1<br>2.3.4.5)");
		assertConnection(topology, "CRP2(CRP2<br>1.1.3.4)", "MASTER1(MASTER1<br>2.3.4.5)");
		assertConnection(topology, "MASTER1(MASTER1<br>2.3.4.5)", "--master1", "KAFKA1");
		assertConnection(topology, "KAFKA1", "--tail1", "TAIL1(TAIL1<br>3.4.5.6)");
		assertConnection(topology, "KAFKA1", "--tail2", "TAIL2(TAIL2<br>3.3.5.6)");

		assertConnection(topology, "CRP3(CRP3<br>4.3.2.1)", "MASTER2(MASTER2<br>5.4.3.2)");
		assertConnection(topology, "CRP4(CRP4<br>4.4.2.1)", "MASTER2(MASTER2<br>5.4.3.2)");
		assertConnection(topology, "MASTER2(MASTER2<br>5.4.3.2)", "--master2", "KAFKA2");
		assertConnection(topology, "KAFKA2", "--tail3", "TAIL3(TAIL3<br>6.5.4.3)");
		assertConnection(topology, "KAFKA2", "--tail4", "TAIL4(TAIL4<br>6.6.4.3)");

		Map<String, Long> metricsAsMap = mermaid.getMetricsAsMap();
		assertEquals(6, metricsAsMap.size());
		assertEquals(400l, metricsAsMap.get("master1").longValue());
		assertEquals(400l, metricsAsMap.get("master2").longValue());
		assertTrue(metricsAsMap.values().contains(600l));
		assertTrue(metricsAsMap.keySet().contains("tail1"));
		assertTrue(metricsAsMap.keySet().contains("tail2"));
	}

	@Test
	public void test_1CRP_1MASTER_1TAIL_1FORK() {

		Set<NodeInfo> nodes = new LinkedHashSet<>();
		nodes.add(crp(DEFAULT_FOREST, "1.2.3.4"));
		nodes.add(master(DEFAULT_FOREST, "2.3.4.5", "400"));
		nodes.add(tail(DEFAULT_FOREST, "3.4.5.6", "600"));

		nodes.add(crp("forest2", "3.3.3.4"));
		nodes.add(fork("forest2", "7.4.5.6", "600"));

		Mermaid mermaid = new Mermaid(nodes);
		mermaid.evaluate();

		String topology = mermaid.getTopology();

		assertGraph(topology);
		assertConnection(topology, "CRP1(CRP1<br>1.2.3.4)", "MASTER1(MASTER1<br>2.3.4.5)");
		assertConnection(topology, "MASTER1(MASTER1<br>2.3.4.5)", "--master1", "KAFKA1");
		assertConnection(topology, "KAFKA1", "--tail1", "TAIL1(TAIL1<br>3.4.5.6)");
		assertConnection(topology, "CRP2(CRP2<br>3.3.3.4)", "FORK1(FORK1<br>7.4.5.6)");
		assertConnection(topology, "FORK1(FORK1<br>7.4.5.6)", "--fork1", "KAFKA2");

		Map<String, Long> metricsAsMap = mermaid.getMetricsAsMap();
		assertEquals(3, metricsAsMap.size());
		assertEquals(400l, metricsAsMap.get("master1").longValue());
		assertTrue(metricsAsMap.values().contains(600l));
		assertTrue(metricsAsMap.keySet().contains("tail1"));
		assertForest(topology, DEFAULT_FOREST);
		assertForest(topology, "forest");
	}

	@Test
	public void test_1FORK_1TAIL() {

		Set<NodeInfo> nodes = new LinkedHashSet<>();
		nodes.add(fork(DEFAULT_FOREST, "2.3.4.5", "400"));
		nodes.add(tail(DEFAULT_FOREST, "3.4.5.6", "600"));

		Mermaid mermaid = new Mermaid(nodes);
		mermaid.evaluate();

		String topology = mermaid.getTopology();

		assertGraph(topology);
		assertConnection(topology, "FORK1(FORK1<br>2.3.4.5)", "--fork1", "KAFKA1");
		assertConnection(topology, "KAFKA1", "--tail1", "TAIL1(TAIL1<br>3.4.5.6)");

		Map<String, Long> metricsAsMap = mermaid.getMetricsAsMap();
		assertEquals(2, metricsAsMap.size());
		assertEquals(400l, metricsAsMap.get("fork1").longValue());
		assertTrue(metricsAsMap.values().contains(600l));
		assertTrue(metricsAsMap.keySet().contains("tail1"));
		assertForest(topology, DEFAULT_FOREST);
	}

	@Test
	public void test_1CRP_1MASTER_1TAIL_1FORK_1TAIL() {

		Set<NodeInfo> nodes = new LinkedHashSet<>();
		nodes.add(crp(DEFAULT_FOREST, "1.2.3.4"));
		nodes.add(master(DEFAULT_FOREST, "2.3.4.5", "400"));
		nodes.add(tail(DEFAULT_FOREST, "3.4.5.6", "600"));

		nodes.add(fork("forest2", "9.3.4.5", "400"));
		nodes.add(tail("forest2", "9.4.5.6", "600"));

		Mermaid mermaid = new Mermaid(nodes);
		mermaid.evaluate();

		String topology = mermaid.getTopology();

		assertGraph(topology);
		assertConnection(topology, "CRP1(CRP1<br>1.2.3.4)", "MASTER1(MASTER1<br>2.3.4.5)");
		assertConnection(topology, "MASTER1(MASTER1<br>2.3.4.5)", "--master1", "KAFKA1");
		assertConnection(topology, "KAFKA1", "--tail1", "TAIL1(TAIL1<br>3.4.5.6)");

		assertConnection(topology, "FORK1(FORK1<br>9.3.4.5)", "--fork1", "KAFKA2");
		assertConnection(topology, "KAFKA2", "--tail2", "TAIL2(TAIL2<br>9.4.5.6)");

		Map<String, Long> metricsAsMap = mermaid.getMetricsAsMap();
		assertEquals(4, metricsAsMap.size());
		assertEquals(400l, metricsAsMap.get("master1").longValue());
		assertTrue(metricsAsMap.values().contains(600l));
		assertTrue(metricsAsMap.keySet().contains("tail1"));
		assertForest(topology, DEFAULT_FOREST);
		assertForest(topology, "forest");
	}

	private NodeInfo crp(String forest, String ip) {
		ApplicationInfoModel info = new ApplicationInfoModel("bm-crp", ip, UUID.randomUUID().toString(), null);
		NodeInfo nodeInfo = new NodeInfo(info);
		nodeInfo.forestId = forest;
		nodeInfo.info.metrics.add(ApplicationMetric.create("record-send-rate", 0l, AppTag.MASTER));
		nodeInfo.info.metrics.add(ApplicationMetric.create("records-lag-max", 0l, AppTag.TAIL));
		nodeInfo.info.state.state = "running";
		nodeInfo.type = NodeType.CRP;
		return nodeInfo;
	}

	private NodeInfo master(String forest, String ip, String sendRate) {
		ApplicationInfoModel info = new ApplicationInfoModel("bm-core", ip, UUID.randomUUID().toString(), null);
		NodeInfo nodeInfo = new NodeInfo(info);
		nodeInfo.forestId = forest;
		nodeInfo.info.metrics.add(ApplicationMetric.create("record-send-rate", Long.valueOf(sendRate), AppTag.MASTER));
		nodeInfo.info.metrics.add(ApplicationMetric.create("records-lag-max", 0l, AppTag.TAIL));
		nodeInfo.info.state.state = "CORE_STATE_RUNNING";
		nodeInfo.type = NodeType.MASTER;
		return nodeInfo;
	}

	private NodeInfo tail(String forest, String ip, String lag) {
		ApplicationInfoModel info = new ApplicationInfoModel("bm-core", ip, UUID.randomUUID().toString(), null);
		NodeInfo nodeInfo = new NodeInfo(info);
		nodeInfo.forestId = forest;
		nodeInfo.info.metrics.add(ApplicationMetric.create("record-send-rate", 0l, AppTag.MASTER));
		nodeInfo.info.metrics.add(ApplicationMetric.create("records-lag-max", Long.valueOf(lag), AppTag.TAIL));
		nodeInfo.info.state.state = "CORE_STATE_CLONING";
		nodeInfo.type = NodeType.TAIL;
		return nodeInfo;
	}

	private NodeInfo fork(String forest, String ip, String sendRate) {
		ApplicationInfoModel info = new ApplicationInfoModel("bm-core", ip, UUID.randomUUID().toString(), null);
		NodeInfo nodeInfo = new NodeInfo(info);
		nodeInfo.forestId = forest;
		nodeInfo.info.metrics.add(ApplicationMetric.create("record-send-rate", Long.valueOf(sendRate), AppTag.MASTER));
		nodeInfo.info.metrics.add(ApplicationMetric.create("records-lag-max", 0l, AppTag.TAIL));
		nodeInfo.info.state.state = "CORE_STATE_RUNNING";
		nodeInfo.type = NodeType.FORK;
		return nodeInfo;
	}

	private void assertIsolatedNode(String mermaid, String node) {
		assertTrue(mermaid.contains(node + "\r\n"));
	}

	private void assertGraph(String mermaid) {
		assertTrue(mermaid.startsWith("graph\r"));
	}

	private void assertConnection(String mermaid, String from, String to) {
		String line = String.format("%s --> %s", from, to);
		assertConnectionLine(mermaid, line);
	}

	private void assertConnection(String mermaid, String from, String link, String to) {
		String line = String.format("%s %s --> %s", from, link, to);
		assertConnectionLine(mermaid, line);
	}

	private void assertConnectionLine(String mermaid, String line) throws AssertionError {
		if (!mermaid.contains(line)) {
			System.err.println("Mermaid: " + mermaid + "\r\nLine: " + line);
			throw new AssertionError();
		}
	}

	private void assertForest(String mermaid, String forest) {
		assertTrue(mermaid.contains("    subgraph " + forest));
		assertTrue(mermaid.contains("    end"));
	}

}
