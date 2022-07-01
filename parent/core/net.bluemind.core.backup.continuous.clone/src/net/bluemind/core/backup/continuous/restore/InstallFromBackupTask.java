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
package net.bluemind.core.backup.continuous.restore;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.IBackupReader;
import net.bluemind.core.backup.continuous.ILiveBackupStreams;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy.ExpectedBehaviour;
import net.bluemind.core.backup.continuous.api.CloneDefaults;
import net.bluemind.core.backup.continuous.restore.domains.DomainRestorationHandler;
import net.bluemind.core.backup.continuous.restore.domains.RestoreState;
import net.bluemind.core.backup.continuous.restore.mbox.DefaultSdsStoreLoader;
import net.bluemind.core.backup.continuous.restore.mbox.ISdsStoreLoader;
import net.bluemind.core.backup.continuous.restore.orphans.RestoreContainerItemIdSeq;
import net.bluemind.core.backup.continuous.restore.orphans.RestoreDomains;
import net.bluemind.core.backup.continuous.restore.orphans.RestoreJobPlans;
import net.bluemind.core.backup.continuous.restore.orphans.RestoreSysconf;
import net.bluemind.core.backup.continuous.restore.orphans.RestoreToken;
import net.bluemind.core.backup.continuous.restore.orphans.RestoreTopology;
import net.bluemind.core.backup.continuous.restore.orphans.RestoreTopology.PromotingServer;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.domain.api.Domain;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.system.api.CloneConfiguration;
import net.bluemind.system.api.SystemConf;

