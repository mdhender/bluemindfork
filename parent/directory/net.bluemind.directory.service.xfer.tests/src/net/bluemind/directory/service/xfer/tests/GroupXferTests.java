/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.directory.service.xfer.tests;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class GroupXferTests extends AbstractMultibackendTests {
	protected String ouUid = "ou-" + UUID.randomUUID().toString();
	protected String groupUid = "grp-" + UUID.randomUUID().toString();
	protected Group group;

	protected String user1Uid;
	protected String user2Uid;

	protected IGroup groupApi;

	@Before
	public void setupGroup() {
		PopulateHelper.addOrgUnit(domainUid, ouUid, "myou", null);
		user1Uid = PopulateHelper.addUser("u1", domainUid, Routing.internal);
		user2Uid = PopulateHelper.addUser("u2", domainUid, Routing.internal);

		groupApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class, domainUid);
		group = new Group();
		group.name = "mygroup";
		group.dataLocation = cyrusServer1.uid;
		group.orgUnitUid = ouUid;
		group.description = "Group Description";
		group.archived = false;
		group.hidden = false;
		group.hiddenMembers = false;
		group.system = false;
		group.emails = Arrays.asList(Email.create(group.name + "@" + domainUid, true));
		group.mailArchived = true;
		groupApi.create(groupUid, group);
		groupApi.add(groupUid, Arrays.asList(Member.user(userUid), Member.user(user1Uid), Member.user(user2Uid)));
	}

	@Test
	public void testXferGroup() {
		assertEquals(group.dataLocation, cyrusServer1.uid);

		TaskRef tr = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.xfer(groupUid, shardIp);
		waitTaskEnd(tr);

		Group xferedGroup = groupApi.get(groupUid);
		assertEquals(shardIp, xferedGroup.dataLocation);
	}

}
