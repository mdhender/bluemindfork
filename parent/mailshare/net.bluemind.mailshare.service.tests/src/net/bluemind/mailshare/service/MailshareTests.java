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
package net.bluemind.mailshare.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.service.DirEntryHandler;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.identity.api.IMailboxIdentity;
import net.bluemind.mailbox.identity.api.Identity;
import net.bluemind.mailbox.identity.api.SignatureFormat;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MailshareTests {

	public static boolean hook = false;
	private String domainUid;
	private SecurityContext domainAdminSecurityContext;
	private SecurityContext domainUserSecurityContext;
	private BmTestContext testContext;
	private ItemValue<Server> dataLocation;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();
		domainUid = "bm.lan";

		String cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(imapServer, esServer);

		PopulateHelper.createTestDomain(domainUid, imapServer);

		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		dataLocation = serverService.getComplete(cyrusIp);

		domainAdminSecurityContext = BmTestContext
				.contextWithSession("d1", "admin", domainUid, SecurityContext.ROLE_ADMIN).getSecurityContext();

		domainUserSecurityContext = BmTestContext.contextWithSession("u1", "user", domainUid).getSecurityContext();

		testContext = new BmTestContext(SecurityContext.SYSTEM);
		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreate() throws ServerFault {
		Mailshare ms = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);

		// check direntry exists
		List<DirEntry> res = testContext.provider().instance(IDirectory.class, domainUid)
				.getEntries(domainUid + "/mailshares");

		assertEquals(1, res.size());
		assertEquals(uid, res.get(0).entryUid);
		assertEquals(DirEntry.Kind.MAILSHARE, res.get(0).kind);
		assertEquals(ms.name, res.get(0).displayName);
		assertEquals("test@bm.lan", res.get(0).email);
	}

	@Test
	public void testCreateWithItem() throws ServerFault, ParseException {
		Mailshare mailshare = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		ItemValue<Mailshare> mailshareItem = ItemValue.create(uid, mailshare);
		mailshareItem.internalId = 73;
		mailshareItem.externalId = "externalId" + System.nanoTime();
		mailshareItem.displayName = "test";
		mailshareItem.created = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:44:21");
		mailshareItem.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		mailshareItem.version = 17;
		service(domainAdminSecurityContext).createWithItem(mailshareItem);

		// check direntry exists
		List<DirEntry> entries = testContext.provider().instance(IDirectory.class, domainUid)
				.getEntries(domainUid + "/mailshares");
		assertEquals(1, entries.size());
		DirEntry res = entries.get(0);
		assertEquals(uid, res.entryUid);
		assertEquals(DirEntry.Kind.MAILSHARE, res.kind);
		assertEquals(mailshare.name, res.displayName);
		assertEquals("test@bm.lan", res.email);

		ItemValue<Mailshare> created = testContext.provider().instance(IMailshare.class, domainUid).getComplete(uid);
		assertEquals(mailshareItem.version, created.version);
		assertEquals(mailshareItem.internalId, created.internalId);
		assertEquals(mailshareItem.uid, created.uid);
		assertEquals(mailshareItem.externalId, created.externalId);
		assertEquals(mailshareItem.created, created.created);

	}

	@Test
	public void testGetAll() throws Exception {
		Mailshare ms = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);
		uid = "ms" + System.currentTimeMillis();
		ms.name = "test2";
		ms.emails = Arrays.asList(Email.create("test2@bm.lan", true, true));
		service(domainAdminSecurityContext).create(uid, ms);

		PopulateHelper.addUser("test123", domainUid);

		IMailshare instance = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailshare.class,
				domainUid);
		List<ItemValue<Mailshare>> allComplete = instance.allComplete();
		assertEquals(2, allComplete.size());
	}

	@Test
	public void testCreateWithVCard() throws ServerFault {
		Mailshare ms = defaultMailshare();
		ms.card = new VCard();
		ms.card.organizational.title = "Master !";

		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);

		// check direntry exists
		ItemValue<VCard> vcard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);
		assertNotNull(vcard);
		assertEquals("Master !", vcard.value.organizational.title);
		assertEquals(ms.name, vcard.value.identification.formatedName.value);
	}

	@Test
	public void testCreateForbidden() throws ServerFault {
		Mailshare ms = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		try {
			service(domainUserSecurityContext).create(uid, ms);
			fail("should fail because simple user cannot create mailshare");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testUpate() throws ServerFault {
		Mailshare ms = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);

		// now, update
		ms = defaultMailshare();
		ms.name = "testupdated" + System.currentTimeMillis();
		ms.card = new VCard();
		ms.card.organizational.title = "Master !";

		service(domainAdminSecurityContext).update(uid, ms);

		// check direntry updated
		List<DirEntry> res = testContext.provider().instance(IDirectory.class, domainUid)
				.getEntries(domainUid + "/mailshares");

		assertEquals(1, res.size());
		assertEquals(ms.name, res.get(0).displayName);

		// check vcard
		ItemValue<VCard> vcard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);
		assertNotNull(vcard);
		assertEquals("Master !", vcard.value.organizational.title);
		assertEquals(ms.name, vcard.value.identification.formatedName.value);
	}

	@Test
	public void testUpdateFormatedName() throws ServerFault {
		Mailshare ms = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);

		// now, update
		ms = defaultMailshare();
		ms.name = "testupdated" + System.currentTimeMillis();
		ms.card = new VCard();
		ms.card.organizational.title = "Master !";
		ms.card.identification.formatedName.value = "John Doe";
		service(domainAdminSecurityContext).update(uid, ms);

		// check direntry updated
		List<DirEntry> res = testContext.provider().instance(IDirectory.class, domainUid)
				.getEntries(domainUid + "/mailshares");

		assertEquals(1, res.size());
		assertEquals("John Doe", res.get(0).displayName);

		// check vcard
		ItemValue<VCard> vcard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);
		assertNotNull(vcard);
		assertEquals("Master !", vcard.value.organizational.title);
		assertEquals("John Doe", vcard.value.identification.formatedName.value);
	}

	@Test
	public void testUpateWithItem() throws ServerFault, ParseException {
		Mailshare mailshare = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, mailshare);

		// now, update
		ItemValue<Mailshare> mailshareItem = ItemValue.create(uid, mailshare);
		mailshareItem.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		mailshareItem.version = 17;
		mailshareItem.value.name = "testupdated" + System.currentTimeMillis();
		mailshareItem.value.card = new VCard();
		mailshareItem.value.card.organizational.title = "Master !";
		service(domainAdminSecurityContext).updateWithItem(mailshareItem);

		// check direntry updated
		List<DirEntry> entries = testContext.provider().instance(IDirectory.class, domainUid)
				.getEntries(domainUid + "/mailshares");

		assertEquals(1, entries.size());
		DirEntry entry = entries.get(0);
		assertEquals(mailshareItem.value.name, entry.displayName);

		// check vcard
		ItemValue<VCard> vcard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);
		assertNotNull(vcard);
		assertEquals("Master !", vcard.value.organizational.title);
		assertEquals(mailshare.name, vcard.value.identification.formatedName.value);

		// check item
		ItemValue<Mailshare> updated = testContext.provider().instance(IMailshare.class, domainUid).getComplete(uid);
		assertEquals(mailshareItem.version, updated.version);
		assertEquals(mailshareItem.updated, updated.updated);
	}

	@Test
	public void testUpdateWithoutVCard() throws ServerFault {
		Mailshare ms = defaultMailshare();
		ms.card = new VCard();
		ms.card.organizational.title = "Master !";
		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);

		// now, update
		ms = defaultMailshare();
		ms.name = "testupdated" + System.currentTimeMillis();
		ms.card = null;
		service(domainAdminSecurityContext).update(uid, ms);
		ItemValue<VCard> vcard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);
		assertNotNull(vcard);
		assertEquals("Master !", vcard.value.organizational.title);
	}

	@Test
	public void testUpateNonExistant() throws ServerFault {
		try {
			service(domainAdminSecurityContext).update("fakeUid", defaultMailshare());
			fail("should fail because doesnt exists");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}
	}

	@Test
	public void testUpdateForbidden() throws ServerFault {
		Mailshare ms = defaultMailshare();
		ms.card = new VCard();
		ms.card.organizational.title = "Master !";
		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);

		// now, update
		ms = defaultMailshare();
		ms.name = "testupdated" + System.currentTimeMillis();

		try {
			service(domainUserSecurityContext).update(uid, ms);
			fail("should fail because simple user cannot create mailshare");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

	}

	@Test
	public void testDelete() throws Exception {
		Mailshare ms = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);

		// test
		TaskRef tr = service(domainAdminSecurityContext).delete(uid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(domainAdminSecurityContext), tr);

		// check direntry deleted
		List<DirEntry> res = testContext.provider().instance(IDirectory.class, domainUid)
				.getEntries(domainUid + "/mailshares");
		assertEquals(0, res.size());
	}

	@Test
	public void testDeleteFromDirectory() throws Exception {
		Mailshare ms = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);

		// test
		TaskRef tr = testContext.provider().instance(IDirectory.class, domainUid)
				.delete(domainUid + "/mailshares/" + uid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(domainAdminSecurityContext), tr);

		assertNull(service(domainAdminSecurityContext).getComplete(uid));
	}

	@Test
	public void tesDeleteForbidden() throws Exception {
		try {
			TaskRef tr = service(domainUserSecurityContext).delete("fakeUid");
			TaskUtils.wait(ServerSideServiceProvider.getProvider(domainAdminSecurityContext), tr);
			fail("should fail because simple user cannot create mailshare");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

	}

	@Test
	public void testDeleteNonExistant() throws Exception {
		TaskRef tr = service(domainAdminSecurityContext).delete("fakeUid");
		TaskStatus status = TaskUtils.wait(ServerSideServiceProvider.getProvider(domainAdminSecurityContext), tr);
		assertEquals(TaskStatus.State.InError, status.state);
	}

	@Test
	public void testHookAreCalled() throws ServerFault {

		hook = false;

		Mailshare ms = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);

		assertTrue(hook);
	}

	@Test
	public void testNoEmail() {
		Mailshare ms = defaultMailshare();
		ms.routing = Routing.internal;
		ms.emails = new ArrayList<Email>();
		String uid = "ms" + System.currentTimeMillis();
		try {
			service(domainAdminSecurityContext).create(uid, ms);
		} catch (ServerFault sf) {
			fail(sf.getMessage());
		}
	}

	@Test
	public void testDeleteWithIdentity() throws Exception {
		IMailshare service = service(domainAdminSecurityContext);

		Mailshare ms = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		service.create(uid, ms);

		IMailboxIdentity is = ServerSideServiceProvider.getProvider(domainAdminSecurityContext)
				.instance(IMailboxIdentity.class, domainUid, uid);
		Identity i = new Identity();
		i.displayname = "test";
		i.name = "test";
		i.email = "test@bm.lan";
		i.format = SignatureFormat.PLAIN;
		i.signature = "woop woop";
		i.sentFolder = "Sent";
		is.create("id-" + uid, i);

		TaskRef tr = service.delete(uid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(domainAdminSecurityContext), tr);

		assertNull(service.getComplete(uid));

	}

	@Test
	public void testSetPhoto() throws ServerFault {
		Mailshare ms = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);

		assertNull(testContext.provider().instance(IDirectory.class, domainUid).getEntryPhoto(uid));
		service(domainAdminSecurityContext).setPhoto(uid, DirEntryHandler.EMPTY_PNG);

		assertNotNull(testContext.provider().instance(IDirectory.class, domainUid).getEntryPhoto(uid));

	}

	@Test
	public void testSetPhotoForbidden() throws ServerFault {
		Mailshare ms = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);

		try {
			service(domainUserSecurityContext).setPhoto(uid, DirEntryHandler.EMPTY_PNG);
			fail("should fail because simple user cannot set mailshare photo");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

	}

	@Test
	public void testSetPhotoInexistant() throws ServerFault {
		try {
			service(domainAdminSecurityContext).setPhoto("fake me", DirEntryHandler.EMPTY_PNG);
			fail("should fail because mailshare doesnt exists");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}
	}

	@Test
	public void testCreateWithDefaultQuota() {
		IDomainSettings domSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settings = domSettingsService.get();
		settings.put(DomainSettingsKeys.mailbox_default_publicfolder_quota.name(), "31");
		domSettingsService.set(settings);

		Mailshare ms = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);

		ItemValue<Mailshare> created = service(domainAdminSecurityContext).getComplete(uid);
		assertEquals(31, created.value.quota.intValue());

	}

	@Test
	public void testUpdateWithDefaultQuota() {
		Mailshare ms = defaultMailshare();
		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);

		IDomainSettings domSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settings = domSettingsService.get();
		settings.put(DomainSettingsKeys.mailbox_default_publicfolder_quota.name(), "31");
		domSettingsService.set(settings);

		service(domainAdminSecurityContext).update(uid, ms);
		ItemValue<Mailshare> up = service(domainAdminSecurityContext).getComplete(uid);
		assertEquals(31, up.value.quota.intValue());

	}

	@Test
	public void testRoutingNone() {
		Mailshare ms = defaultMailshare();
		ms.name = "routing-none";
		ms.routing = Routing.none;
		String uid = "ms" + System.currentTimeMillis();
		try {
			service(domainAdminSecurityContext).create(uid, ms);
		} catch (ServerFault sf) {
			fail(sf.getMessage());
		}

	}

	@Test
	public void testMailShareVCard() throws ServerFault {
		String name = "MyName";
		String firstname = "MyNickName";

		Mailshare ms = defaultMailshare();
		ms.card = new VCard();
		ms.card.identification.name = Name.create(name, firstname, null, null, null, null);

		String uid = "ms" + System.currentTimeMillis();
		service(domainAdminSecurityContext).create(uid, ms);

		// check direntry exists
		ItemValue<VCard> vcard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);
		assertNotNull(vcard);
		System.out.println("HZIEFHZE : " + vcard.value.identification.formatedName.value);
		assertEquals(firstname + " " + name, vcard.value.identification.formatedName.value);
	}

	protected IMailshare service(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(IMailshare.class, domainUid);
	}

	private Mailshare defaultMailshare() {
		Mailshare ms = new Mailshare();
		ms.name = "test";
		ms.dataLocation = dataLocation.uid;
		ms.emails = Arrays.asList(Email.create("test@bm.lan", true, true));
		ms.routing = Routing.internal;
		return ms;
	}

}