public class InstallFromBackupTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(InstallFromBackupTask.class);

	private final String sourceMcastId;
	private final IBackupReader backupStore;
	private final TopologyMapping topologyMapping;
	private final IServiceProvider target;
	private final Map<String, IResumeToken> processedStreams;
	private final ISdsStoreLoader sdsAccess;

	private List<IClonePhaseObserver> observers;

	private final SysconfOverride confOver;

	private final CloneConfiguration cloneConf;

	public InstallFromBackupTask(CloneConfiguration conf, IBackupReader store, SysconfOverride over,
			TopologyMapping map, IServiceProvider target) {
		this(conf, store, over, map, new DefaultSdsStoreLoader(), target);
	}

	@VisibleForTesting
	public InstallFromBackupTask(CloneConfiguration conf, IBackupReader store, SysconfOverride over,
			TopologyMapping map, ISdsStoreLoader sdsAccess, IServiceProvider target) {
		this.sourceMcastId = conf.sourceInstallationId;
		this.cloneConf = conf;
		this.target = target;
		this.processedStreams = new HashMap<>();
		this.topologyMapping = map;
		this.backupStore = store;
		this.sdsAccess = sdsAccess;
		this.observers = new ArrayList<>();
		this.confOver = over;
	}

	public void registerObserver(IClonePhaseObserver obs) {
		observers.add(obs);
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {

		monitor.begin(100, "Topology, domains then directories...");
		System.setProperty(CloneDefaults.WORKERS_SYSPROP, "" + cloneConf.cloneWorkers);

		Path cloneStatePath = Paths.get(CloneDefaults.CLONE_STATE_PATH);

		ILiveBackupStreams streams = backupStore.forInstallation(sourceMcastId);
		ILiveStream orphansStream = streams.orphans();
		CloneState cloneState = new CloneState(cloneStatePath, orphansStream);

		ClonedOrphans orphans = cloneOrphans(monitor.subWork(1), orphansStream, cloneState);

		cloneContainerItemIdSeq(monitor, orphansStream, orphans, cloneState);

		List<ILiveStream> domainStreams = streams.domains();
		ISdsSyncStore sdsStore = sdsAccess.forSysconf(orphans.sysconf);
		cloneDomains(monitor.subWork(99), domainStreams, cloneState, orphans, sdsStore);
	}

	public static class ClonedOrphans {

		public final Map<String, PromotingServer> topology;
		public final Map<String, ItemValue<Domain>> domains;
		public final SystemConf sysconf;
		public final String token;

		public ClonedOrphans(Map<String, PromotingServer> topology, Map<String, ItemValue<Domain>> domains,
				SystemConf sysconf, String coreTok) {
			this.topology = topology;
			this.domains = domains;
			this.sysconf = sysconf;
			this.token = coreTok;
		}

	}

	private ClonedOrphans cloneOrphans(IServerTaskMonitor monitor, ILiveStream orphansStream, CloneState cloneState) {
		monitor.begin(3, "Cloning orphans (cross-domain data) of installation " + sourceMcastId);
		Map<String, List<DataElement>> orphansByType = new HashMap<>();
		IResumeToken prevState = cloneState.forTopic(orphansStream);
		monitor.log("IGNORE prevState for " + orphansStream + " -> " + prevState);
		IResumeToken orphansStreamIndex = orphansStream.subscribe(null,
				de -> orphansByType.computeIfAbsent(de.key.type, key -> new ArrayList<>()).add(de));

		String coreTok = new RestoreToken().restore(monitor,
				orphansByType.getOrDefault("installation", new ArrayList<>()));
		Map<String, PromotingServer> topology = new RestoreTopology(target, topologyMapping).restore(monitor,
				orphansByType.getOrDefault("installation", new ArrayList<>()));

		new RestoreContainerItemIdSeq(topology.values()).restore(monitor,
				orphansByType.getOrDefault("container_item_id_seq", new ArrayList<>()));

		Map<String, ItemValue<Domain>> domains = new RestoreDomains(target, topology.values()).restore(monitor,
				orphansByType.getOrDefault("domains", new ArrayList<>()));

		SystemConf sysconf = new RestoreSysconf(target, confOver).restore(monitor,
				orphansByType.getOrDefault("sysconf", new ArrayList<>()));

		new RestoreJobPlans(target).restore(monitor, orphansByType.getOrDefault("job_plans", new ArrayList<>()));

		recordProcessed(monitor, cloneState, orphansStream, orphansStreamIndex);
		orphansByType.clear();
		monitor.end(true, "Orphans cloned", null);
		return new ClonedOrphans(topology, domains, sysconf, coreTok);
	}

	private void cloneContainerItemIdSeq(IServerTaskMonitor monitor, ILiveStream orphansStream, ClonedOrphans orphans,
			CloneState cloneState) {
		RestoreContainerItemIdSeq idSeq = new RestoreContainerItemIdSeq(orphans.topology.values());
		CompletableFuture.supplyAsync(() -> {
			return orphansStream.subscribe(cloneState.forTopic(orphansStream), de -> {
				if (!de.key.type.equals("container_item_id_seq")) {
					return;
				}
				idSeq.restore(monitor, Arrays.asList(de));
			}, infos -> (cloneState.isTerminated()) ? ExpectedBehaviour.ABORT : ExpectedBehaviour.RETRY);
		}, Executors.newSingleThreadExecutor());
	}

	private void cloneDomains(IServerTaskMonitor monitor, List<ILiveStream> domainStreams, CloneState cloneState,
			ClonedOrphans orphans, ISdsSyncStore sdsStore) {
		monitor.begin(domainStreams.size(), "Cloning domains");

		int goal = domainStreams.size();
		ExecutorService clonePool = Executors.newFixedThreadPool(goal + 1);

		RecordStarvationHandler starvation = new RecordStarvationHandler(monitor, cloneConf, orphans, target,
				cloneState);

		CompletableFuture<?>[] toWait = new CompletableFuture<?>[goal];
		int slot = 0;
		for (ILiveStream domainStream : domainStreams) {
			ItemValue<Domain> domain = orphans.domains.get(domainStream.domainUid());
			IServerTaskMonitor domainMonitor = monitor.subWork(domain.value.defaultAlias, 1);
			toWait[slot++] = CompletableFuture.supplyAsync(() -> {
				domainMonitor.begin(1, "Working on domain " + domain.uid);

				IResumeToken domainPrevIndex = cloneState.forTopic(domainStream);
				IResumeToken domainStreamIndex = domainPrevIndex;

				try (RestoreState state = new RestoreState(domain.uid, orphans.topology)) {
					DomainRestorationHandler restoration = new DomainRestorationHandler(domainMonitor,
							cloneConf.skippedContainerTypes, domain, target, observers, sdsStore, starvation, state);
					IResumeToken prevState = cloneState.forTopic(domainStream);
					monitor.log("prevState for " + domainStream + " => " + prevState);
					domainStreamIndex = domainStream.subscribe(prevState, restoration::handle, starvation); // , false
				} catch (IOException e) {
					logger.error("unexpected error when closing", e);
					domainMonitor.end(false, "Fail to restore " + domain.uid + ": " + e.getMessage(), null);
				} catch (Exception e) {
					logger.error("unexpected error", e);
					domainMonitor.end(false, "Fail to restore " + domain.uid + ": " + e.getMessage(), null);
				} finally {
					recordProcessed(domainMonitor, cloneState, domainStream, domainStreamIndex);
					domainMonitor.end(true, "Domain " + domain.uid + " fully restored", null);
				}
				return null;
			}, clonePool);
		}
		CompletableFuture<Void> globalProm = CompletableFuture.allOf(toWait);
		monitor.log("Waiting for domains cloning global promise...");
		globalProm.join();
		cloneState.terminate();
	}

	private void recordProcessed(IServerTaskMonitor monitor, CloneState cloneState, ILiveStream stream,
			IResumeToken index) {
		monitor.log("Processed " + stream + " up to " + index);
		processedStreams.put(stream.domainUid(), index);
		cloneState.track(stream.fullName(), index).save();
	}
}
