/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.dataprotect.mailbox.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.bluemind.aws.s3.utils.S3Configuration;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.config.Token;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.dataprotect.mailbox.MboxRestoreService;
import net.bluemind.dataprotect.mailbox.MboxRestoreService.Mode;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.s3.S3StoreFactory;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;

public class MboxRestoreSdsTests extends AbstractRestoreTests {
	@Override
	protected S3Configuration initSdsStore() throws Exception {
		String bucket = "junit-" + System.currentTimeMillis();
		S3Configuration config = S3Configuration
				.withEndpointAndBucket("http://" + DockerEnv.getIp("bluemind/s3") + ":8000", bucket);
		ImmutableMap<String, String> freshConf = new ImmutableMap.Builder<String, String>() //
				.put(SysConfKeys.archive_kind.name(), "s3") //
				.put(SysConfKeys.archive_days.name(), "0") //
				.put(SysConfKeys.sds_s3_access_key.name(), config.getAccessKey()) //
				.put(SysConfKeys.sds_s3_secret_key.name(), config.getSecretKey()) //
				.put(SysConfKeys.sds_s3_endpoint.name(), config.getEndpoint()) //
				.put(SysConfKeys.sds_s3_region.name(), config.getRegion()) //
				.put(SysConfKeys.sds_s3_bucket.name(), config.getBucket()) //
				.build();
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		ISystemConfiguration sysConfApi = prov.instance(ISystemConfiguration.class);
		sysConfApi.updateMutableValues(freshConf);
		return config;
	}

	@Override
	protected void testSDSStore(S3Configuration config) throws Exception {
		// append mail
		byte[] emlData = ("From: john" + System.currentTimeMillis() + "@junit.test\r\n\r\nBonjour toi").getBytes();
		@SuppressWarnings("deprecation")
		ByteBuf hash = Unpooled.wrappedBuffer(Hashing.sha1().hashBytes(emlData).asBytes());
		String guid = ByteBufUtil.hexDump(hash);
		// System.err.println("Body guid should be " + guid);
		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			assertTrue(sc.login());
			int added = sc.append("INBOX", new ByteArrayInputStream(emlData), new FlagsList());
			assertTrue(added > 0);
			inboxMessages += 1;
		}

