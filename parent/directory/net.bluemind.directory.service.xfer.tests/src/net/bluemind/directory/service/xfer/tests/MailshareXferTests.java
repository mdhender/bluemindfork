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
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUserSubscription;

public class MailshareXferTests extends AbstractMultibackendTests {
	protected ItemValue<Mailshare> mailshare;

	protected String user1Uid;
	protected IMailshare mailshareApi;

	@Before
	public void setupGroup() {
		user1Uid = PopulateHelper.addUser("u1", domainUid, Routing.internal);
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		mailshareApi = provider.instance(IMailshare.class, domainUid);

		Mailshare ms = new Mailshare();
		ms.routing = Routing.internal;
		ms.name = "share-" + System.currentTimeMillis();
		ms.emails = Arrays.asList(Email.create(ms.name + "@" + domainUid, true));

		mailshareApi.create(ms.name, ms);
		mailshare = mailshareApi.getComplete(ms.name);
		assertNotNull(mailshare);

		IContainerManagement aclApi = provider.instance(IContainerManagement.class,
				IMailboxAclUids.uidForMailbox(ms.name));
		aclApi.setAccessControlList(Arrays.asList(AccessControlEntry.create(user1Uid, Verb.Write)));

		IUserSubscription userSubscriptionApi = provider.instance(IUserSubscription.class, domainUid);
		ContainerSubscription cs = new ContainerSubscription();
		cs.containerUid = IMailboxAclUids.uidForMailbox(ms.name);
		userSubscriptionApi.subscribe(user1Uid, Arrays.asList(cs));
	}

	@Test
	public void testXferMailshare() {
		assertEquals(mailshare.value.dataLocation, cyrusServer1.uid);

		TaskRef tr = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.xfer(mailshare.uid, shardIp);
		waitTaskEnd(tr);

		Mailshare xferedMailshare = mailshareApi.get(mailshare.uid);
		assertEquals(shardIp, xferedMailshare.dataLocation);
	}

}
