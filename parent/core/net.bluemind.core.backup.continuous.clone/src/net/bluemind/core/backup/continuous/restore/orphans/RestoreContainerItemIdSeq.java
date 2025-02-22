/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.restore.orphans;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.dto.ContainerItemIdSeq;
import net.bluemind.core.backup.continuous.restore.orphans.RestoreTopology.PromotingServer;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;

public class RestoreContainerItemIdSeq {

	private static final Long SEQ_SHIFT = 1_000_000L;
	private static final Long SEQ_HIGH_WATERMARK = 900_000L;
	private static final ValueReader<ItemValue<ContainerItemIdSeq>> reader = JsonUtils
			.reader(new TypeReference<ItemValue<ContainerItemIdSeq>>() {
			});

	private final Collection<ItemValue<Server>> serverItems;

	private final Map<String, Long> nextBump = new ConcurrentHashMap<>();

	public RestoreContainerItemIdSeq(Collection<PromotingServer> servers) {
		this.serverItems = servers.stream().map(ps -> ps.clone).collect(Collectors.toList());
	}

	public void restore(IServerTaskMonitor monitor, List<DataElement> dataElements) {
		monitor.log("Bumping container item id sequence for {} server(s)", serverItems.size());

		String payload = new String(dataElements.get(dataElements.size() - 1).payload);
		ContainerItemIdSeq itemIdSeq = reader.read(payload).value;
		bumpAllContainerItemId(monitor, itemIdSeq);
		monitor.progress(1, "item_id seq managed.");
	}

	private void bumpAllContainerItemId(IServerTaskMonitor monitor, ContainerItemIdSeq itemIdSeq) {
		serverItems.stream() //
				.filter(serverItem -> serverItem.value.tags.contains(TagDescriptor.bm_pgsql.getTag())).findFirst()
				.ifPresent(serverItem -> bumpContainerItemId(monitor, "bj", serverItem.value.ip,
						itemIdSeq.defaultDataSourceSeq));
		serverItems.stream() //
				.filter(serverItem -> serverItem.value.tags.contains(TagDescriptor.bm_pgsql_data.getTag())) //
				.forEach(serverItem -> bumpContainerItemId(monitor, "bj-data", serverItem.value.ip,
						itemIdSeq.mailboxDataSourceSeq.get(serverItem.uid)));
	}

	private void bumpContainerItemId(IServerTaskMonitor monitor, String dbName, String ip, long seq) {
		INodeClient nodeClient = NodeActivator.get(ip);
		Long next = nextBump.get(dbName + ":" + ip);
		if (next != null && seq < next) {
			monitor.log("Leaving item_id_seq alone, waiting for {} before bump", next);
			return;
		}
		nextBump.put(dbName + ":" + ip, seq + SEQ_HIGH_WATERMARK);
		long shiftedSeq = SEQ_SHIFT + seq;
		monitor.log("Bumping t_container_item_id_seq to {} (server '{}', db '{}', leader seq '{}')", shiftedSeq, ip,
				dbName, seq);

		String bumpCmd = String.format(
				"PGPASSWORD='%s' psql -h localhost -c \"select setval('t_container_item_id_seq', %d)\" %s %s",
				new BmConfIni().get("password"), shiftedSeq, dbName, "bj");
		String bumpCmdPath = "/tmp/dump-container-item-id-seq-" + System.nanoTime() + ".sh";
		nodeClient.writeFile(bumpCmdPath, new ByteArrayInputStream(bumpCmd.getBytes()));
		try {
			NCUtils.execNoOut(nodeClient, "chmod", "+x", bumpCmdPath);
			ExitList el = NCUtils.exec(nodeClient, bumpCmdPath);
			for (String log : el) {
				if (!log.isEmpty()) {
					monitor.log(log);
				}
			}
		} finally {
			NCUtils.execNoOut(nodeClient, "rm", "-f", bumpCmdPath);
		}
	}

}
