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

import java.sql.SQLException;
import java.util.Collections;
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

import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.Database;
import net.bluemind.system.api.UpgradeReport;
import net.bluemind.system.persistence.Upgrader;
import net.bluemind.system.persistence.Upgrader.UpgradePhase;
import net.bluemind.system.persistence.UpgraderStore;
import net.bluemind.system.schemaupgrader.ISchemaUpgradersProvider;
import net.bluemind.system.schemaupgrader.UpdateAction;
import net.bluemind.system.schemaupgrader.UpdateResult;
import net.bluemind.system.schemaupgrader.Updater;

public class SchemaUpgrade {
	private final Database database;
	private final DataSource pool;
	private final String server;
	private final UpgraderStore upgraderStore;

	public SchemaUpgrade(Database database, String server, DataSource pool, boolean onlySchema,
			UpgraderStore upgraderStore) {
		this.database = database;
		this.pool = pool;
		this.server = server;
		this.upgraderStore = upgraderStore;
	}

	private static final Logger logger = LoggerFactory.getLogger(SchemaUpgrade.class);

	public UpdateResult schemaUpgrade(IServerTaskMonitor monitor, UpgradeReport report, List<Updater> phase1,
			List<Updater> phase2, Set<UpdateAction> handledActions) {

		UpdateResult schemaUpgrade = upgrade(monitor.subWork(1), report, phase1, phase2, handledActions);

		if (schemaUpgrade.equals(UpdateResult.failed())) {
			monitor.end(false, "Upgrade failed !", "");
			return UpdateResult.failed();

		}

		monitor.end(true, "Schema upgrade complete.", "");
		return UpdateResult.ok();

	}

	public UpdateResult upgrade(IServerTaskMonitor subWork, UpgradeReport report, List<Updater> phase1,
			List<Updater> phase2, Set<UpdateAction> handledActions) throws ServerFault {

		List<Updater> phase1Filtered = phase1.stream().filter(this::updaterPending).collect(Collectors.toList());
		List<Updater> phase2Filtered = phase2.stream().filter(this::updaterPending).collect(Collectors.toList());

		UpdateResult phase1Result = executeUpdates(subWork, report, handledActions, UpgradePhase.SCHEMA_UPGRADE,
				phase1Filtered);
		if (phase1Result.equals(UpdateResult.failed())) {
			return UpdateResult.failed();
		}

		CompletableFuture<Void> ret = new CompletableFuture<>();
		if (ServerSideServiceProvider.mailboxDataSource == null
				|| ServerSideServiceProvider.mailboxDataSource.isEmpty()) {
			VertxPlatform.getVertx().eventBus().request("mailbox.ds.lookup", new JsonObject(),
					new DeliveryOptions().setSendTimeout(7000), (AsyncResult<Message<String>> event) -> {
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

		subWork.log("Starting upgrader phase 2");
		return executeUpdates(subWork, report, handledActions, UpgradePhase.POST_SCHEMA_UPGRADE, phase2Filtered);
	}

	private UpdateResult executeUpdates(IServerTaskMonitor subWork, UpgradeReport report,
			Set<UpdateAction> handledActions, UpgradePhase phase, List<Updater> updates) {
		UpdateResult ur = UpdateResult.noop();
		for (Updater u : updates) {
			logger.info("Starting {}", u);
			subWork.log("Starting " + u);
			try {
				ur = u.executeUpdate(subWork, pool, new HashSet<>(handledActions));
				handledActions.addAll(ur.actions);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				subWork.log(e.getMessage());
				ur = UpdateResult.failed();
			}

			saveUpgraderStatus(ur, u, phase);

			if (ur.equals(UpdateResult.failed())) {
				report.upgraders.add(UpgradeReport.UpgraderReport.create(UpgradeReport.Status.FAILED));
				subWork.end(false, "Schema upgrade failed.", "");
				return ur;
			} else {
				report.upgraders.add(UpgradeReport.UpgraderReport.create(UpgradeReport.Status.OK));
			}

			subWork.progress(1, "Updater " + u + " complete (result: " + ur.result.name() + ")");
		}
		return ur;
	}

	private void saveUpgraderStatus(UpdateResult updateResult, Updater updater, UpgradePhase phase) {
		Upgrader upgraderStatus = new Upgrader() //
				.phase(phase) //
				.database(database) //
				.server(server) //
				.upgraderId(updater.date(), updater.sequence()) //
				.success(!updateResult.equals(UpdateResult.failed()));
		upgraderStore.store(upgraderStatus);
	}

	public static List<Updater> getUpgradePath() {

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
			msg.append("* upgraders are not active. Make sure your subscription is valid.");
			msg.append("*********************************************************");
			logger.warn("{}", msg);
			throw new ServerFault("Upgraders are not available");

		}

		LinkedList<Updater> upgradePath = new LinkedList<>();
		upgradePath.addAll(upgradersProvider.allJavaUpdaters());
		upgradePath.addAll(upgradersProvider.allSqlUpdaters());

		Collections.sort(upgradePath, (updater1, updater2) -> Upgrader.toId(updater1.date(), updater1.sequence())
				.compareTo(Upgrader.toId(updater2.date(), updater2.sequence())));

		return upgradePath;
	}

	private boolean updaterPending(Updater updater) {
		try {
			return (updater.database() == Database.ALL || updater.database() == database) && !upgraderStore
					.upgraderCompleted(Upgrader.toId(updater.date(), updater.sequence()), server, database);
		} catch (SQLException e) {
			throw ServerFault.create(ErrorCode.SQL_ERROR, e);
		}
	}
}
