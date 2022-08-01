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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cli.adm;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.ISchemaMgmt;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "schema", description = "check database schema")
public class SchemaCheck implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("maintenance");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return SchemaCheck.class;
		}
	}

	@Option(required = true, description = "the name of the database to compare (bj or bj-data)", names = {
			"--database", "-d" })
	public String dbName;

	private static final String PG_PATH = "/var/lib/postgresql";
	private static final String COMPARE_SCHEMA_SCRIPT = "scripts/compareSchemas.sh";

	private CliContext ctx;
	private String tmpDbName;
	private File compareScript;
	private File compareResult;
	private ISchemaMgmt service;
	private boolean dbCreated = false;
	private INodeClient nc;

	@Override
	public void run() {
		try {
			Map<ItemValue<Server>, List<String>> statements = check();
			statements.forEach(this::displayStatement);
		} catch (Exception e) {
			throw new CliException(e.getMessage());
		}
	}

	private Map<ItemValue<Server>, List<String>> check() throws Exception {
		Map<ItemValue<Server>, List<String>> resultMap = new HashMap<>();

		IServer serversApi = ctx.adminApi().instance(IServer.class, InstallationId.getIdentifier());
		List<ItemValue<Server>> servers = serversApi.allComplete();

		servers.stream()
				.filter(s -> (dbName.equals("bj") && s.value.tags.contains(TagDescriptor.bm_pgsql.getTag()))
						|| (dbName.equals("bj-data") && s.value.tags.contains(TagDescriptor.bm_pgsql_data.getTag())))
				.forEach(s -> {
					nc = NodeActivator.get(s.value.address());
					ctx.info("### Check " + dbName + " schema on server " + s.displayName + "\n=============");
					dbCreated = false;
					try {
						tmpDbName = dbName.replace("-", "").concat(String.valueOf(System.currentTimeMillis()));
						compareResult = new File(
								PG_PATH + "/output_" + s.uid + "_" + dbName + "_" + tmpDbName + ".sql");
						service = ctx.adminApi().instance(ISchemaMgmt.class, s.value.address(), tmpDbName);

						checkMigra(s);
						checkDatabases();
						dbCreated = createReferenceDatabase();
						copyScript();

						ctx.info("Compare databases schemas...");
						NCUtils.exec(nc, compareScript.getPath() + " " + dbName + " " + tmpDbName + " "
								+ compareResult.getPath(), 30, TimeUnit.SECONDS);

						ctx.info("Read comparison result file for " + dbName + "...");
						if (!compareResult.exists()) {
							throw new CliException(
									"Comparison result file " + compareResult.getPath() + " does not exists");
						}
						List<String> statements = Files.readAllLines(compareResult.toPath());
						if (statements != null && !statements.isEmpty()) {
							statements.removeIf(String::isEmpty);
						} else {
							ctx.warn("Comparison result file " + compareResult.getPath() + " is empty");
						}

						resultMap.put(s, statements);
					} catch (Exception e) {
						throw new CliException(e.getMessage());
					} finally {
						clear();
					}

				});

		return resultMap;
	}

	private void displayStatement(ItemValue<Server> s, List<String> statements) {

		if (statements == null || statements.isEmpty()) {
			ctx.info("\nThe {} schema on server {} is compliant, there is no statements to execute. \n", dbName,
					s.displayName);
		} else {
			ctx.info("\nFollowing {} statements could be executed on server {} to be compliant with {} schema: \n",
					statements.size(), s.displayName, dbName);
			statements.stream().forEach(ctx::info);
			ctx.info("\n");
		}
	}

	private boolean dropReferenceDatabase() {
		ctx.info("Drop reference database for " + dbName + "...");

		TaskRef dropTask = service.dropReferenceDb();
		TaskStatus taskState = Tasks.follow(ctx, dropTask, "Drop database", "Fail to delete database " + tmpDbName);
		if (taskState.state != TaskStatus.State.Success) {
			ctx.error("Reference database deletion task fails");
			throw new CliException(taskState.result);
		}

		return true;
	}

	private boolean createReferenceDatabase() {
		ctx.info("Create reference database for " + dbName + "...");

		TaskRef initializationTask = service.installReferenceDb();
		TaskStatus taskState = Tasks.follow(ctx, initializationTask, "Create database",
				"Fail to install database " + tmpDbName);
		if (taskState.state != TaskStatus.State.Success) {
			ctx.error("Reference database creation task fails");
			throw new CliException(taskState.result);
		}

		return true;
	}

	private void copyScript() {
		ctx.info("Copy script for schemas comparison...");

		compareScript = new File("/usr/share/bm-cli/compareSchemas_" + tmpDbName + ".sh");
		InputStream inputStream = SchemaCheck.class.getClassLoader().getResourceAsStream(COMPARE_SCHEMA_SCRIPT);
		nc.writeFile(compareScript.getPath(), inputStream);

		NCUtils.execOrFail(nc, "chmod +x " + compareScript.getPath());
	}

	private void clear() {
		ctx.info("Cleanup...");

		if (compareScript != null && compareScript.exists()) {
			nc.deleteFile(compareScript.getPath());
		}

		if (compareResult != null && compareResult.exists()) {
			nc.deleteFile(compareResult.getPath());
		}

		if (dbCreated) {
			dropReferenceDatabase();
		}
	}

	private void checkMigra(ItemValue<Server> server) {
		ctx.info("Check Migra installed on server {}:{}...", server.uid, server.value.address());

		try {
			ExitList checkMigra = NCUtils.exec(nc, "pip list");
			if (checkMigra.getExitCode() != 0 || checkMigra.getFirst() == null
					|| !checkMigra.getFirst().contains("migra")) {
				String errorMsg = "Migra is not installed : please run \n" //
						+ "- sudo pip install migra\n" //
						+ "- sudo pip install migra[pg]";
				throw new CliException(errorMsg);
			}
		} catch (ServerFault e) {
			String errorMsg = "Please install pyhton pip before: 'sudo apt install python3-pip' \n"
					+ "Then install Migra : \n" //
					+ "- sudo pip install migra\n" //
					+ "- sudo pip install migra[pg]";
			throw new CliException(errorMsg);
		}
	}

	private void checkDatabases() {
		ctx.info("Check database...");
		if (!dbName.equals("bj") && !dbName.equals("bj-data")) {
			throw new CliException("database name must be bj or bj-data");
		}

		ExitList dbExisting = NCUtils.exec(nc,
				"sudo -u postgres bash -c \"psql -lqt | cut -d \\| -f 1 | grep -w " + tmpDbName + "\"", 30,
				TimeUnit.SECONDS);
		if (dbExisting.getExitCode() == 0) {
			throw new CliException("database " + tmpDbName + " already exists");
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
