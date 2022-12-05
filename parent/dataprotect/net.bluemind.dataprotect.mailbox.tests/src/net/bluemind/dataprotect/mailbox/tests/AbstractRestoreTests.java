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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.IDataProtect;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.api.RestorableKind;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
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
import net.bluemind.server.api.Server;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.IDomainTemplate;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUserSubscription;

public class AbstractRestoreTests {
	protected static final boolean RUN_AS_ROOT = System.getProperty("user.name").equals("root");

	static final String password = "changeme";
	protected static final String SHARE_FOLDERS = "Dossiers partagés";

	protected String login;
	protected String mailshareLogin;
	protected String mailshareUid;
	protected String domain;
	protected String latd;

	protected DataProtectGeneration latestGen;
	protected IDataProtect backupApi;
	protected IMailboxes mboxApi;
	protected ItemValue<Mailbox> mbox;
	protected String userUid;
	protected int inboxMessages = 0;

	protected ServerSideServiceProvider sp;
	protected Server imapServer;
	protected ItemValue<Domain> testDomain;
	protected ItemValue<Mailbox> sharedMbox;
	protected String subFolder;
	protected String subFolderWithSpace;
	protected Restorable restorable;

	protected S3Configuration initSdsStore() throws Exception {
		// overridden in MboxRestoreSdsTests
		return null;
	}

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.port", "1143");
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

		imapServer = new Server();
		imapServer.ip = PopulateHelper.FAKE_CYRUS_IP;
		imapServer.tags = Lists.newArrayList("mail/imap", "mail/archive");

		Server dbServer = new Server();
		dbServer.ip = new BmConfIni().get("host");
		dbServer.tags = Lists.newArrayList("bm/pgsql", "bm/pgsql-data");

		warmUpNodes(esServer, dbServer);

//		INodeClient nc = NodeActivator.get(imapServer.address());
//		NCUtils.exec(nc, "rm -fr /var/backups/bluemind/dp_spool");
//		NCUtils.exec(nc, "rm -fr /var/backups/bluemind/temp");
//		NCUtils.exec(nc, "rm -fr /var/backups/bluemind/work");

		PopulateHelper.initGlobalVirt(false, core, esServer, dbServer, imapServer);
		PopulateHelper.addDomainAdmin("admin0", "global.virt");
		PopulateHelper.addDomain(domain, Routing.none);
		S3Configuration s3config = initSdsStore();
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		userUid = PopulateHelper.addUser(login, password, domain, Routing.internal);

