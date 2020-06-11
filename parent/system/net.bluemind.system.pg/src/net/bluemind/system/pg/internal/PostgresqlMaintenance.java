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
package net.bluemind.system.pg.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.jdbc.MaintenanceScripts;
import net.bluemind.core.jdbc.MaintenanceScripts.MaintenanceScript;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.system.api.Database;
import net.bluemind.system.pg.api.IInternalPostgresMaintenance;
import net.bluemind.system.pg.api.IPostgresqlMaintenance;

public class PostgresqlMaintenance implements IPostgresqlMaintenance, IInternalPostgresMaintenance {

	private static final Logger logger = LoggerFactory.getLogger(PostgresqlMaintenance.class);

	public static final class Factory
			implements ServerSideServiceProvider.IServerSideServiceFactory<IPostgresqlMaintenance> {

		@Override
		public Class<IPostgresqlMaintenance> factoryClass() {
			return IPostgresqlMaintenance.class;
		}

		@Override
		public IPostgresqlMaintenance instance(BmContext context, String... params) throws ServerFault {
			RBACManager.forContext(context).check(BasicRoles.ROLE_SYSTEM_MANAGER);
			return new PostgresqlMaintenance(context);
		}

	}

	public static final class InternalFactory
			implements ServerSideServiceProvider.IServerSideServiceFactory<IInternalPostgresMaintenance> {

		@Override
		public Class<IInternalPostgresMaintenance> factoryClass() {
			return IInternalPostgresMaintenance.class;
		}

		@Override
		public IInternalPostgresMaintenance instance(BmContext context, String... params) throws ServerFault {
			RBACManager.forContext(context).check(BasicRoles.ROLE_SYSTEM_MANAGER);
			return new PostgresqlMaintenance(context);
		}

	}

	private BmContext context;

	public PostgresqlMaintenance(BmContext context) {
		this.context = context;
	}

	@Override
	public TaskRef executeMaintenanceQueries() {
		return context.provider().instance(ITasksManager.class).run("pgmaintenancequery", (IServerTaskMonitor m) -> {
			executeMaintenanceQueries(m);
		});
	}

	// FIXME load scripts from /etc/bm/local/maintenance-sql ?
	@Override
	public void executeMaintenanceQueries(IServerTaskMonitor m) {
		List<MaintenanceScript> scripts = MaintenanceScripts.getScripts();
		logger.info("executing pgmaintenance, {} scripts to execute", scripts.size() + 1);
		m.begin(scripts.size() + 1, "Going to play " + scripts.size() + 1);
		for (MaintenanceScript script : scripts) {
			executeScript(script.name, script.script, script.database, m.subWork(1));
		}

		executeScript("vacuum", "VACUUM ANALYZE", Database.ALL, m.subWork(1));
	}

	private void executeScript(String name, String script, Database db, IServerTaskMonitor monitor) {
		monitor.begin(1, "Executing script " + name);
		logger.info("executing script {} : {}", name, script);
		long time = System.currentTimeMillis();
		if (db == Database.ALL || db == Database.DIRECTORY) {
			monitor.begin(1, "Executing script " + name + " on database BJ");
			logger.info("executing script {} : {} on database BJ", name, script);
			execute(name, script, monitor, time, context.getDataSource());
		}
		if (db == Database.ALL || db == Database.SHARD) {
			for (DataSource ds : context.getAllMailboxDataSource()) {
				monitor.begin(1, "Executing script " + name + " on database BJ-DATA");
				logger.info("executing script {} : {} on database BJ-DATA", name, script);
				execute(name, script, monitor, time, ds);
			}
		}
	}

	private void execute(String name, String script, IServerTaskMonitor monitor, long time, DataSource ds) {
		try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
			st.execute(script);
			monitor.progress(1, "script " + name + " played");
			logger.info("script {} took {}ms to execute", name, System.currentTimeMillis() - time);
		} catch (SQLException e) {
			logger.error("error executing script {}", name, e);
			monitor.progress(1, "script " + name + " failed :" + e.getMessage());
		}
	}

}
