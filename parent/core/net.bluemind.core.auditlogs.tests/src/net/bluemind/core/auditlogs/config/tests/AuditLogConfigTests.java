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

import static org.junit.Assert.assertEquals;
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

	private File confFile;
	private static final String CONF_FILE_PATH = "/etc/bm/auditlog-store.conf";
	private final String domainUid1 = "domain01.blue";
	private final String domainUid2 = "domain02.blue";

	@Before
	public void before() throws Exception {
		confFile = new File(CONF_FILE_PATH);
		if (confFile.exists()) {
			confFile.delete();
		}
		AuditLogStoreConfig.clear();
		JdbcTestHelper.getInstance().beforeTest();
		ElasticsearchTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");
		PopulateHelper.initGlobalVirt(esServer);

		PopulateHelper.createDomain(domainUid1);
		PopulateHelper.createDomain(domainUid2);

	}

	@After
	public void after() throws Exception {
		if (confFile.exists()) {
			confFile.delete();
		}
		AuditLogStoreConfig.clear();
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
	}

	@Test
	public void testDefaultAuditLogStoreConfiguration() throws Exception {
		AuditLogStoreConfig.clear();
		File file = new File(CONF_FILE_PATH);
		try (FileOutputStream fos = new FileOutputStream(file)) {
			String toWrite = "auditlog {\n activate = true\n }";
			fos.write(toWrite.getBytes());
		}
		AuditLogStoreConfig.get();
		assertTrue(AuditLogStoreConfig.isActivated());
		assertEquals(AuditLogStoreConfig.AUDITLOG_DATASTREAM_NAME, AuditLogStoreConfig.getDataStreamName());
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
		assertEquals(AuditLogStoreConfig.AUDITLOG_DATASTREAM_NAME, AuditLogStoreConfig.getDataStreamName());
	}

	@Test
	public void testMultipleDataStreamTotoForAuditLogStoreConfiguration() throws Exception {
		AuditLogStoreConfig.clear();
		File file = new File(CONF_FILE_PATH);
		try (FileOutputStream fos = new FileOutputStream(file)) {
			String toWrite = "auditlog {\n activate = true\n, domain_datastream = toto_%d \n }";
			fos.write(toWrite.getBytes());
		}
		AuditLogStoreConfig.get();
		assertTrue(AuditLogStoreConfig.isActivated());
		assertEquals("toto_%d", AuditLogStoreConfig.getDataStreamName());
		assertTrue(false);
	}

}
