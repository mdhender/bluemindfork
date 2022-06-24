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
package net.bluemind.backend.cyrus.replication.server.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import net.bluemind.backend.cyrus.partitions.CyrusBoxes.ReplicatedBox;
import net.bluemind.backend.cyrus.replication.server.utils.ReplicatedBoxes;

public class ReplicatedBoxesTests {

	@Test
	public void testQuotedMboxName() {
		String box = "\"ex2016.vmw!user.tom.Deleted Messages\"";
		ReplicatedBox rBox = ReplicatedBoxes.forCyrusMailbox(box);
		assertNotNull(rBox);
		System.out.println("partition: '" + rBox.partition + "'");
		assertEquals("ex2016_vmw", rBox.partition);
		assertEquals("tom", rBox.local);
	}

	@Test
	public void testFromLogin() {
		String box = "tom@ex2016.vmw";
		ReplicatedBox rBox = ReplicatedBoxes.forLoginAtDomain(box);
		assertNotNull(rBox);
		assertEquals("ex2016_vmw", rBox.partition);
	}

	@Test
	public void testUnqualified() {
		ReplicatedBox rBox = ReplicatedBoxes.forLoginAtDomain("admin");
		assertNull(rBox);
	}

	@Test
	public void testDeletedMbox() {
		String box = "bm.lan!DELETED.user.david.yeahyeah.5C614D43";
		ReplicatedBox rBox = ReplicatedBoxes.forCyrusMailbox(box);
		assertEquals("david", rBox.local);
		assertEquals("bm_lan", rBox.partition);
		assertEquals("yeahyeah/5C614D43", rBox.folderName);
	}

	@Test
	public void testDeletedRootMbox() {
		String box = "d7acb642.internal!DELETED.user.rozenberga";
		ReplicatedBox rBox = ReplicatedBoxes.forCyrusMailbox(box);
		assertEquals("rozenberga", rBox.local);
		assertEquals("d7acb642.internal", rBox.partition);
		assertEquals("INBOX", rBox.folderName);
	}

	@Test
	public void testSharedDeletedMbox() {
		String box = "bm.lan!DELETED.mailshare.yeahyeah.5C614D43";
		ReplicatedBox rBox = ReplicatedBoxes.forCyrusMailbox(box);
		assertEquals("mailshare", rBox.local);
		assertEquals("bm_lan", rBox.partition);
		assertEquals("yeahyeah/5C614D43", rBox.folderName);
	}

}