		mboxApi = sp.instance(IMailboxes.class, domain);
		mbox = mboxApi.byEmail(latd);
		testDomain = sp.instance(IDomains.class).get(domain);
		assertNotNull(mbox);

		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			System.err.println("LATD: " + latd + " password " + password);
			assertTrue(sc.login());
			for (String fname : Arrays.asList("data/junit.eml", "data/vip.eml")) {
				try (InputStream mail = this.getClass().getClassLoader().getResourceAsStream(fname)) {
					int added = sc.append("INBOX", mail, new FlagsList());
					assertTrue("Appending an email to the mailbox failed.", added > 0);
					inboxMessages += (added > 0 ? 1 : 0);
				}
			}

		}

		IMailboxFolders foldersService = sp.getContext().su("junit-" + UUID.randomUUID().toString(), userUid, domain)
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
		// Inject a good amount of emails in a folder
		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			sc.login();
			for (int i = 0; i < 100; i++) {
				try (InputStream in = new ByteArrayInputStream(("From: Me-" + System.currentTimeMillis()
						+ "@junit.test\r\n\r\nTo you " + i + System.nanoTime() + "\r\n").getBytes())) {
					int added = sc.append("INBOX", in, new FlagsList());
					inboxMessages += (added > 0 ? 1 : 0);
				}
			}
		}

		IMailshare msApi = sp.instance(IMailshare.class, domain);
		Mailshare share = new Mailshare();
		share.name = mailshareLogin;
		Email e = new Email();
		e.address = mailshareLogin + "@" + domain;
		e.isDefault = true;
		share.emails = Arrays.asList(e);
		share.dataLocation = imapServer.ip;
		share.routing = Mailbox.Routing.internal;
		mailshareUid = mailshareLogin + "_" + domain;
		msApi.create(mailshareUid, share);
		String containerName = IMailboxAclUids.uidForMailbox(mailshareLogin + "_" + domain);
		IContainerManagement contMgmtApi = sp.instance(IContainerManagement.class, containerName);
		System.out.println("********** share the mailshare ***********");
		List<AccessControlEntry> accessControlList = new ArrayList<>(contMgmtApi.getAccessControlList());
		accessControlList.add(AccessControlEntry.create(userUid, Verb.All));
		contMgmtApi.setAccessControlList(accessControlList);
		sharedMbox = mboxApi.byEmail(mailshareLogin + "@" + domain);
		// add email in the mailshare
		List<String> mailshareFolders = Arrays.asList( //
				share.name + "/ms-sub1", //
				share.name + "/ms-sub2", //
				share.name + "/ms-sub1/sub1-sub1", //
				share.name + "/ms-sub2/sub2-sub2", //
				share.name + "/ms-sub1/sub1-sub1/sub1-sub1-sub1"//
		);

		IUserSubscription subs = sp.instance(IUserSubscription.class, domain);
		subs.subscribe(userUid, Collections
				.singletonList(ContainerSubscription.create(IMailboxAclUids.uidForMailbox(mailshareUid), true)));

		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			assertTrue(sc.login());
			String sentFolderName = "Dossiers partagés/" + mailshareLogin + "/Sent";
			try (InputStream mail = this.getClass().getClassLoader().getResourceAsStream("data/junit.eml")) {
				int added = sc.append(sentFolderName, mail, new FlagsList());
				assertTrue("Unable to add email to " + sentFolderName, added > 0);
				System.out.println("Added email uid: " + added + " to the mailshare");
			}
			for (String fn : mailshareFolders) {
				String fullFolderName = "Dossiers partagés/" + fn;
				assertTrue("Unable to create folder " + fullFolderName, sc.create(fullFolderName));
			}
		}

		for (String fn : mailshareFolders) {
			waitFolderAvailable(sharedMbox, login, domain, fn, 30, TimeUnit.SECONDS);
		}

		subFolder = "subFolder folder " + System.nanoTime();
		subFolderWithSpace = subFolder + "/this is sub folder";

		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			assertTrue(sc.login());
			assertTrue(sc.create(subFolder));
			assertTrue(sc.create(subFolderWithSpace));
		}

		for (String fn : Arrays.asList(subFolder, subFolderWithSpace)) {
			waitFolderAvailable(mbox, login, domain, fn, 30, TimeUnit.SECONDS);
		}

		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			assertTrue(sc.login());
			try (InputStream mail = this.getClass().getClassLoader().getResourceAsStream("data/coucou.eml")) {
				int added = sc.append("INBOX", mail, new FlagsList());
				assertTrue("Appending an archived email to the mailbox failed.", added > 0);
				inboxMessages++;
			}
		}

		assertEquals("incorrect number of inbox messages", 103, inboxMessages);

		restorable = new Restorable();
		restorable.domainUid = domain;
		restorable.entryUid = userUid;
		restorable.kind = RestorableKind.USER;

		testSDSStore(s3config);

		// Still some replication stuff ...
		Thread.sleep(1000);
	}

	protected void waitFolderAvailable(ItemValue<Mailbox> mbox, String login, String domain, String fn, long timeout,
			TimeUnit timeoutUnit) {
		IMailboxFolders folderservice = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext()
				.su("junit-checkfolder-" + UUID.randomUUID().toString(), userUid, domain).getServiceProvider()
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

		Process p = Runtime.getRuntime().exec("sudo chown -R " + System.getProperty("user.name") + ":"
				+ System.getProperty("user.name") + " /var/spool/bm-hollowed");
		p.waitFor(10, TimeUnit.SECONDS);
	}

	protected void makeBackupFilesReadable() {
		if (!RUN_AS_ROOT) {
			try {
				Process p = Runtime.getRuntime()
						.exec("sudo chown -R " + System.getProperty("user.name") + " /var/backups/bluemind");
				p.waitFor(10, TimeUnit.SECONDS);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace(System.err);
			}
		}
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

	@After
	public void after() throws Exception {
		try {
			TaskRef task = backupApi.forget(latestGen.id);
			track(task);
			JdbcTestHelper.getInstance().afterTest();
		} catch (Exception e) {
		}

		System.err.println("Waiting for last events...");
		JdbcTestHelper.getInstance().afterTest();
		Thread.sleep(2000);
	}

}
