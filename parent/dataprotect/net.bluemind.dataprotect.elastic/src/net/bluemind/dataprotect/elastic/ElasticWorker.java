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

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.worker.DefaultWorker;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class ElasticWorker extends DefaultWorker {

	private static final Logger logger = LoggerFactory.getLogger(ElasticWorker.class);

	private static final String dir = "/var/backups/bluemind/work/elasticsearch";

	private static final String repo = "/var/spool/bm-elasticsearch/repo";

	private static final String repository = "bm-elasticsearch";

	private static final String snapshot = "snapshot-es";

	public ElasticWorker() {
	}

	@Override
	public boolean supportsTag(String tag) {
		return "bm/es".equals(tag);
	}

	@Override
	public Set<String> getDataDirs() {
		return Sets.newHashSet(dir);
	}

	@Override
	public void prepareDataDirs(IDPContext ctx, String tag, ItemValue<Server> toBackup) throws ServerFault {
		ClusterAdminClient cluster = ESearchActivator.getClient().admin().cluster();

		registerRepositoryIfNecessary(cluster);

		GetSnapshotsResponse snaps = cluster.prepareGetSnapshots(repository).get();
		if (!snaps.getSnapshots().isEmpty()
				&& snaps.getSnapshots().stream().allMatch(s -> snapshot.equals(s.snapshotId().getName()))) {
			cluster.deleteSnapshot(new DeleteSnapshotRequest().repository(repository).snapshot(snapshot)).actionGet();
		}

		CreateSnapshotResponse backup = cluster.createSnapshot(new CreateSnapshotRequest() //
				.repository(repository) //
				.snapshot(snapshot)).actionGet();

		if (backup.status() != RestStatus.ACCEPTED) {
			throw new ServerFault("Unable to snapshot elasticsearch " + backup.status().name());
		}

		snaps = cluster.prepareGetSnapshots(repository).addSnapshots(snapshot).get();
		SnapshotInfo snap = snaps.getSnapshots().get(0);

		long ts = System.currentTimeMillis();
		while (snap.state() != SnapshotState.SUCCESS) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
				Thread.currentThread().interrupt();
			}
			logger.info("Wait for es snapshot...");
			snaps = cluster.prepareGetSnapshots(repository).addSnapshots(snapshot).get();
			snap = snaps.getSnapshots().get(0);
		}

		logger.info("ES snapshot done in {}s", (System.currentTimeMillis() - ts) / 1000);

		// copy es repo to backup location
		IServer serverApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		serverApi.submitAndWait(toBackup.uid, String.format("rm -rf %s", dir));
		serverApi.submitAndWait(toBackup.uid, String.format("mkdir -p %s", dir));
		serverApi.submitAndWait(toBackup.uid, String.format("rsync -r %s %s", repo, dir));
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
