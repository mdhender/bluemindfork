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
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import net.bluemind.backend.mail.replica.service.tests.ReplicationEventsRecorder.Hierarchy;
import net.bluemind.backend.mail.replica.service.tests.ReplicationEventsRecorder.Stats;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.mime.MimeTree;

public class RollingReplicationTests extends AbstractRollingReplicationTests {

	@Test
	public void ensureReplicationStatusIsOk() throws IMAPException, InterruptedException {
		System.err.println("Test starts..........................");
		imapAsUser(sc -> {
			int added = sc.append("INBOX", testEml(), new FlagsList());
			assertTrue(added > 0);
			sc.select("INBOX");
			Collection<MimeTree> bs = sc.uidFetchBodyStructure(Arrays.asList(added));
			MimeTree tree = bs.iterator().next();
			System.out.println("Mail " + added + " added:\n" + tree);
			return null;
		});

		imapAsUser(sc -> {
			sc.select("INBOX");
			Collection<Integer> allMails = sc.uidSearch(new SearchQuery());
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			fl.add(Flag.SEEN);
			sc.uidStore(allMails, fl, true);
			System.err.println("Expunge the mailbox...");
			sc.expunge();
			return null;
		});
		System.err.println("sleeping....");
		Thread.sleep(5000);
		System.err.println("End of sleep");
		Stats stats = rec.stats(domainUid, userUid);
		assertNotNull(stats);
		System.err.println("minor " + stats.minorHierarchyChanges.longValue() + " major "
				+ stats.majorHierarchyChanges.longValue());
		// assertTrue(stats.majorHierarchyChanges.longValue() > 0);
		// assertTrue(stats.minorHierarchyChanges.longValue() > 0);

		Hierarchy hier = rec.hierarchy(domainUid, userUid);
		assertNotNull(hier);
		System.err.print("Hierarchy is " + hier);
		assertFalse(hier.mailboxUniqueIds.isEmpty());
	}

}
