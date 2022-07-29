package net.bluemind.backend.cyrus.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.config.Token;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.Acl;
import net.bluemind.imap.Annotation;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MailboxOpsTests {
	private String imapServerAddress;
	private String domainUid;

	@Before
	public void before() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

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
	public void createUserFolders() throws ServerFault, IOException, IMAPException {
		String userLogin = "user." + System.currentTimeMillis();
		PopulateHelper.addUser(userLogin, domainUid);

		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			Map<String, Acl> acl = sc.listAcl("user/" + userLogin + "@" + domainUid);
			assertEquals(2, acl.size());
			acl.values().forEach(a -> assertEquals(Acl.ALL.toString(), a.toString()));

			ListResult folders = sc.listSubFoldersMailbox("user/" + userLogin + "@" + domainUid);
			assertEquals(DefaultFolder.USER_FOLDERS_NAME.size(), folders.size());

			folders.forEach(f -> {
				try {
					Map<String, Acl> acls = sc.listAcl(f.getName());
					assertEquals(2, acls.size());

					acls.entrySet().forEach(e -> assertEquals(Acl.ALL.toString(), e.getValue().toString()));
				} catch (IMAPException imape) {
					fail("Test thrown an exception: " + imape.getMessage());
				}
			});
		}

		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, userLogin + "@" + domainUid, "password")) {
			assertTrue(sc.login());

			DefaultFolder.USER_FOLDERS.forEach(df -> {
				Annotation annotation = sc.getAnnotation(df.name).get("/specialuse");
				assertNotNull(annotation);
				assertNull(annotation.valueShared);
				assertTrue(df.specialuseEquals(annotation.valuePriv));
			});
		}
	}

	@Test
	public void createMailshareFolders() throws ServerFault, IOException, IMAPException {
		String mailshareName = "mailshare." + System.currentTimeMillis();

		Mailshare m = new Mailshare();
		m.name = mailshareName;
		m.routing = Routing.internal;
		m.dataLocation = imapServerAddress;
		m.emails = Arrays.asList(Email.create(mailshareName + "@" + domainUid, true, false));
		m.card = new VCard();

		String mailshareUid = UUID.randomUUID().toString();

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailshare.class, domainUid)
				.create(mailshareUid, m);

		ItemValue<Mailbox> miv = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, domainUid).getComplete(mailshareUid);
		Map<String, Acl> acls = new HashMap<>();
		acls.put("anyone", Acl.POST);
		acls.put("admin0", Acl.ALL);
		acls.put("toto@" + domainUid, Acl.RW);
		MailboxOps.setAcls(miv, domainUid, acls);

		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			acls = sc.listAcl(mailshareName + "@" + domainUid);
			assertEquals(3, acls.size());
			assertEquals(Acl.POST.toString(), acls.get("anyone").toString());
			assertEquals(Acl.ALL.toString(), acls.get("admin0").toString());
			assertEquals(Acl.RW.toString(), acls.get("toto@" + domainUid).toString());

			ListResult folders = sc.listSubFoldersMailbox(mailshareName + "@" + domainUid);
			assertEquals(DefaultFolder.MAILSHARE_FOLDERS_NAME.size(), folders.size());

			folders.forEach(f -> {
				try {
					Map<String, Acl> subAcls = sc.listAcl(f.getName());
					assertEquals(3, subAcls.size());
					assertEquals(Acl.POST.toString(), subAcls.get("anyone").toString());
					assertEquals(Acl.ALL.toString(), subAcls.get("admin0").toString());
					assertEquals(Acl.RW.toString(), subAcls.get("toto@" + domainUid).toString());
				} catch (IMAPException imape) {
					fail("Test thrown an exception: " + imape.getMessage());
				}
			});
		}
	}
}