		// check s3
		ISdsBackingStore s3 = new S3StoreFactory().create(VertxPlatform.getVertx(), config.asJson(), "unused_by_s3");
		GetRequest gr = new GetRequest();
		gr.mailbox = login;
		gr.guid = guid;
		Path tmp = Files.createTempFile("toto" + System.currentTimeMillis(), ".eml");
		gr.filename = tmp.toFile().getAbsolutePath();
		Files.delete(tmp);
		s3.download(gr).get(10, TimeUnit.SECONDS);
		byte[] content = Files.readAllBytes(tmp);
		assertTrue(Arrays.equals(content, emlData));
	}

	@Before
	public void backupAllBefore() throws Exception {
		backupAll();
	}

	@Test
	public void restoreSdsUserInSubfolder() throws Exception {
		MboxRestoreService mbr = new MboxRestoreService();
		assertNotNull(mbr);
		makeBackupFilesReadable();

		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, mbox, testDomain, Mode.Subfolder, monitor);
		assertTrue(monitor.finished);

		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			assertTrue(sc.login());
			boolean found = false;
			ListResult list = sc.listAll();
			for (ListInfo li : list) {
				System.err.println(li);
				if (li.getName().startsWith("restored-") && li.isSelectable()) {
					found = true;
					assertTrue(sc.select(li.getName()));
					Collection<Integer> all = sc.uidSearch(new SearchQuery());
//					for (Integer i : all) {
//						System.err.println(
//								i + ": " + new String(sc.uidFetchMessage(i).source().read(), StandardCharsets.UTF_8));
//					}
					assertEquals("wrong number of messages", inboxMessages, all.size());
					break;
				}
			}
			assertTrue("A restore-xxxx directory should exist in the imap hierarchy", found);
		}
	}

	@Test
	public void restoreSdsUserReplace() throws Exception {
		/* Create a new subfolder, must be deleted by the restore process */
		IMailboxFolders foldersService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext()
				.su("junit-" + UUID.randomUUID().toString(), userUid, domain).getServiceProvider()
				.instance(IMailboxFoldersByContainer.class, IMailReplicaUids.subtreeUid(domain, mbox));
		foldersService.createBasic(MailboxFolder.of("petitchat"));
		foldersService.createBasic(MailboxFolder.of("petitchat/groschien"));
		MboxRestoreService mbr = new MboxRestoreService();
		assertNotNull(mbr);
		makeBackupFilesReadable();

		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, mbox, testDomain, Mode.Replace, monitor);
		assertTrue(monitor.finished);

		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			assertTrue(sc.login());
			ListResult list = sc.listAll();
			for (ListInfo li : list) {
				System.err.println("Folder " + li.getName());
				if (li.getName().contains("petitchat")) {
					fail("petitchat* should not exist");
				}
				if (li.getName().contains("groschien")) {
					fail("groschien should not exist");
				}
			}
		}
	}

	@Test
	public void restoreSdsMailshareInSubfolder() throws Exception {
		MboxRestoreService mbr = new MboxRestoreService();
		assertNotNull(mbr);
		makeBackupFilesReadable();

		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, sharedMbox, testDomain, Mode.Subfolder, monitor);
		assertTrue(monitor.finished);

		List<String> expectedSubFolders = Lists.newArrayList( //
				"", // default folder: INBOX
				"Sent", // default folder
				"Trash", // default folder
				"ms-sub1", //
				"ms-sub1/sub1-sub1", //
				"ms-sub1/sub1-sub1/sub1-sub1-sub1", //
				"ms-sub2", //
				"ms-sub2/sub2-sub2" //
		).stream().map(fn -> "Dossiers partagés/" + sharedMbox.value.name + (fn.isEmpty() ? fn : "/" + fn))
				.collect(Collectors.toList());

		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			assertTrue(sc.login());
			ListResult list = sc.listAll();
			for (ListInfo li : list) {
				System.err.println("Folder: " + li.getName());
				if (expectedSubFolders.contains(li.getName())) {
					expectedSubFolders.remove(li.getName());
				}
			}
			assertTrue("Some folders were not found in the hierarchy: " + expectedSubFolders,
					expectedSubFolders.isEmpty());
		}
	}

	@Test
	public void restoreSdsMailshareReplace() throws Exception {
		/* Create a new subfolder, must be deleted by the restore process */
		IMailboxFolders foldersService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext()
				.su("junit-" + UUID.randomUUID().toString(), userUid, domain).getServiceProvider()
				.instance(IMailboxFoldersByContainer.class, IMailReplicaUids.subtreeUid(domain, sharedMbox));
		foldersService.createBasic(MailboxFolder.of(mailshareLogin + "/petitchat"));
		foldersService.createBasic(MailboxFolder.of(mailshareLogin + "/petitchat/groschien"));

		MboxRestoreService mbr = new MboxRestoreService();
		assertNotNull(mbr);
		makeBackupFilesReadable();

		int size = 0;
		// empty the mailbox
		try (StoreClient sc = new StoreClient("localhost", 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			assertTrue(sc.select(mailshareLogin + "/Sent@" + domain));
			Collection<Integer> all = sc.uidSearch(new SearchQuery());
			size = all.size();
			assertTrue(size > 0);
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			sc.uidStore(all, fl, true);
			sc.expunge();
			all = sc.uidSearch(new SearchQuery());
			assertTrue(mailshareLogin + "/Sent@" + domain + " should be empty after expunge", all.isEmpty());
		}

		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, sharedMbox, testDomain, Mode.Replace, monitor);
		assertTrue(monitor.finished);

		try (StoreClient sc = new StoreClient("localhost", 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			sc.select(mailshareLogin + "/Sent@" + domain);
			Collection<Integer> all = sc.uidSearch(new SearchQuery());
			assertEquals(size, all.size());
			assertFalse(mailshareLogin + "/Sent@" + domain + "should not be empty after restore", all.isEmpty());
		}

		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			assertTrue(sc.login());
			ListResult list = sc.listAll();
			for (ListInfo li : list) {
				System.err.println("Folder" + li.getName());
				if (li.getName().startsWith(mailshareLogin + "/petitchat")) {
					fail(mailshareLogin + "/petitchat* should not exist");
				}
				if (li.getName().startsWith(mailshareLogin + "/petitchat/groschien")) {
					fail(mailshareLogin + "/petitchat/groschien should not exist");
				}
			}
		}

	}

}
