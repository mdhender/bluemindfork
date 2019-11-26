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

package net.bluemind.dataprotect.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.VersionInfo;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.service.internal.DPContext;
import net.bluemind.dataprotect.service.internal.PgContext;
import net.bluemind.dataprotect.service.internal.Workers;
import net.bluemind.pool.BMPoolActivator;
import net.bluemind.pool.Pool;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.UpgradeReport;
import net.bluemind.system.api.UpgradeReport.Status;
import net.bluemind.system.schemaupgrader.runner.SchemaUpgrade;

/**
 * Allows accessing backed-up data through the {@link IServiceProvider}
 * interface.
 *
 */
public class BackupDataProvider implements AutoCloseable {

	private String targetDatabase;
	private SecurityContext sc;
	private static Logger logger = LoggerFactory.getLogger(BackupDataProvider.class);
	private List<PgContext> pgContext;
	private IServerTaskMonitor monitor;

	/**
	 * @param target the name of the database the data will be restored into
	 */
	public BackupDataProvider(String target, SecurityContext sc, IServerTaskMonitor monitor) {
		this.targetDatabase = target != null ? target : "dp" + UUID.randomUUID().toString().replace("-", "");
		this.sc = sc;
		this.monitor = monitor;
		pgContext = new ArrayList<PgContext>();
	}

	public BackupDataProvider(String target, IServerTaskMonitor monitor) {
		this(target, SecurityContext.SYSTEM, monitor);
	}

	public BmContext create(PartGeneration pgPart, VersionInfo dpVersion) throws Exception {
		List<IBackupWorker> workers = Workers.get();
		IBackupWorker pgWorker = null;
		for (IBackupWorker bw : workers) {
			if (bw.supportsTag("bm/pgsql")) {
				pgWorker = bw;
				break;
			}
		}
		if (pgWorker == null) {
			monitor.end(false, "PG worker is missing", null);
			throw new ServerFault("PG worker is missing");
		}
		DPContext dpc = new DPContext(monitor);
		BmConfIni ini = new BmConfIni();
		Map<String, Object> params = ImmutableMap.<String, Object>of("toDatabase", targetDatabase, "user",
				ini.get("user"), "pass", ini.get("password"));
		pgWorker.restore(dpc, pgPart, params);

		IServiceProvider liveSp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IServer srvApi = liveSp.instance(IServer.class, InstallationId.getIdentifier());
		ItemValue<Server> theServer = srvApi.getComplete(pgPart.server);

		monitor.progress(1, "Fetching data from temporary database...");
		Pool pool = BMPoolActivator.getDefault().newPool("PGSQL", ini.get("user"), ini.get("password"), targetDatabase,
				theServer.value.address(), 2, JdbcActivator.getInstance().getSchemaName());

		pgContext.add(PgContext.create(pool, pgWorker, pgPart, targetDatabase));

		upgradeSchema(dpVersion, pool.getDataSource());

		return new BackupContext(pool.getDataSource(), null, sc);
	}

	public BmContext createContextWithData(DataProtectGeneration dpg, Restorable restorable) throws Exception {
		VersionInfo dpVersion = dpg.blueMind;

		monitor.progress(1, "Fetching data from temporary database...");

		PgContext pgContext = restorePg(dpg, "bm/pgsql", targetDatabase, restorable);
		PgContext pgDataContext = restorePg(dpg, "bm/pgsql-data", targetDatabase + "data", restorable);

		upgradeSchema(dpVersion, pgContext.pool.getDataSource());
		upgradeSchema(dpVersion, pgDataContext.pool.getDataSource());

		return new BackupContext(pgContext.pool.getDataSource(), pgDataContext.pool.getDataSource(), sc);
	}

	private void upgradeSchema(VersionInfo dpVersion, DataSource ds) {
		VersionInfo to = VersionInfo.create(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInstallation.class).getVersion().softwareVersion);

		SchemaUpgrade schemaUpgrader = new SchemaUpgrade(ds);
		UpgradeReport report = new UpgradeReport();
		schemaUpgrader.schemaUpgrade(monitor.subWork(50), report, dpVersion, to);

		if (report.status == Status.FAILED) {
			logger.warn("Could not upgrade backup database from version {} to {}", dpVersion.toString(), to.toString());
			throw new ServerFault(String.format("Could not upgrade backup database from version %s to %s",
					dpVersion.toString(), to.toString()));
		}
	}

	private PgContext restorePg(DataProtectGeneration dpg, String tag, String dbName, Restorable restorable)
			throws Exception {
		Optional<PartGeneration> pgPart = dpg.parts.stream().filter(g -> g.tag.equals(tag)).findFirst();
		if (!pgPart.isPresent()) {
			throw ServerFault.notFound("This backup lacks a " + tag + " part.");
		}

		Optional<IBackupWorker> worker = Workers.get().stream().filter(w -> w.supportsTag(tag)).findFirst();
		if (!worker.isPresent()) {
			monitor.end(false, "PG worker is missing", null);
			throw new ServerFault("PG worker is missing");
		}

		DPContext dpc = new DPContext(monitor);
		BmConfIni ini = new BmConfIni();
		Map<String, Object> params = ImmutableMap.<String, Object>of("toDatabase", dbName, "user", ini.get("user"),
				"pass", ini.get("password"));
		worker.get().restore(dpc, pgPart.get(), params);

		String server = pgPart.get().server;

		ItemValue<Server> theServer = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).getComplete(server);

		Pool pool = BMPoolActivator.getDefault().newPool("PGSQL", ini.get("user"), ini.get("password"), dbName,
				theServer.value.address(), 2, JdbcActivator.getInstance().getSchemaName());

		return PgContext.create(pool, worker.get(), pgPart.get(), dbName);
	}

	@Override
	public void close() throws Exception {
		for (PgContext ctx : pgContext) {
			if (ctx.pool == null) {
				return;
			}
			logger.info("Destroy dataprotected database {}", ctx.databaseName);
			ctx.pool.getDataSource().close();

			DPContext dpc = new DPContext(monitor);
			Map<String, Object> params = ImmutableMap.<String, Object>of("database", ctx.databaseName);
			ctx.pgWorker.cleanup(dpc, ctx.pgPart, params);
		}
	}

}
