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
package net.bluemind.backend.cyrus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.internal.CyrusMailboxesStorage;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.imap.Acl;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.IMAPRuntimeException;
import net.bluemind.imap.QuotaInfo;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.network.utils.NetworkHelper;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class CyrusMailboxStorageTests {

	private String cyrusIp;
	private BmTestContext context;
	private ItemValue<Server> server;
	private String cyrusIp2;
	private ItemValue<Server> server2;
	private String domainUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		// register TEST_TAG host to locator
		cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		cyrusIp2 = new BmConfIni().get("imap2-role");
		assertNotNull(cyrusIp2);
		Server imapServer2 = new Server();
		imapServer2.ip = cyrusIp2;
		imapServer2.tags = Lists.newArrayList("mail/imap");

		// DataLocation server
		Server fakeImapServer = new Server();
		fakeImapServer.ip = "10.0.0.1";
		fakeImapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer, imapServer2, fakeImapServer);

		context = new BmTestContext(SecurityContext.SYSTEM);
		server = context.provider().instance(IServer.class, InstallationId.getIdentifier()).getComplete(imapServer.ip);
		assertNotNull(server);

		server2 = context.provider().instance(IServer.class, InstallationId.getIdentifier())
				.getComplete(imapServer2.ip);
		assertNotNull(server2);

		domainUid = "test" + System.nanoTime() + ".fr";

		// create domain parititon on cyrus instances
		new CyrusService(cyrusIp).createPartition(domainUid);
		new CyrusService(cyrusIp).refreshPartitions(Arrays.asList(domainUid));
		new CyrusService(cyrusIp).reload();

		new CyrusService(cyrusIp2).createPartition(domainUid);
		new CyrusService(cyrusIp2).refreshPartitions(Arrays.asList(domainUid));
		new CyrusService(cyrusIp2).reload();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreate_user() throws Exception {
		Mailbox mb = defaultMailbox(Mailbox.Type.user);
		ItemValue<Mailbox> item = item("mbox-uid-" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		assertAppendMail("user/" + mb.name + "@" + domainUid);

		assertTrue(isAcl("user/" + mb.name + "@" + domainUid, item.uid + "@" + domainUid, Acl.ALL.toString()));
		assertTrue(isAcl("user/" + mb.name + "@" + domainUid, "admin0", Acl.ALL.toString()));
		assertEquals(2, aclCount("user/" + mb.name + "@" + domainUid));
	}

	@Test
	public void testRecreatedUserWithSameUidIsAppendable() throws Exception {
		Mailbox mb = defaultMailbox(Mailbox.Type.user);
		ItemValue<Mailbox> item = item("mbox-uid-" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		assertAppendMail("user/" + mb.name + "@" + domainUid);

		System.err.println("Deleting....");
		storage().delete(context, domainUid, item);

		storage().create(context, domainUid, item);
		assertAppendMail("user/" + mb.name + "@" + domainUid);

	}

	@Test
	public void testCreate_user_doubleCreate() throws Exception {
		// if mbox already exists, do nothing
		Mailbox mb = defaultMailbox(Mailbox.Type.user);
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		try {
			storage().create(context, domainUid, item);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertAppendMail("user/" + mb.name + "@" + domainUid);
	}

	@Test
	public void testCreate_user_RoutingNone() throws Exception {

		Mailbox mb = defaultMailbox(Mailbox.Type.user);
		mb.routing = Routing.none;
		mb.dataLocation = null;
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		assertCantAppendMail("user/" + mb.name + "@" + domainUid);

		mb.dataLocation = server.uid;
		storage().create(context, domainUid, item);
		assertAppendMail("user/" + mb.name + "@" + domainUid);
	}

	@Test
	public void testCreate_user_RoutingExternal() throws Exception {

		Mailbox mb = defaultMailbox(Mailbox.Type.user);
		mb.routing = Routing.external;
		mb.dataLocation = null;
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		assertCantAppendMail("user/" + mb.name + "@" + domainUid);
	}

	@Test
	public void testCreate_mailshare() throws Exception {

		Mailbox mb = defaultMailbox(Mailbox.Type.mailshare);
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		assertAppendMail(mb.name + "@" + domainUid);
		// check that default folders are created
		for (String folder : DefaultFolder.MAILSHARE_FOLDERS_NAME) {
			assertAppendMail(mb.name + "/" + folder + "@" + domainUid);
		}

		assertTrue(isAcl(mb.name + "@" + domainUid, "anyone", Acl.POST.toString()));
		assertTrue(isAcl(mb.name + "@" + domainUid, "admin0", Acl.ALL.toString()));
		assertEquals(2, aclCount(mb.name + "@" + domainUid));
	}

	private int aclCount(String mailbox) throws IMAPException {
		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			sc.login();
			Map<String, Acl> acls = sc.listAcl(mailbox);
			acls.forEach((s, acl) -> System.err.println(String.format("%s %s", s, acl.toString())));
			return acls.size();
		}
	}

	private boolean isAcl(String mailbox, String userLogin, String acl) throws IMAPException {
		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			sc.login();

			Map<String, Acl> acls = sc.listAcl(mailbox);
			for (String user : acls.keySet()) {
				if (user.equals(userLogin) && acls.get(user).toString().equals(acl)) {
					return true;
				}
			}
		}

		return false;
	}

	@Test
	public void testOnMailboxCreated_group() throws Exception {
		Mailbox mb = defaultMailbox(Mailbox.Type.group);
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		assertAppendMail(mb.name + "@" + domainUid);

		assertTrue(isAcl(mb.name + "@" + domainUid, "anyone", Acl.POST.toString()));
		assertTrue(isAcl(mb.name + "@" + domainUid, "admin0", Acl.ALL.toString()));
		assertEquals(2, aclCount(mb.name + "@" + domainUid));
	}

	@Test
	public void testOnMailboxCreated_groupRoutingNone() throws Exception {
		Mailbox mb = defaultMailbox(Mailbox.Type.group);
		mb.routing = Routing.none;
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		assertAppendMail(mb.name + "@" + domainUid);
	}

	@Test
	public void testUpdate_fromNotManagedToNotManaged() throws Exception {
		// should do nothing
		Mailbox mb = defaultMailbox(Mailbox.Type.user);
		mb.routing = Mailbox.Routing.external;
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		// begin test
		Mailbox mbUpdated = defaultMailbox(Mailbox.Type.user);
		mbUpdated.name = mb.name;
		mbUpdated.routing = Mailbox.Routing.none;
		ItemValue<Mailbox> itemUpdated = item(item.uid, mbUpdated);
		storage().update(context, domainUid, item, itemUpdated);
		assertAppendMail("user/" + mb.name + "@" + domainUid);
	}

	@Test
	public void testUpdate_fromManagedToNotManaged() throws Exception {
		// should do nothing
		Mailbox mb = defaultMailbox(Mailbox.Type.user);
		mb.routing = Mailbox.Routing.internal;
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);
		storage().create(context, domainUid, item);

		// begin test
		Mailbox mbUpdated = defaultMailbox(Mailbox.Type.user);
		mbUpdated.name = mb.name;
		mbUpdated.routing = Mailbox.Routing.none;
		ItemValue<Mailbox> itemUpdated = item(item.uid, mbUpdated);
		storage().update(context, domainUid, item, itemUpdated);
		assertAppendMail("user/" + mb.name + "@" + domainUid);
	}

	@Test
	public void testUpdate_fromNotManagedToManaged() throws Exception {
		// should do nothing
		Mailbox mb = defaultMailbox(Mailbox.Type.user);
		mb.routing = Mailbox.Routing.external;
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);
		storage().create(context, domainUid, item);

		// begin test
		Mailbox mbUpdated = defaultMailbox(Mailbox.Type.user);
		mbUpdated.name = mb.name;
		mbUpdated.routing = Mailbox.Routing.internal;
		ItemValue<Mailbox> itemUpdated = item(item.uid, mbUpdated);
		storage().update(context, domainUid, item, itemUpdated);
		assertAppendMail("user/" + mb.name + "@" + domainUid);
	}

	@Test
	public void testUpdateRename_user() throws Exception {
		Mailbox mb = defaultMailbox(Mailbox.Type.user);
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);

		Map<String, Acl> acl = getAcl(mb.type.cyrAdmPrefix + mb.name + "@" + domainUid);
		acl.put("other@" + domainUid, Acl.ALL);
		CyrusAclService.sync(server.value.address()).setAcl(mb.type.cyrAdmPrefix + mb.name + "@" + domainUid, acl);

		checkAcl(mb.type.cyrAdmPrefix + mb.name + "@" + domainUid, acl);

		// begin test
		Mailbox mbRenamed = defaultMailbox();
		ItemValue<Mailbox> itemRenamed = item(item.uid, mbRenamed);
		storage().update(context, domainUid, item, itemRenamed);

		assertAppendMail(mb.type.cyrAdmPrefix + mbRenamed.name + "@" + domainUid);

		checkAcl(mbRenamed.type.cyrAdmPrefix + mbRenamed.name + "@" + domainUid, acl);
	}

	@Test
	public void testUpdateRename_mailshare() throws Exception {
		Mailbox mb = defaultMailbox(Mailbox.Type.mailshare);
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);

		Map<String, Acl> acl = getAcl(mb.type.cyrAdmPrefix + mb.name + "@" + domainUid);
		acl.put("other@" + domainUid, Acl.ALL);
		CyrusAclService.sync(server.value.address()).setAcl(mb.type.cyrAdmPrefix + mb.name + "@" + domainUid, acl);

		checkAcl(mb.type.cyrAdmPrefix + mb.name + "@" + domainUid, acl);

		// begin test
		Mailbox mbRenamed = defaultMailbox(Mailbox.Type.mailshare);
		ItemValue<Mailbox> itemRenamed = item(item.uid, mbRenamed);
		storage().update(context, domainUid, item, itemRenamed);

		assertAppendMail(mbRenamed.name + "@" + domainUid);

		checkAcl(mbRenamed.type.cyrAdmPrefix + mbRenamed.name + "@" + domainUid, acl);
	}

	@Test
	public void testUpdateRename_group() throws Exception {
		// nothing should happen..
		Mailbox mb = defaultMailbox(Mailbox.Type.group);
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);

		Map<String, Acl> acl = getAcl(mb.type.cyrAdmPrefix + mb.name + "@" + domainUid);
		acl.put("other@" + domainUid, Acl.ALL);
		CyrusAclService.sync(server.value.address()).setAcl(mb.type.cyrAdmPrefix + mb.name + "@" + domainUid, acl);

		checkAcl(mb.type.cyrAdmPrefix + mb.name + "@" + domainUid, acl);

		// begin test
		Mailbox mbRenamed = defaultMailbox(Mailbox.Type.group);
		ItemValue<Mailbox> itemRenamed = item(item.uid, mbRenamed);
		storage().update(context, domainUid, item, itemRenamed);

		assertAppendMail(mbRenamed.name + "@" + domainUid);

		checkAcl(mb.type.cyrAdmPrefix + mbRenamed.name + "@" + domainUid, acl);
	}

	private Map<String, Acl> getAcl(String mailboxName) throws IMAPException {
		try (StoreClient storeClient = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			storeClient.login();
			return storeClient.listAcl(mailboxName);
		}
	}

	private void checkAcl(String mailboxName, Map<String, Acl> expectedAcls) throws IMAPException {
		Map<String, Acl> currentAcls = getAcl(mailboxName);

		assertEquals(expectedAcls.size(), currentAcls.size());
		expectedAcls.entrySet().forEach(entry -> checkAcl(currentAcls, entry));
	}

	private void checkAcl(Map<String, Acl> currentAcls, Entry<String, Acl> entry) {
		assertTrue(currentAcls.containsKey(entry.getKey()));
		assertEquals(entry.getValue(), currentAcls.get(entry.getKey()));
	}

	@Test
	public void testOnMailboxUpdate_groupInternalToNone() throws Exception {
		// nothing should happen..
		Mailbox mb = defaultMailbox(Mailbox.Type.group);
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		assertAppendMail(mb.name + "@" + domainUid);

		// begin test
		Mailbox updated = cloneMailbox(mb);
		updated.routing = Routing.none;
		ItemValue<Mailbox> itemNone = item(item.uid, updated);
		storage().update(context, domainUid, item, itemNone);

		assertAppendMail(updated.name + "@" + domainUid);
	}

	@Test
	public void testOnMailboxUpdate_groupNoneToInternal() throws Exception {
		Mailbox mb = defaultMailbox(Mailbox.Type.group);
		mb.routing = Routing.none;
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		assertAppendMail(mb.name + "@" + domainUid);

		// begin test
		Mailbox updated = cloneMailbox(mb);
		updated.routing = Routing.internal;
		ItemValue<Mailbox> itemInternal = item(item.uid, updated);
		storage().update(context, domainUid, item, itemInternal);

		assertAppendMail(updated.name + "@" + domainUid);
	}

	private Mailbox cloneMailbox(Mailbox mb) {
		Mailbox clone = new Mailbox();
		clone.archived = mb.archived;
		clone.dataLocation = mb.dataLocation;
		clone.emails = new ArrayList<Email>();
		clone.emails.addAll(mb.emails);
		clone.hidden = mb.hidden;
		clone.name = mb.name;
		clone.routing = mb.routing;
		clone.system = mb.system;
		clone.type = mb.type;

		return clone;
	}

	@Test
	public void testUpdateMove_group() throws Exception {

		Mailbox mb = defaultMailbox(Mailbox.Type.group);
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);
		storage().create(context, domainUid, item);

		// begin test
		Mailbox mbMoved = defaultMailbox(Mailbox.Type.group);
		mbMoved.dataLocation = server2.uid;
		mbMoved.name = mb.name;

		ItemValue<Mailbox> itemMoved = item(item.uid, mbMoved);
		storage().update(context, domainUid, item, itemMoved);
		assertAppendMail(server2.value.address(), mb.name + "@" + domainUid);
	}

	@Test
	public void testUpdateMove_user() throws Exception {

		Mailbox mb = defaultMailbox();
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);
		storage().create(context, domainUid, item);

		// begin test
		Mailbox mbMoved = defaultMailbox();
		mbMoved.dataLocation = server2.uid;
		mbMoved.name = mb.name;

		ItemValue<Mailbox> itemMoved = item(item.uid, mbMoved);
		storage().update(context, domainUid, item, itemMoved);
		assertAppendMail(server2.value.address(), "user/" + mb.name + "@" + domainUid);
	}

	@Test
	public void testUpdateMove_mailshare() throws Exception {

		Mailbox mb = defaultMailbox(Mailbox.Type.mailshare);
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);
		storage().create(context, domainUid, item);

		// begin test
		Mailbox mbMoved = defaultMailbox(Mailbox.Type.mailshare);
		mbMoved.dataLocation = server2.uid;
		mbMoved.name = mb.name;

		ItemValue<Mailbox> itemMoved = item(item.uid, mbMoved);
		storage().update(context, domainUid, item, itemMoved);
		assertAppendMail(server2.value.address(), mb.name + "@" + domainUid);
	}

	private InputStream mailContent() {
		return getClass().getResourceAsStream("/data/test.eml");
	}

	static private <T> ItemValue<T> item(String uid, T value) {
		ItemValue<T> item = new ItemValue<>();
		item.uid = uid;
		item.value = value;
		return item;
	}

	private CyrusMailboxesStorage storage() {
		return new CyrusMailboxesStorage();
	}

	private Mailbox defaultMailbox() {
		return defaultMailbox(Mailbox.Type.user);
	}

	private Mailbox defaultMailbox(Mailbox.Type type) {
		return defaultMailbox(type, "test" + System.nanoTime());
	}

	private Mailbox defaultMailbox(Mailbox.Type type, String name) {
		Mailbox m = new Mailbox();
		m.archived = false;
		m.dataLocation = server.uid;
		m.emails = Arrays.asList(Email.create("test@bm.lan", true));
		m.routing = Mailbox.Routing.internal;
		m.type = type;
		m.name = name;
		return m;
	}

	private void assertAppendMail(String mbox) throws IMAPException {
		assertAppendMail(server.value.address(), mbox);
	}

	private void assertAppendMail(String serverAddress, String mbox) throws IMAPException {
		try (StoreClient sc = new StoreClient(serverAddress, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			assertTrue("append mail to " + mbox, sc.append(mbox, mailContent(), new FlagsList()) != -1);
		}
	}

	private void assertCantAppendMail(String mbox) throws IMAPException {
		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			assertFalse(sc.append(mbox, mailContent(), new FlagsList()) != -1);
		}
	}

	private void assertMailboxDoesNotExist(String mbox) throws IMAPException {
		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			assertFalse(sc.isExist(mbox));
			assertTrue(sc.listSubFoldersMailbox(mbox).size() == 0);
		}
	}

	@Test
	public void testOnMailboxDeleted_group() throws Exception {
		Mailbox mb = defaultMailbox(Mailbox.Type.group);
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		assertAppendMail(mb.name + "@" + domainUid);

		// begin test
		storage().delete(context, domainUid, item);
		assertCantAppendMail(mb.name + "@" + domainUid);
		assertMailboxDoesNotExist(mb.name + "@" + domainUid);
	}

	@Test
	public void testCreateCyrusMailboxOnMailboxUpdateIfNotExist() throws Exception {
		Mailbox mb = defaultMailbox(Mailbox.Type.user);
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		deleteCyrusMailbox("user/" + mb.name + "@" + domainUid);

		ItemValue<Mailbox> prevItem = item(item.uid, cloneMailbox(mb));
		mb.routing = Routing.none;
		storage().update(context, domainUid, prevItem, item);

		assertTrue(isAcl("user/" + mb.name + "@" + domainUid, item.uid + "@" + domainUid, Acl.ALL.toString()));
		assertTrue(isAcl("user/" + mb.name + "@" + domainUid, "admin0", Acl.ALL.toString()));
		assertEquals(2, aclCount("user/" + mb.name + "@" + domainUid));
	}

	private void deleteCyrusMailbox(String mbox) throws IMAPException {
		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			sc.deleteMailbox(mbox);
		}
	}

	@Test
	public void checkAndRepairQuota_nullDbQuota() {
		Mailbox mb = defaultMailbox(Mailbox.Type.user, "test." + System.nanoTime());
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			assertTrue(sc.setQuota("user/" + mb.name + "@" + domainUid, 12));
		}

		mb.quota = null;
		storage().checkAndRepairQuota(context, domainUid, item);

		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			QuotaInfo qi = sc.quota("user/" + mb.name + "@" + domainUid);
			assertFalse(qi.isEnable());
			assertEquals(0, qi.getLimit());
		}
	}

	@Test
	public void checkAndRepairQuota_zeroDbQuota() {
		Mailbox mb = defaultMailbox(Mailbox.Type.user, "test." + System.nanoTime());
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			assertTrue(sc.setQuota("user/" + mb.name + "@" + domainUid, 12));
		}

		mb.quota = 0;
		storage().checkAndRepairQuota(context, domainUid, item);

		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			QuotaInfo qi = sc.quota("user/" + mb.name + "@" + domainUid);
			assertFalse(qi.isEnable());
			assertEquals(0, qi.getLimit());
		}
	}

	@Test
	public void checkAndRepairQuota_differentCyrusAndDbQuota() {
		Mailbox mb = defaultMailbox(Mailbox.Type.user, "test." + System.nanoTime());
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			assertTrue(sc.setQuota("user/" + mb.name + "@" + domainUid, 1000));
		}

		mb.quota = 50;
		storage().checkAndRepairQuota(context, domainUid, item);

		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			QuotaInfo qi = sc.quota("user/" + mb.name + "@" + domainUid);
			assertTrue(qi.isEnable());
			assertEquals(50, qi.getLimit());
		}
	}

	@Test
	public void checkAndRepairQuota_corruptedQuotaFile() {
		Mailbox mb = defaultMailbox(Mailbox.Type.user, "test." + System.nanoTime());
		mb.quota = 50;
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);

		writeQuotaFile(mb, "invalidcontent");

		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			assertFalse(sc.quota("user/" + mb.name + "@" + domainUid).isEnable());
		}

		storage().checkAndRepairQuota(context, domainUid, item);

		String mailboxQuotaPath = "/var/lib/cyrus/domain" + "/t/" + domainUid + "/quota/t/user."
				+ mb.name.replace(".", "^");
		assertNotEquals("invalidcontent", new String(context.provider()
				.instance(IServer.class, InstallationId.getIdentifier()).readFile(mb.dataLocation, mailboxQuotaPath)));

		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			QuotaInfo qi = sc.quota("user/" + mb.name + "@" + domainUid);
			assertTrue(qi.isEnable());
			assertEquals(50, qi.getLimit());
		}
	}

	@Test
	public void checkAndRepairQuota_corruptedQuotaUsed() {
		Mailbox mb = defaultMailbox(Mailbox.Type.user, "test." + System.nanoTime());
		mb.quota = 512;
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);

		int quotaUsage = 0;
		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			QuotaInfo qi = sc.quota("user/" + mb.name + "@" + domainUid);
			assertTrue(qi.isEnable());
			assertEquals(512, qi.getLimit());
			assertEquals(0, qi.getUsage());

			assertNotEquals(-1, sc.append("user/" + mb.name + "@" + domainUid, mailContent(), new FlagsList()));
			assertNotEquals(-1, sc.append("user/" + mb.name + "@" + domainUid, mailContent(), new FlagsList()));
			assertNotEquals(-1, sc.append("user/" + mb.name + "@" + domainUid, mailContent(), new FlagsList()));
			assertNotEquals(-1, sc.append("user/" + mb.name + "@" + domainUid, mailContent(), new FlagsList()));

			qi = sc.quota("user/" + mb.name + "@" + domainUid);
			assertTrue(qi.isEnable());
			assertEquals(512, qi.getLimit());
			assertNotEquals(0, qi.getUsage());
			quotaUsage = qi.getUsage();
		}

		writeQuotaFile(mb, String.format("%%(S (%s %s) M (0) AS (0) NF (1))\n", 0, 512));

		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			QuotaInfo qi = sc.quota("user/" + mb.name + "@" + domainUid);
			assertTrue(qi.isEnable());
			assertEquals(512, qi.getLimit());
			assertEquals(0, qi.getUsage());
		}

		storage().checkAndRepairQuota(context, domainUid, item);

		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			QuotaInfo qi = sc.quota("user/" + mb.name + "@" + domainUid);
			assertTrue(qi.isEnable());
			assertEquals(512, qi.getLimit());
			assertEquals(quotaUsage, qi.getUsage());
		}
	}

	@Test
	public void checkAppendVeryBig() {
		Mailbox mb = defaultMailbox(Mailbox.Type.user, "test." + System.nanoTime());
		mb.quota = 100 * 1024;
		ItemValue<Mailbox> item = item("test" + System.currentTimeMillis(), mb);

		storage().create(context, domainUid, item);
		StoreClient sc;
		sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0());
		assertTrue(sc.login());

		byte[] big = new byte[50 * 1024 * 1024];
		InputStream content = new ByteArrayInputStream(big);
		try {
			sc.append("user/" + mb.name + "@" + domainUid, content, new FlagsList());
			fail("we should not be able to append 50MB to cyrus");
		} catch (IMAPRuntimeException e) {
			System.err.println("Ensure we are disconnected and store client is ok with that");
			assertTrue(sc.isClosed());
		}
	}

	private void writeQuotaFile(Mailbox mb, String content) {
		String mailboxQuotaPath = "/var/lib/cyrus/domain" + "/t/" + domainUid + "/quota/t/user."
				+ mb.name.replace(".", "^");
		INodeClient nc = NodeActivator.get(server.value.address());
		nc.writeFile(mailboxQuotaPath, new ByteArrayInputStream(content.getBytes()));
		NCUtils.exec(nc, "service bm-cyrus-imapd restart");
		new NetworkHelper(server.value.address()).waitForListeningPort(1143, 30, TimeUnit.SECONDS);
	}
}