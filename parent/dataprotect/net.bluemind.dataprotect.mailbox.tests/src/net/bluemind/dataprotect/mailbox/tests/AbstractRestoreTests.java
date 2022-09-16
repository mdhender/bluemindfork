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
package net.bluemind.dataprotect.mailbox.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.google.common.collect.Lists;

import net.bluemind.aws.s3.utils.S3Configuration;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.ServerHook;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusReplicationHelper;
import net.bluemind.backend.cyrus.replication.testhelper.SyncServerHelper;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
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
import net.bluemind.core.jdbc.JdbcActivator;
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
import net.bluemind.dataprotect.mailbox.tests.ApplyMailboxReplicationObserver.Watcher;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.imap.Acl;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.IDomainTemplate;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class AbstractRestoreTests {
	protected static final boolean RUN_AS_ROOT = System.getProperty("user.name").equals("root");

	static final String password = "changeme";
	protected static final String SHARE_FOLDERS = "Dossiers partagés";

	protected String login;
	protected String mailshareLogin;
	protected String domain;
	protected String latd;

	protected DataProtectGeneration latestGen;
	protected IDataProtect backupApi;
	protected IMailboxes mboxApi;
	protected ItemValue<Mailbox> mbox;
	protected String loginUid;
	protected int inboxMessages = 0;

	protected ServerSideServiceProvider sp;
	protected Server imapServer;
	protected ItemValue<Domain> testDomain;
	protected ItemValue<Mailbox> sharedMbox;
	protected String subFolder;
	protected String subFolderWithSpace;
	protected Restorable restorable;
	protected CyrusService cyrusService;
	protected CyrusReplicationHelper cyrusReplication;

	protected S3Configuration initSdsStore() throws Exception {
		// overridden in MboxRestoreSdsTests
		return null;
	}

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@Before
	public void restoreTestsSetup() throws Exception {
		try {
			setup();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			throw t;
		}
	}

	public void setup() throws Exception {
		login = "chang" + System.currentTimeMillis();
		mailshareLogin = "ms-" + login;
		domain = "junit-" + UUID.randomUUID().toString() + ".lan";
		latd = login + "@" + domain;

		prepareLocalFilesystem();

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
		S3Configuration s3config = initSdsStore();
		setupReplication(cyrusIp);
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		startReplication();
		sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		ItemValue<Domain> domainItem = sp.instance(IDomains.class).get(domain);

		IServer serverService = sp.instance(IServer.class, InstallationId.getIdentifier());

		ItemValue<Server> serverValue = serverService.getComplete(cyrusIp);
		BmContext context = new BmTestContext(SecurityContext.SYSTEM);
		new ServerHook().onServerAssigned(context, serverValue, domainItem, "mail/imap");

		loginUid = PopulateHelper.addUser(login, password, domain, Routing.internal);

		mboxApi = sp.instance(IMailboxes.class, domain);
		mbox = mboxApi.byEmail(latd);
		testDomain = sp.instance(IDomains.class).get(domain);
		assertNotNull(mbox);

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, password)) {
			assertTrue(sc.login());
			for (String fname : Arrays.asList("data/junit.eml", "data/vip.eml")) {
				try (InputStream mail = this.getClass().getClassLoader().getResourceAsStream(fname)) {
					int added = sc.append("INBOX", mail, new FlagsList());
					assertTrue("Appending an email to the mailbox failed.", added > 0);
					inboxMessages += (added > 0 ? 1 : 0);
				}
			}

		}

		IMailboxFolders foldersService = sp.getContext().su("junit-" + UUID.randomUUID().toString(), loginUid, domain)
				.getServiceProvider()
				.instance(IMailboxFoldersByContainer.class, IMailReplicaUids.subtreeUid(domain, mbox));
		for (int i = 0; i < 30; i++) {
			ItemValue<MailboxFolder> inbox = foldersService.byName("INBOX");
			if (inbox != null) {
				break;
			} else {
				Thread.sleep(100);
			}
		}
		String inboxUid = foldersService.byName("INBOX").uid;
		Watcher w = ApplyMailboxReplicationObserver.addWatcher(inboxUid);
		// Inject a good amount of emails in a folder
		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, password)) {
			sc.login();
			for (int i = 0; i < 100; i++) {
				try (InputStream in = new ByteArrayInputStream(("From: Me-" + System.currentTimeMillis()
						+ "@junit.test\r\n\r\nTo you " + i + System.nanoTime() + "\r\n").getBytes())) {
					int added = sc.append("INBOX", in, new FlagsList());
					inboxMessages += (added > 0 ? 1 : 0);
					w.waitForUid(added);
				}
			}
		}
		assertTrue("Replication events not received in time", w.await(2, TimeUnit.MINUTES));

		IMailshare msApi = sp.instance(IMailshare.class, domain);
		Mailshare share = new Mailshare();
		share.name = mailshareLogin;
		Email e = new Email();
		e.address = mailshareLogin + "@" + domain;
		e.isDefault = true;
		share.emails = Arrays.asList(e);
		share.dataLocation = imapServer.ip;
		share.routing = Mailbox.Routing.internal;

		msApi.create(mailshareLogin + "_" + domain, share);
		String containerName = IMailboxAclUids.uidForMailbox(mailshareLogin + "_" + domain);
		IContainerManagement contApi = sp.instance(IContainerManagement.class, containerName);
		System.out.println("********** share the mailshare ***********");
		contApi.setAccessControlList(Arrays.asList(AccessControlEntry.create(loginUid, Verb.All)));

		sharedMbox = mboxApi.byEmail(mailshareLogin + "@" + domain);
		// add email in the mailshare
		List<String> mailshareFolders = Arrays.asList( //
				share.name + "/ms-sub1", //
				share.name + "/ms-sub2", //
				share.name + "/ms-sub1/sub1-sub1", //
				share.name + "/ms-sub2/sub2-sub2", //
				share.name + "/ms-sub1/sub1-sub1/sub1-sub1-sub1"//
		);

		IMailboxFolders mailshareFoldersService = sp.getContext()
				.su("junit-ms-" + UUID.randomUUID().toString(), loginUid, domain).getServiceProvider()
				.instance(IMailboxFoldersByContainer.class, IMailReplicaUids.subtreeUid(domain, sharedMbox));
		String mailshareSentUid = mailshareFoldersService.byName(mailshareLogin + "/Sent").uid;
		Watcher sharedw = ApplyMailboxReplicationObserver.addWatcher(mailshareSentUid);

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			// assertTrue("Create of " + share.name + " failed", sc.create(share.name + "@"
			// + domain));
			assertTrue(sc.setAcl(share.name + "@" + domain, latd, Acl.RW));

			try (InputStream mail = this.getClass().getClassLoader().getResourceAsStream("data/junit.eml")) {
				int added = sc.append(mailshareLogin + "/Sent@" + domain, mail, new FlagsList());
				assertTrue("Unable to add email to " + mailshareLogin + "/Sent@" + domain, added > 0);
				sharedw.waitForUid(added);
				System.out.println("Added email uid: " + added + " to the mailshare");
			}
			for (String fn : mailshareFolders) {
				assertTrue("Unable to create folder " + fn + "@" + domain, sc.create(fn + "@" + domain));
			}
		}
		assertTrue("Replication events not received in time for mailshare", sharedw.await(2, TimeUnit.MINUTES));

		for (String fn : mailshareFolders) {
			waitFolderAvailable(sharedMbox, login, domain, fn, 30, TimeUnit.SECONDS);
		}

		subFolder = "subFolder folder " + System.nanoTime();
		subFolderWithSpace = subFolder + "/this is sub folder";

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, password)) {
			assertTrue(sc.login());
			assertTrue(sc.create(subFolder));
			assertTrue(sc.create(subFolderWithSpace));
		}

		for (String fn : Arrays.asList(subFolder, subFolderWithSpace)) {
			waitFolderAvailable(mbox, login, domain, fn, 30, TimeUnit.SECONDS);
		}

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, password)) {
			assertTrue(sc.login());
			try (InputStream mail = this.getClass().getClassLoader().getResourceAsStream("data/coucou.eml")) {
				int added = sc.append("INBOX", mail, new FlagsList());
				w.waitForUid(added);
				assertTrue("Appending an archived email to the mailbox failed.", added > 0);
				inboxMessages++;
			}
		}

		assertEquals("incorrect number of inbox messages", 103, inboxMessages);
		assertTrue("Replication events not received in time", w.await(30, TimeUnit.SECONDS));

		restorable = new Restorable();
		restorable.domainUid = domain;
		restorable.entryUid = loginUid;
		restorable.kind = RestorableKind.USER;

		testSDSStore(s3config);

		// Still some replication stuff ...
		Thread.sleep(1000);
	}

	protected void waitFolderAvailable(ItemValue<Mailbox> mbox, String login, String domain, String fn, long timeout,
			TimeUnit timeoutUnit) {
		IMailboxFolders folderservice = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext()
				.su("junit-checkfolder-" + UUID.randomUUID().toString(), loginUid, domain).getServiceProvider()
				.instance(IMailboxFoldersByContainer.class, IMailReplicaUids.subtreeUid(domain, mbox));
		long timeoutms = timeoutUnit.toMillis(timeout);
		long waited = 0;
		while (folderservice.byName(fn) == null) {
			System.err.println("Waiting for " + fn + " folder...");
			if (waited > timeoutms) {
				System.err.println("Timeout waiting for folder " + fn);
				System.err.println("Available folders: "
						+ folderservice.all().stream().map(ivmf -> ivmf.value.fullName).collect(Collectors.toList()));
				throw new ServerFault("Timeout waiting for folder " + fn);
			}
			waited += 100;
			try {
				Thread.sleep(100);
			} catch (InterruptedException ie) {
			}
		}
	}

	protected void testSDSStore(S3Configuration config) throws Exception {
		// ok
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
		for (String s : tracker.getCurrentLogs(0)) {
			System.out.println(s);
		}
	}

	protected CyrusReplicationHelper setupReplication(String cyrusIp) {
		cyrusReplication = new CyrusReplicationHelper(cyrusIp);
		cyrusReplication.installReplication();
		JdbcActivator.getInstance().addMailboxDataSource(cyrusReplication.server().uid,
				JdbcTestHelper.getInstance().getMailboxDataDataSource());
		return cyrusReplication;
	}

	protected void startReplication() throws Exception {
		MQ.init().get(30, TimeUnit.SECONDS);
		Topology.get();
		SyncServerHelper.waitFor();
		cyrusReplication.startReplication().get(5, TimeUnit.SECONDS);
	}

	protected void disableCyrusArchive(String cyrusIp) {
		INodeClient nodeClient = NodeActivator.get(cyrusIp);
		try (InputStream in = new ByteArrayInputStream(
				("object_storage_enabled: 0\n" + "archive_enabled: 0\n").getBytes())) {
			nodeClient.writeFile("/etc/cyrus-hsm", in);
			cyrusService.reload();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			throw new ServerFault(e);
		}
	}

	@After
	public void after() throws Exception {
		try {
			TaskRef task = backupApi.forget(latestGen.id);
			track(task);
			JdbcTestHelper.getInstance().afterTest();
		} catch (Exception e) {
		}

		System.err.println("Waiting for last events...");
		cyrusReplication.stopReplication().get(5, TimeUnit.SECONDS);
		JdbcTestHelper.getInstance().afterTest();
		disableCyrusArchive(cyrusService.server().value.ip);
		Thread.sleep(2000);
	}

}
