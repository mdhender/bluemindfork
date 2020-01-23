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
package net.bluemind.system.schemaupgrader.runner;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.google.common.collect.ImmutableList;

import net.bluemind.core.api.VersionInfo;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.UpgradeReport;
import net.bluemind.system.persistence.SchemaVersion;
import net.bluemind.system.persistence.SchemaVersion.UpgradePhase;
import net.bluemind.system.persistence.SchemaVersionStore;
import net.bluemind.system.schemaupgrader.ClassUpdater;
import net.bluemind.system.schemaupgrader.ComponentVersion;
import net.bluemind.system.schemaupgrader.ComponentVersionExtensionPoint;
import net.bluemind.system.schemaupgrader.ISchemaUpgradersProvider;
import net.bluemind.system.schemaupgrader.IVersionedUpdater;
import net.bluemind.system.schemaupgrader.SqlUpdater;
import net.bluemind.system.schemaupgrader.UpdateAction;
import net.bluemind.system.schemaupgrader.UpdateResult;
import net.bluemind.system.schemaupgrader.Updater;
import net.bluemind.system.schemaupgrader.Versions;

public class SchemaUpgrade {
	private DataSource pool;
	private SchemaVersionStore upgraderStore;
	private boolean onlySchema;

	public SchemaUpgrade(DataSource pool, boolean onlySchema) {
		super();
		this.pool = pool;
		this.onlySchema = onlySchema;
		this.upgraderStore = new SchemaVersionStore(pool);
	}

	public SchemaUpgrade(DataSource pool) {
		this(pool, false);
	}

	private static final Logger logger = LoggerFactory.getLogger(SchemaUpgrade.class);

	public UpdateResult schemaUpgrade(IServerTaskMonitor monitor, UpgradeReport report, VersionInfo from,
			VersionInfo to) {
		List<ComponentVersion> installedComponents = ComponentVersionExtensionPoint.getComponentsVersion();
		List<ComponentVersion> componentDbVersion = getComponentsVersion();

		List<ComponentVersion> toUpdate = installedComponents.stream()
				.map(ic -> componentDbVersion.stream().filter(cdb -> cdb.identifier.equals(ic.identifier)).findAny()
						.orElse(new ComponentVersion(ic.identifier,
								"bm/core".equals(ic.identifier) ? from.toString() : "0.0.1")))
				.collect(Collectors.toList());
		monitor.begin(installedComponents.size(), null);

		for (ComponentVersion comp : toUpdate) {
			VersionInfo lfrom = VersionInfo.checkAndCreate(comp.version);
			logger.info("component version {} : {}", comp.identifier, lfrom);
			UpdateResult schemaUpgrade = schemaUpgrade(monitor.subWork(1), report, lfrom, to, comp.identifier);

			if (schemaUpgrade.equals(UpdateResult.failed())) {
				monitor.end(false, "Upgrade failed !", "");
				return UpdateResult.failed();
			}
		}

		updateSchemaVersion(to.major, to.minor, Integer.parseInt(to.release));
		monitor.end(true, "Schema upgrade complete.", "");
		return UpdateResult.ok();

	}

	private List<ComponentVersion> getComponentsVersion() {
		try {
			return upgraderStore.getComponentsVersion();
		} catch (Exception e) {
			logger.info("error retrieving database version : {}", e.getMessage(), e);
			return ImmutableList.of();
		}
	}

