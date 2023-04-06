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

package net.bluemind.dataprotect.mailbox.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import net.bluemind.dataprotect.mailbox.MboxRestoreService;
import net.bluemind.dataprotect.mailbox.MboxRestoreService.Mode;
import net.bluemind.imap.Envelope;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.service.common.DefaultFolder;

public class MboxRestoreServiceTests extends AbstractRestoreTests {
	@Test
	public void testRestoreUserInSubfolder() throws Exception {
		backupAll();

		MboxRestoreService mbr = new MboxRestoreService();
		makeBackupFilesReadable();
		assertNotNull(mbr);

		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, mbox, testDomain, Mode.SUBFOLDER, monitor);
		assertTrue(monitor.finished);

		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			assertTrue(sc.login());
			boolean found = false;
			ListResult list = sc.listAll();
			for (ListInfo li : list) {
				if (li.getName().startsWith("restored-")) {
					found = true;
				}
			}
			assertTrue("A restore-xxxx directory should exist in the imap hierarchy", found);
		}
	}

	@Test
	public void testRestoreMailshareInSubfolder() throws Exception {
		backupAll();

		MboxRestoreService mbr = new MboxRestoreService();
		makeBackupFilesReadable();
		assertNotNull(mbr);

		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, sharedMbox, testDomain, Mode.SUBFOLDER, monitor);
		assertTrue(monitor.finished);

		System.out.println("Login to IMAP as: " + latd);
		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			assertTrue(sc.login());
			boolean found = false;
			ListResult list = sc.listAll();
			for (ListInfo li : list) {
				System.out.println("On " + li.getName());
				if (li.getName().contains("restored-") && li.getName().startsWith(SHARE_FOLDERS)) {
					found = true;
				}
			}
			assertTrue("A restore-xxxx directory should exist in the imap hierarchy", found);
		}
	}

	@Test
	public void testRestoreUserReplace() throws Exception {
		backupAll();

		MboxRestoreService mbr = new MboxRestoreService();
		makeBackupFilesReadable();
		assertNotNull(mbr);

		// empty the mailbox
		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			assertTrue(sc.login());
			sc.select("INBOX");
			SearchQuery q = new SearchQuery();
			q.setUndeleted(true);
			Collection<Integer> all = sc.uidSearch(q);
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			sc.uidStore(all, fl, true);
			sc.expunge();
			all = sc.uidSearch(q);
			assertTrue("INBOX should be empty after expunge", all.isEmpty());
			sc.deleteMailbox(subFolderWithSpace);
			sc.deleteMailbox(subFolder);
		}

		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, mbox, testDomain, Mode.REPLACE, monitor);
		assertTrue(monitor.finished);

		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			assertTrue(sc.login());
			sc.select("INBOX");
			Collection<Integer> all = sc.uidSearch(new SearchQuery());
			assertFalse("INBOX should not be empty after restore", all.isEmpty());

			Collection<Envelope> allEmails = sc.uidFetchEnvelope(all);
			assertTrue(allEmails.stream().filter(env -> "coucou".equals(env.getSubject())).count() > 0);

			assertTrue(sc.select(subFolder));
			assertTrue(sc.select(subFolderWithSpace));

			DefaultFolder.USER_FOLDERS.forEach(df -> {
				assertTrue(String.format("Folder %s must exixts", df.name), sc.isExist(df.name));
			});
		}
	}

	@Test
	public void testRestoreMailshareReplace() throws Exception {
		backupAll();

		MboxRestoreService mbr = new MboxRestoreService();
		makeBackupFilesReadable();
		assertNotNull(mbr);

		int size = 0;

		String sentFolder = "Dossiers partagés/" + sharedMbox.value.name + "/Sent";

		// empty the mailbox
		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			assertTrue(sc.login());
			assertTrue(sc.select(sentFolder));
			Collection<Integer> all = sc.uidSearch(new SearchQuery());
			size = all.size();
			assertTrue(size > 0);
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			sc.uidStore(all, fl, true);
			sc.expunge();
			all = sc.uidSearch(new SearchQuery());
			assertTrue(sentFolder + " should be empty after expunge", all.isEmpty());
		}

		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, sharedMbox, testDomain, Mode.REPLACE, monitor);
		assertTrue(monitor.finished);

		try (StoreClient sc = new StoreClient("localhost", 1143, latd, password)) {
			assertTrue(sc.login());
			sc.select(sentFolder);
			Collection<Integer> all = sc.uidSearch(new SearchQuery());
			assertEquals(size, all.size());
			assertFalse(sentFolder + " should not be empty after restore", all.isEmpty());
		}
	}

}
