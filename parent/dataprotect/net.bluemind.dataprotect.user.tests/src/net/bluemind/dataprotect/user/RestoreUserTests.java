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

package net.bluemind.dataprotect.user;

import static net.bluemind.mailbox.api.rules.conditions.MailFilterRuleOperatorName.EQUALS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.VCard.Identification.Gender;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
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
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilterEquals;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.IDomainTemplate;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.Tag;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;
import net.bluemind.user.service.IInCoreUser;

public class RestoreUserTests {
	private static final boolean RUN_AS_ROOT = System.getProperty("user.name").equals("root");

	static final String login = "chang";
	static final String domain = "junit.lan";
	static final String latd = login + "@" + domain;

	private DataProtectGeneration latestGen;
	private Server imapServer;
	private String changUid;
	private BmTestContext testContext;
	private Restorable restorable;

	@Before
	public void before() throws Exception {
		prepareLocalFilesystem();

		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		ElasticsearchTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server core = new Server();
		core.ip = new BmConfIni().get("node-host");
		core.tags = getTagsExcept("bm/es", "mail/imap", "bm/pgsql", "bm/pgsql-data");

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		String cyrusIp = new BmConfIni().get("imap-role");
		imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		Server dbServer = new Server();
		dbServer.ip = new BmConfIni().get("host");
		dbServer.tags = Lists.newArrayList("bm/pgsql", "bm/pgsql-data");

		PopulateHelper.initGlobalVirt(false, core, esServer, dbServer, imapServer);
		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		PopulateHelper.addDomain(domain, Routing.none);

		testContext = new BmTestContext(SecurityContext.SYSTEM);

		changUid = PopulateHelper.addUser(login, domain, Routing.internal);
		testContext.provider().instance(ISystemConfiguration.class)
				.updateMutableValues(ImmutableMap.of("db_version", "3.1.0"));

		restorable = new Restorable();
		restorable.domainUid = domain;
		restorable.entryUid = changUid;
		restorable.kind = RestorableKind.USER;

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

	@Test(timeout = 120000)
	public void testRestoreDeletedUser() throws Exception {
		doBackup();
		TaskRef tr = testContext.provider().instance(IUser.class, domain).delete(changUid);
		track(tr);
		RestoreUserTask ru = new RestoreUserTask(latestGen, restorable);
		TestMonitor monitor = new TestMonitor();
		ru.run(monitor);

		ItemValue<User> restoredUser = testContext.provider().instance(IUser.class, domain).getComplete(changUid);
		assertNotNull(restoredUser);

		assertTrue("restore failed", monitor.success);

		// testUser password = testUser login
		IInCoreUser userServerService = testContext.provider().instance(IInCoreUser.class, domain);
		assertTrue(userServerService.checkPassword(restoredUser.value.login, restoredUser.value.login));
	}

	@Test(timeout = 120000)
	public void testRestoreRecreatedUser() throws Exception {
		doBackup();
		ItemValue<User> user = testContext.provider().instance(IUser.class, domain).getComplete(changUid);
		TaskRef tr = testContext.provider().instance(IUser.class, domain).delete(changUid);
		track(tr);

		user.value.password = "newpassword";
		testContext.provider().instance(IUser.class, domain).create("newuid", user.value);

		RestoreUserTask ru = new RestoreUserTask(latestGen, restorable);
		TestMonitor monitor = new TestMonitor();
		ru.run(monitor);

		ItemValue<User> restoredUser = testContext.provider().instance(IUser.class, domain).getComplete(changUid);
		assertNull(restoredUser);

		assertTrue("restore failed", monitor.success);

		// testUser password = testUser login
		IInCoreUser userServerService = testContext.provider().instance(IInCoreUser.class, domain);
		assertTrue(userServerService.checkPassword(user.value.login, user.value.login));
	}

	@Test(timeout = 120000)
	public void testRestoreExistantUser() throws Exception {
		doBackup();
		ItemValue<User> restoredUser = testContext.provider().instance(IUser.class, domain).getComplete(changUid);
		restoredUser.value.contactInfos.identification.gender = Gender.create("bla", "bla");
		testContext.provider().instance(IUser.class, domain).update(changUid, restoredUser.value);

		RestoreUserTask ru = new RestoreUserTask(latestGen, restorable);
		TestMonitor monitor = new TestMonitor();
		ru.run(monitor);

		restoredUser = testContext.provider().instance(IUser.class, domain).getComplete(changUid);
		assertNotNull(restoredUser);
		assertNull(restoredUser.value.contactInfos.identification.gender.value);

		assertTrue("restore failed", monitor.success);
	}

	@Test(timeout = 120000)
	public void testRestoreSettings() throws Exception {

		IUserSettings userSettingService = testContext.provider().instance(IUserSettings.class, domain);
		Map<String, String> settings = userSettingService.get(changUid);
		settings.put("working_days", "wed");
		userSettingService.set(changUid, settings);

		doBackup();
		TaskRef tr = testContext.provider().instance(IUser.class, domain).delete(changUid);
		track(tr);

		RestoreUserTask ru = new RestoreUserTask(latestGen, restorable);
		TestMonitor monitor = new TestMonitor();
		ru.run(monitor);

		ItemValue<User> restoredUser = testContext.provider().instance(IUser.class, domain).getComplete(changUid);
		assertNotNull(restoredUser);
		settings = userSettingService.get(changUid);
		assertEquals("wed", settings.get("working_days"));

		assertTrue("restore failed", monitor.success);
	}

	@Test(timeout = 120000)
	public void testRestoreTags() throws Exception {

		ITags tagsService = testContext.provider().instance(ITags.class, "tags_" + changUid);
		tagsService.create("testRestoreUserTags", Tag.create("string", "color"));
		assertEquals(1, tagsService.all().size());

		doBackup();
		TaskRef tr = testContext.provider().instance(IUser.class, domain).delete(changUid);
		track(tr);

		assertEquals(0, tagsService.all().size()); // ensure tag is deleted

		RestoreUserTask ru = new RestoreUserTask(latestGen, restorable);
		TestMonitor monitor = new TestMonitor();
		ru.run(monitor);

		ItemValue<User> restoredUser = testContext.provider().instance(IUser.class, domain).getComplete(changUid);
		assertNotNull(restoredUser);

		tagsService = testContext.provider().instance(ITags.class, "tags_" + changUid);
		assertEquals(1, tagsService.all().size());
		ItemValue<Tag> tag = tagsService.getComplete("testRestoreUserTags");
		assertEquals("string", tag.value.label);
		assertEquals("color", tag.value.color);

		assertTrue("restore failed", monitor.success);
	}

	@Test(timeout = 120000)
	public void testRestoreSieve() throws Exception {
		IMailboxes mboxesService = testContext.provider().instance(IMailboxes.class, domain);

		MailFilterRule rule = new MailFilterRule();
		rule.active = true;
		rule.conditions.add(MailFilterRuleCondition.equal("subject", "bang"));
		rule.addMove("bang-bang");
		MailFilter filter = MailFilter.create(rule);
		mboxesService.setMailboxFilter(changUid, filter);

		doBackup();
		TaskRef tr = testContext.provider().instance(IUser.class, domain).delete(changUid);
		track(tr);

		RestoreUserTask ru = new RestoreUserTask(latestGen, restorable);
		TestMonitor monitor = new TestMonitor();
		ru.run(monitor);

		ItemValue<User> restoredUser = testContext.provider().instance(IUser.class, domain).getComplete(changUid);
		assertNotNull(restoredUser);

		filter = mboxesService.getMailboxFilter(changUid);
		assertNotNull(filter);
		assertEquals(1, filter.rules.size());
		rule = filter.rules.get(0);
		assertTrue(rule.active);
		assertEquals("subject", rule.conditions.get(0).filter.fields.get(0));
		assertEquals(EQUALS, rule.conditions.get(0).filter.operator);
		MailFilterRuleFilterEquals equals = (MailFilterRuleFilterEquals) rule.conditions.get(0).filter;
		assertEquals("bang", equals.values.get(0));
		assertEquals("bang-bang", rule.move().map(move -> move.folder()).orElse(null));

		assertTrue("restore failed", monitor.success);
	}
}