	public UpdateResult schemaUpgrade(IServerTaskMonitor subWork, UpgradeReport report, VersionInfo from,
			VersionInfo to, String component) throws ServerFault {
		List<Updater> pathToGlory = null;
		try {
			pathToGlory = getUpgradePath(from, to, component);
		} catch (ServerFault e) {
			return UpdateResult.failed();
		}
		logger.info("Schema update path contains {} updater(s)", pathToGlory.size());
		subWork.begin(pathToGlory.size(), "Starting schema upgrades....");
		Set<UpdateAction> handledActions = EnumSet.noneOf(UpdateAction.class);

		List<Updater> phase1 = pathToGlory.stream().filter(u -> !u.afterSchemaUpgrade()).collect(Collectors.toList());

		List<Updater> phase2 = pathToGlory.stream().filter(u -> u.afterSchemaUpgrade() && !onlySchema)
				.collect(Collectors.toList());

		UpdateResult phase1Result = executeUpdates(subWork, report, handledActions, UpgradePhase.SCHEMA_UPGRADE,
				phase1);
		if (phase1Result.equals(UpdateResult.failed())) {
			return UpdateResult.failed();
		}

		CompletableFuture<Void> ret = new CompletableFuture<>();
		if (ServerSideServiceProvider.mailboxDataSource == null
				|| ServerSideServiceProvider.mailboxDataSource.isEmpty()) {
			VertxPlatform.getVertx().eventBus().sendWithTimeout("mailbox.ds.lookup", new JsonObject(), 7000l,
					(AsyncResult<Message<String>> event) -> {
						if (event.failed()) {
							ret.completeExceptionally(event.cause());
						} else {
							ret.complete(null);
						}
					});
		} else {
			ret.complete(null);
		}

		try {
			ret.get(10, TimeUnit.SECONDS);
		} catch (Exception e1) {
			logger.warn("Error while looking up mailbox datasource", e1);
			return UpdateResult.failed();
		}

		subWork.log("going phase 2");
		return executeUpdates(subWork, report, handledActions, UpgradePhase.POST_SCHEMA_UPGRADE, phase2);
	}

	private UpdateResult executeUpdates(IServerTaskMonitor subWork, UpgradeReport report,
			Set<net.bluemind.system.schemaupgrader.UpdateAction> handledActions, UpgradePhase phase,
			List<Updater> updates) {
		UpdateResult ur = UpdateResult.noop();
		for (Updater u : updates) {
			logger.info("Starting {}", u);
			subWork.log("Starting " + u);
			try {
				ur = u.update(subWork, new HashSet<>(handledActions));
				handledActions.addAll(ur.actions);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				subWork.log(e.getMessage());
				ur = UpdateResult.failed();
			}

			saveUpgraderStatus(ur, u, phase, u.getComponent());

			if (ur.equals(UpdateResult.failed())) {
				report.upgraders
						.add(UpgradeReport.UpgraderReport.create(u.major(), u.build(), UpgradeReport.Status.FAILED));
				subWork.end(false, "Schema upgrade failed.", "");
				return ur;
			} else {
				report.upgraders
						.add(UpgradeReport.UpgraderReport.create(u.major(), u.build(), UpgradeReport.Status.OK));

			}

			subWork.progress(1, "Updater " + u + " complete (result: " + ur.result.name() + ")");
		}
		return ur;
	}

	private void saveUpgraderStatus(UpdateResult updateResult, Updater updater, UpgradePhase phase, String component) {
		SchemaVersion upgraderStatus = new SchemaVersion(updater.major(), updater.build()) //
				.phase(phase) //
				.component(component) //
				.success(!updateResult.equals(UpdateResult.failed()));
		upgraderStore.add(upgraderStatus);
	}

	private void updateSchemaVersion(String major, String minor, int build) {

		SchemaVersionStore store = new SchemaVersionStore(pool);

		JdbcAbstractStore.doOrFail(() -> {
			for (ComponentVersion cp : ComponentVersionExtensionPoint.getComponentsVersion()) {
				store.updateComponentVersion(cp.identifier, major + "." + minor + "." + build);
			}
			return null;
		});
	}

