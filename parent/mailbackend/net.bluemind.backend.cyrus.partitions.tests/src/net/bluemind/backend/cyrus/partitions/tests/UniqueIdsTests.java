/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.Test;

import net.bluemind.backend.cyrus.partitions.CyrusUniqueIds;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;

public class UniqueIdsTests {

	@Test
	public void testMailshareStableIdentifiers() {
		String uid = "E85AC5D5-9D2B-4820-979B-9B83052A7AEE";
		String domainUid = "blue-mind.net";
		Mailbox mb = new Mailbox();
		mb.type = Type.mailshare;
		mb.name = "dpo";
		ItemValue<Mailbox> ivm = ItemValue.create(uid, mb);
		UUID result = CyrusUniqueIds.forMailbox(domainUid, ivm, mb.name);
		System.err.println("result: " + result);
		assertNotNull(result);

		UUID sent = CyrusUniqueIds.forMailbox(domainUid, ivm, mb.name + "/Sent");
		System.err.println("sent: " + sent);
		assertNotNull(sent);

		mb.name = "pdo";
		UUID renamed = CyrusUniqueIds.forMailbox(domainUid, ivm, mb.name);
		System.err.println("renamed: " + renamed);

		assertEquals(result, renamed);

		mb.name = "pdo";
		UUID sentRen = CyrusUniqueIds.forMailbox(domainUid, ivm, mb.name + "/Sent");
		System.err.println("sentRen: " + sentRen);
		assertEquals(sent, sentRen);

	}

}
