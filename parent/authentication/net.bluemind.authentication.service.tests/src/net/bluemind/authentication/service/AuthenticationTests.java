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
package net.bluemind.authentication.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.APIKey;
import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAPIKeys;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.ISecurityToken;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.authentication.api.incore.IInCoreAuthentication;
import net.bluemind.config.Token;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.role.api.DefaultRoles;
import net.bluemind.role.service.IInternalRoles;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class AuthenticationTests {

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = ret -> future.set(null);
		VertxPlatform.spawnVerticles(done);
		future.get();

		Server esServer = new Server();
		esServer.ip = new BmConfIni().get("es-host");
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);

		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.external);

		PopulateHelper.createTestDomain("bm.lan", esServer);
		IDomainSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, "bm.lan");
		Map<String, String> domainSettings = settings.get();
		domainSettings.put(DomainSettingsKeys.mail_routing_relay.name(), "external@test.fr");
		domainSettings.put(DomainSettingsKeys.domain_max_basic_account.name(), "");
		domainSettings.put(DomainSettingsKeys.password_lifetime.name(), "10");
		settings.set(domainSettings);
		PopulateHelper.addDomainAdmin("admin", "bm.lan", Routing.external);
		PopulateHelper.addUser("toto", "bm.lan", Routing.external);
		PopulateHelper.addUser("archived", "bm.lan", Routing.external);
		PopulateHelper.addUser("nomail", "bm.lan", Routing.none);
		PopulateHelper.addSimpleUser("simple", "bm.lan", Routing.external);
		createUserWithEpiredPassword();

		StateContext.setState("reset");
		StateContext.setState("core.started");
	}

	private void createUserWithEpiredPassword() throws SQLException {
		PopulateHelper.addUser("expiredpassword", "bm.lan", Routing.external);

		Connection conn = JdbcTestHelper.getInstance().getDataSource().getConnection();
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(
					"UPDATE t_domain_user SET password_lastchange=now() - interval '10 year' WHERE login='expiredpassword'");
			st.executeUpdate();
		} finally {
			JdbcHelper.cleanup(conn, null, st);
		}
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testLogin() throws Exception {
		initState();

		IAuthentication authentication = getService(null);

		LoginResponse response = authentication.login("admin0@global.virt", "admin", "junit");

		assertEquals(Status.Ok, response.status);
		assertNotNull(response.authKey);

		String authKey = response.authKey;

		response = authentication.login("admin0@global.virt", authKey, "auth-key");
		assertEquals(Status.Ok, response.status);
		assertEquals(authKey, response.authKey);

		response = authentication.login("nomail@bm.lan", "nomail", "junit");
		assertEquals(Status.Ok, response.status);

		response = authentication.login("expiredpassword@bm.lan", "expiredpassword", "junit");
		assertEquals(Status.Expired, response.status);

		response = authentication.login("expiredpassword@bm.lan", "badexpiredpassword", "junit");
		assertEquals(Status.Bad, response.status);

		response = authentication.login("admin0@global.virt", "not_valid", "invalid-junit");
		assertEquals(Status.Bad, response.status);
	}

	private void initState() {
		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
	}

	@Test
	public void testAlias() throws Exception {
		initState();
		IAuthentication authentication = getService(null);

		LoginResponse response = authentication.login("admin-alias@bm.lan", "admin", "junit");

		assertEquals(Status.Ok, response.status);
		assertNotNull(response.authKey);

		String authKey = response.authKey;

		response = authentication.login("admin-alias@bm.lan", authKey, "auth-key");
		assertEquals(Status.Ok, response.status);
		assertEquals(authKey, response.authKey);

		// admin-alias is not allAlias
		response = authentication.login("admin-alias@aliasbm.lan", authKey, "auth-key");
		assertEquals(Status.Bad, response.status);

		response = authentication.login("admin-allalias@bm.lan", authKey, "auth-key");
		assertEquals(Status.Ok, response.status);
		assertEquals(authKey, response.authKey);

		response = authentication.login("admin-allalias@aliasbm.lan", authKey, "auth-key");
		assertEquals(Status.Ok, response.status);
		assertEquals(authKey, response.authKey);

		// user without email, login using alias
		response = authentication.login("admin@aliasbm.lan", authKey, "auth-key");
		assertEquals(Status.Bad, response.status);

		response = authentication.login("admin-alias@bm.lan", "not_valid", "invalid-junit");
		assertEquals(Status.Bad, response.status);

		response = authentication.login("admin-alias-invalid@bm.lan", "not_valid", "invalid-junit");
		assertEquals(Status.Bad, response.status);
	}

	private IAuthentication getService(String sessionId) throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", sessionId)
				.instance(IAuthentication.class);
	}

	private ISecurityToken getTokenService(String sessionId) throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", sessionId).instance(ISecurityToken.class,
				sessionId);
	}

	@Test
	public void testLogout() throws Exception {
		initState();
		IAuthentication authentication = getService(null);
		LoginResponse response = authentication.login("admin0@global.virt", "admin", "junit");
		assertEquals(Status.Ok, response.status);

		// check auth with the authkey
		response = authentication.login("admin0@global.virt", response.authKey, "junit");
		assertEquals(Status.Ok, response.status);

		authentication = getService(response.authKey);
		authentication.logout();
		assertTrue(TestLogoutHook.latch.await(15, TimeUnit.SECONDS));

		authentication = getService(null);
		response = authentication.login("admin0@global.virt", response.authKey, "junit");
		assertEquals(Status.Bad, response.status);
	}

	@Test
	public void testSu() throws Exception {
		initState();
		IAuthentication authentication = getService(null);
		LoginResponse response = authentication.login("admin0@global.virt", "admin", "junit");

		authentication = getService(response.authKey);

		response = authentication.su("admin0@global.virt");
		assertEquals(LoginResponse.Status.Ok, response.status);
		assertNotNull(response.authKey);
	}

	@Test
	public void testSuDomainAlias() throws Exception {
		initState();
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class).setAliases("bm.lan",
				new HashSet<>(Arrays.asList("bm-alias.lan")));

		IAuthentication authentication = getService(null);
		LoginResponse response = authentication.login("admin0@global.virt", "admin", "junit");

		authentication = getService(response.authKey);

		response = authentication.su("toto@bm.lan");
		assertEquals(LoginResponse.Status.Ok, response.status);
		assertNotNull(response.authKey);

		response = authentication.su("toto@bm-alias.lan");
		assertEquals(LoginResponse.Status.Ok, response.status);
		assertNotNull(response.authKey);
	}

	@Test
	public void testApiKey() throws ServerFault {
		initState();

		SecurityContext ctx = new SecurityContext(null, "admin0", Arrays.<String>asList(), Arrays.<String>asList(),
				"global.virt");

		IAPIKeys service = ServerSideServiceProvider.getProvider(ctx).instance(IAPIKeys.class);

		APIKey key = service.create("testApiKey");

		assertNotNull(key);

		IAuthentication authentication = getService(null);
		LoginResponse response = authentication.login("admin0@global.virt", key.sid, "testApiKey");
		assertEquals(Status.Ok, response.status);

		service.delete(key.sid);

		response = authentication.login("admin0@global.virt", key.sid, "testApiKey");
		assertEquals(Status.Bad, response.status);
	}

	@Test
	public void testLoginArchived() throws Exception {
		initState();

		IAuthentication authentication = getService(null);

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				"bm.lan");
		ItemValue<User> archived = userService.byLogin("archived");
		archived.value.archived = true;
		userService.update(archived.uid, archived.value);

		LoginResponse response = authentication.login("archived@bm.lan", "archived", "testLoginArchived");
		assertEquals(Status.Bad, response.status);
		assertNull(response.authKey);
	}

	@Test
	public void testGetCurrentUser() throws Exception {
		initState();

		IAuthentication authentication = getService(null);

		LoginResponse response = authentication.login("toto@bm.lan", "toto", "junit");
		assertEquals(Status.Ok, response.status);
		assertNotNull(response.authKey);

		String authKey = response.authKey;
		AuthUser user = getService(authKey).getCurrentUser();
		assertNotNull(user);
		assertEquals("bm.lan", user.domainUid);
	}

	@Test
	public void testSystemInMaintenanceModeShouldPreventNormalUsersFromAuthenticating() throws Exception {
		initState();
		IAuthentication authentication = getService(null);
		LoginResponse response = authentication.login("toto@bm.lan", "toto", "junit");
		assertEquals(Status.Ok, response.status);

		StateContext.setState("core.upgrade.start");

		response = authentication.login("toto@bm.lan", "toto", "junit");
		assertEquals(Status.Bad, response.status);
	}

	@Test
	public void testSystemInMaintenanceModeShouldStillAllowAdmin0FromAuthenticating() throws Exception {
		initState();
		IAuthentication authentication = getService(null);
		LoginResponse response = authentication.login("toto@bm.lan", "toto", "junit");
		assertEquals(Status.Ok, response.status);

		StateContext.setState("core.upgrade.start");

		response = authentication.login("toto@bm.lan", "toto", "junit");
		assertEquals(Status.Bad, response.status);

		response = authentication.login("admin0@global.virt", Token.admin0(), "junit");
		assertEquals(Status.Ok, response.status);
	}

	@Test
	public void testSimpleUserLogin_Roles() {
		initState();

		IAuthentication authentication = getService(null);
		LoginResponse response = authentication.login("simple@bm.lan", "simple", "testSimpleUserLogin_Roles");

		IInternalRoles roleService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInternalRoles.class);

		assertEquals(Status.Ok, response.status);
		assertEquals(roleService.resolve(DefaultRoles.SIMPLE_USER_DEFAULT_ROLES), response.authUser.roles);
		assertTrue(response.authUser.rolesByOU.isEmpty());
	}

	@Test
	public void testLoginInUsingAliasEmailShouldReturnCorrectUser() throws Exception {
		initState();

		PopulateHelper.addUser("user1", "bm.lan", Routing.external);
		PopulateHelper.addUser("user2", "bm.lan", Routing.external);

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				"bm.lan");
		ItemValue<User> user2 = userService.getComplete("user2");
		user2.value.emails = new ArrayList<>(user2.value.emails);
		user2.value.emails.add(Email.create("user1@aliasbm.lan", false));
		userService.update("user2", user2.value);

		IAuthentication authentication = getService(null);
		LoginResponse response = authentication.login("user1@aliasbm.lan", "user2", "junit");

		assertEquals(Status.Ok, response.status);
		assertEquals(response.authUser.uid, "user2");
	}

	@Test
	public void testPromoteToToken() throws Exception {
		initState();
		IAuthentication authentication = getService(null);
		LoginResponse response = authentication.login("admin0@global.virt", "admin", "junit");
		assertEquals(Status.Ok, response.status);

		ISecurityToken secToken = getTokenService(response.authKey);
		secToken.upgrade();
		String permToken = response.authKey;

		// destroy the volatile auth key
		authentication = getService(response.authKey);
		authentication.logout();

		// verify the token is re-activated after logout
		authentication = getService(permToken);
		AuthUser current = authentication.getCurrentUser();
		assertNotNull(current);
		assertEquals("admin0", current.uid);
		secToken.renew();

		expireSome();

		System.err.println("destroying...");
		secToken.destroy();
		try {
			authentication.ping();
			fail("ping should fail after token is destroyed");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.AUTHENTICATION_FAIL, sf.getCode());
		}
		expireSome();
	}

	private void expireSome() throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Integer> resp = new CompletableFuture<>();
		VertxPlatform.eventBus().request("hollow.tokens.store.expire", new JsonObject(),
				(AsyncResult<Message<Integer>> ar) -> {
					if (ar.succeeded()) {
						resp.complete(ar.result().body());
					} else {
						resp.completeExceptionally(ar.cause());
					}
				});
		int expired = resp.get(10, TimeUnit.SECONDS);
		System.err.println("Expired " + expired);
	}

	@Test
	public void testResetTokens() throws Exception {
		initState();
		IAuthentication authentication = getService(null);
		LoginResponse response = authentication.login("admin0@global.virt", "admin", "junit");
		assertEquals(Status.Ok, response.status);

		ISecurityToken secToken = getTokenService(response.authKey);
		secToken.upgrade();
		String permToken = response.authKey;

		// destroy the volatile auth key
		authentication = getService(response.authKey);
		authentication.logout();

		// verify the token is re-activated after logout
		authentication = getService(permToken);
		AuthUser current = authentication.getCurrentUser();
		assertNotNull(current);
		assertEquals("admin0", current.uid);
		secToken.renew();

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreAuthentication.class)
				.resetTokens();

		authentication = getService(permToken);
		current = authentication.getCurrentUser();
		assertNotNull(current);
		assertEquals("admin0", current.uid);
	}

	@Test
	public void testServiceUsingApiKey() {
		initState();

		SecurityContext ctx = new SecurityContext(null, "admin0", Arrays.<String>asList(), Arrays.<String>asList(),
				"global.virt");
		IAPIKeys service = ServerSideServiceProvider.getProvider(ctx).instance(IAPIKeys.class);
		APIKey key = service.create("testApiKey");

		IAuthentication authentication = getService(key.sid);
		AuthUser current = authentication.getCurrentUser();
		assertNotNull(current);
		assertEquals("admin0", current.uid);

		assertNotNull(key);

	}

}