	public List<Updater> getUpgradePath(VersionInfo source, VersionInfo target, String component) throws ServerFault {
		VersionInfo dbScriptStart = VersionInfo.create(source.toString());
		VersionInfo dbScriptEnd = VersionInfo.create(target.toString());

		// If end is in the dev branch, script major = version major + 1
		if (!dbScriptEnd.stable()) {
			dbScriptEnd.major = "" + ((Integer.parseInt(dbScriptEnd.major)) + 1);
		}

		// If start from a dev branch, script major = version major + 1
		if (!dbScriptStart.stable()) {
			dbScriptStart.major = "" + ((Integer.parseInt(dbScriptStart.major)) + 1);
		}

		LinkedList<Updater> upgradePath = new LinkedList<>();

		logger.info("dbScriptStart: {}, dbScriptEnd: {}", dbScriptStart, dbScriptEnd);

		ISchemaUpgradersProvider upgradersProvider = ISchemaUpgradersProvider.getSchemaUpgradersProvider();
		if (upgradersProvider == null) {
			StringBuilder msg = new StringBuilder("*********************************************************");
			msg.append("* No upgraders found. Make sure the package bm-core-upgraders is installed.");
			msg.append("*********************************************************");
			logger.warn("{}", msg);
			throw new ServerFault("Upgraders are not available");
		}

		if (!upgradersProvider.isActive()) {
			StringBuilder msg = new StringBuilder("*********************************************************");
			msg.append("* upgraders is not active. Make sure your subscription is valid.");
			msg.append("*********************************************************");
			logger.warn("{}", msg);
			throw new ServerFault("Upgraders are not available");

		}

		List<IVersionedUpdater> allJavaUpdaters = upgradersProvider.allJavaUpdaters(pool);
		List<Updater> allSqlUpdaters = upgradersProvider.allSqlUpdaters(pool);

		int major = Integer.parseInt(dbScriptStart.major);
		int start;
		int end;
		while (major <= Integer.parseInt(dbScriptEnd.major)) {
			start = 0;
			if (major == Integer.parseInt(dbScriptStart.major)) {
				start = Integer.parseInt(dbScriptStart.release);
			}
			if (major < Integer.parseInt(dbScriptEnd.major)) {
				end = Integer.MAX_VALUE;
			} else {
				end = Integer.parseInt(dbScriptEnd.release);
			}

			logger.debug("add to upgrade paths: major: {}, start: {}, end: {}", major, start, end);

			UpdaterFilter filter = new UpdaterFilter(major, start, end, upgraderStore.get(major, start), component);
			collectSqlFiles(upgradePath, filter, allSqlUpdaters);
			collectClassInstances(upgradePath, filter, allJavaUpdaters);

			major++;
		}
		// make sure, updaters will get executed in order
		Versions.sort(upgradePath);
		return upgradePath;
	}

	// FIXME add component to classInsance
	private void collectClassInstances(LinkedList<Updater> upgradePath, UpdaterFilter filter,
			List<IVersionedUpdater> updaters) {
		Set<String> classes = new HashSet<>();
		for (IVersionedUpdater instance : updaters) {
			String upgraderClassName = instance.getClass().getName();
			if (!classes.contains(upgraderClassName)) {
				if (filter.accept(instance.major(), instance.buildNumber(), "bm/core",
						instance.afterSchemaUpgrade() ? UpgradePhase.POST_SCHEMA_UPGRADE
								: UpgradePhase.SCHEMA_UPGRADE)) {
					ClassUpdater u = new ClassUpdater(pool, instance, "bm/core");
					upgradePath.add(u);
					logger.info("Accepted {}", u);
				} else {
					logger.debug("Not Accepted {}", new ClassUpdater(pool, instance, "bm/core"));
				}
				classes.add(upgraderClassName);
			} else {
				logger.info("Skipping duplicate java file upgrader {}", upgraderClassName);
			}
		}
	}

	private void collectSqlFiles(LinkedList<Updater> upgradePath, UpdaterFilter filter, List<Updater> updaters) {
		Set<String> files = new HashSet<>();
		for (Updater u : updaters) {
			String file = ((SqlUpdater) u).file.toString();

			if (!files.contains(file)) {
				if (filter.accept(u.major(), u.build(), u.getComponent(),
						u.afterSchemaUpgrade() ? UpgradePhase.POST_SCHEMA_UPGRADE : UpgradePhase.SCHEMA_UPGRADE)) {
					upgradePath.add(u);
					files.add(file);
					logger.info("Accepted {}", u);
				} else {
					logger.debug("Not Accepted {}", u);
				}
			} else {
				logger.info("Skipping duplicate sql file upgrader {}", file);
			}
		}
	}

}
