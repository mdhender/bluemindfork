/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.cyrus.partitions.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.bluemind.backend.cyrus.partitions.CyrusFileSystemPathHelper;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.partitions.MailboxDescriptor;
import net.bluemind.mailbox.api.Mailbox;

public class PathHelperTest {

	@Test
	public void testMailshareSubfolderPath() {
		MailboxDescriptor mboxDescriptor = new MailboxDescriptor();
		mboxDescriptor.mailboxName = "social";
		mboxDescriptor.type = Mailbox.Type.mailshare;
		mboxDescriptor.utf7FolderPath = "social/CommissaireAComptes";
		CyrusPartition partition = CyrusPartition.forServerAndDomain("srv", "bm.lan");
		String path = CyrusFileSystemPathHelper.getFileSystemPath("bm.lan", mboxDescriptor, partition, 42L);
		System.err.println("path: " + path);
		assertEquals("/var/spool/cyrus/data/srv__bm_lan/domain/b/bm.lan/c/social/CommissaireAComptes/42.", path);
	}

	@Test
	public void testMailshareRootPath() {
		MailboxDescriptor mboxDescriptor = new MailboxDescriptor();
		mboxDescriptor.mailboxName = "social";
		mboxDescriptor.type = Mailbox.Type.mailshare;
		mboxDescriptor.utf7FolderPath = "social";
		CyrusPartition partition = CyrusPartition.forServerAndDomain("srv", "bm.lan");
		String path = CyrusFileSystemPathHelper.getFileSystemPath("bm.lan", mboxDescriptor, partition, 42L);
		System.err.println("path: " + path);
		assertEquals("/var/spool/cyrus/data/srv__bm_lan/domain/b/bm.lan/s/social/42.", path);
	}

	@Test
	public void testUserINBOX() {
		MailboxDescriptor mboxDescriptor = new MailboxDescriptor();
		mboxDescriptor.mailboxName = "roberto";
		mboxDescriptor.type = Mailbox.Type.user;
		mboxDescriptor.utf7FolderPath = "INBOX";
		CyrusPartition partition = CyrusPartition.forServerAndDomain("srv", "bm.lan");
		String path = CyrusFileSystemPathHelper.getFileSystemPath("bm.lan", mboxDescriptor, partition, 42L);
		System.err.println("path: " + path);
		assertEquals("/var/spool/cyrus/data/srv__bm_lan/domain/b/bm.lan/r/user/roberto/42.", path);
	}

	@Test
	public void testUserSub() {
		MailboxDescriptor mboxDescriptor = new MailboxDescriptor();
		mboxDescriptor.mailboxName = "roberto";
		mboxDescriptor.type = Mailbox.Type.user;
		mboxDescriptor.utf7FolderPath = "social/call";
		CyrusPartition partition = CyrusPartition.forServerAndDomain("srv", "bm.lan");
		String path = CyrusFileSystemPathHelper.getFileSystemPath("bm.lan", mboxDescriptor, partition, 42L);
		System.err.println("path: " + path);
		assertEquals("/var/spool/cyrus/data/srv__bm_lan/domain/b/bm.lan/r/user/roberto/social/call/42.", path);
	}

}
