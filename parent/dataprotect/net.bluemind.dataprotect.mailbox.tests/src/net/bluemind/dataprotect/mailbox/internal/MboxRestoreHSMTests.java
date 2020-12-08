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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.backend.cyrus.ServerHook;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
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
import net.bluemind.hsm.api.Demote;
import net.bluemind.hsm.api.IHSM;
import net.bluemind.hsm.api.TierChangeResult;
import net.bluemind.hsm.processor.HSMContext;
import net.bluemind.hsm.processor.HSMContext.HSMLoginContext;
import net.bluemind.hsm.processor.HSMRunStats;
import net.bluemind.hsm.storage.impl.SnappyStore;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.InternalDate;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.IDomainTemplate;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class MboxRestoreHSMTests {
	private static final boolean RUN_AS_ROOT = System.getProperty("user.name").equals("root");

	static final String login = "changhsm" + System.currentTimeMillis();
	static final String domain = "junit.lan";
	static final String latd = login + "@" + domain;

	private DataProtectGeneration latestGen;
	private IDataProtect backupApi;
	private IMailboxes mboxApi;
	private ItemValue<Mailbox> mbox;
	private ServerSideServiceProvider sp;
	private Server imapServer;
	private ItemValue<Domain> testDomain;
	private String hsmId;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@Before
	public void before() throws Exception {
		prepareLocalFilesystem();

		ElasticsearchTestHelper.getInstance().beforeTest();

		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

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
		BmContext ctx = new BmTestContext(SecurityContext.SYSTEM);
		new ServerHook().onServerAssigned(ctx, serverValue, domainItem, "mail/imap");

		String changUid = PopulateHelper.addUser(login, domain, Routing.internal);

		int toDemote = 0;
		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, login)) {
			assertTrue(sc.login());
			try (InputStream mail = MboxRestoreService.class.getClassLoader().getResourceAsStream("data/junit.eml")) {
				toDemote = sc.append("INBOX", mail, new FlagsList());
				System.out.println("Added email uid to demote: " + toDemote);
				assertTrue("Appending an email to the mailbox failed.", toDemote > 0);
			}
		}

		// fuk
		Demote demote = Demote.create(changUid, "INBOX", toDemote);
		BmContext bmCtx = BmTestContext.contextWithSession("sid3" + System.currentTimeMillis(), login + "@" + domain,
				domain);

		HSMContext context = getHSMContext(bmCtx, changUid);
		TierChangeResult tcr = demote(context, "INBOX", Arrays.asList(demote)).get(0);
		hsmId = tcr.hsmId;
		sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

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
	public void testRestoreUserReplaceWithHSM() throws Exception {
		SecurityContext changSC = new SecurityContext("sess", mbox.uid, Arrays.<String>asList(),
				Arrays.<String>asList(), domain);
		IHSM hsmApi = ServerSideServiceProvider.getProvider(changSC).instance(IHSM.class, domain);

		try {
			hsmApi.fetch(mbox.uid, hsmId);
		} catch (ServerFault sf) {
			fail();
		}

		// delete archived email
		SnappyStore store = new SnappyStore();
		store.open(NodeActivator.get(new BmConfIni().get("imap-role")));
		store.delete(domain, mbox.uid, hsmId);
		try {
			hsmApi.fetch(mbox.uid, hsmId);
			fail();
		} catch (ServerFault sf) {
		}

		// restore
		MboxRestoreService mbr = new MboxRestoreService();
		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, mbox, testDomain, Mode.Replace, monitor);
		for (String s : monitor.logs) {
			System.out.println("restore: " + s);
		}
		assertTrue(monitor.finished);

		// check archived email is restored
		try {
			hsmApi.fetch(mbox.uid, hsmId);
		} catch (ServerFault sf) {
			fail();
		}

	}

	private List<TierChangeResult> demote(HSMContext context, String folderPath, Collection<Demote> demote) {
		try (StoreClient sc = context.connect(folderPath)) {

			InternalDate[] ids = sc
					.uidFetchInternalDate(demote.stream().map(d -> d.imapId).collect(Collectors.toList()));
			if (ids.length == 0) {
				throw new ServerFault("The mail id " + demote + " to demote was not found in folder " + folderPath);
			}

			List<InternalDate> dates = new ArrayList<InternalDate>(ids.length);
			Collections.addAll(dates, ids);
			DemoteCommand dc = new DemoteCommand(folderPath, sc, context, dates, Optional.empty());
			HSMRunStats stats = new HSMRunStats();
			return dc.run(stats);

		} catch (IMAPException | IOException e) {
			throw new ServerFault(e);
		}
	}

	private HSMContext getHSMContext(BmContext bmContext, String userUid) throws ServerFault {

		ItemValue<User> user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, bmContext.getSecurityContext().getContainerUid()).getComplete(userUid);
		ItemValue<Server> server = bmContext.su().provider().instance(IServer.class, InstallationId.getIdentifier())
				.getComplete(user.value.dataLocation);

		HSMLoginContext loginContext = new HSMLoginContext(user.value.login, user.uid, server.value.address());
		return HSMContext.get(bmContext.getSecurityContext(), loginContext);
	}

}
