/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.metrics.core.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.metrics.alerts.api.ITickConfiguration;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class TickConfigurationServiceTests {

	private String apiKey;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		Server imap = new Server();
		imap.ip = new BmConfIni().get("imap-role");
		imap.tags = Arrays.asList("mail/imap");
		PopulateHelper.initGlobalVirt(imap);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		this.apiKey = "yeah-yeah";
		Sessions.get().put(apiKey, SecurityContext.SYSTEM);
	}

	@Test
	public void testConfigurationService() throws Exception {
		ClientSideServiceProvider prov = ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", apiKey);
		ITickConfiguration configApi = prov.instance(ITickConfiguration.class);
		assertNotNull(configApi);
		TaskRef task = configApi.reconfigure();
		waitEnd(task);
	}

	private TaskStatus waitEnd(TaskRef ref) throws Exception {
		return waitEnd(ref, TaskStatus.State.Success);
	}

	private TaskStatus waitEnd(TaskRef ref, TaskStatus.State expectedState) throws Exception {
		TaskStatus status = null;
		while (true) {
			ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, ref.id);
			status = task.status();
			if (status.state.ended) {
				break;
			}
		}
		System.out.println("state: " + status.state);
		assertTrue(status.state == expectedState);
		return status;
	}

}
