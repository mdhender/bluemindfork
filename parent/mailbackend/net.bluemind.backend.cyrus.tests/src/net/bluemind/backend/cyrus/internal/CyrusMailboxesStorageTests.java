package net.bluemind.backend.cyrus.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.imap.Annotation;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.service.IMailboxesStorage.CheckAndRepairStatus;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.mailbox.service.common.DefaultFolder.Status;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class CyrusMailboxesStorageTests {
	private String imapServerAddress;
	private String domainUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(10, TimeUnit.SECONDS);

		imapServerAddress = new BmConfIni().get("imap-role");
		assertNotNull(imapServerAddress);
		Server imapServer = new Server();
		imapServer.ip = imapServerAddress;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer);

		domainUid = "test-" + System.currentTimeMillis() + ".loc";
		PopulateHelper.createTestDomain(domainUid, imapServer);

		// create domain parititon on cyrus instances
		new CyrusService(imapServerAddress).createPartition(domainUid);
		new CyrusService(imapServerAddress).refreshPartitions(Arrays.asList(domainUid));
		new CyrusService(imapServerAddress).reload();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void userMailboxExists_noneRouting() throws SQLException, ServerFault, IOException {
		String userLogin = "test" + System.nanoTime();
		PopulateHelper.addUser(userLogin, domainUid, Routing.none);

		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist("user/" + userLogin + "@" + domainUid));

			DefaultFolder.USER_FOLDERS.forEach(
					df -> assertTrue(sc.isExist(String.format("user/%s/%s@%s", userLogin, df.name, domainUid))));
		}
	}

	@Test
	public void userMailboxExists_internalRouting() throws SQLException, ServerFault, IOException {
		String userLogin = "test" + System.nanoTime();
		PopulateHelper.addUser(userLogin, domainUid, Routing.internal);

		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist("user/" + userLogin + "@" + domainUid));

			DefaultFolder.USER_FOLDERS.forEach(
					df -> assertTrue(sc.isExist(String.format("user/%s/%s@%s", userLogin, df.name, domainUid))));
		}
	}

	@Test
	public void userMailboxExists_externalRouting() throws SQLException, ServerFault, IOException {
		Map<String, String> domainSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid).get();
		domainSettings.put(DomainSettingsKeys.mail_routing_relay.name(), "mail.routing.tld");
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domainUid)
				.set(domainSettings);

		String userLogin = "test" + System.nanoTime();
		PopulateHelper.addUser(userLogin, domainUid, Routing.external);

		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist("user/" + userLogin + "@" + domainUid));

			DefaultFolder.USER_FOLDERS.forEach(df -> {
				String folderName = String.format("user/%s/%s@%s", userLogin, df.name, domainUid);
				String sharedSeenAnnotation = "/vendor/cmu/cyrus-imapd/sharedseen";
				assertTrue(sc.isExist(folderName));
				sc.getAnnotation(folderName, sharedSeenAnnotation).get(sharedSeenAnnotation);
			});
		}
	}

	@Test
	public void checkAndRepairSharedSeen() throws IOException, IMAPException {

		String userLogin = "testsharedseen." + System.currentTimeMillis();
		String userUid = PopulateHelper.addUser(userLogin, domainUid);

		ItemValue<Mailbox> userMailbox = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, domainUid).getComplete(userUid);

		String strangeFN = "La fille du bédoin... " + System.currentTimeMillis();
		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, userLogin + "@" + domainUid, "password")) {
			assertTrue(sc.login());
			assertTrue(sc.create(strangeFN));
		}

		CyrusMailboxesStorage cms = new CyrusMailboxesStorage();
		int toFix = 0;
		try {
			CheckAndRepairStatus status = cms.checkAndRepairSharedSeen(new BmTestContext(SecurityContext.SYSTEM),
					domainUid, userMailbox, false);
			toFix = status.broken;
		} catch (Exception e) {
			fail(e.getMessage());
		}
		System.err.println("toFix: " + toFix);
		assertEquals(0, toFix);

		StateContext.setState("core.started");
		StateContext.setState("core.upgrade.start");
		StateContext.setState("core.upgrade.end");

		try {
			CheckAndRepairStatus status = cms.checkAndRepairSharedSeen(new BmTestContext(SecurityContext.SYSTEM),
					domainUid, userMailbox, false);
			toFix = status.broken;
		} catch (Exception e) {
			fail(e.getMessage());
		}
		System.err.println("toFix: " + toFix);
		assertEquals(1, toFix);

		try {
			CheckAndRepairStatus status = cms.checkAndRepairSharedSeen(new BmTestContext(SecurityContext.SYSTEM),
					domainUid, userMailbox, true);
			assertTrue(status.checked > 0);
			assertEquals(toFix, status.fixed);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		try {
			CheckAndRepairStatus status = cms.checkAndRepairSharedSeen(new BmTestContext(SecurityContext.SYSTEM),
					domainUid, userMailbox, true);
			assertTrue(status.checked > 0);
			assertEquals("A second repair run should have nothing to do.", 0, status.fixed);
		} catch (Exception e) {
			fail(e.getMessage());
		}

	}

	@Test
	public void checkAndRepairDefaultFolders_checkOk() throws ServerFault, IOException, IMAPException {
		String userLogin = "user." + System.currentTimeMillis();
		String userUid = PopulateHelper.addUser(userLogin, domainUid);

		ItemValue<Mailbox> userMailbox = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, domainUid).getComplete(userUid);

		CyrusMailboxesStorage cms = new CyrusMailboxesStorage();
		Status status = cms.checkAndRepairDefaultFolders(new BmTestContext(SecurityContext.SYSTEM), domainUid,
				userMailbox, false);

		assertTrue(status.isOk());
	}

	@Test
	public void checkAndRepairDefaultFolders_missingDefaultFolder() throws ServerFault, IOException, IMAPException {
		String userLogin = "user." + System.currentTimeMillis();
		String userUid = PopulateHelper.addUser(userLogin, domainUid);

		ItemValue<Mailbox> userMailbox = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, domainUid).getComplete(userUid);

		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.deleteMailbox("user/" + userLogin + "/Sent@" + domainUid).isOk());
			assertTrue(sc.deleteMailbox("user/" + userLogin + "/Outbox@" + domainUid).isOk());
			assertTrue(sc.deleteMailbox("user/" + userLogin + "/Junk@" + domainUid).isOk());
			assertTrue(sc.deleteMailbox("user/" + userLogin + "/Templates@" + domainUid).isOk());

		}

		CyrusMailboxesStorage cms = new CyrusMailboxesStorage();
		Status status = cms.checkAndRepairDefaultFolders(new BmTestContext(SecurityContext.SYSTEM), domainUid,
				userMailbox, false);

		assertFalse(status.isOk());
		assertEquals(0, status.fixed.size());
		assertEquals(0, status.invalidSpecialuse.size());
		assertEquals(4, status.missing.size());
		Set<String> missing = status.missing.stream().map(df -> df.name).collect(Collectors.toSet());
		assertTrue(missing.contains("Sent"));
		assertTrue(missing.contains("Junk"));
		assertTrue(missing.contains("Outbox"));
		assertTrue(missing.contains("Templates"));

		status = cms.checkAndRepairDefaultFolders(new BmTestContext(SecurityContext.SYSTEM), domainUid, userMailbox,
				true);
		assertTrue(status.isOk());
		assertEquals(0, status.missing.size());
		assertEquals(0, status.invalidSpecialuse.size());
		assertEquals(4, status.fixed.size());

		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, userLogin + "@" + domainUid, "password")) {
			assertTrue(sc.login());
			Set<String> allFolders = sc.listAll().stream().map(li -> li.getName()).collect(Collectors.toSet());
			assertTrue(allFolders.contains("Junk"));
			assertTrue(allFolders.contains("Sent"));
			assertTrue(allFolders.contains("Outbox"));
			assertTrue(allFolders.contains("Templates"));
			DefaultFolder.USER_FOLDERS.forEach(df -> {
				assertTrue(sc.isExist(df.name));

				Annotation annotation = sc.getAnnotation(df.name).get("/specialuse");
				assertNotNull(annotation);
				assertNull(annotation.valueShared);
				assertTrue(df.specialuseEquals(annotation.valuePriv));
			});
		}
	}

	@Test
	public void checkAndRepairDefaultFolders_invalidSpecialuseDefaultFolder()
			throws ServerFault, IOException, IMAPException {
		String userLogin = "user." + System.currentTimeMillis();
		String userUid = PopulateHelper.addUser(userLogin, domainUid);

		ItemValue<Mailbox> userMailbox = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, domainUid).getComplete(userUid);

		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.deleteMailbox("user/" + userLogin + "/Sent@" + domainUid).isOk());
			assertTrue(sc.create("user/" + userLogin + "/Sent@" + domainUid));
		}

		CyrusMailboxesStorage cms = new CyrusMailboxesStorage();
		Status status = cms.checkAndRepairDefaultFolders(new BmTestContext(SecurityContext.SYSTEM), domainUid,
				userMailbox, false);

		assertFalse(status.isOk());
		assertEquals(0, status.fixed.size());
		assertEquals(0, status.missing.size());
		assertEquals(1, status.invalidSpecialuse.size());
		assertEquals("Sent", status.invalidSpecialuse.iterator().next().name);

		status = cms.checkAndRepairDefaultFolders(new BmTestContext(SecurityContext.SYSTEM), domainUid, userMailbox,
				true);
		assertTrue(status.isOk());
		assertEquals(0, status.missing.size());
		assertEquals(0, status.invalidSpecialuse.size());
		assertEquals(1, status.fixed.size());
		assertEquals("Sent", status.fixed.iterator().next().name);

		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, userLogin + "@" + domainUid, "password")) {
			assertTrue(sc.login());
			DefaultFolder.USER_FOLDERS.forEach(df -> {
				assertTrue(sc.isExist(df.name));

				Annotation annotation = sc.getAnnotation(df.name).get("/specialuse");
				assertNotNull(annotation);
				assertNull(annotation.valueShared);
				assertTrue(df.specialuseEquals(annotation.valuePriv));
			});
		}
	}

	@Test
	public void checkAndRepairDefaultFolders_missingMailshareDefaultFolder() throws ServerFault, IMAPException {
		String mailshareUid = "mailshare" + System.currentTimeMillis();
		Mailshare mailshare = new Mailshare();
		mailshare.name = mailshareUid;
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domainUid, true));
		mailshare.routing = Routing.internal;

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailshare.class, domainUid)
				.create(mailshareUid, mailshare);

		ItemValue<Mailbox> mbox = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, domainUid).getComplete(mailshareUid);

		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.deleteMailbox(mailshareUid + "/Sent@" + domainUid).isOk());
		}

		CyrusMailboxesStorage cms = new CyrusMailboxesStorage();
		Status status = cms.checkAndRepairDefaultFolders(new BmTestContext(SecurityContext.SYSTEM), domainUid, mbox,
				false);

		assertFalse(status.isOk());
		assertTrue(status.fixed.isEmpty());
		assertTrue(status.invalidSpecialuse.isEmpty());
		assertEquals(1, status.missing.size());
		Set<String> missing = status.missing.stream().map(df -> df.name).collect(Collectors.toSet());
		assertTrue(missing.contains("Sent"));

		status = cms.checkAndRepairDefaultFolders(new BmTestContext(SecurityContext.SYSTEM), domainUid, mbox, true);
		assertTrue(status.isOk());
		assertTrue(status.missing.isEmpty());
		assertTrue(status.invalidSpecialuse.isEmpty());
		assertEquals(1, status.fixed.size());

		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, "admin0", "password")) {
			assertTrue(sc.login());
			Set<String> allFolders = sc.listAll().stream().map(li -> li.getName()).collect(Collectors.toSet());
			assertTrue(allFolders.contains(mailshareUid + "/Sent@" + domainUid));
		}
	}

}
