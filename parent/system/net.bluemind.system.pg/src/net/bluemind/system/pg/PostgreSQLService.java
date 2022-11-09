/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.system.pg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.jdbc.DbSchemaService;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.BMPoolActivator;
import net.bluemind.pool.Pool;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;

public class PostgreSQLService {
	private static final Logger logger = LoggerFactory.getLogger(PostgreSQLService.class);

	private static final String PG_CONF_PATH = "/etc/postgresql/15/main";
	private static final String SQL_DROP_PATH = "scripts/dropTmpDatabase.sh";
	private static final String SQL_SW_PATH = "/usr/share/bm-setup-wizard/tpl/sql/install_bmdb_pgsql_0.sh";
	private static final String SQL_IW_PATH = "/usr/share/bm-installation-wizard/tpl/sql/install_bmdb_pgsql_0.sh";

	public void installReferenceDb(String host, String dbName) {
		logger.info("new data server address {}, dbName {}", host, dbName);

		try {
			INodeClient nc = NodeActivator.get(host);
			createDatabaseAndInitSchema(nc, dbName, host);
		} catch (Exception e) {
			throw new ServerFault("Fail to initialize reference database schema", e);
		}
	}

	public void deleteReferenceDb(String host, String dbName) {
		if (dbName.equals("bj") || dbName.equals("bj-data")) {
			throw new ServerFault(dbName + " database cannot be deleted.", ErrorCode.FORBIDDEN);
		}

		logger.info("try to delete db {} on server address {}", dbName, host);
		INodeClient nc = NodeActivator.get(host);
		final String tmpDropFilePath = "/tmp/dropTmpDatabase.sh";

		// create bj-data database
		try {
			logger.info(".. write dropTmpDatabase.sh");
			nc.writeFile(tmpDropFilePath, getDropDbScript());
		} catch (ServerFault | IOException e) {
			throw new ServerFault("Fail to read delete script", e);
		}
		NCUtils.execOrFail(nc, "chmod +x " + tmpDropFilePath);

		logger.info(".. exec dropTmpDatabase.sh");

		String cmd = String.format("%s %s", tmpDropFilePath, dbName);
		ExitList ret = NCUtils.exec(nc, cmd);
		if (ret.getExitCode() != 0) {
			ret.forEach(logger::error);
		} else {
			ret.forEach(logger::info);
		}

		if (new File(tmpDropFilePath).exists()) {
			nc.deleteFile(tmpDropFilePath);
		}

		if (ret.getExitCode() != 0) {
			throw new ServerFault("Fail to execute command '" + cmd + "'");
		}

	}

	public void addDataServer(ItemValue<Server> server, String dbName) {
		logger.info("new data server {} (address: {}), dbName {}", server.uid, server.value.address(), dbName);
		INodeClient nc = NodeActivator.get(server.value.address());

		configurePg(nc);

		Pool pool = createDatabaseAndInitSchema(nc, dbName, server.value.ip);

		BMPoolActivator.getDefault().addMailboxDataSource(server.uid, pool);
		JdbcActivator.getInstance().addMailboxDataSource(server.uid, pool.getDataSource());
	}

	private Pool createDatabaseAndInitSchema(INodeClient nc, String dbName, String serverIp) {
		final String tmpCreateFilePath = "/tmp/createdb.sh";

		// create bj-data database
		try {
			logger.info(".. write createdb.sh");
			nc.writeFile(tmpCreateFilePath, getCreateDbScript());
		} catch (ServerFault | IOException e) {
			throw new ServerFault("Fail to read install script", e);
		}
		NCUtils.execOrFail(nc, "chmod +x " + tmpCreateFilePath);

		logger.info(".. exec createdb.sh");

		BmConfIni oci = new BmConfIni();
		String dbType = oci.get("dbtype");
		String user = oci.get("user");
		String password = oci.get("password");

		String cmd = String.format("%s %s %s %s fr full", tmpCreateFilePath, dbName, user, password);
		ExitList ret = NCUtils.exec(nc, cmd);
		if (ret.getExitCode() != 0) {
			ret.forEach(logger::error);
		} else {
			ret.forEach(logger::info);
		}

		if (new File(tmpCreateFilePath).exists()) {
			nc.deleteFile(tmpCreateFilePath);
		}

		if (ret.getExitCode() != 0) {
			throw new ServerFault("Fail to execute command '" + cmd + "'");
		}

		Pool pool;
		try {
			logger.info(".. start pool");
			pool = BMPoolActivator.getDefault().startPool(dbType, user, password, serverIp, dbName);
		} catch (Exception e) {
			throw new ServerFault("Fail to start pool", e);
		}

		DbSchemaService dbService = DbSchemaService.getService(pool.getDataSource(), true);
		dbService.initialize();

		return pool;

	}

	private void configurePg(INodeClient nc) {
		// copy pg conf to the brand new server
		logger.info(".. stop postgresql");
		NCUtils.execOrFail(nc, "service postgresql stop");

		logger.info(".. touch postgresql.conf.pimp");
		NCUtils.execOrFail(nc, "touch " + PG_CONF_PATH + "/postgresql.conf.pimp");

		logger.info(".. touch postgresql.conf.local");
		NCUtils.execOrFail(nc, "touch " + PG_CONF_PATH + "/postgresql.conf.local");

		try {
			logger.info(".. write postgresql.conf");
			nc.writeFile(PG_CONF_PATH + "/postgresql.conf", getConf("postgresql.conf"));
		} catch (ServerFault | IOException e) {
			throw new ServerFault("Fail to write postgresql.conf", e);
		}

		try {
			logger.info(".. write pg_hba.conf");
			nc.writeFile(PG_CONF_PATH + "/pg_hba.conf", getConf("pg_hba.conf"));
		} catch (ServerFault | IOException e) {
			throw new ServerFault("Fail to write pg_hba.conf", e);
		}

		logger.info(".. start postgresql");
		NCUtils.execOrFail(nc, "service postgresql start");
	}

	protected InputStream getCreateDbScript() throws IOException {
		if (new File(SQL_SW_PATH).exists()) {
			return Files.asByteSource(new File(SQL_SW_PATH)).openStream();
		}
		return Files.asByteSource(new File(SQL_IW_PATH)).openStream();
	}

	private InputStream getDropDbScript() throws IOException {
		return PostgreSQLService.class.getClassLoader().getResourceAsStream(SQL_DROP_PATH);
	}

	protected InputStream getConf(String conf) throws IOException {
		return Files.asByteSource(new File(PG_CONF_PATH + "/" + conf)).openStream();
	}

}
