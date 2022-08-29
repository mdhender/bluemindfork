/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.system.ldap.export.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.UUID;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.config.Token;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.system.ldap.export.LdapHelper;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class LdapServerHookTests {
	private static final String LDAPTAG = "directory/bm-master";

	private String ldapRoleServerIp = new BmConfIni().get("bluemind/ldap");
	private ItemValue<Server> ldapRoleServer;

	private ItemValue<Domain> domain;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

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

		getLdapRoleServer();

		INodeClient nodeClient = NodeActivator.get(ldapRoleServerIp);
		updateUserPassword(nodeClient, "admin0@global.virt", Token.admin0());

		NCUtils.exec(nodeClient, "service slapd stop");
		NCUtils.exec(nodeClient, "rm -rf /etc/ldap/slapd.d");
		NCUtils.exec(nodeClient, "rm -rf /etc/ldap/sasl2/slapd.conf");
		NCUtils.exec(nodeClient, "rm -rf /var/lib/ldap");
	}

	private void updateUserPassword(INodeClient nodeClient, String login, String passwd) {
		NCUtils.exec(nodeClient, "/usr/local/sbin/updateUserPassword.sh " + login + " " + passwd);
	}

	private void getLdapRoleServer() {
		String uid = UUID.randomUUID().toString();

		Server lrs = new Server();
		lrs.ip = ldapRoleServerIp;
		lrs.tags = Collections.emptyList();

		ldapRoleServer = ItemValue.create(Item.create(uid, null), lrs);
	}

	@Test
	public void serverTag() throws LdapException, CursorException {
		new LdapServerHook().onServerTagged(null, ldapRoleServer, LDAPTAG);

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		assertNotNull(ldapCon);

		SearchRequest searchRequest = new SearchRequestImpl();
		searchRequest.setBase(new Dn("dc=local"));
		searchRequest.setScope(SearchScope.SUBTREE);
		searchRequest.setFilter("(objectclass=*)");
		SearchCursor cursor = ldapCon.search(searchRequest);

		assertNotNull(cursor);
		int count = 0;
		while (cursor.next()) {
			count++;
			Response response = cursor.get();

			assertEquals(MessageTypeEnum.SEARCH_RESULT_ENTRY, response.getType());

			Entry entry = ((SearchResultEntry) response).getEntry();
			assertTrue(entry.containsAttribute("dc"));
			assertEquals("local", entry.get("dc").getString());

			assertTrue(entry.containsAttribute("o"));
			assertEquals("BlueMind", entry.get("o").getString());

			assertTrue(entry.containsAttribute("description"));
			assertEquals("BlueMind LDAP directory", entry.get("description").getString());
		}

		assertEquals(1, count);
	}

	@Test
	public void serverAssigned() throws LdapException, CursorException {
		new LdapServerHook().onServerTagged(null, ldapRoleServer, LDAPTAG);
		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		assertNotNull(ldapCon);

		BmTestContext context = BmTestContext.contextWithSession("testUser", "test", domain.uid,
				SecurityContext.ROLE_SYSTEM);

		LdapServerHook lhs = new LdapServerHook();
		TaskRef tr = lhs.initDomainLdapTree(context, ldapRoleServer, domain, LDAPTAG);
		waitFor(tr);

		ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		assertNotNull(ldapCon);

		checkDomainRoot(context, ldapCon);
		checkDomainUsersRoot(ldapCon);
		checkDomainGroupsRoot(ldapCon);
	}

	private void waitFor(TaskRef taskRef) {
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

	private void checkDomainGroupsRoot(LdapConnection ldapCon) throws LdapException, CursorException {
		SearchRequest searchRequest = new SearchRequestImpl();
		searchRequest.setBase(new Dn("ou=groups,dc=" + domain.value.name + ",dc=local"));
		searchRequest.setScope(SearchScope.OBJECT);
		searchRequest.setFilter("(objectclass=*)");
		SearchCursor cursor = ldapCon.search(searchRequest);

		assertNotNull(cursor);
		int count = 0;
		while (cursor.next()) {
			count++;
			Response response = cursor.get();

			assertEquals(MessageTypeEnum.SEARCH_RESULT_ENTRY, response.getType());

			Entry entry = ((SearchResultEntry) response).getEntry();
			assertTrue(entry.containsAttribute("objectclass"));
			assertTrue(entry.get("objectClass").contains("organizationalUnit"));

			assertTrue(entry.containsAttribute("ou"));
			assertEquals("groups", entry.get("ou").getString());

			assertTrue(entry.containsAttribute("description"));
			assertEquals(domain.value.name + " domain groups", entry.get("description").getString());
		}

		assertEquals(1, count);
	}

	private void checkDomainUsersRoot(LdapConnection ldapCon) throws LdapException, CursorException {
		SearchRequest searchRequest = new SearchRequestImpl();
		searchRequest.setBase(new Dn("ou=users,dc=" + domain.value.name + ",dc=local"));
		searchRequest.setScope(SearchScope.OBJECT);
		searchRequest.setFilter("(objectclass=*)");
		SearchCursor cursor = ldapCon.search(searchRequest);

		assertNotNull(cursor);
		int count = 0;
		while (cursor.next()) {
			count++;
			Response response = cursor.get();

			assertEquals(MessageTypeEnum.SEARCH_RESULT_ENTRY, response.getType());

			Entry entry = ((SearchResultEntry) response).getEntry();
			assertTrue(entry.containsAttribute("objectclass"));
			assertTrue(entry.get("objectClass").contains("organizationalUnit"));

			assertTrue(entry.containsAttribute("ou"));
			assertEquals("users", entry.get("ou").getString());

			assertTrue(entry.containsAttribute("description"));
			assertEquals(domain.value.name + " domain users", entry.get("description").getString());
		}

		assertEquals(1, count);
	}

	private void checkDomainRoot(BmTestContext context, LdapConnection ldapCon)
			throws LdapInvalidDnException, LdapException, CursorException, LdapInvalidAttributeValueException {
		SearchRequest searchRequest = new SearchRequestImpl();
		searchRequest.setBase(new Dn("dc=" + domain.value.name + ",dc=local"));
		searchRequest.setScope(SearchScope.OBJECT);
		searchRequest.setFilter("(objectclass=*)");
		SearchCursor cursor = ldapCon.search(searchRequest);

		assertNotNull(cursor);
		int count = 0;
		while (cursor.next()) {
			count++;
			Response response = cursor.get();

			assertEquals(MessageTypeEnum.SEARCH_RESULT_ENTRY, response.getType());

			Entry entry = ((SearchResultEntry) response).getEntry();
			assertTrue(entry.containsAttribute("objectclass"));
			assertTrue(entry.get("objectClass").contains("bmDomain", "dcObject"));

			assertTrue(entry.containsAttribute("dc"));
			assertEquals(domain.value.name, entry.get("dc").getString());

			assertTrue(entry.containsAttribute("o"));
			assertEquals(domain.value.name + " label", entry.get("o").getString());

			assertTrue(entry.containsAttribute("description"));
			assertEquals(domain.value.name + " description", entry.get("description").getString());

			assertTrue(entry.containsAttribute("bmVersion"));
			assertEquals(context.provider().instance(IDirectory.class, domain.uid).changeset(0l).version,
					Long.parseLong(entry.get("bmVersion").getString()));
		}

		assertEquals(1, count);
	}

	@Test
	public void serverUnassigned()
			throws LdapInvalidDnException, LdapInvalidAttributeValueException, LdapException, CursorException {
		new LdapServerHook().onServerTagged(null, ldapRoleServer, LDAPTAG);

		TaskRef tr = new LdapServerHook().initDomainLdapTree(
				BmTestContext.contextWithSession("testUser", "test", domain.uid, SecurityContext.ROLE_SYSTEM),
				ldapRoleServer, domain, LDAPTAG);
		waitFor(tr);

		new LdapServerHook().onServerUnassigned(null, ldapRoleServer, domain, LDAPTAG);
		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		assertNotNull(ldapCon);

		checkDomainRootDoesntExist(ldapCon);
	}

	private void checkDomainRootDoesntExist(LdapConnection ldapCon)
			throws LdapInvalidDnException, LdapException, CursorException, LdapInvalidAttributeValueException {
		SearchRequest searchRequest = new SearchRequestImpl();
		searchRequest.setBase(new Dn("dc=" + domain.value.name + ",dc=local"));
		searchRequest.setScope(SearchScope.OBJECT);
		searchRequest.setFilter("(objectclass=*)");
		SearchCursor cursor = ldapCon.search(searchRequest);

		assertNotNull(cursor);
		while (cursor.next()) {
			Response response = cursor.get();

			if (response.getType() == MessageTypeEnum.SEARCH_RESULT_ENTRY) {
				fail("LDAP domain root must not be found");
			}
		}
	}
}
