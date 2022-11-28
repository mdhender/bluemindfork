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
package net.bluemind.system.pg.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.system.pg.PostgreSQLService;

public class PostgreSQLServiceTests {

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

	}

	@Test
	public void testAddDataServer() {
		BmConfIni conf = new BmConfIni();
		String dbHost = conf.get("bluemind/postgres-tests");

		assertNotNull(dbHost);

		Server srv = new Server();
		srv.fqdn = dbHost;
		srv.ip = dbHost;
		srv.name = dbHost;
		srv.tags = Lists.newArrayList("bm/pgsql-data");

		ItemValue<Server> server = ItemValue.create(UUID.randomUUID().toString(), srv);

		PostgreSQLService service = new TestPostgreSQLService();
		String dbName = "db-test-" + System.currentTimeMillis();
		service.addDataServer(server, dbName);

		INodeClient nc = NodeActivator.get(server.value.address());
		String res = NCUtils.exec(nc, "sudo -n -u postgres -i -- psql --list").stream().reduce("", (output, elem) -> {
			return output.concat(elem);
		});

		System.err.println(res);

		assertTrue(res.contains(dbName));

	}

}
