/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import net.bluemind.backend.cyrus.replication.testhelper.MailboxUniqueId;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class DbReplicatedMailboxesServiceTests extends AbstractReplicatedMailboxesServiceTests<IDbReplicatedMailboxes> {

	protected IDbReplicatedMailboxes getService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbReplicatedMailboxes.class, partition,
				mboxDescriptor.fullName());
	}

	@Test
	public void testCrud() {
		IDbReplicatedMailboxes mboxes = getService(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		System.out.println("impl: " + mboxes.getClass().getCanonicalName());
		String boxUid = MailboxUniqueId.random();
		MailboxReplica replica = new MailboxReplica();
		replica.name = "INBOX";
		replica.fullName = "INBOX";
		replica.options = "P";
		mboxes.create(boxUid, replica);
		ItemValue<MailboxFolder> loaded = mboxes.getComplete(boxUid);
		assertNotNull(loaded);
	}

}
