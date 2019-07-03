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

package net.bluemind.imap;

import java.util.Arrays;

import net.bluemind.imap.MailboxChanges.AddedMessage;
import net.bluemind.imap.MailboxChanges.UpdatedMessage;

public class QResyncTests extends LoggedTestCase {

	public void testEnable() {
		boolean enabled = sc.enable("QRESYNC");
		assertTrue(enabled);
	}

	public void testGetUidValidity() {
		SyncStatus validity = sc.getUidValidity("INBOX");
		assertTrue(validity.uidValidity > 0);
		System.out.println("validity: " + validity);
	}

	public void testSync() throws IMAPException {
		sc.enable("QRESYNC");
		// ensure 1 mail in box before first full sync
		int added = sc.append("INBOX", getRfc822Message(), new FlagsList());
		assertTrue(added > 0);
		SyncStatus validity = sc.getUidValidity("INBOX");
		SyncData sd = new SyncData(validity.uidValidity);
		MailboxChanges changes = sc.sync("INBOX", sd);
		assertNotNull(changes);
		long modseq = changes.modseq;
		assertTrue(modseq > 1);
		assertTrue(changes.fetches >= 1);
		assertEquals(added, changes.highestUid);

		// sc.tagged("unselect INBOX");

		// add one mail and do an incremental sync
		int addedAfterSync = sc.append("INBOX", getRfc822Message(), new FlagsList());
		assertEquals(changes.highestUid + 1, addedAfterSync);
		sd = new SyncData(validity.uidValidity, modseq, changes.highestUid, 0);
		System.out.println("**** +1 mail (uid " + addedAfterSync + ") ****");
		MailboxChanges afterOneAdd = sc.sync("INBOX", sd);
		assertTrue(afterOneAdd.modseq > modseq);
		assertEquals(1, afterOneAdd.fetches);
		assertEquals(1, afterOneAdd.added.size());
		AddedMessage addChange = afterOneAdd.added.get(0);
		assertEquals(addedAfterSync, addChange.uid);

		// flag it as seen
		FlagsList seen = new FlagsList();
		seen.add(Flag.SEEN);
		boolean flagged = sc.uidStore(Arrays.asList(addedAfterSync), seen, true);
		assertTrue(flagged);

		System.out.println("**** marked 1 as read ****");
		sd = new SyncData(validity.uidValidity, afterOneAdd.modseq, afterOneAdd.highestUid, 0);
		MailboxChanges flagChange = sc.sync("INBOX", sd);
		assertTrue(flagChange.modseq > afterOneAdd.modseq);
		assertEquals(1, flagChange.updated.size());
		for (UpdatedMessage um : flagChange.updated) {
			System.out.println("Updated at seq " + um.modseq);
			assertTrue(um.modseq > 0);
		}

		FlagsList del = new FlagsList();
		del.add(Flag.DELETED);
		boolean deleted = sc.uidStore(Arrays.asList(addedAfterSync, added), del, true);
		assertTrue(deleted);

		System.out.println("**** flagged 2 as deleted ****");
		sd = new SyncData(validity.uidValidity, flagChange.modseq, flagChange.highestUid, 0);
		MailboxChanges twoDeletes = sc.sync("INBOX", sd);
		// ensure we already flag as deleted before expunge
		assertEquals(2, twoDeletes.deleted.size());

		sc.expunge();

		twoDeletes = sc.sync("INBOX", sd);
		assertEquals(2, twoDeletes.deleted.size());

	}

	public void testSyncPerf() throws IMAPException {
		int CNT = 1000;
		sc.enable("QRESYNC");
		// ensure 1 mail in box before first full sync
		int added = sc.append("INBOX", getRfc822Message(), new FlagsList());
		assertTrue(added > 0);
		SyncStatus validity = sc.getUidValidity("INBOX");
		SyncData sd = new SyncData(validity.uidValidity, 1, 1, 0);
		MailboxChanges changes = sc.sync("INBOX", sd);
		assertNotNull(changes);
		long modseq = changes.modseq;
		assertTrue(modseq > 1);
		assertTrue(changes.fetches >= 1);
		assertEquals(added, changes.highestUid);

		for (int i = 0; i < CNT; i++) {
			sc.sync("INBOX", sd);
		}

		FlagsList del = new FlagsList();
		del.add(Flag.DELETED);
		boolean deleted = sc.uidStore(Arrays.asList(added), del, true);
		assertTrue(deleted);
		sc.expunge();

	}

}
