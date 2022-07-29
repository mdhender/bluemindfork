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
package net.bluemind.system.importation.commons.hooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.authentication.provider.IAuthProvider;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.system.importation.commons.ICoreServices;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.enhancer.IEntityEnhancer;
import net.bluemind.system.importation.commons.managers.UserManager;
import net.bluemind.system.importation.commons.scanner.IImportLogger;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class ImportLoginValidationTests {
	private ItemValue<Domain> domain;
	private IAuthProvider fakeAuthProvider = new IAuthProvider() {
		@Override
		public int priority() {
			return 0;
		}

		@Override
		public AuthResult check(IAuthContext authContext) throws ServerFault {
			return null;
		}
	};

	private class ImportLoginValidationTest extends ImportLoginValidation {
		@Override
		protected boolean mustValidLogin(IAuthProvider authenticationService) {
			fail("Must not be there!");
			return false;
		}

		ItemValue<User> getBMUser(String userLogin, String domainName) {
			fail("Must not be there!");
			return null;
		}

		@Override
		protected void manageUserGroups(ICoreServices build, Parameters ldapParameters, UserManager userManager) {
			fail("Must not be there!");
		}

		@Override
		protected Optional<UserManager> getDirectoryUser(Parameters adParameters, ItemValue<Domain> domain,
				String userLogin) throws ServerFault {
			fail("Must not be there!");
			return Optional.empty();
		}

		@Override
		protected Parameters getDirectoryParameters(ItemValue<Domain> domain, Map<String, String> domainSettings) {
			return Parameters.disabled();
		}
	};

	@Test
	public void ImportLoginValidation_unsupportedAuthProvider() {
		ImportLoginValidation unsupportedAuthProvider = new ImportLoginValidationTest() {
			@Override
			protected boolean mustValidLogin(IAuthProvider authenticationService) {
				return false;
			}
		};

		unsupportedAuthProvider.onValidLogin(fakeAuthProvider, false, null, null, null);
	}

	@Test
	public void ImportLoginValidation_invalidDomainUid() {
		ImportLoginValidation importLoginValidationTest = new ImportLoginValidationTest() {
			@Override
			protected boolean mustValidLogin(IAuthProvider authenticationService) {
				return true;
			}
		};

		try {
			importLoginValidationTest.onValidLogin(fakeAuthProvider, false, null, "invaliddomainuid", null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Domain uid invaliddomainuid not found", sf.getMessage());
		}
	}

	@Test
	public void ImportLoginValidation_userAlreadyExists() {
		ImportLoginValidation importLoginValidationTest = new ImportLoginValidationTest() {
			@Override
			protected boolean mustValidLogin(IAuthProvider authenticationService) {
				return true;
			}

			ItemValue<User> getBMUser(String userLogin, String domainName) {
				return ItemValue.create(Item.create("uid", "externalId"), new User());
			}
		};

		importLoginValidationTest.onValidLogin(fakeAuthProvider, true, "login", domain.uid, null);
	}

	@Test
	public void ImportLoginValidation_userNotFoundInDirectory() {
		ImportLoginValidation importLoginValidationTest = new ImportLoginValidationTest() {
			@Override
			protected boolean mustValidLogin(IAuthProvider authenticationService) {
				return true;
			}

			ItemValue<User> getBMUser(String userLogin, String domainName) {
				return null;
			}

			@Override
			protected Optional<UserManager> getDirectoryUser(Parameters adParameters, ItemValue<Domain> domain,
					String userLogin) throws ServerFault {
				return Optional.empty();
			}
		};

		try {
			importLoginValidationTest.onValidLogin(fakeAuthProvider, false, "login", domain.uid, null);
		} catch (ServerFault sf) {
			assertEquals("Can't find user: login@" + domain.uid + " in directory server", sf.getMessage());
		}
	}

	@Test
	public void ImportLoginValidation_userFoundInDirectory() {
		class UserManagerTestImpl extends UserManager {
			public UserManagerTestImpl(ItemValue<Domain> domain) {
				super(domain, null);
				user = ItemValue.create("" + System.nanoTime(), new User());
				user.value.login = "login-" + System.nanoTime();
				user.value.routing = Routing.internal;
				user.value.contactInfos = new VCard();
				user.value.contactInfos.identification.name.familyNames = "myName";
			}

			@Override
			public List<? extends UuidMapper> getUserGroupsMemberGuid(LdapConnection ldapCon) {
				return Collections.emptyList();
			}

			@Override
			public String getExternalId(IImportLogger importLogger) {
				return null;
			}

			@Override
			protected void setLoginFromDefaultAttribute(IImportLogger importLogger)
					throws LdapInvalidAttributeValueException {
			}

			@Override
			protected void manageArchived() {
			}

			@Override
			protected void setMailRouting() {
			}

			@Override
			protected List<String> getEmails() {
				return Collections.emptyList();
			}

			@Override
			protected Parameters getDirectoryParameters() {
				return null;
			}

			@Override
			protected List<IEntityEnhancer> getEntityEnhancerHooks() {
				return Collections.emptyList();
			}

			@Override
			protected void manageContactInfos() throws LdapInvalidAttributeValueException {
			}

			@Override
			protected void manageQuota(IImportLogger importLogger) throws LdapInvalidAttributeValueException {
			}
		}

		UserManager userManagerTest = new UserManagerTestImpl(domain);

		class ImportLoginValidationFake extends ImportLoginValidationTest {
			public boolean userGroupsManaged = false;

			@Override
			protected boolean mustValidLogin(IAuthProvider authenticationService) {
				return true;
			}

			ItemValue<User> getBMUser(String userLogin, String domainName) {
				return null;
			}

			@Override
			protected void manageUserGroups(ICoreServices build, Parameters ldapParameters, UserManager userManager) {
				userGroupsManaged = true;
			}

			@Override
			protected Optional<UserManager> getDirectoryUser(Parameters adParameters, ItemValue<Domain> domain,
					String userLogin) throws ServerFault {
				return Optional.of(userManagerTest);
			}
		}

		ImportLoginValidationFake importLoginValidationTest = new ImportLoginValidationFake();
		importLoginValidationTest.onValidLogin(fakeAuthProvider, false, "login", domain.uid, null);

		assertNotNull(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain.uid)
				.getComplete(userManagerTest.user.uid));
		assertTrue(importLoginValidationTest.userGroupsManaged);
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		PopulateHelper.initGlobalVirt();

		String domainUid = "test" + System.currentTimeMillis() + ".lan";

		Domain d = Domain.create(domainUid, domainUid + " label", domainUid + " description", Collections.emptySet());
		domain = PopulateHelper.createTestDomain(domainUid, d);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}
}
