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
package net.bluemind.addressbook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.server.api.Server;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper.DeleteTreeException;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class AddressBooksMgmtTests {
	private SecurityContext domainAdmin;
	private String domainUid;
	private BmContext testContext;
	private SecurityContext dummy;
	private SecurityContext dummy2;

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() {
		System.err.println("BEFORE_CLASS...........");
		LdapDockerTestHelper.initLdapServer();
		System.err.println("AFTER_CLASS...........");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");
		PopulateHelper.initGlobalVirt(esServer);

		domainUid = "test" + System.currentTimeMillis() + ".lan";

		domainAdmin = BmTestContext.contextWithSession("testUser", "test", domainUid, SecurityContext.ROLE_ADMIN)
				.getSecurityContext();

		PopulateHelper.createTestDomain(domainUid, esServer);
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

		dummy = BmTestContext.contextWithSession("dummy", PopulateHelper.addUser("dummy", domainUid), domainUid)
				.getSecurityContext();
		dummy2 = BmTestContext.contextWithSession("dummy2", PopulateHelper.addUser("dummy2", domainUid), domainUid)
				.getSecurityContext();

		testContext = new BmTestContext(SecurityContext.SYSTEM);

	}

	@Test
	public void testCreate_Domain() throws ServerFault, SQLException {
		String abUid = "testdomab" + System.currentTimeMillis();
		IAddressBooksMgmt service = service(domainAdmin);
		service.create(abUid, AddressBookDescriptor.create("test", domainUid, domainUid), false);
		AddressBookDescriptor book = service.getComplete(abUid);
		assertNotNull(book);
		assertEquals(domainUid, book.domainUid);
		assertEquals(abUid, book.owner);
		assertEquals("test", book.name);

		// check dir entry is there
		IDirectory dir = testContext.provider().instance(IDirectory.class, domainUid);
		DirEntry entry = dir.findByEntryUid(abUid);
		assertNotNull(entry);
		assertEquals(DirEntry.Kind.ADDRESSBOOK, entry.kind);
		assertEquals("test", entry.displayName);

		abUid = "testdomab" + System.currentTimeMillis();
		service = service(dummy);
		try {
			service.create(abUid, AddressBookDescriptor.create("test", domainUid, domainUid), false);
			fail("should not be possible");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testDelete_Domain() throws ServerFault, SQLException {
		String abUid = "testdomab" + System.currentTimeMillis();
		IAddressBooksMgmt service = service(domainAdmin);
		service.create(abUid, AddressBookDescriptor.create("test", domainUid, domainUid), false);

		DataSource bookDS = DataSourceRouter.get(testContext, abUid);
		ContainerStore cs = new ContainerStore(null, bookDS, domainAdmin);
		assertNotNull("container " + abUid + " must exist at this point", cs.get(abUid));

		service.delete(abUid);
		assertNull(cs.get(abUid));

		// dummy cannot do it
		abUid = "testdomab" + System.currentTimeMillis();
		service = service(domainAdmin);
		service.create(abUid, AddressBookDescriptor.create("test", domainUid, domainUid), false);
		bookDS = DataSourceRouter.get(testContext, abUid);
		cs = new ContainerStore(null, bookDS, domainAdmin);
		assertNotNull("container " + abUid + " must exist at this point", cs.get(abUid));

		service = service(dummy);
		try {
			service.delete(abUid);
			fail("should not be possible");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
		assertNotNull("container " + abUid + " must exist at this point", cs.get(abUid));

	}

	@Test
	public void testCreate_User() throws ServerFault, SQLException {
		String abUid = "testdomab" + System.currentTimeMillis();
		IAddressBooksMgmt service = service(domainAdmin);
		service.create(abUid, AddressBookDescriptor.create("test", dummy.getSubject(), domainUid), false);
		AddressBookDescriptor book = service.getComplete(abUid);
		assertNotNull(book);
		assertEquals(domainUid, book.domainUid);
		assertEquals(dummy.getSubject(), book.owner);
		assertEquals("test", book.name);

		// check dir entry is not there
		IDirectory dir = testContext.provider().instance(IDirectory.class, domainUid);
		DirEntry entry = dir.findByEntryUid(abUid);
		assertNull(entry);

		abUid = "testdomab" + System.currentTimeMillis();
		service = service(dummy);
		// dummy can do it
		service.create(abUid, AddressBookDescriptor.create("test", dummy.getSubject(), domainUid), false);
		abUid = "testdomab" + System.currentTimeMillis();
		service = service(dummy);

		try {
			service.create(abUid, AddressBookDescriptor.create("test", dummy2.getSubject(), domainUid), false);
			fail("should not be possible");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testDelete_User() throws ServerFault, SQLException {
		String abUid = "testdomab" + System.currentTimeMillis();
		IAddressBooksMgmt service = service(dummy);
		service.create(abUid, AddressBookDescriptor.create("test", dummy.getSubject(), domainUid), false);

		service.delete(abUid);

		ContainerStore cs = new ContainerStore(null, JdbcTestHelper.getInstance().getMailboxDataDataSource(),
				domainAdmin);
		assertNull(cs.get(abUid));

		// dummy2 cannot do it
		abUid = "testdomab" + System.currentTimeMillis();
		service.create(abUid, AddressBookDescriptor.create("test", dummy.getSubject(), domainUid), false);
		service = service(dummy2);
		try {
			service.delete(abUid);
			fail("should not be possible");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
		cs = new ContainerStore(null, JdbcTestHelper.getInstance().getMailboxDataDataSource(), domainAdmin);
		assertNotNull(cs.get(abUid));

	}

	@Test
	public void testABSettings() throws ServerFault {
		String abUid = "testABSettings" + System.currentTimeMillis();
		IAddressBooksMgmt service = service(domainAdmin);

		Map<String, String> settings = new HashMap<String, String>();
		settings.put("type", "abType");
		settings.put("prop", "val");

		AddressBookDescriptor bookDescriptor = AddressBookDescriptor.create("test", domainUid, domainUid, settings);

		service.create(abUid, bookDescriptor, false);
		AddressBookDescriptor book = service.getComplete(abUid);
		assertNotNull(book);
		assertEquals(2, book.settings.size());
		assertEquals("abType", book.settings.get("type"));
		assertEquals("val", book.settings.get("prop"));
	}

	@Test
	public void testLdapAB() throws ServerFault, LdapException, DeleteTreeException, IOException {
		LdapDockerTestHelper.initLdapTree(this.getClass(), testName);

		String abUid = "testLdapAB" + System.currentTimeMillis();
		IAddressBooksMgmt service = service(domainAdmin);

		Map<String, String> settings = new HashMap<String, String>();
		settings.put("type", "ldap");
		settings.put("hostname", new BmConfIni().get(DockerContainer.LDAP.getName()));
		settings.put("protocol", "plain");
		settings.put("allCertificate", "false");
		settings.put("baseDn", "dc=local");
		settings.put("loginDn", "uid=admin,dc=local");
		settings.put("loginPw", "admin!");
		settings.put("userFilter", "(objectClass=inetOrgPerson)");

		AddressBookDescriptor bookDescriptor = AddressBookDescriptor.create("test", domainUid, domainUid, settings);

		try {
			service.create(abUid, bookDescriptor, false);
			fail("AB creation should failed because of invalid ldap connection");
		} catch (ServerFault sf) {
			assertEquals("LDAP connection failed: INVALID_CREDENTIALS", sf.getMessage());
		}

		settings.put("loginPw", "admin");
		bookDescriptor = AddressBookDescriptor.create("test", domainUid, domainUid, settings);
		service.create(abUid, bookDescriptor, false);

		AddressBookDescriptor book = service.getComplete(abUid);
		assertNotNull(book);
	}

	private IAddressBooksMgmt service(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(IAddressBooksMgmt.class, domainUid);
	}
}
