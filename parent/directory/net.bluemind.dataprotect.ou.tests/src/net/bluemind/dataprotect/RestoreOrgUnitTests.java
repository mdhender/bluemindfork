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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.dataprotect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.IDataProtect;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.api.RestorableKind;
import net.bluemind.dataprotect.ou.RestoreOUTask;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.IDomainTemplate;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class RestoreOrgUnitTests {
	private static final boolean RUN_AS_ROOT = System.getProperty("user.name").equals("root");

	private DataProtectGeneration latestGen;
	private String changUid = UUID.randomUUID().toString();
	private BmTestContext testContext;
	private Restorable restorable;
	IOrgUnits ouService;

	static final String domain = "junitou.lan";

	@AfterClass
	public static void afterClass() {
		VertxPlatform.getVertx().close();
	}

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.port", "1143");
	}

	@Before
	public void before() throws Exception {
		prepareLocalFilesystem();

		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server core = new Server();
		core.ip = new BmConfIni().get("node-host");
		core.tags = getTagsExcept(TagDescriptor.bm_es.getTag(), TagDescriptor.mail_imap.getTag(),
				TagDescriptor.bm_pgsql.getTag(), TagDescriptor.bm_pgsql_data.getTag());

		Server imapServer = new Server();
		imapServer.ip = PopulateHelper.FAKE_CYRUS_IP;
		imapServer.tags = Lists.newArrayList(TagDescriptor.mail_imap.getTag(), "mail/archive");

		Server dbServer = new Server();
		dbServer.ip = new BmConfIni().get("host");
		dbServer.tags = Lists.newArrayList(TagDescriptor.bm_pgsql.getTag(), TagDescriptor.bm_pgsql_data.getTag());

		PopulateHelper.initGlobalVirt(false, core, dbServer, imapServer);
		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		PopulateHelper.addDomain(domain, Routing.none);

		testContext = new BmTestContext(SecurityContext.SYSTEM);

		testContext.provider().instance(ISystemConfiguration.class).updateMutableValues(
				Map.of("db_version", "3.1.0", SysConfKeys.dataprotect_skip_datatypes.name(), "sds,sds-spool"));

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
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

		if (!RUN_AS_ROOT) {
			Process p = Runtime.getRuntime().exec(new String[] { "sudo", "chown", "-R", System.getProperty("user.name"),
					":", System.getProperty("user.name"), "/var/spool/bm-hollowed" });
			p.waitFor(10, TimeUnit.SECONDS);
		}
	}

	@Test
	public void testRestoreDeletedOU() throws Exception {
		ouService = testContext.provider().instance(IOrgUnits.class, domain);
		OrgUnit ou = new OrgUnit();
		ou.name = "France";
		ouService.create(changUid, ou);

		restorable = new Restorable();
		restorable.domainUid = domain;
		restorable.entryUid = changUid;
		restorable.kind = RestorableKind.OU;

		doBackup();

		ouService.delete(changUid);

		RestoreOUTask rou = new RestoreOUTask(latestGen, restorable);
		TestMonitor monitor = new TestMonitor();
		rou.run(monitor);

		ItemValue<OrgUnit> restoredOU = ouService.getComplete(changUid);
		assertNotNull(restoredOU);
		assertEquals("France", restoredOU.value.name);
	}

	@Test
	public void testRestoreDeletedSubOU() throws Exception {
		String changSubUid = changUid + "sub";
		ouService = testContext.provider().instance(IOrgUnits.class, domain);
		OrgUnit ou = new OrgUnit();
		ou.name = "France";
		ouService.create(changUid, ou);

		ou = new OrgUnit();
		ou.name = "Toulouse";
		ou.parentUid = changUid;
		ouService.create(changSubUid, ou);

		restorable = new Restorable();
		restorable.domainUid = domain;
		restorable.entryUid = changSubUid;
		restorable.kind = RestorableKind.OU;

		doBackup();

		ouService.delete(changSubUid);
		ouService.delete(changUid);

		RestoreOUTask rou = new RestoreOUTask(latestGen, restorable);
		TestMonitor monitor = new TestMonitor();
		rou.run(monitor);

		ItemValue<OrgUnit> restoredOU = ouService.getComplete(changSubUid);
		assertNotNull(restoredOU);
		assertEquals("Toulouse", restoredOU.value.name);
		assertEquals(changUid, restoredOU.value.parentUid);

		restoredOU = ouService.getComplete(changUid);
		assertNotNull(restoredOU);
		assertEquals("France", restoredOU.value.name);
	}

	private void doBackup() throws Exception {

		TaskRef task = testContext.provider().instance(IDataProtect.class).saveAll();
		track(task);
		List<DataProtectGeneration> generations = testContext.provider().instance(IDataProtect.class)
				.getAvailableGenerations();
		assertTrue(generations.size() > 0);
		latestGen = null;
		for (DataProtectGeneration dpg : generations) {
			if (latestGen == null || dpg.id > latestGen.id) {
				latestGen = dpg;
			}
			System.out.println("On generation " + dpg.id + ", time: " + dpg.protectionTime);
			for (PartGeneration pg : dpg.parts) {
				System.out.println("   * " + pg.server + " " + pg.tag + " saved @ " + pg.end);
			}
		}
	}

	private void track(TaskRef task) throws ServerFault, InterruptedException {
		ITask tracker = testContext.provider().instance(ITask.class, "" + task.id);
		TaskStatus status = tracker.status();
		while (!status.state.ended) {
			status = tracker.status();
			Thread.sleep(500);
		}
		for (String s : tracker.getCurrentLogs(0)) {
			System.out.println(s);
		}

		assertEquals(State.Success, status.state);
	}

}
