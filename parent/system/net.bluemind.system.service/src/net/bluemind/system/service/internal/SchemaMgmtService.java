/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.system.api.ISchemaMgmt;
import net.bluemind.system.pg.PostgreSQLService;

public class SchemaMgmtService implements ISchemaMgmt {

	private BmContext context;
	private RBACManager rbacManager;
	private String host;
	private String dbName;

	public SchemaMgmtService(BmContext context, String host, String dbName) {
		this.context = context;
		this.rbacManager = new RBACManager(context);
		this.host = host;
		this.dbName = dbName;
	}

	public static class Factory implements ServerSideServiceProvider.IServerSideServiceFactory<ISchemaMgmt> {

		@Override
		public Class<ISchemaMgmt> factoryClass() {
			return ISchemaMgmt.class;
		}

		@Override
		public ISchemaMgmt instance(BmContext context, String... params) throws ServerFault {
			if (params == null || params.length != 2) {
				throw new ServerFault("wrong number of instance parameters");
			}
			return new SchemaMgmtService(context, params[0], params[1]);
		}

	}

	@Override
	public TaskRef installReferenceDb() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_ADMIN);
		IServerTask installTask = new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				installReferenceDb(monitor, host, dbName);
			}
		};

		return context.provider().instance(ITasksManager.class).run(installTask);
	}

	private void installReferenceDb(IServerTaskMonitor monitor, String host, String dbName) {
		monitor.begin(100, "Initializing temporary reference database ...");

		try {
			new PostgreSQLService().installReferenceDb(host, dbName);
			monitor.log("Temporary {} database schema initialized", dbName);
		} catch (Exception e) {
			monitor.end(false, e.getMessage(), "Fail to initialize reference database schema");
			return;
		}

		monitor.end(true, "Initialized temporary reference database", null);
	}

	@Override
	public TaskRef dropReferenceDb() {
		rbacManager.check(BasicRoles.ROLE_ADMIN);
		if (dbName.equals("bj") || dbName.equals("bj-data")) {
			throw new ServerFault(dbName + " database cannot be deleted.", ErrorCode.FORBIDDEN);
		}
		IServerTask dropTask = new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				dropReferenceDb(monitor, host, dbName);
			}
		};

		return context.provider().instance(ITasksManager.class).run(dropTask);
	}

	private void dropReferenceDb(IServerTaskMonitor monitor, String host, String dbName) {
		monitor.begin(100, "Delete temporary reference database ...");

		try {
			new PostgreSQLService().deleteReferenceDb(host, dbName);
			monitor.log("Temporary {} reference database deleted", dbName);
		} catch (Exception e) {
			monitor.end(false, e.getMessage(), "Fail to delete temporary reference database");
			return;
		}

		monitor.end(true, "Deleted temporary reference database", null);
	}
}