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

package net.bluemind.dataprotect.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.GenerationContent;
import net.bluemind.dataprotect.api.IDataProtect;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.api.RetentionPolicy;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.IDomainTemplate;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DPServiceTests {
	private static final boolean RUN_AS_ROOT = System.getProperty("user.name").equals("root");

	private BmTestContext withRoleDataprotect;
	private BmTestContext withAdminRoleBmLan;
	private BmTestContext withManageRestoreBmLan;
	private BmTestContext withAdminRoleTestLan;
	private BmTestContext adminZero;

	@Before
	public void before() throws Exception {
		prepareLocalFilesystem();

		JdbcTestHelper.getInstance().beforeTest();

		Server core = new Server();
		core.ip = new BmConfIni().get("node-host");
		core.tags = getTagsExcept("bm/es", "mail/imap", "bm/pgsql", "bm/pgsql-data");

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		String cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		Server dbServer = new Server();
		dbServer.ip = new BmConfIni().get("host");
		dbServer.tags = Lists.newArrayList("bm/pgsql", "bm/pgsql-data");

		PopulateHelper.initGlobalVirt(false, core, esServer, imapServer, dbServer);
		PopulateHelper.createDomain("bm.lan");
		PopulateHelper.createDomain("test.lan");
		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		adminZero = BmTestContext.contextWithSession("admin0", "admin0", "global.virt", SecurityContext.ROLE_SYSTEM);

		withRoleDataprotect = BmTestContext.contextWithSession("adminDp", "adminDp", "bm.lan",
				BasicRoles.ROLE_DATAPROTECT);
		withAdminRoleBmLan = BmTestContext.contextWithSession("bmLandAdmin", "bmLandAdmin", "bm.lan",
				SecurityContext.ROLE_ADMIN);

		withManageRestoreBmLan = BmTestContext.contextWithSession("bmLandDPAdmin", "bmLandDPAdmin", "bm.lan",
				BasicRoles.ROLE_MANAGE_RESTORE);

		withAdminRoleTestLan = BmTestContext.contextWithSession("testLanAdmin", "testLanAdmin", "test.lan",
				SecurityContext.ROLE_ADMIN);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		ISystemConfiguration confService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		Map<String, String> values = new HashMap<>();
		// we don't test the schema upgrade here
		values.put("db_version", "9.9.9");
		confService.updateMutableValues(values);

		System.err.println("Waiting for hollow repl...");
		Thread.sleep(2000);
	}

	private List<String> getTagsExcept(String... except) {
		// tag & assign host for everything
		List<String> tags = new LinkedList<>();

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

	private void prepareLocalFilesystem() throws IOException, InterruptedException {
		if (RUN_AS_ROOT) {
			Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", "rm -rf /var/spool/bm-hollowed/*" });
			p.waitFor(10, TimeUnit.SECONDS);
			p = Runtime.getRuntime().exec(new String[] { "bash", "-c", "rm -rf /var/backups/bluemind/*" });
			p.waitFor(10, TimeUnit.SECONDS);
		} else {
			Process p = Runtime.getRuntime()
					.exec(new String[] { "sudo", "bash", "-c", "rm -rf /var/spool/bm-hollowed/*" });
			p.waitFor(10, TimeUnit.SECONDS);
			p = Runtime.getRuntime().exec(new String[] { "sudo", "bash", "-c", "rm -rf /var/backups/bluemind/*" });
			p.waitFor(10, TimeUnit.SECONDS);
		}

		Process p = Runtime.getRuntime()
				.exec("sudo chown -R " + System.getProperty("user.name") + " /var/spool/bm-hollowed");
		p.waitFor(10, TimeUnit.SECONDS);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testServiceFactory() throws ServerFault {
		IDataProtect dp = getService(IDataProtect.class);
		assertNotNull(dp);
	}

	@Test
	public void testSaveAll() throws Exception {
		IDataProtect dp = getService(IDataProtect.class);
		TaskRef ref = dp.saveAll();
		assertNotNull(ref);
		TaskStatus result = track(ref);
		assertTrue(result.state.succeed);

		System.out.println("========== BEFORE RE-BACKUP ===========");

		ref = dp.saveAll();
		assertNotNull(ref);
		result = track(ref);
		assertTrue(result.state.succeed);
	}

	@Test
	public void testRetPolicy() throws Exception {
		IDataProtect dp = getService(IDataProtect.class);
		TaskRef ref = dp.saveAll();
		assertNotNull(ref);
		TaskStatus result = track(ref);
		assertTrue(result.state.succeed);

		RetentionPolicy rp = new RetentionPolicy();
		rp.daily = 1;
		dp.updatePolicy(rp);
		System.out.println("========== BEFORE RE-BACKUP ===========");

		ref = dp.saveAll();
		assertNotNull(ref);
		result = track(ref);
		assertTrue(result.state.succeed);

		List<DataProtectGeneration> gens = dp.getAvailableGenerations();
		assertEquals(1, gens.size());
	}

	@Test
	public void testForget() throws Exception {
		IDataProtect dp = getService(IDataProtect.class);
		TaskRef ref = dp.saveAll();
		assertNotNull(ref);
		TaskStatus result = track(ref);
		assertTrue(result.state.succeed);
		List<DataProtectGeneration> gens = dp.getAvailableGenerations();
		DataProtectGeneration gen = gens.get(0);
		Set<String> paths = new HashSet<>();
		String server = null;
		for (PartGeneration pg : gen.parts) {
			paths.add("/var/backups/bluemind/dp_spool/rsync/" + pg.server + "/" + pg.tag + "/" + pg.id);
			server = pg.server;
		}
		TaskRef forget = dp.forget(gen.id);
		track(forget);
		IServer srvApi = getService(IServer.class, InstallationId.getIdentifier());
		ItemValue<Server> srv = srvApi.getComplete(server);
		INodeClient nc = NodeActivator.get(srv.value.address());
		for (String path : paths) {
			assertTrue("forget should have removed " + path, nc.listFiles(path).isEmpty());
		}

	}

	@Test
	public void testGetGenerationContentWithRoleSystem() throws Exception {
		getGenerationContentWithContext(adminZero, "bm.lan", "test.lan");
	}

	@Test
	public void testGetGenerationContentWithRoleAdminOfBmLan() throws Exception {
		getGenerationContentWithContext(withAdminRoleBmLan, "bm.lan");
	}

	@Test
	public void testGetGenerationContentWithRoleDataprotect() throws Exception {
		getGenerationContentWithContext(withRoleDataprotect, "bm.lan");
	}

	@Test
	public void testGetGenerationContentWithRoleManageRestore() throws Exception {
		getGenerationContentWithContext(withManageRestoreBmLan, "bm.lan");
	}

	@Test
	public void testGetGenerationContentWithRoleAdminOfTestLan() throws Exception {
		getGenerationContentWithContext(withAdminRoleTestLan, "test.lan");
	}

	private void getGenerationContentWithContext(BmContext testCtx, String... expectedDomains) throws Exception {
		IDataProtect dp = getService(adminZero, IDataProtect.class);
		List<DataProtectGeneration> gensBefore = dp.getAvailableGenerations();
		TaskRef ref = dp.saveAll();
		assertNotNull(ref);
		TaskStatus result = track(ref);
		assertTrue(result.state.succeed);

		dp = getService(testCtx, IDataProtect.class);

		List<DataProtectGeneration> gensAfter = dp.getAvailableGenerations();
		assertEquals("We don't have one more backup after saveAll", gensAfter.size(), gensBefore.size() + 1);

		DataProtectGeneration gen = gensAfter.get(0);
		for (PartGeneration pg : gen.parts) {
			System.out.println("Got part " + pg.server + " " + pg.tag + " size: " + pg.size);
		}

		TaskRef loadContent = dp.getContent(gen.id + "");
		result = track(loadContent);
		System.out.println(result.result);
		assertTrue(result.state.succeed);
		assertNotNull("result is null with context " + testCtx, result.result);

		GenerationContent content = JsonUtils.read(result.result, GenerationContent.class);
		assertNotNull(content);
		assertNotNull(content.domains);
		assertEquals(expectedDomains.length, content.domains.size());
		Set<String> wantedDomains = Sets.newHashSet(expectedDomains);
		assertTrue("All expected domains " + wantedDomains + " are not in " + content.domains,
				content.domains.stream().map(iv -> iv.uid).allMatch(uid -> wantedDomains.contains(uid)));
		assertNotNull(content.entries);
		for (ItemValue<DirEntry> deItem : content.entries) {
			DirEntry de = deItem.value;
			System.out.println(" * [" + deItem.uid + "]: " + de.displayName);
		}

	}

	@Test
	public void testSkipEmailBackup() throws Exception {
		IDataProtect dp = getService(IDataProtect.class);

		RetentionPolicy rp = new RetentionPolicy();
		rp.daily = 2;
		dp.updatePolicy(rp);

		TaskRef ref = dp.saveAll();
		assertNotNull(ref);
		TaskStatus result = track(ref);
		assertTrue(result.state.succeed);

		List<DataProtectGeneration> gensAfter = dp.getAvailableGenerations();
		DataProtectGeneration gen = gensAfter.get(0);
		boolean gotImapTag = false;
		for (PartGeneration pg : gen.parts) {
			if (pg.tag.equals("mail/imap")) {
				gotImapTag = true;
				break;
			}
		}
		assertTrue(gotImapTag);

		ISystemConfiguration sysApi = getService(ISystemConfiguration.class);
		Map<String, String> values = new HashMap<String, String>();
		values.put(SysConfKeys.dpBackupSkipTags.name(), "mail/imap,mail/archive");
		sysApi.updateMutableValues(values);

		ref = dp.saveAll();
		assertNotNull(ref);
		result = track(ref);
		assertTrue(result.state.succeed);

		gensAfter = dp.getAvailableGenerations();
		gen = gensAfter.get(1);
		gotImapTag = false;
		for (PartGeneration pg : gen.parts) {
			if (pg.tag.equals("mail/imap")) {
				gotImapTag = true;
				break;
			}
		}
		assertFalse(gotImapTag);
	}

	/**
	 * @param ref
	 * @return
	 * @throws ServerFault
	 * @throws InterruptedException
	 */
	private TaskStatus track(TaskRef ref) throws ServerFault, InterruptedException {
		ITask taskTracker = getService(ITask.class, "" + ref.id);

		TaskStatus status = null;
		do {
			Thread.sleep(200);
			status = taskTracker.status();
		} while (!status.state.ended);
		List<String> logs = taskTracker.getCurrentLogs(0);
		for (String l : logs) {
			System.out.println("log: " + l);
		}
		System.out.println("result: " + status.result);
		return status;
	}

	protected IServiceProvider fromContext(BmContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx);
	}

	private <T> T getService(BmContext ctx, Class<T> klass, String... params) throws ServerFault {
		return fromContext(ctx).instance(klass, params);
	}

	private <T> T getService(Class<T> klass, String... params) throws ServerFault {
		return fromContext(adminZero).instance(klass, params);
	}

}
