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
package net.bluemind.addressbook.ldap.sync.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
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
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerSync;
import net.bluemind.core.container.model.ContainerSyncResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.server.api.Server;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class LdapAddressBookSyncTests {
	private String domainUid = "bm.lan";

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() {
		LdapDockerTestHelper.initLdapServer();
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		ElasticsearchTestHelper.getInstance().beforeTest();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");
		PopulateHelper.initGlobalVirt(esServer);

		PopulateHelper.createTestDomain(domainUid);
		PopulateHelper.addDomainAdmin("admin", domainUid);

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

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testLdapAddressBookSync() {
		String abUid = "testLdapAddressBookSync" + System.currentTimeMillis();
		IAddressBooksMgmt abMgmtService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IAddressBooksMgmt.class, domainUid);

		Map<String, String> settings = new HashMap<String, String>();
		settings.put("type", "ldap");
		settings.put("hostname", new BmConfIni().get(DockerContainer.LDAP.getName()));
		settings.put("protocol", "plain");
		settings.put("allCertificate", "false");
		settings.put("baseDn", LdapDockerTestHelper.LDAP_ROOT_DN);
		settings.put("loginDn", LdapDockerTestHelper.LDAP_LOGIN_DN);
		settings.put("loginPw", LdapDockerTestHelper.LDAP_LOGIN_PWD);
		settings.put("filter", "(objectClass=inetOrgPerson)");
		settings.put("entryUUID", "uid");

		AddressBookDescriptor bookDescriptor = AddressBookDescriptor.create("testLdapAddressBookSync", domainUid,
				domainUid, settings);
		abMgmtService.create(abUid, bookDescriptor, false);

		assertNotNull(abMgmtService.getComplete(abUid));

		IContainerSync containerSyncService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerSync.class, abUid);
		TaskRef taskRef = containerSyncService.sync();
		assertNotNull(taskRef);
		ContainerSyncResult res = waitTaskRef(taskRef);
		assertNotNull(res);
		assertEquals(3, res.added);
		assertEquals(0, res.updated);
		assertEquals(0, res.removed);
		Long ns = res.status.nextSync;
		assertTrue(ns > 0);
		assertNotNull(res.status.syncTokens);

		IAddressBook abService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IAddressBook.class, abUid);
		assertEquals(res.added, abService.allUids().size());

		ItemValue<VCard> contact = abService.getComplete("user00");
		assertNotNull(contact);
		assertEquals("John", contact.value.identification.name.givenNames);
		assertEquals("Bang", contact.value.identification.name.familyNames);
		assertEquals("Description de John Bang", contact.value.explanatory.note);
		assertNotNull(abService.getPhoto("user00"));

		contact = abService.getComplete("user01");
		assertNotNull(contact);
		assertEquals("Jimi", contact.value.identification.name.givenNames);
		assertEquals("Hendrix", contact.value.identification.name.familyNames);
		assertEquals("Description JH", contact.value.explanatory.note);
		assertEquals(2, contact.value.communications.emails.size());
		int found = 0;
		for (Email email : contact.value.communications.emails) {
			if ("jimi.hendrix@voodoochild.lan".equals(email.value)) {
				found++;
			}
			if ("jimi@voodoochild.lan".equals(email.value)) {
				found++;
			}
			assertEquals(Arrays.asList("home"), email.getParameterValues("TYPE"));
		}
		assertEquals(2, found);
		assertEquals(1, contact.value.communications.tels.size());
		Tel tel = contact.value.communications.tels.get(0);
		assertEquals("0612131415", tel.value);
		assertEquals(Arrays.asList("cell", "voice"), tel.getParameterValues("TYPE"));
		assertNull(abService.getPhoto("user01"));

		contact = abService.getComplete("user02");
		assertNotNull(contact);
		assertEquals("François", contact.value.identification.name.givenNames);
		assertEquals("Hollande", contact.value.identification.name.familyNames);
		assertEquals(1, contact.value.communications.emails.size());
		Email email = contact.value.communications.emails.get(0);
		assertEquals("fh@elysee.lan", email.value);
		assertEquals(1, contact.value.deliveryAddressing.size());
		DeliveryAddressing da = contact.value.deliveryAddressing.get(0);
		assertEquals(Arrays.asList("home"), da.address.getParameterValues("TYPE"));
		assertEquals("55 Rue du Faubourg Saint-Honoré", da.address.streetAddress);
		assertEquals("75008", da.address.postalCode);
		assertEquals("Paris", da.address.locality);
		assertEquals("France", da.address.countryName);
		assertNull(abService.getPhoto("user02"));

		taskRef = containerSyncService.sync();
		assertNotNull(taskRef);
		res = waitTaskRef(taskRef);
		assertNotNull(res);
		assertEquals(0, res.added);
		assertEquals(0, res.updated);
		assertEquals(0, res.removed);
	}

	private ContainerSyncResult waitTaskRef(TaskRef taskRef) throws ServerFault {
		ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, taskRef.id);
		while (!task.status().state.ended) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new ServerFault(e);
			}
		}

		TaskStatus status = task.status();
		if (status.state == State.InError) {
			throw new ServerFault("import error");
		}
		return JsonUtils.read(status.result, ContainerSyncResult.class);
	}

}
