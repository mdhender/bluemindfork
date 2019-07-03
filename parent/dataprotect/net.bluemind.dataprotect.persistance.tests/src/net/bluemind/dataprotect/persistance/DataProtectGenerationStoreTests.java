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
package net.bluemind.dataprotect.persistance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.api.VersionInfo;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.IDomainTemplate;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DataProtectGenerationStoreTests {
	private DataProtectGenerationStore generationStore;
	private int genId;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		Server core = new Server();
		core.ip = new BmConfIni().get("node-host");
		core.tags = getTagsExcept("bm/es", "mail/imap", "bm/pgsql", "bm/pgsql-data");

		Server dbServer = new Server();
		dbServer.ip = new BmConfIni().get("host");
		dbServer.tags = Lists.newArrayList("bm/pgsql", "bm/pgsql-data");

		PopulateHelper.initGlobalVirt(false, core, dbServer);

		generationStore = new DataProtectGenerationStore(JdbcTestHelper.getInstance().getDataSource());
		DataProtectGeneration gen = generationStore.newGeneration(VersionInfo.create("0.4.0", "BM-Test"));
		genId = gen.id;
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	private List<String> getTagsExcept(String... except) {
		// tag & assign host for everything
		List<String> tags = new LinkedList<String>();

		IDomainTemplate dt = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainTemplate.class);
		DomainTemplate template = dt.getTemplate();
		for (DomainTemplate.Kind kind : template.kinds) {
			for (DomainTemplate.Tag tag : kind.tags) {
				if (tag.autoAssign) {
					tags.add(tag.value);
				}
			}
		}

		tags.removeAll(Arrays.asList(except));

		return tags;
	}

	@Test
	public void testSchemaIsWellRegsited() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("dataprotect-schema"));
	}

	@Test
	public void testNewPart() throws SQLException {
		generationStore.newPart(genId, "test", "127.0.0.1", "woot");
		List<PartGeneration> parts = generationStore.listParts("test", "127.0.0.1");
		for (PartGeneration pg : parts) {
			System.out.println("PG: tag: " + pg.tag + ", srv: " + pg.server + ", begin: " + pg.begin);
		}
	}

	@Test
	public void testListParts() throws SQLException {
		generationStore.newPart(genId, "test", "testUid", "osef");
		generationStore.newPart(genId, "test", "testUid", "osef");
		generationStore.newPart(genId, "test", "testUid", "osef");
		generationStore.newPart(genId, "test2", "testUid", "osef");
		generationStore.newPart(genId, "test", "testUid2", "osef");

		assertEquals(3, generationStore.listParts("test", "testUid").size());
	}

	@Test
	public void testUpdate() throws SQLException {
		generationStore.newPart(genId, "test", "testUid", "osef");
		PartGeneration part = generationStore.listParts("test", "testUid").get(0);

		part.end = new Date();
		generationStore.updatePart(part);

		part = generationStore.listParts("test", "testUid").get(0);

		// FIXME should check date is good
		assertNotNull(part.end);
	}

	@Test
	public void rewrite() throws Exception {
		generationStore.newPart(genId, "tag", "vmtest", "osef");
		generationStore.newPart(genId, "tag", "vmtest", "osef");
		generationStore.newPart(genId, "tag", "vmtest", "osef");

		DataProtectGeneration dpg = generationStore.newGeneration(VersionInfo.create("0.4.0", "BM-Test"));
		dpg.parts = Arrays.asList(createPartGeneration(1, "vmtest", "tag"), createPartGeneration(2, "vmtest", "tag"));

		DataProtectGeneration dpg2 = generationStore.newGeneration(VersionInfo.create("0.4.0", "BM-Test"));
		dpg2.parts = Arrays.asList(createPartGeneration(3, "vmtest", "tag1"),
				createPartGeneration(42, "vmtest2", "tag2"));

		List<DataProtectGeneration> generations = Arrays.asList(dpg, dpg2);
		generationStore.rewriteGenerations(generations);

		assertEquals(42, readSeq());
		assertEquals(2, generationStore.listParts("tag", "vmtest").size());
		assertEquals(1, generationStore.listParts("tag1", "vmtest").size());
		assertEquals(1, generationStore.listParts("tag2", "vmtest2").size());

		List<DataProtectGeneration> storeGens = generationStore.getGenerations();
		assertEquals(storeGens.size(), generations.size());
	}

	private int readSeq() throws SQLException {
		Connection conn = JdbcTestHelper.getInstance().getDataSource().getConnection();

		try (Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery("select last_value from partgeneration_id_seq")) {

			rs.next();
			return rs.getInt(1);
		}
	}

	private PartGeneration createPartGeneration(int partId, String srv, String tag) {
		Random rnd = new Random();

		PartGeneration g = new PartGeneration();
		g.id = partId;
		g.begin = new Date();
		g.end = new Date();
		g.size = rnd.nextInt();
		g.server = srv;
		g.tag = tag;
		return g;
	}
}
