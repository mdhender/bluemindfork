/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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

package net.bluemind.dataprotect.addressbook.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.addressbook.api.VCard.Parameter;
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
import net.bluemind.dataprotect.addressbook.impl.RestoreDomainBooksTask;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.IDataProtect;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.api.RestorableKind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.IDomainTemplate;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class RestoreDomainAddressBooksTaskTests {
	private static final boolean RUN_AS_ROOT = System.getProperty("user.name").equals("root");

	static final String login = "chang";
	static final String domain = "junit.lan";
	static final String latd = login + "@" + domain;

	private DataProtectGeneration latestGen;
	private Server imapServer;
	private BmTestContext testContext;

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
		ElasticsearchTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server core = new Server();
		core.ip = new BmConfIni().get("node-host");
		core.tags = getTagsExcept("bm/es", "mail/imap", "bm/pgsql", "bm/pgsql-data");

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		imapServer = new Server();
		imapServer.ip = PopulateHelper.FAKE_CYRUS_IP;
		imapServer.tags = Lists.newArrayList("mail/imap", "mail/archive");

		Server dbServer = new Server();
		dbServer.ip = new BmConfIni().get("host");
		dbServer.tags = Lists.newArrayList("bm/pgsql", "bm/pgsql-data");

		PopulateHelper.initGlobalVirt(false, core, esServer, dbServer, imapServer);
		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		PopulateHelper.addDomain(domain, Routing.none);

		testContext = new BmTestContext(SecurityContext.SYSTEM);

		PopulateHelper.addUser(login, domain, Routing.internal);
		testContext.provider().instance(ISystemConfiguration.class).updateMutableValues(Map.of("db_version", "3.1.0"));
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

		Process p = Runtime.getRuntime().exec(new String[] { "sudo", "chown", "-R", System.getProperty("user.name"),
				":", System.getProperty("user.name"), "/var/spool/bm-hollowed" });
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

	@Test
	public void testRestoreDeletedAddressBook() throws Exception {
		IAddressBooksMgmt abMgmt = testContext.provider().instance(IAddressBooksMgmt.class, domain);
		abMgmt.create("testAB", AddressBookDescriptor.create("testAB", domain, domain), false);
		DirEntry entry = testContext.provider().instance(IDirectory.class, domain).findByEntryUid("testAB");

		doBackup();
		abMgmt.delete(entry.entryUid);

		Restorable restorable = new Restorable();
		restorable.domainUid = domain;
		restorable.entryUid = entry.entryUid;
		restorable.kind = RestorableKind.ADDRESSBOOK;

		new RestoreDomainBooksTask(latestGen, restorable).run(new TestMonitor());

		AddressBookDescriptor addressBookDescriptor = abMgmt.get(entry.entryUid);
		assertNotNull(addressBookDescriptor);
		assertEquals("testAB", addressBookDescriptor.name);
	}

	@Test
	public void testRestoreAddressBookWithVCardChanges() throws Exception {
		IAddressBooksMgmt abMgmt = testContext.provider().instance(IAddressBooksMgmt.class, domain);
		abMgmt.create("testAB", AddressBookDescriptor.create("testAB", domain, domain), false);
		DirEntry entry = testContext.provider().instance(IDirectory.class, domain).findByEntryUid("testAB");

		IAddressBook addbookapi = testContext.provider().instance(IAddressBook.class, entry.entryUid);
		addbookapi.create("vcard1", defaulVCard());
		addbookapi.create("vcard2", defaulVCard());

		doBackup();

		// update vcard 1
		VCard vcard1 = addbookapi.get("vcard1");
		vcard1.communications.tels.add(Tel.create("0500010203", Arrays.asList(Parameter.create("TYPE", "Cellphone"))));
		addbookapi.update("vcard1", vcard1);
		// remove vcard 2
		addbookapi.delete("vcard2");
		// create vcard 3
		addbookapi.create("vcard3", defaulVCard());

		doBackup();

		Restorable restorable = new Restorable();
		restorable.domainUid = domain;
		restorable.entryUid = entry.entryUid;
		restorable.kind = RestorableKind.ADDRESSBOOK;

		new RestoreDomainBooksTask(latestGen, restorable).run(new TestMonitor());

		AddressBookDescriptor addressBookDescriptor = abMgmt.get(entry.entryUid);
		assertNotNull(addressBookDescriptor);
		assertEquals("testAB", addressBookDescriptor.name);

		List<String> allUids = addbookapi.allUids();
		assertEquals(2, allUids.size());
		List<ItemValue<VCard>> vcardsRestored = addbookapi.multipleGet(allUids);
		vcardsRestored.forEach(c -> {
			if ("vcard1".equals(c.uid)) {
				assertEquals(1, c.value.communications.tels.size());
			} else if ("vcard3".equals(c.uid)) {
				assertTrue(c.value.communications.tels.isEmpty());
			} else {
				fail();
			}
		});
	}

	private VCard defaulVCard() {
		return RestoreUserAddressBooksTaskTests.defaulVCard();
	}

}
