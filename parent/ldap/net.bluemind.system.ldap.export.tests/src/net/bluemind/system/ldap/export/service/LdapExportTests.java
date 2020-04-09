package net.bluemind.system.ldap.export.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.junit.Before;

import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.ldap.export.LdapHelper;
import net.bluemind.system.ldap.export.verticle.LdapExportVerticle;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class LdapExportTests {
	public static final String LDAPTAG = "directory/bm-master";

	protected ItemValue<Domain> domain;
	protected ItemValue<Server> ldapRoleServer;

	@Before
	public void before() throws Exception {
		LdapExportVerticle.suspended = true;

		JdbcTestHelper.getInstance().beforeTest();

		PopulateHelper.initGlobalVirt();

		String domainUid = "test" + System.currentTimeMillis() + ".lan";

		SecurityContext domainAdmin = BmTestContext
				.contextWithSession("testUser", "test", domainUid, SecurityContext.ROLE_ADMIN).getSecurityContext();

		domain = PopulateHelper.createTestDomain(domainUid);
		PopulateHelper.domainAdmin(domainUid, domainAdmin.getSubject());

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		ldapRoleServer = initAndAssignLdapExportServer(domain);
	}

	public ItemValue<Server> initAndAssignLdapExportServer(ItemValue<Domain> domain)
			throws ServerFault, SQLException, InterruptedException {
		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		String ldapRoleServerUid = UUID.randomUUID().toString();
		Server server = new Server();
		server.name = "LDAP export server";
		server.ip = new BmConfIni().get("bluemind/ldap");
		waitFor(serverService.create(ldapRoleServerUid, server));

		ItemValue<Server> ldapRoleServer = serverService.getComplete(ldapRoleServerUid);

		INodeClient nodeClient = NodeActivator.get(ldapRoleServer.value.ip);
		updateUserPassword(nodeClient, "admin0@global.virt", Token.admin0());

		waitFor(serverService.setTags(ldapRoleServerUid, Arrays.asList(LDAPTAG)));

		serverService.assign(ldapRoleServerUid, domain.uid, LDAPTAG);
		// Wait for domain assign ending
		Thread.sleep(1000);

		return ldapRoleServer;
	}

	protected void waitFor(TaskRef taskRef) {
		ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, taskRef.id);
		while (!task.status().state.ended) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		assertEquals(State.Success, task.status().state);
	}

	private void updateUserPassword(INodeClient nodeClient, String login, String passwd) {
		NCUtils.exec(nodeClient, "/usr/local/sbin/updateUserPassword.sh " + login + " " + passwd);
	}

	protected Entry getUserEntry(String userUid) throws ServerFault, LdapException, CursorException {
		EntryCursor result = LdapHelper.connectDirectory(ldapRoleServer).search("dc=local",
				String.format("(bmuid=%s)", userUid), SearchScope.SUBTREE, "*");

		assertTrue(result.next());
		Entry user = result.get();
		assertFalse(result.next());

		return user;
	}
}
