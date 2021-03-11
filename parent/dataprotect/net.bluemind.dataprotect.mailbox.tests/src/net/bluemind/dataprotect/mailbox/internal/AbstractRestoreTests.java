/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.dataprotect.mailbox.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.ServerHook;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusReplicationHelper;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.IDataProtect;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.api.RestorableKind;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.imap.Acl;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.locator.LocatorVerticle;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.IDomainTemplate;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.vertx.testhelper.Deploy;

public class AbstractRestoreTests {
	protected static final boolean RUN_AS_ROOT = System.getProperty("user.name").equals("root");

	static final String login = "chang" + System.currentTimeMillis();
	static final String domain = "junit.lan";
	static final String latd = login + "@" + domain;
	protected static final String SHARE_FOLDERS = "Dossiers partagés";

	protected DataProtectGeneration latestGen;
	protected IDataProtect backupApi;
	protected IMailboxes mboxApi;
	protected ItemValue<Mailbox> mbox;
	protected ServerSideServiceProvider sp;
	protected Server imapServer;
	protected ItemValue<Domain> testDomain;
	protected ItemValue<Mailbox> sharedMbox;
	protected String subFolder;
	protected String subFolderWithSpace;
	protected Restorable restorable;
	protected CyrusService cyrusService;

	@After
	public void after() throws Exception {
		try {
			TaskRef task = backupApi.forget(latestGen.id);
			track(task);
			JdbcTestHelper.getInstance().afterTest();
		} catch (Exception e) {
		}
	}

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@Before
	public void before() throws Exception {
		prepareLocalFilesystem();
		Deploy.verticles(false, LocatorVerticle::new).get(5, TimeUnit.SECONDS);

		JdbcTestHelper.getInstance().beforeTest();
		ElasticsearchTestHelper.getInstance().beforeTest();

		Server core = new Server();
		core.ip = new BmConfIni().get("node-host");
		core.tags = getTagsExcept("bm/es", "mail/imap", "bm/pgsql", "bm/pgsql-data");

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		String cyrusIp = new BmConfIni().get("imap-role");
		imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap", "mail/archive");

		ItemValue<Server> cyrusServer = ItemValue.create("imapserver", imapServer);
		cyrusService = new CyrusService(cyrusServer);
		cyrusService.reset();

		Server dbServer = new Server();
		dbServer.ip = new BmConfIni().get("host");
		dbServer.tags = Lists.newArrayList("bm/pgsql", "bm/pgsql-data");

		warmUpNodes(esServer, dbServer, imapServer);

		INodeClient nc = NodeActivator.get(imapServer.address());
		NCUtils.exec(nc, "rm -fr /var/backups/bluemind/dp_spool");
		NCUtils.exec(nc, "rm -fr /var/backups/bluemind/temp");
		NCUtils.exec(nc, "rm -fr /var/backups/bluemind/work");

		PopulateHelper.initGlobalVirt(false, core, esServer, dbServer, imapServer);
		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		PopulateHelper.addDomain(domain, Routing.none);

		setupReplication(cyrusIp);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		startReplication();

		ItemValue<Domain> domainItem = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class).get(domain);

		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		ItemValue<Server> serverValue = serverService.getComplete(cyrusIp);
		BmContext context = new BmTestContext(SecurityContext.SYSTEM);
		new ServerHook().onServerAssigned(context, serverValue, domainItem, "mail/imap");

		String changUid = PopulateHelper.addUser(login, domain, Routing.internal);

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, login)) {
			assertTrue(sc.login());
			for (String fname : Arrays.asList("data/junit.eml", "data/vip.eml")) {
				try (InputStream mail = MboxRestoreService.class.getClassLoader().getResourceAsStream(fname)) {
					int added = sc.append("INBOX", mail, new FlagsList());
					System.out.println("Added email uid: " + added);
					assertTrue("Appending an email to the mailbox failed.", added > 0);
				}
			}

		}
		sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		IMailshare msApi = sp.instance(IMailshare.class, domain);
		Mailshare share = new Mailshare();
		share.name = "chong";
		Email e = new Email();
		e.address = "chong@" + domain;
		e.isDefault = true;
		share.emails = Arrays.asList(e);
		share.dataLocation = imapServer.ip;
		share.routing = Mailbox.Routing.internal;

		msApi.create("chong_" + domain, share);
		String containerName = IMailboxAclUids.uidForMailbox("chong_" + domain);
		IContainerManagement contApi = sp.instance(IContainerManagement.class, containerName);
		System.out.println("********** share the mailshare ***********");
		contApi.setAccessControlList(Arrays.asList(AccessControlEntry.create(changUid, Verb.All)));

		// add email in the mailshare
		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			sc.create(share.name + "@" + domain);
			sc.setAcl(share.name + "@" + domain, latd, Acl.RW);

			try (InputStream mail = MboxRestoreService.class.getClassLoader().getResourceAsStream("data/junit.eml")) {
				int added = sc.append("chong/Sent@junit.lan", mail, new FlagsList());
				System.out.println("Added email uid: " + added + " to the mailshare");
			}
		}

		subFolder = "subFolder folder " + System.nanoTime();
		subFolderWithSpace = subFolder + "/this is sub folder";

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, login)) {
			assertTrue(sc.login());
			assertTrue(sc.create(subFolder));
			assertTrue(sc.create(subFolderWithSpace));
		}

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, login)) {
			assertTrue(sc.login());
			try (InputStream mail = MboxRestoreService.class.getClassLoader().getResourceAsStream("data/coucou.eml")) {
				int added = sc.append("INBOX", mail, new FlagsList());
				System.out.println("Added archived email uid: " + added);
				assertTrue("Appending an archived email to the mailbox failed.", added > 0);
			}
		}
		restorable = new Restorable();
		restorable.domainUid = domain;
		restorable.entryUid = changUid;
		restorable.kind = RestorableKind.USER;
	}

	protected CyrusReplicationHelper setupReplication(String cyrusIp) {
		return null;
	}

	protected void startReplication() throws Exception {
		System.err.println("NOT IMPLEMENTED: startReplication()");
	}

	public void backupAll() throws Exception {
		backupApi = sp.instance(IDataProtect.class);
		TaskRef task = backupApi.saveAll();
		track(task);
		List<DataProtectGeneration> generations = backupApi.getAvailableGenerations();
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
		mboxApi = sp.instance(IMailboxes.class, domain);
		mbox = mboxApi.byEmail(latd);
		sharedMbox = mboxApi.byEmail("chong@" + domain);
		testDomain = sp.instance(IDomains.class).get(domain);
		assertNotNull(mbox);
		System.out.println("The mailbox to work on is: " + mbox.uid);

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

	private void warmUpNodes(Server... nodes) {
		for (Server s : nodes) {
			try {
				System.out.println("WARMUP node on " + s.address() + ", tag: " + s.tags.get(0));
				INodeClient nc = NodeActivator.get(s.address());
				NCUtils.exec(nc, "ls /");
			} catch (Exception e) {
				throw new RuntimeException(
						"Error contacting node on " + s.ip + ", tag: " + s.tags.get(0) + ": " + e.getMessage(), e);
			}
		}
	}

	private void track(TaskRef task) throws ServerFault, InterruptedException {
		ITask tracker = sp.instance(ITask.class, "" + task.id);
		TaskStatus status = tracker.status();
		while (!status.state.ended) {
			status = tracker.status();
			Thread.sleep(500);
		}
		for (String s : tracker.getCurrentLogs()) {
			System.out.println(s);
		}
	}

}
