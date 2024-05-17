/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.eas.http.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;

import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.eas.http.tests.helpers.CoreEmailHelper;
import net.bluemind.mailbox.api.IMailboxAclUids;

public class AbstractMailshareTest extends AbstractEasTest {

	protected ItemValue<ContainerHierarchyNode> inboxFolderUser1;

	protected ItemValue<ContainerSubscriptionModel> subUser2Write;
	protected ItemValue<ContainerSubscriptionModel> subUser3Read;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		inboxFolderUser1 = CoreEmailHelper.getUserMailFolder("user", domain.uid, "INBOX", domainUserSecurityContext);

		// user shared its mailbox to user 2 write
		List<ItemValue<ContainerSubscriptionModel>> subs2 = CoreEmailHelper.shareMailbox(domain.uid,
				IMailboxAclUids.uidForMailbox("user"), "user2", Verb.Write, domainUser2SecurityContext);
		assertEquals(1, subs2.size());
		subUser2Write = subs2.get(0);

		// user shared its mailbox to user 3 read
		List<ItemValue<ContainerSubscriptionModel>> subs3 = CoreEmailHelper.shareMailbox(domain.uid,
				IMailboxAclUids.uidForMailbox("user"), "user3", Verb.Read, domainUser3SecurityContext);
		assertEquals(1, subs3.size());
		subUser3Read = subs3.get(0);

	}
}
