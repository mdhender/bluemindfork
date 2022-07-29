package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.mail.replica.api.ICyrusValidation;
import net.bluemind.core.api.Email;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class CyrusValidationServiceTests {
	private SecurityContext domainAdminSecurityContext;
	private String backendIp;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		BmConfIni ini = new BmConfIni();
		Server cyrus = new Server();
		cyrus.ip = ini.get("imap-role");
		this.backendIp = cyrus.ip;
		cyrus.tags = Arrays.asList("mail/imap");

		PopulateHelper.initGlobalVirt(cyrus);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		PopulateHelper.addDomain("devenv.blue", Routing.internal);

		IServiceTopology topo = Topology.get();
		assertNotNull(topo);

		IMailboxes mboxApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class,
				"devenv.blue");
		Mailbox leslie = new Mailbox();
		leslie.dataLocation = topo.any("mail/imap").uid;
		leslie.name = "leslie";
		leslie.type = Type.user;
		leslie.routing = Routing.internal;
		leslie.emails = Arrays.asList(Email.create("leslie@devenv.blue", true));
		mboxApi.create("leslie", leslie);

		Mailbox superVision = new Mailbox();
		superVision.dataLocation = topo.any("mail/imap").uid;
		superVision.name = "super.vision";
		superVision.type = Type.mailshare;
		superVision.routing = Routing.internal;
		superVision.emails = Arrays.asList(Email.create("super.vision@devenv.blue", true));
		mboxApi.create("super.vision", superVision);

		System.err.println("******** BEFORE ********");
	}

	@After
	public void after() throws Exception {
		System.err.println("********* AFTER *****");
		JdbcTestHelper.getInstance().afterTest();
	}

	private ICyrusValidation getService(SecurityContext context) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICyrusValidation.class);
	}

	@Test
	public void emailDefaultIsNotValid() {
		ICyrusValidation cli = getService(domainAdminSecurityContext);
		boolean result = cli.prevalidate("default", "");
		assertFalse(result);
	}

	@Test
	public void emailNullIsNotValid() {
		ICyrusValidation cli = getService(domainAdminSecurityContext);
		boolean result = cli.prevalidate(null, "");
		assertFalse(result);
		result = cli.prevalidate(null, null);
		assertFalse(result);
	}

	@Test
	public void subfolderOkwithNullPartition() {
		ICyrusValidation cli = getService(domainAdminSecurityContext);
		boolean result = cli.prevalidate("devenv.blue!user.leslie.Sent", null);
		assertTrue(result);
	}

	@Test
	public void rootFolderInValidPartition() {
		ICyrusValidation cli = getService(domainAdminSecurityContext);
		boolean result = cli.prevalidate("devenv.blue!user.leslie", backendIp + "__devenv_blue");
		assertTrue(result);
	}

	@Test
	public void rootFolderInWrongDomain() {
		ICyrusValidation cli = getService(domainAdminSecurityContext);
		boolean result = cli.prevalidate("devenv.blue!user.leslie", backendIp + "__titi_caca");
		assertFalse(result);
	}

	@Test
	public void rootFolderInInvalidPartition() {
		ICyrusValidation cli = getService(domainAdminSecurityContext);
		boolean result = cli.prevalidate("devenv.blue!user.leslie", "bingo");
		assertFalse(result);
	}

	@Test
	public void sharedRootInValidPartition() {
		ICyrusValidation cli = getService(domainAdminSecurityContext);
		boolean result = cli.prevalidate("devenv.blue!super^vision", backendIp + "__devenv_blue");
		assertTrue(result);
	}

	@Test
	public void sharedRootUnknownBackend() {
		ICyrusValidation cli = getService(domainAdminSecurityContext);
		boolean result = cli.prevalidate("devenv.blue!super^vision", "8.8.8.8__devenv_blue");
		assertFalse(result);
	}

	@Test
	public void sharedSubfolderInValidPartition() {
		ICyrusValidation cli = getService(domainAdminSecurityContext);
		boolean result = cli.prevalidate("devenv.blue!super^vision.Sent", null);
		assertTrue(result);
	}

	@Test
	public void inboxIsNotValid() {
		ICyrusValidation cli = getService(domainAdminSecurityContext);
		boolean result = cli.prevalidate("devenv.blue!user.leslie.INBOX", backendIp + "__devenv_blue");
		assertFalse(result);
	}

	@Test
	public void inboxSubfolderIsValid() {
		ICyrusValidation cli = getService(domainAdminSecurityContext);
		boolean result = cli.prevalidate("devenv.blue!user.leslie.sub.INBOX", backendIp + "__devenv_blue");
		assertTrue(result);
	}
}
