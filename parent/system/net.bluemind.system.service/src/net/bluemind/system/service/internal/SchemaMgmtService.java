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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.ISchemaMgmt;
import net.bluemind.system.api.SchemaCheckInfo;
import net.bluemind.system.pg.PostgreSQLService;

public class SchemaMgmtService implements ISchemaMgmt {

	private BmContext context;
	private RBACManager rbacManager;

	private static final String PG_PATH = "/var/lib/postgresql";
	private static final String COMPARE_SCHEMA_SCRIPT = "scripts/compareSchemas.sh";

	public SchemaMgmtService(BmContext context) {
		this.context = context;
		this.rbacManager = new RBACManager(context);
	}

	public static class Factory implements ServerSideServiceProvider.IServerSideServiceFactory<ISchemaMgmt> {

		@Override
		public Class<ISchemaMgmt> factoryClass() {
			return ISchemaMgmt.class;
		}

		@Override
		public ISchemaMgmt instance(BmContext context, String... params) throws ServerFault {
			if (params.length != 0) {
				throw new ServerFault("wrong number of instance parameters");
			}
			return new SchemaMgmtService(context);
		}

	}

	@Override
	public TaskRef verify() {
		return context.provider().instance(ITasksManager.class).run(mon -> verifyServers(mon));
	}

	public void verifyServers(IServerTaskMonitor mon) {
		rbacManager.check(BasicRoles.ROLE_ADMIN);
		List<SchemaCheckInfo> resultList = new ArrayList<>();

		IServer serversApi = context.provider().instance(IServer.class, InstallationId.getIdentifier());
		List<ItemValue<Server>> servers = serversApi.allComplete();

		servers.stream().filter(s -> s.value.tags.contains(TagDescriptor.bm_pgsql.getTag())
				|| s.value.tags.contains(TagDescriptor.bm_pgsql_data.getTag())).forEach(s -> {
					INodeClient nc = NodeActivator.get(s.value.address());

					checkMigra(s, nc);
					String tmpDbName = "refdb".concat(String.valueOf(System.currentTimeMillis()));
					ExitList dbExisting = NCUtils.exec(nc,
							"sudo -u postgres bash -c \"psql -lqt | cut -d \\| -f 1 | grep -w " + tmpDbName + "\"", 30,
							TimeUnit.SECONDS);
					if (dbExisting.getExitCode() != 0) {
						installReferenceDb(s.value.address(), tmpDbName);
					}
					File compareScript = copyScript(nc, tmpDbName);
					try {
						List<String> dbNames = new ArrayList<>();
						if (s.value.tags.contains(TagDescriptor.bm_pgsql.getTag())) {
							dbNames.add("bj");
						}
						if (s.value.tags.contains(TagDescriptor.bm_pgsql_data.getTag())) {
							dbNames.add("bj-data");
						}

						dbNames.forEach(dbName -> {
							verifyDb(resultList, s, nc, tmpDbName, compareScript, dbName);

						});
					} finally {
						dropReferenceDb(s.value.address(), tmpDbName);
						clearServer(nc, compareScript);
					}
				});

		mon.end(true, "", JsonUtils.asString(resultList));

	}

	private void verifyDb(List<SchemaCheckInfo> resultMap, ItemValue<Server> s, INodeClient nc, String tmpDbName,
			File compareScript, String dbName) {
		File compareResult = new File(PG_PATH + "/output_" + s.uid + "_" + dbName + "_" + tmpDbName + ".sql");
		try {
			NCUtils.exec(nc, compareScript.getPath() + " " + dbName + " " + tmpDbName + " " + compareResult.getPath(),
					30, TimeUnit.SECONDS);

			String statements = new String(nc.read(compareResult.getAbsolutePath()));
			statements = Arrays.asList(statements.split("\n")).stream().filter(line -> !line.trim().isEmpty())
					.reduce("", (a, b) -> a + "\n" + b);

			SchemaCheckInfo check = new SchemaCheckInfo();
			check.server = s.displayName;
			check.db = dbName;
			check.statements = statements;
			resultMap.add(check);
		} catch (Exception e) {
			throw new ServerFault(e.getMessage());
		} finally {
			clearResult(nc, compareResult);
		}
	}

	private void installReferenceDb(String host, String dbName) throws ServerFault {
		new PostgreSQLService().installReferenceDb(host, dbName);
	}

	private void dropReferenceDb(String host, String dbName) {
		if (dbName.equals("bj") || dbName.equals("bj-data")) {
			throw new ServerFault(dbName + " database cannot be deleted.", ErrorCode.FORBIDDEN);
		}
		new PostgreSQLService().deleteReferenceDb(host, dbName);
	}

	private void clearResult(INodeClient nc, File compareResult) {
		nc.deleteFile(compareResult.getPath());
	}

	private void clearServer(INodeClient nc, File compareScript) {
		nc.deleteFile(compareScript.getPath());
	}

	private File copyScript(INodeClient nc, String tmpDbName) {
		File compareScript = new File("/usr/share/bm-cli/compareSchemas_" + tmpDbName + ".sh");
		InputStream inputStream = SchemaMgmtService.class.getClassLoader().getResourceAsStream(COMPARE_SCHEMA_SCRIPT);
		nc.writeFile(compareScript.getPath(), inputStream);

		NCUtils.execOrFail(nc, "chmod +x " + compareScript.getPath());
		return compareScript;
	}

	private void checkMigra(ItemValue<Server> server, INodeClient nc) {
		try {
			ExitList checkMigra = NCUtils.exec(nc, "pip list");
			if (checkMigra.getExitCode() != 0 || checkMigra.getFirst() == null
					|| !checkMigra.getFirst().contains("migra")) {
				String errorMsg = "Migra is not installed : please run \n" //
						+ "- sudo pip install migra\n" //
						+ "- sudo pip install migra[pg]";
				throw new ServerFault(errorMsg);
			}
		} catch (ServerFault e) {
			String errorMsg = "Please install pyhton pip before: 'sudo apt install python3-pip' \n"
					+ "Then install Migra : \n" //
					+ "- sudo pip install migra\n" //
					+ "- sudo pip install migra[pg]";
			throw new ServerFault(errorMsg);
		}
	}

}