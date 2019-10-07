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

package net.bluemind.dataprotect.mailbox.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.ServerHook;
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
import net.bluemind.dataprotect.mailbox.internal.MboxRestoreService.Mode;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.imap.Acl;
import net.bluemind.imap.Annotation;
import net.bluemind.imap.Envelope;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.service.common.DefaultFolder;
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
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MboxRestoreServiceTests {
	private static final boolean RUN_AS_ROOT = System.getProperty("user.name").equals("root");

	static final String login = "chang" + System.currentTimeMillis();
	static final String domain = "junit.lan";
	static final String latd = login + "@" + domain;
	private static final String SHARE_FOLDERS = "Dossiers partagés";

	private DataProtectGeneration latestGen;
	private IDataProtect backupApi;
	private IMailboxes mboxApi;
	private ItemValue<Mailbox> mbox;
	private ServerSideServiceProvider sp;
	private Server imapServer;
	private ItemValue<Domain> testDomain;
	private ItemValue<Mailbox> sharedMbox;
	private String subFolder;
	private String subFolderWithSpace;

	@BeforeClass
	public static void oneShotBefore() {
		System.setProperty("es.mailspool.count", "1");
	}

	@Before
	public void before() throws Exception {
		prepareLocalFilesystem();

		JdbcTestHelper.getInstance().beforeTest();
		ElasticsearchTestHelper.getInstance().beforeTest();

		final CountDownLatch cdl = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				cdl.countDown();
			}
		});
		cdl.await();

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

		Server dbServer = new Server();
		dbServer.ip = new BmConfIni().get("host");
		dbServer.tags = Lists.newArrayList("bm/pgsql");

		warmUpNodes(esServer, dbServer, imapServer);

		INodeClient nc = NodeActivator.get(imapServer.address());
		NCUtils.exec(nc, "rm -fr /var/backups/bluemind/dp_spool");
		NCUtils.exec(nc, "rm -fr /var/backups/bluemind/temp");
		NCUtils.exec(nc, "rm -fr /var/backups/bluemind/work");

		PopulateHelper.initGlobalVirt(false, core, esServer, imapServer, dbServer);
		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		PopulateHelper.addDomain(domain, Routing.none);

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
			try (InputStream mail = MboxRestoreService.class.getClassLoader().getResourceAsStream("data/junit.eml")) {
				int added = sc.append("INBOX", mail, new FlagsList());
				System.out.println("Added email uid: " + added);
				assertTrue("Appending an email to the mailbox failed.", added > 0);
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

		// enable cyrus hsm
		System.out.println("Enable cyrus hsm");
		Map<String, String> values = new HashMap<>();
		values.put("archive_kind", "cyrus");
		values.put("archive_days", "7");
		values.put("archive_size_threshold", "100");
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ISystemConfiguration.class)
				.updateMutableValues(values);
		new CyrusService(cyrusIp).reload();
		Thread.sleep(3000);

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, login)) {
			assertTrue(sc.login());
			try (InputStream mail = MboxRestoreService.class.getClassLoader().getResourceAsStream("data/coucou.eml")) {
				int added = sc.append("INBOX", mail, new FlagsList());
				System.out.println("Added archived email uid: " + added);
				assertTrue("Appending an archived email to the mailbox failed.", added > 0);
			}
		}

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

	@After
	public void after() throws Exception {
		try {
			TaskRef task = backupApi.forget(latestGen.id);
			track(task);
			JdbcTestHelper.getInstance().afterTest();
		} catch (Exception e) {
		}
	}

	@Test
	public void testRestoreUserInSubfolder() throws ServerFault, IMAPException {
		MboxRestoreService mbr = new MboxRestoreService();
		assertNotNull(mbr);

		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, mbox, testDomain, Mode.Subfolder, monitor);
		for (String s : monitor.logs) {
			System.out.println("restore: " + s);
		}
		assertTrue(monitor.finished);

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, login)) {
			assertTrue(sc.login());
			boolean found = false;
			ListResult list = sc.listAll();
			for (ListInfo li : list) {
				if (li.getName().startsWith("restored-")) {
					found = true;
				}
			}
			assertTrue("A restore-xxxx directory should exist in the imap hierarchy", found);
		}
	}

	@Test
	public void testRestoreMailshareInSubfolder() throws ServerFault, IMAPException {
		MboxRestoreService mbr = new MboxRestoreService();
		assertNotNull(mbr);

		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, sharedMbox, testDomain, Mode.Subfolder, monitor);
		for (String s : monitor.logs) {
			System.out.println("restore: " + s);
		}
		assertTrue(monitor.finished);

		System.out.println("Login to IMAP as: " + latd);
		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, login)) {
			assertTrue(sc.login());
			boolean found = false;
			ListResult list = sc.listAll();
			for (ListInfo li : list) {
				System.out.println("On " + li.getName());
				if (li.getName().contains("restored-") && li.getName().startsWith(SHARE_FOLDERS)) {
					found = true;
				}
			}
			assertTrue("A restore-xxxx directory should exist in the imap hierarchy", found);
		}
	}

	@Test
	public void testRestoreUserReplace() throws ServerFault, IMAPException {
		MboxRestoreService mbr = new MboxRestoreService();
		assertNotNull(mbr);

		// empty the mailbox
		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, login)) {
			assertTrue(sc.login());
			sc.select("INBOX");
			Collection<Integer> all = sc.uidSearch(new SearchQuery());
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			sc.uidStore(all, fl, true);
			sc.expunge();
			all = sc.uidSearch(new SearchQuery());
			assertTrue("INBOX should be empty after expunge", all.isEmpty());

			sc.deleteMailbox(subFolderWithSpace);
			sc.deleteMailbox(subFolder);
		}

		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, mbox, testDomain, Mode.Replace, monitor);
		for (String s : monitor.logs) {
			System.out.println("restore: " + s);
		}
		assertTrue(monitor.finished);

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, login)) {
			assertTrue(sc.login());
			sc.select("INBOX");
			Collection<Integer> all = sc.uidSearch(new SearchQuery());
			assertFalse("INBOX should not be empty after restore", all.isEmpty());

			Collection<Envelope> allEmails = sc.uidFetchEnvelope(all);
			assertTrue(allEmails.stream().filter(env -> "coucou".equals(env.getSubject())).count() > 0);

			assertTrue(sc.select(subFolder));
			assertTrue(sc.select(subFolderWithSpace));

			DefaultFolder.USER_FOLDERS.forEach(df -> {
				assertTrue(String.format("Folder %s must exixts", df.name), sc.isExist(df.name));

				Annotation annotation = sc.getAnnotation(df.name).get("/specialuse");
				assertNotNull(annotation);
				assertNull(annotation.valueShared);
				assertTrue(df.specialuseEquals(annotation.valuePriv));
			});
		}
	}

	@Test
	public void testRestoreMailshareReplace() throws ServerFault, IMAPException {
		MboxRestoreService mbr = new MboxRestoreService();
		assertNotNull(mbr);

		int size = 0;
		// empty the mailbox
		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			sc.select("chong/Sent@junit.lan");
			Collection<Integer> all = sc.uidSearch(new SearchQuery());
			size = all.size();
			assertTrue(size > 0);
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			sc.uidStore(all, fl, true);
			sc.expunge();
			all = sc.uidSearch(new SearchQuery());
			assertTrue("chong/Sent@junit.lan should be empty after expunge", all.isEmpty());
		}

		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, sharedMbox, testDomain, Mode.Replace, monitor);
		for (String s : monitor.logs) {
			System.out.println("restore: " + s);
		}
		assertTrue(monitor.finished);

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			sc.select("chong/Sent@junit.lan");
			Collection<Integer> all = sc.uidSearch(new SearchQuery());
			assertEquals(size, all.size());
			assertFalse("chong/Sent@junit.lan should not be empty after restore", all.isEmpty());
		}
	}

}
