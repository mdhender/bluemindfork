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
package net.bluemind.system.service.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.VersionInfo;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.system.api.Database;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.InstallationVersion;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.UpdateHistory;
import net.bluemind.system.api.UpgradeReport;
import net.bluemind.system.persistence.UpgraderStore;
import net.bluemind.system.schemaupgrader.DatedUpdater;
import net.bluemind.system.schemaupgrader.SqlUpdater;
import net.bluemind.system.schemaupgrader.UpdateAction;
import net.bluemind.system.schemaupgrader.UpdateResult;
import net.bluemind.system.schemaupgrader.Updater;
import net.bluemind.system.schemaupgrader.runner.SchemaUpgrade;
import net.bluemind.system.service.UpgraderMigration;
import net.bluemind.system.state.StateContext;

public class InstallationUpgradeTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(InstallationUpgradeTask.class);
	private final DataSource pool;
	private final VersionInfo from;
	private final String to;

	public InstallationUpgradeTask(BmContext context, InstallationVersion version) {
		this.pool = context.getDataSource();
		this.from = VersionInfo.checkAndCreate(version.databaseVersion);
		this.to = version.softwareVersion + " (" + version.versionName + ")";
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(ServerSideServiceProvider.mailboxDataSource.size() + 1, "Begin upgrade");
		notifyUpgradeStatus("core.upgrade.start");

		UpgraderStore store = new UpgraderStore(pool);
		checkDatabaseStatus(store);

		Set<UpdateAction> handledActions = EnumSet.noneOf(UpdateAction.class);
		List<DatedUpdater> upgraders = adaptUpgraders(SchemaUpgrade.getUpgradePath());

		executeUpgrades(upgraders, handledActions, store, monitor);

		updateUpgradeHistory();

		monitor.end(true, "Core upgrade complete", "");
		logger.info("Core upgrade ended");
		notifyUpgradeStatus("core.upgrade.end");
	}

	private void updateUpgradeHistory() {
		ISystemConfiguration config = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		UpdateHistory history = config.getValues().convertedValue(SysConfKeys.upgrade_history.name(),
				(s -> JsonUtils.read(s, UpdateHistory.class)), new UpdateHistory());
		history.add(from.toString(), to);
		config.updateMutableValues(
				Collections.singletonMap(SysConfKeys.upgrade_history.name(), JsonUtils.asString(history)));
	}

	/**
	 * During the migration from BlueMind 3.5 to version 4, the database bj-data is
	 * created based on a copy of bj. Since no sql upgraders will be executed on
	 * bj-data, we need to ensure that bj is up-to-date, including all changes
	 * targeting bj-data. Therefore, in this special case, we redeclare all sql
	 * updgraders defined as "SHARD" as upgraders "ALL".
	 */
	private List<DatedUpdater> adaptUpgraders(List<DatedUpdater> upgraders) {
		if (from.major.equals("3")) {
			return upgraders.stream().map(upgrader -> {
				if (upgrader instanceof SqlUpdater && upgrader.database() == Database.SHARD) {
					SqlUpdater sql = (SqlUpdater) upgrader;
					return new SqlUpdater(sql.file(), sql.ignoreErrors(), sql.afterSchemaUpgrade(), Database.ALL,
							sql.date(), sql.sequence());
				}
				return upgrader;
			}).collect(Collectors.toList());
		}
		return upgraders;
	}

	private void executeUpgrades(List<DatedUpdater> upgraders, Set<UpdateAction> handledActions, UpgraderStore store,
			IServerTaskMonitor monitor) {
		logger.info("Schema update path contains {} updater(s)", upgraders.size());

		List<DatedUpdater> phase1 = upgraders.stream().filter(u -> !u.afterSchemaUpgrade())
				.collect(Collectors.toList());
		List<DatedUpdater> phase2 = upgraders.stream().filter(Updater::afterSchemaUpgrade).collect(Collectors.toList());

		for (Entry<String, DataSource> mbDS : ServerSideServiceProvider.mailboxDataSource.entrySet()) {
			doUpgradeForDataSource(Database.SHARD, mbDS.getKey(), mbDS.getValue(), monitor.subWork(mbDS.getKey(), 1),
					store, phase1, phase2, handledActions);
		}
		doUpgradeForDataSource(Database.DIRECTORY, "master", pool, monitor.subWork("master", 1), store, phase1, phase2,
				handledActions);
	}

	private void checkDatabaseStatus(UpgraderStore store) throws Exception {
		boolean needsMigration = store.needsMigration();
		if (needsMigration) {
			List<String> servers = new ArrayList<>(ServerSideServiceProvider.mailboxDataSource.entrySet().stream()
					.map(Entry::getKey).collect(Collectors.toList()));
			servers.add("master");
			UpgraderMigration.migrate(store, from, servers);
		}
	}

	private void doUpgradeForDataSource(Database database, String server, DataSource pool, IServerTaskMonitor monitor,
			UpgraderStore store, List<DatedUpdater> phase1, List<DatedUpdater> phase2,
			Set<UpdateAction> handledActions) {

		UpgradeReport report = new UpgradeReport();
		report.upgraders = new LinkedList<>();
		SchemaUpgrade schemaUpgrader = new SchemaUpgrade(database, server, pool, store);
		UpdateResult schemaUpgrade = schemaUpgrader.schemaUpgrade(monitor, report, phase1, phase2, handledActions);
		if (schemaUpgrade.equals(UpdateResult.failed())) {
			throw new ServerFault("Upgrade failed");
		}
	}

	private void notifyUpgradeStatus(String operation) {
		StateContext.setState(operation);
	}

}
