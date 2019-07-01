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
package net.bluemind.system.ldap.importation.hooks;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.authentication.provider.IAuthProvider.AuthResult;
import net.bluemind.authentication.provider.IAuthProvider.IAuthContext;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.system.ldap.importation.api.LdapProperties;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;

public class ImportLdapAuthenticationServiceTests {
	private ItemValue<Domain> domain;

	private class AuthContextTest implements IAuthContext {
		private ItemValue<Domain> domain;
		private String realUserLogin;
		private String password;
		private ItemValue<User> userItem;

		public AuthContextTest(ItemValue<Domain> domain) {
			this.domain = domain;
		}

		public AuthContextTest(ItemValue<Domain> domain, String realUserLogin, String password) {
			this.domain = domain;
			this.realUserLogin = realUserLogin;
			this.password = password;
		}

		public AuthContextTest(ItemValue<Domain> domain, ItemValue<User> userItem, String password) {
			this.domain = domain;
			this.userItem = userItem;
			this.password = password;
		}

		@Override
		public SecurityContext getSecurityContext() {
			return null;
		}

		@Override
		public ItemValue<Domain> getDomain() {
			return domain;
		}

		@Override
		public ItemValue<User> getUser() {
			return userItem;
		}

		@Override
		public String getRealUserLogin() {
			return realUserLogin;
		}

		@Override
		public String getUserPassword() {
			return password;
		}
	}

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() {
		LdapDockerTestHelper.initLdapServer();
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		PopulateHelper.initGlobalVirt();

		String domainUid = "test" + System.currentTimeMillis() + ".lan";

		Domain d = Domain.create(domainUid, domainUid + " label", domainUid + " description", Collections.emptySet());
		domain = PopulateHelper.createTestDomain(domainUid, d);

		SecurityContext domainAdmin = BmTestContext
				.contextWithSession("testUser", "test", domainUid, SecurityContext.ROLE_ADMIN).getSecurityContext();
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

		LdapDockerTestHelper.initLdapTree(this.getClass(), testName);
	}

	private void setDomainLdapProperties(ItemValue<Domain> domain, boolean enabled) {
		domain.value.properties.put(LdapProperties.import_ldap_enabled.name(), Boolean.TRUE.toString());
		domain.value.properties.put(LdapProperties.import_ldap_hostname.name(),
				new BmConfIni().get(DockerContainer.LDAP.getName()));
		domain.value.properties.put(LdapProperties.import_ldap_base_dn.name(), "dc=local");
		domain.value.properties.put(LdapProperties.import_ldap_login_dn.name(), "uid=admin,dc=local");
		domain.value.properties.put(LdapProperties.import_ldap_password.name(), "admin");
		domain.value.properties.put(LdapProperties.import_ldap_ext_id_attribute.name(), "entryuuid");
		domain.value.properties.put(LdapProperties.import_ldap_lastupdate.name(), "lastupdate");
	}

	@Test
	public void check_importLdapDisabled() {
		IAuthContext authContext = new AuthContextTest(domain);

		domain.value.properties.remove(LdapProperties.import_ldap_enabled.name());
		AuthResult result = new ImportLdapAuthenticationService().check(authContext);
		assertEquals(AuthResult.UNKNOWN, result);

		setDomainLdapProperties(domain, false);
		result = new ImportLdapAuthenticationService().check(authContext);
		assertEquals(AuthResult.UNKNOWN, result);
	}

	@Test
	public void check_nullAuthContextItemValueUser_nullRealUserLogin() {
		setDomainLdapProperties(domain, true);

		IAuthContext authContext = new AuthContextTest(domain);

		AuthResult result = new ImportLdapAuthenticationService().check(authContext);
		assertEquals(AuthResult.UNKNOWN, result);
	}

	@Test
	public void check_nullAuthContextItemValueUser_realUserLoginNotExists() {
		setDomainLdapProperties(domain, true);

		IAuthContext authContext = new AuthContextTest(domain, "notexist", null);
		AuthResult result = new ImportLdapAuthenticationService().check(authContext);
		assertEquals(AuthResult.UNKNOWN, result);
	}

