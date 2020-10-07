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
package net.bluemind.dataprotect.postgresql.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.service.IDPContext.IToolConfig;
import net.bluemind.dataprotect.service.IDPContext.IToolSession;
import net.bluemind.dataprotect.worker.DefaultWorker;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public abstract class AbstractPgWorker extends DefaultWorker {

	protected String dbUser;
	protected String dbPassword;
	protected String dbName;
	private Set<String> excludeData = Sets.newHashSet("t_container_changelog", "t_job_log_entry", "t_eas_*",
			"t_message_body", "t_mailbox_replica", "t_mailbox_record");

	public AbstractPgWorker() {
		BmConfIni ini = new BmConfIni();
		dbUser = ini.get("user");
		dbPassword = ini.get("password");
		dbName = ini.get("db");
	}

	protected abstract String getBackupDirectory();

	private String dataString(String file) throws ServerFault {
		try (InputStream in = AbstractPgWorker.class.getClassLoader().getResourceAsStream(file)) {
			return new String(ByteStreams.toByteArray(in));
		} catch (IOException ioe) {
			throw new ServerFault(ioe);
		}
	}

	@Override
	public void prepareDataDirs(IDPContext ctx, String tag, ItemValue<Server> toBackup) throws ServerFault {
		super.prepareDataDirs(ctx, tag, toBackup);

		String dir = getBackupDirectory();

		logger.info("Should do the dump of {} ...", dbName);
		String s = dataString("scripts/dump.sh");
		s = s.replace("${file}", dir + "/dump.sql");
		s = s.replace("${user}", dbUser);
		s = s.replace("${pass}", dbPassword);
		s = s.replace("${db}", dbName);
		INodeClient nc = NodeActivator.get(toBackup.value.address());
		NCUtils.execNoOut(nc, "mkdir -p " + dir);
		nc.writeFile(dir + "/dump.sh", new ByteArrayInputStream(s.getBytes()));
		try {
			NCUtils.execNoOut(nc, "chmod +x " + dir + "/dump.sh");
			ExitList el = NCUtils.exec(nc, dir + "/dump.sh");

			for (String log : el) {
				if (!StringUtils.isBlank(log)) {
					ctx.info("en", "DUMP: " + log);
				}
			}
			if (el.getExitCode() != 0) {
				throw new ServerFault("pg_dump failed with exit code " + el.getExitCode());
			}
		} finally {
			NCUtils.execNoOut(nc, "rm -f " + dir + "/dump.sh");
		}

		logger.info("Backup postgresql configuration files");
		NCUtils.execNoOut(nc, "rm -rf " + dir + "/configuration");
		ExitList el = NCUtils.exec(nc, "cp -r /etc/postgresql " + dir + "/configuration");
		for (String log : el) {
			if (!StringUtils.isBlank(log)) {
				ctx.info("en", "copy postgresql conf: " + log);
			}
		}
		if (el.getExitCode() != 0) {
			throw new ServerFault("copy postgresql config failed with exit code " + el.getExitCode());
		}
	}

	@Override
	public Set<String> getDataDirs() {
		return Sets.newHashSet(getBackupDirectory());
	}

	@Override
	public void restore(IDPContext ctx, PartGeneration part, Map<String, Object> params) throws ServerFault {
		logger.info("Should restore postgresql from part {}", part.id);

		String dir = getBackupDirectory();

		IServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IServer serverApi = sp.instance(IServer.class, InstallationId.getIdentifier());
		ItemValue<Server> server = serverApi.getComplete(part.server);
		IToolConfig oc = ctx.tool().configure(server, part.tag, getDataDirs());
		IToolSession os = ctx.tool().newSession(oc);

		String restRoot = os.tmpDirectory();
		logger.info("Starting restore of {} into {}", dir, restRoot);
		os.restore(part.id, ImmutableSet.of(dir), restRoot);
		String finalDir = restRoot + dir;
		logger.info("Final dir is {}", finalDir);

		String db = dbName;
		if (params.containsKey("toDatabase")) {
			db = params.get("toDatabase").toString();
		}
		String user = dbUser;
		if (params.containsKey("user")) {
			user = params.get("user").toString();
		}
		String pass = dbPassword;
		if (params.containsKey("pass")) {
			pass = params.get("pass").toString();
		}

		String script = dataString("scripts/restore.sh");
		script = script.replace("${db}", db);
		script = script.replace("${user}", user);
		script = script.replace("${pass}", pass);
		script = script.replace("${dumpPath}", restRoot + dir + "/dump.sql");

		StringBuilder sb = new StringBuilder();
		sb.append("TABLE DATA public (");
		sb.append(Joiner.on("|").join(excludeData));
		sb.append(")");
		script = script.replace("${excludeData}", sb.toString());

		INodeClient nc = NodeActivator.get(server.value.address());
		nc.writeFile(finalDir + "/restore.sh", new ByteArrayInputStream(script.getBytes()));
		NCUtils.exec(nc, "chmod +x " + finalDir + "/restore.sh");
		List<String> pgRestOutput = NCUtils.exec(nc, finalDir + "/restore.sh");
		for (String s : pgRestOutput) {
			ctx.info("en", "PGRESTORE: " + s);
			ctx.info("fr", "PGRESTORE: " + s);
		}

	}

	@Override
	public void cleanup(IDPContext ctx, PartGeneration part, Map<String, Object> params) throws ServerFault {
		IServer serverApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());
		ItemValue<Server> server = serverApi.getComplete(part.server);
		IToolConfig oc = ctx.tool().configure(server, part.tag, getDataDirs());
		IToolSession os = ctx.tool().newSession(oc);

		String restRoot = os.tmpDirectory();
		String finalDir = restRoot + getBackupDirectory();

		String db = dbName;
		if (params.containsKey("database")) {
			db = params.get("database").toString();
		}

		if (dbName.equals(db)) {
			logger.error("Cannot delete BlueMind database");
			return;
		}

		String script = dataString("scripts/dropdb.sh");
		script = script.replace("${db}", db);
		INodeClient nc = NodeActivator.get(server.value.address());
		nc.writeFile(finalDir + "/dropdb.sh", new ByteArrayInputStream(script.getBytes()));
		NCUtils.exec(nc, "chmod +x " + finalDir + "/dropdb.sh");
		List<String> pgRestOutput = NCUtils.exec(nc, finalDir + "/dropdb.sh");
		for (String s : pgRestOutput) {
			ctx.info("en", "DROPDB: " + s);
			ctx.info("fr", "DROPDB: " + s);
		}
	}

	@Override
	public String getDataType() {
		return "pg";
	}
}
