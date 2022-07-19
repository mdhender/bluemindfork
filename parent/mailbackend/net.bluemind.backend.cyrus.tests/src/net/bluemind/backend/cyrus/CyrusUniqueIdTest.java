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
package net.bluemind.backend.cyrus;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;

import net.bluemind.backend.cyrus.partitions.CyrusUniqueIds;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;

public class CyrusUniqueIdTest {

	@Test
	public void testGen() {
		Mailbox mb = new Mailbox();
		mb.type = Type.user;
		UUID sentUid = CyrusUniqueIds.forMailbox("492a4d51.internal",
				ItemValue.create("cli-created-21f10a7b-40c0-4320-9e09-9f6730da9758", mb), "Sent");
		System.err.println("sentUid: " + sentUid);

		UUID another = CyrusUniqueIds.forMailbox("492a4d51.internal",
				ItemValue.create("cli-created-21f10a7b-40c0-4320-9e09-9f6730da9758", mb), "Sent");
		System.err.println("another: " + sentUid);
		assertEquals(sentUid, another);
	}

}
