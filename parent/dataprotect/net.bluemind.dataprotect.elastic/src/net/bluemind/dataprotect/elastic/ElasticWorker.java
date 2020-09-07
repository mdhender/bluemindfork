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

import java.util.Set;

import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesRequest;
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesResponse;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.elasticsearch.snapshots.SnapshotState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.worker.DefaultWorker;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.server.api.Server;

public class ElasticWorker extends DefaultWorker {

	private static final Logger logger = LoggerFactory.getLogger(ElasticWorker.class);

	private static final String dir = "/var/backups/bluemind/work/elasticsearch";

	private static final String repo = "/var/spool/bm-elasticsearch/repo";

	private static final String repository = "bm-elasticsearch";

	private static final String snapshot = "snapshot-es";

	@Override
	public boolean supportsTag(String tag) {
		return "bm/es".equals(tag);
	}

	@Override
	public Set<String> getDataDirs() {
		return Sets.newHashSet(repo);
	}

	@Override
	public void prepareDataDirs(IDPContext ctx, String tag, ItemValue<Server> toBackup) throws ServerFault {
		ClusterAdminClient cluster = ESearchActivator.getClient().admin().cluster();

		registerRepositoryIfNecessary(cluster);

		deleteExistingSnapshots(cluster);

		CreateSnapshotResponse backup = cluster.createSnapshot(new CreateSnapshotRequest() //
				.repository(repository) //
				.snapshot(snapshot)).actionGet();

		if (backup.status() != RestStatus.ACCEPTED) {
			throw new ServerFault("Unable to snapshot elasticsearch " + backup.status().name());
		}

		GetSnapshotsResponse snaps = cluster.prepareGetSnapshots(repository).addSnapshots(snapshot).get();
		SnapshotInfo snap = snaps.getSnapshots().get(0);

		long ts = System.currentTimeMillis();
		while (!snap.state().completed()) {
			logger.info("Waiting for ES snapshot. Current state: {}", snap.state());
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
				Thread.currentThread().interrupt();
			}
			snaps = cluster.prepareGetSnapshots(repository).addSnapshots(snapshot).get();
			snap = snaps.getSnapshots().get(0);
		}

		logger.info("ES snapshot done in {}s, state: {}", (System.currentTimeMillis() - ts) / 1000,
				snap.state().name());

		if (snap.state() == SnapshotState.FAILED || snap.state() == SnapshotState.INCOMPATIBLE) {
			throw new ServerFault("Cannot create ES Snapshot: " + snap.state().name());
		}

	}

	@Override
	public void dataDirsSaved(IDPContext ctx, String tag, ItemValue<Server> backedUp) throws ServerFault {
		logger.info("Cleanup ES snapshot after backup of {}", backedUp.value.address());
		ClusterAdminClient cluster = ESearchActivator.getClient().admin().cluster();
		deleteExistingSnapshots(cluster);
	}

	private void deleteExistingSnapshots(ClusterAdminClient cluster) {
		GetSnapshotsResponse snaps = cluster.prepareGetSnapshots(repository).get();
		if (!snaps.getSnapshots().isEmpty()
				&& snaps.getSnapshots().stream().allMatch(s -> snapshot.equals(s.snapshotId().getName()))) {
			cluster.deleteSnapshot(new DeleteSnapshotRequest().repository(repository).snapshot(snapshot)).actionGet();
		}
	}

	private void registerRepositoryIfNecessary(ClusterAdminClient cluster) {
		GetRepositoriesResponse repos = cluster.getRepositories(new GetRepositoriesRequest()).actionGet();
		for (RepositoryMetaData r : repos.repositories()) {
			if (r.name().equals(repository)) {
				return;
			}
		}
		logger.info("Creating Elasticsearch repository {}", repository);
		Settings settings = Settings.builder().put("location", repo).build();
		cluster.putRepository(new PutRepositoryRequest().name(repository).type("fs").settings(settings)).actionGet();
	}

	@Override
	public String getDataType() {
		return "es";
	}
}
