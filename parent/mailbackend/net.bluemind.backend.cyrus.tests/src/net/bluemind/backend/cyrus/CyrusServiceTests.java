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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.imap.Acl;
import net.bluemind.imap.Annotation;
import net.bluemind.imap.AnnotationList;
import net.bluemind.imap.CreateMailboxResult;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class CyrusServiceTests {

	private CyrusService service;
	private String imapServerAddress;
	private ItemValue<Server> destServer;
	private CyrusService destService;

	@Before
	public void before() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		imapServerAddress = new BmConfIni().get("imap-role");
		assertNotNull(imapServerAddress);
		Server imapServer = new Server();
		imapServer.ip = imapServerAddress;
		imapServer.tags = Lists.newArrayList("mail/imap");

		String imap2 = new BmConfIni().get("imap2-role");
		assertNotNull(imap2);
		Server imapServer2 = new Server();
		imapServer2.ip = imap2;
		imapServer2.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer, imapServer2);
		service = new CyrusService(imapServerAddress);

		destService = new CyrusService(imap2);
		destServer = destService.server();
	}

	@Test
	public void testReload() throws ServerFault {
		service.reload();
	}

	@Test
	public void testCreatePartition() throws Exception {
		String paritition = "part" + System.nanoTime() + ".lan";
		service.createPartition(paritition);
		service.refreshPartitions(Arrays.asList(paritition));
		service.reload();

		// check that partition was well created by creating a mbox into the
		// partition
		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, "admin0", Token.admin0())) {

			assertTrue(sc.login());
			CreateMailboxResult ok = sc.createMailbox("test" + System.nanoTime() + "part",
					CyrusPartition.forServerAndDomain(service.server(), paritition).name);

			assertEquals(true, ok.isOk());

			// create the same partition
			service.createPartition(paritition);
			service.refreshPartitions(Arrays.asList(paritition));

			// should not fail
			service.reload();

			assertTrue(sc.login());
			// and we can create a mbox on the partition
			ok = sc.createMailbox("test" + System.nanoTime() + "part",
					CyrusPartition.forServerAndDomain(service.server(), paritition).name);

			assertEquals(true, ok.isOk());
		}

	}

	@Test
	public void testCreateMBox() throws ServerFault {

		String partition = "bm" + System.nanoTime() + ".lan";
		service.createPartition(partition);
		service.refreshPartitions(Arrays.asList(partition));
		service.reload();

		try {
			service.createBox("test" + System.nanoTime(), partition);
		} catch (ServerFault e) {
			fail(e.getMessage());
		}

		try {
			service.createBox("test" + System.nanoTime(), "notpart");
			fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testUserMboxHasShareSeenEnabled() throws ServerFault {

		String partition = "bm" + System.nanoTime() + ".lan";
		service.createPartition(partition);
		service.refreshPartitions(Arrays.asList(partition));
		service.reload();

		String n = "user/test" + System.nanoTime();
		try {
			service.createBox(n, partition);
			Optional<Annotation> sharedSeen = checkAnnotations(n, "/vendor/cmu/cyrus-imapd/sharedseen");
			assertTrue(sharedSeen.isPresent());
			assertEquals("true", sharedSeen.map(s -> s.valueShared).orElse("false"));
		} catch (ServerFault e) {
			fail(e.getMessage());
		} catch (IMAPException ie) {
			fail(ie.getMessage());
		}
	}

	private Optional<Annotation> checkAnnotations(String boxName, String annot) throws IMAPException {
		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, "admin0", Token.admin0())) {
			sc.login();

			AnnotationList annots = sc.getAnnotation(boxName, annot);
			return Optional.ofNullable(annots.get(annot));
		}
	}

	@Test
	public void testRenameMBox() throws Exception {

		String partition = "bm" + System.nanoTime() + ".lan";
		service.createPartition(partition);
		service.refreshPartitions(Arrays.asList(partition));
		service.reload();
		String mbox = "test" + System.currentTimeMillis();
		service.createBox(mbox, partition);

		String nmbox = mbox + "rename";
		service.renameBox(mbox, nmbox);
	}

	@Test
	public void testXfer() throws Exception {
		String domain = "bm" + System.nanoTime() + ".lan";
		service.createPartition(domain);
		service.refreshPartitions(Arrays.asList(domain));
		service.reload();

		destService.createPartition(domain);
		destService.refreshPartitions(Arrays.asList(domain));
		destService.reload();

		String mbox = "test" + System.currentTimeMillis();
		service.createBox(mbox, domain);
		service.xfer(mbox, domain, destServer);
	}

	@Test
	public void testDeleteMailsharePartialHierarchy() throws Exception {
		String partition = "bm" + System.nanoTime() + ".lan";
		service.createPartition(partition);
		service.refreshPartitions(Arrays.asList(partition));
		service.reload();
		String mbox = "test" + System.currentTimeMillis();
		service.createBox(mbox + "@" + partition, partition);

		Map<String, Acl> acl = new HashMap<>();
		acl.put("admin0", Acl.ALL);
		service.setAcl(mbox + "@" + partition, acl);

		String mboxSub1 = mbox + "/sub1";
		service.createBox(mboxSub1 + "@" + partition, partition);
		String mboxSub1Sub1 = mbox + "/sub1/sub1";
		service.createBox(mboxSub1Sub1 + "@" + partition, partition);

		service.deleteBox(mboxSub1 + "@" + partition, partition);

		assertTrue(isExist(mbox + "@" + partition));
		assertFalse(isExist(mboxSub1 + "@" + partition));
		assertFalse(isExist(mboxSub1Sub1 + "@" + partition));
	}

	private boolean isExist(String boxName) throws IMAPException {
		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, "admin0", Token.admin0())) {
			sc.login();

			ListResult dirs;
			dirs = sc.listAll();
			for (ListInfo info : dirs) {
				if (info.getName().equals(boxName)) {
					return true;
				}
			}
		}

		return false;
	}

	@Test
	public void testDeleteMailshare() throws ServerFault, IMAPException {
		String partition = "bm" + System.nanoTime() + ".lan";
		service.createPartition(partition);
		service.refreshPartitions(Arrays.asList(partition));
		service.reload();
		String mbox = "test" + System.currentTimeMillis();
		service.createBox(mbox + "@" + partition, partition);

		Map<String, Acl> acl = new HashMap<>();
		acl.put("admin0", Acl.ALL);
		service.setAcl(mbox + "@" + partition, acl);

		String mboxSub1 = mbox + "/sub1";
		service.createBox(mboxSub1 + "@" + partition, partition);
		String mboxSub1Sub1 = mbox + "/sub1/sub1";
		service.createBox(mboxSub1Sub1 + "@" + partition, partition);

		service.deleteBox(mbox + "@" + partition, partition);

		assertFalse(isExist(mbox + "@" + partition));
		assertFalse(isExist(mboxSub1 + "@" + partition));
		assertFalse(isExist(mboxSub1Sub1 + "@" + partition));
	}

	@Test
	public void testDeleteMailbox() throws ServerFault, IMAPException {
		String partition = "bm" + System.nanoTime() + ".lan";
		service.createPartition(partition);
		service.refreshPartitions(Arrays.asList(partition));
		service.reload();
		String mbox = "user/test" + System.currentTimeMillis();
		service.createBox(mbox + "@" + partition, partition);

		Map<String, Acl> acl = new HashMap<>();
		acl.put("admin0", Acl.ALL);
		service.setAcl(mbox + "@" + partition, acl);

		String mboxSub1 = mbox + "/sub1";
		service.createBox(mboxSub1 + "@" + partition, partition);
		String mboxSub1Sub1 = mbox + "/sub1/sub1";
		service.createBox(mboxSub1Sub1 + "@" + partition, partition);

		service.deleteBox(mbox + "@" + partition, partition);

		assertFalse(isExist(mbox + "@" + partition));
		assertFalse(isExist(mboxSub1 + "@" + partition));
		assertFalse(isExist(mboxSub1Sub1 + "@" + partition));
	}

	@Test
	public void setUserMailboxAcls() throws ServerFault, IMAPException {
		String domainUid = "bm" + System.nanoTime() + ".lan";
		service.createPartition(domainUid);
		service.refreshPartitions(Arrays.asList(domainUid));
		service.reload();

		String userLogin = "test." + System.nanoTime();
		String userAtDomain = userLogin + "@" + domainUid;
		String boxNamePrefix = "user/" + userLogin;
		String boxName = boxNamePrefix + "@" + domainUid;

		service.createBox(boxName, domainUid);
		service.createBox(boxNamePrefix + "/test@" + domainUid, domainUid);
		for (String f : DefaultFolder.USER_FOLDERS_NAME) {
			service.createBox(boxNamePrefix + "/" + f + "@" + domainUid, domainUid);
		}

		Map<String, Acl> acl = new HashMap<>();
		acl.put("admin0", Acl.ALL);
		acl.put("anyone", Acl.ALL);
		acl.put(userAtDomain, Acl.ALL);
		acl.put("rw@" + domainUid, Acl.RW);
		acl.put("ro@" + domainUid, Acl.RO);
		service.setAcl(boxName, acl);

		assertTrue(isAcl(boxName, "admin0", Acl.ALL.toString()));
		assertTrue(isAcl(boxName, "anyone", Acl.ALL.toString()));
		assertTrue(isAcl(boxName, userAtDomain, Acl.ALL.toString()));
		assertTrue(isAcl(boxName, "rw@" + domainUid, Acl.RW.toString()));
		assertTrue(isAcl(boxName, "ro@" + domainUid, Acl.RO.toString()));

		assertTrue(isAcl(boxNamePrefix + "/test@" + domainUid, "admin0", Acl.ALL.toString()));
		assertTrue(isAcl(boxNamePrefix + "/test@" + domainUid, "anyone", Acl.ALL.toString()));
		assertTrue(isAcl(boxNamePrefix + "/test@" + domainUid, userAtDomain, Acl.ALL.toString()));
		assertTrue(isAcl(boxNamePrefix + "/test@" + domainUid, "rw@" + domainUid, Acl.RW.toString()));
		assertTrue(isAcl(boxNamePrefix + "/test@" + domainUid, "ro@" + domainUid, Acl.RO.toString()));

		for (String f : DefaultFolder.USER_FOLDERS_NAME) {
			assertTrue(isAcl(boxNamePrefix + "/" + f + "@" + domainUid, "admin0", Acl.ALL.toString()));

			CreateMailboxResult result = tryDelete(userAtDomain, f);
			assertFalse(result.isOk());
			assertTrue(result.getMessage().contains("NO Permission denied"));

			result = tryDelete("usertest@" + domainUid, String.format("Autres utilisateurs/%s/%s", userLogin, f));
			assertFalse(result.isOk());
			assertTrue(result.getMessage().contains("NO Permission denied"));

			result = tryDelete("rw@" + domainUid, String.format("Autres utilisateurs/%s/%s", userLogin, f));
			assertFalse(result.isOk());
			assertTrue(result.getMessage().contains("NO Permission denied"));

			result = tryDelete("ro@" + domainUid, String.format("Autres utilisateurs/%s/%s", userLogin, f));
			assertFalse(result.isOk());
			assertTrue(result.getMessage().contains("NO Permission denied"));
		}
	}

	@Test
	public void setMailshareMailboxAcls() throws ServerFault, IMAPException {
		String domainUid = "bm" + System.nanoTime() + ".lan";
		service.createPartition(domainUid);
		service.refreshPartitions(Arrays.asList(domainUid));
		service.reload();

		String boxNamePrefix = "test" + System.nanoTime();
		String boxName = boxNamePrefix + "@" + domainUid;

		service.createBox(boxName, domainUid);
		service.createBox(boxNamePrefix + "/test@" + domainUid, domainUid);
		for (String f : DefaultFolder.MAILSHARE_FOLDERS_NAME) {
			service.createBox(boxNamePrefix + "/" + f + "@" + domainUid, domainUid);
		}

		Map<String, Acl> acl = new HashMap<>();
		acl.put("admin0", Acl.ALL);
		acl.put("anyone", Acl.ALL);
		acl.put("usertest@" + domainUid, Acl.ALL);
		acl.put("rw@" + domainUid, Acl.RW);
		acl.put("ro@" + domainUid, Acl.RO);
		service.setAcl(boxName, acl);

		assertTrue(isAcl(boxName, "admin0", Acl.ALL.toString()));
		assertTrue(isAcl(boxName, "anyone", Acl.ALL.toString()));
		assertTrue(isAcl(boxName, "usertest@" + domainUid, Acl.ALL.toString()));
		assertTrue(isAcl(boxName, "rw@" + domainUid, Acl.RW.toString()));
		assertTrue(isAcl(boxName, "ro@" + domainUid, Acl.RO.toString()));

		assertTrue(isAcl(boxNamePrefix + "/test@" + domainUid, "admin0", Acl.ALL.toString()));
		assertTrue(isAcl(boxNamePrefix + "/test@" + domainUid, "anyone", Acl.ALL.toString()));
		assertTrue(isAcl(boxNamePrefix + "/test@" + domainUid, "usertest@" + domainUid, Acl.ALL.toString()));
		assertTrue(isAcl(boxNamePrefix + "/test@" + domainUid, "rw@" + domainUid, Acl.RW.toString()));
		assertTrue(isAcl(boxNamePrefix + "/test@" + domainUid, "ro@" + domainUid, Acl.RO.toString()));

		for (String f : DefaultFolder.MAILSHARE_FOLDERS_NAME) {
			assertTrue(isAcl(boxNamePrefix + "/" + f + "@" + domainUid, "admin0", Acl.ALL.toString()));

			CreateMailboxResult result = tryDelete("usertest@" + domainUid,
					String.format("Dossiers partagés/%s/%s", boxNamePrefix, f));
			assertFalse(result.isOk());
			assertTrue(result.getMessage().contains("NO Permission denied"));

			result = tryDelete("rw@" + domainUid, String.format("Dossiers partagés/%s/%s", boxNamePrefix, f));
			assertFalse(result.isOk());
			assertTrue(result.getMessage().contains("NO Permission denied"));

			result = tryDelete("ro@" + domainUid, String.format("Dossiers partagés/%s/%s", boxNamePrefix, f));
			assertFalse(result.isOk());
			assertTrue(result.getMessage().contains("NO Permission denied"));
		}
	}

	private CreateMailboxResult tryDelete(String userLogin, String mailbox) throws IMAPException {
		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, userLogin, "fakepassword")) {
			sc.login();
			return sc.deleteMailbox(mailbox);
		}
	}

	private boolean isAcl(String mailbox, String userLogin, String acl) throws IMAPException {
		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, "admin0", Token.admin0())) {
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
}
