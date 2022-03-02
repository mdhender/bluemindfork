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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.maintenance.postgresql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.maintenance.IMaintenanceScript;

public class Analyze implements IMaintenanceScript {
	private static final Logger logger = LoggerFactory.getLogger(Analyze.class);

	public void run(IServerTaskMonitor monitor) {
		logger.info("Launch analyze");

		DataSource directoryPool = ServerSideServiceProvider.defaultDataSource;
		execute(directoryPool, monitor.subWork("bm-master", 1), "VACUUM ANALYZE");

		for (Entry<String, DataSource> dsEntry : ServerSideServiceProvider.mailboxDataSource.entrySet()) {
			monitor.log("vacuum analyze on pool " + dsEntry.getKey());
			execute(dsEntry.getValue(), monitor.subWork(dsEntry.getKey(), 1), "VACUUM ANALYZE");
		}
	}

	private void execute(DataSource ds, IServerTaskMonitor monitor, String query) {
		long start = System.currentTimeMillis();
		try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
			st.execute(query);
			SQLWarning warn = st.getWarnings();
			while (warn != null) {
				monitor.log(warn.getMessage());
				warn = warn.getNextWarning();
			}
			monitor.end(true, query + " took " + (System.currentTimeMillis() - start) + " ms to execute", null);
		} catch (SQLException e) {
			monitor.log("error exexuting " + query + ": " + e);
			monitor.end(false, null, e.getMessage());
		}
	}
}