	@Test
	public void check_nullAuthContextItemValueUser_realUserLoginExists() {
		setDomainLdapProperties(domain, true);

		IAuthContext authContext = new AuthContextTest(domain, "user00", "invalidPassword");
		AuthResult result = new ImportLdapAuthenticationService().check(authContext);
		assertEquals(AuthResult.NO, result);

		authContext = new AuthContextTest(domain, "user00", "test");
		result = new ImportLdapAuthenticationService().check(authContext);
		assertEquals(AuthResult.YES, result);
	}

	@Test
	public void check_notLdapExternalId() {
		setDomainLdapProperties(domain, true);

		ItemValue<User> userItem = ItemValue.create(Item.create("uid", "invalidLdapExternalId"), new User());
		userItem.value.login = "userlogin";

		IAuthContext authContext = new AuthContextTest(domain, userItem, null);
		AuthResult result = new ImportLdapAuthenticationService().check(authContext);
		assertEquals(AuthResult.UNKNOWN, result);
	}

	@Test
	public void check_invalidExternalId() {
		setDomainLdapProperties(domain, true);

		ItemValue<User> userItem = ItemValue.create(Item.create("uid", "ldap://invalidLdapExternalId"), new User());
		userItem.value.login = "userlogin";

		AuthContextTest authContext = new AuthContextTest(domain, userItem, null);
		AuthResult result = new ImportLdapAuthenticationService().check(authContext);
		assertEquals(AuthResult.NO, result);
	}

	@Test
	public void check_ldapFail() {
		setDomainLdapProperties(domain, true);
		domain.value.properties.put(LdapProperties.import_ldap_hostname.name(), "127.0.0.1");

		ItemValue<User> userItem = ItemValue.create(Item.create("uid", "ldap://00000000-0000-0000-0000-000000000000"),
				new User());
		userItem.value.login = "userlogin";

		IAuthContext authContext = new AuthContextTest(domain, userItem, null);
		AuthResult result = new ImportLdapAuthenticationService().check(authContext);
		assertEquals(AuthResult.NO, result);
	}

	@Test
	public void check_guidNotFound() {
		setDomainLdapProperties(domain, true);

		ItemValue<User> userItem = ItemValue.create(Item.create("uid", "ldap://00000000-0000-0000-0000-000000000000"),
				new User());
		userItem.value.login = "userlogin";

		IAuthContext authContext = new AuthContextTest(domain, userItem, null);
		AuthResult result = new ImportLdapAuthenticationService().check(authContext);
		assertEquals(AuthResult.NO, result);
	}

	@Test
	public void check() throws IOException, LdapException {
		setDomainLdapProperties(domain, true);

		ItemValue<User> userItem = ItemValue.create(Item.create("uid", getTestUserEntryUuid(domain)), new User());
		userItem.value.login = "test00";

		IAuthContext authContext = new AuthContextTest(domain, userItem, "test");
		AuthResult result = new ImportLdapAuthenticationService().check(authContext);
		assertEquals(AuthResult.YES, result);

		authContext = new AuthContextTest(domain, userItem, "invalidpassword");
		result = new ImportLdapAuthenticationService().check(authContext);
		assertEquals(AuthResult.NO, result);

		authContext = new AuthContextTest(domain, userItem, "");
		result = new ImportLdapAuthenticationService().check(authContext);
		assertEquals(AuthResult.NO, result);
	}

	private String getTestUserEntryUuid(ItemValue<Domain> domain) throws IOException, LdapException {
		BindRequest bindRequest = new BindRequestImpl();
		bindRequest.setSimple(true);
		bindRequest.setName(domain.value.properties.get(LdapProperties.import_ldap_login_dn.name()));
		bindRequest.setCredentials(domain.value.properties.get(LdapProperties.import_ldap_password.name()));

		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(domain.value.properties.get(LdapProperties.import_ldap_hostname.name()));
		config.setLdapPort(389);
		config.setTrustManagers(new NoVerificationTrustManager());
		config.setUseTls(true);
		config.setUseSsl(false);

		Entry entry;
		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			ldapCon.bind(bindRequest);
			entry = ldapCon.lookup("uid=user00,dc=local", "entryuuid");
		}

		return "ldap://" + entry.get("entryuuid").getString();
	}
}
