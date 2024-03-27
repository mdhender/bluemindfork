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
package net.bluemind.dataprotect.elastic;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.snapshot.CreateSnapshotResponse;
import co.elastic.clients.elasticsearch.snapshot.GetRepositoryResponse;
import co.elastic.clients.elasticsearch.snapshot.GetSnapshotResponse;
import co.elastic.clients.elasticsearch.snapshot.SnapshotInfo;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dataprotect.api.IBackupWorker;
import net.bluemind.dataprotect.api.IDPContext;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;

public class ElasticWorker implements IBackupWorker {
	private static final Logger logger = LoggerFactory.getLogger(ElasticWorker.class);

	private static final String REPO = "/var/spool/bm-elasticsearch/repo";
	private static final String REPOSITORY = "bm-elasticsearch";
	private static final String SNAPSHOT = "snapshot-es";

	private enum SnapshotState {
		FAILED, IN_PROGRESS, INCOMPATIBLE, PARTIAL, SUCCESS;

		public boolean match(String state) {
			return this.name().equals(state);
		}
	}

	@Override
	public boolean supportsTag(String tag) {
		return TagDescriptor.bm_es.getTag().equals(tag);
	}

	@Override
	public Set<String> getDataDirs() {
		return Sets.newHashSet(REPO);
	}

	@Override
	public void prepareDataDirs(IDPContext ctx, String tag, ItemValue<Server> toBackup) throws ServerFault {
		ElasticsearchClient esClient = ESearchActivator.getClient();

		try {
			registerRepositoryIfNecessary(esClient);
			deleteExistingSnapshots(esClient);
			CreateSnapshotResponse created = createSnapshot(esClient);
			if (Boolean.FALSE.equals(created.accepted())) {
				throw new ServerFault("Unable to snapshot elasticsearch");
			}
		} catch (ElasticsearchException | IOException e) {
			throw new ServerFault("Unable to snapshot elasticsearch", e);
		}

		try {
			long ts = System.currentTimeMillis();
			SnapshotInfo snapshotInfo = waitForSnapshot(esClient);
			logger.info("ES snapshot done in {}s, state: {}", (System.currentTimeMillis() - ts) / 1000,
					snapshotInfo.state());
			if (SnapshotState.FAILED.match(snapshotInfo.state())
					|| SnapshotState.INCOMPATIBLE.match(snapshotInfo.state())) {
				throw new ServerFault("Cannot create ES Snapshot: " + snapshotInfo.state());
			}
		} catch (ElasticsearchException | IOException e) {
			throw new ServerFault("Failed to wait until elasticsearch snapshot is done", e);
		}
	}

	@Override
	public void dataDirsSaved(IDPContext ctx, String tag, ItemValue<Server> backedUp) throws ServerFault {
		logger.info("Cleanup ES snapshot after backup of {}", backedUp.value.address());
		ElasticsearchClient esClient = ESearchActivator.getClient();
		try {
			deleteExistingSnapshots(esClient);
		} catch (ElasticsearchException | IOException e) {
			logger.error("Failed to cleanup ES snapshot after backup", e);
		}
	}

	private void registerRepositoryIfNecessary(ElasticsearchClient esClient)
			throws ElasticsearchException, IOException {
		GetRepositoryResponse response = esClient.snapshot().getRepository();
		boolean exists = response.result().keySet().stream().anyMatch(REPOSITORY::equals);
		if (!exists) {
			esClient.snapshot().createRepository(r -> r.name(REPOSITORY).type("fs").settings(s -> s.location(REPO)));
		}
	}

	private void deleteExistingSnapshots(ElasticsearchClient esClient) throws ElasticsearchException, IOException {
		GetSnapshotResponse response = esClient.snapshot().get(s -> s.repository(REPOSITORY).snapshot("*"));
		if (response.snapshots().stream().anyMatch(s -> SNAPSHOT.equals(s.snapshot()))) {
			esClient.snapshot().delete(s -> s.repository(REPOSITORY).snapshot(SNAPSHOT));
		}
	}

	private CreateSnapshotResponse createSnapshot(ElasticsearchClient esClient)
			throws ElasticsearchException, IOException {
		return esClient.snapshot().create(s -> s.repository(REPOSITORY).snapshot(SNAPSHOT));
	}

	public SnapshotInfo waitForSnapshot(ElasticsearchClient esClient) throws ElasticsearchException, IOException {
		SnapshotInfo snapshotInfo = esClient.snapshot().get(s -> s.repository(REPOSITORY).snapshot(SNAPSHOT))
				.snapshots().get(0);
		while (SnapshotState.IN_PROGRESS.match(snapshotInfo.state())) {
			logger.info("Waiting for ES snapshot. Current state: {}", snapshotInfo.state());
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
				Thread.currentThread().interrupt();
			}
			snapshotInfo = esClient.snapshot().get(s -> s.repository(REPOSITORY).snapshot(SNAPSHOT)).snapshots().get(0);
		}
		return snapshotInfo;
	}

	@Override
	public String getDataType() {
		return "es";
	}
}
