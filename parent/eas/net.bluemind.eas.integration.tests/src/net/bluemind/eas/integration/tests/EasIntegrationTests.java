/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.eas.integration.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import net.bluemind.backend.mailapi.testhelper.MailApiTestsBase;
import net.bluemind.eas.client.FolderSyncResponse;
import net.bluemind.eas.client.OPClient;
import net.bluemind.eas.config.global.GlobalConfig;

public class EasIntegrationTests extends MailApiTestsBase {

	private EasServerSetup setup;
	private OPClient client;

	@BeforeEach
	public void before(TestInfo info) throws Exception {
		super.before(info);

		this.setup = new EasServerSetup("toto" + System.currentTimeMillis(), domUid);
		setup.beforeTest();
		this.client = new OPClient(setup.loginAtDomain(), setup.password(), setup.device().identifier, "junit",
				"junit-agent", "http://127.0.0.1:" + GlobalConfig.EAS_PORT + "/Microsoft-Server-ActiveSync");

	}

	@AfterEach
	public void after(TestInfo info) throws Exception {
		client.destroy();
		setup.afterTest();
		super.after(info);
	}

	@Test
	public void testEasIsWorking() throws Exception {
		try {
			client.options();
			FolderSyncResponse fSync = client.folderSync("0");
			assertNotNull(fSync);
			assertFalse(fSync.getFolders().isEmpty());
		} catch (Exception e) {
			dumpThreadDump();
			throw e;
		}
	}

	public void dumpThreadDump() {
		ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
		for (ThreadInfo ti : threadMxBean.dumpAllThreads(true, true)) {
			System.err.print(ti.toString());
		}
	}

}
