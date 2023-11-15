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
package net.bluemind.core.auditlogs.config.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.auditlogs.client.loader.config.AuditLogStoreConfig;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class AuditLogConfigTests {

	private String domainUid;
	private File confFile;
//	private SecurityContext defaultSecurityContext;
	private static final String CONF_FILE_PATH = "/etc/bm/auditlog-store.conf";
//	private static ElasticContainer esContainer = new ElasticContainer();

	@Before
	public void before() throws Exception {
//		esContainer.start();
		confFile = new File(CONF_FILE_PATH);
		if (confFile.exists()) {
			confFile.delete();
		}
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		ElasticsearchTestHelper.getInstance().beforeTest();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);
	}

	@After
	public void after() throws Exception {
//		esContainer.stop();
		AuditLogStoreConfig.clear();
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
	}

	@Test
	public void testDefaultAuditLogStoreConfiguration() throws Exception {
//		Integer mappedPort = esContainer.getMappedPort(9200);

		assertTrue(AuditLogStoreConfig.isActivated());
	}

	@Test
	public void testAuditLogStoreDeactivatedConfiguration() throws Exception {
		AuditLogStoreConfig.clear();
		File file = new File(CONF_FILE_PATH);
		try (FileOutputStream fos = new FileOutputStream(file)) {
			String toWrite = "auditlog {\n activate = false\n }";
			fos.write(toWrite.getBytes());
		}
		AuditLogStoreConfig.get();
		assertFalse(AuditLogStoreConfig.isActivated());
	}

}
