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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Strings;

import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesMgmt;
import net.bluemind.backend.mail.replica.api.ResolvedMailbox;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ManyMailboxesTests extends AbstractRollingReplicationTests {

	private List<String> mailboxes;

	/**
	 * each user produces 6 folders
	 */
	public static final int TOTAL = 6;
	public static final int SHARED_EVERY_N = 2;

	@Before
	@Override
	public void before() throws Exception {
		super.before();
		int CNT = TOTAL;
		this.mailboxes = new ArrayList<>(10 * CNT);
		for (int i = 1; i <= CNT; i++) {
			String uid = null;
			if (i % SHARED_EVERY_N == 0) {
				uid = "shared.junit" + Strings.padStart(Integer.toString(i), 5, '0');
				ServerSideServiceProvider apis = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
				IMailshare sharesApi = apis.instance(IMailshare.class, domainUid);
				Mailshare ms = new Mailshare();
				ms.routing = Routing.internal;
				ms.name = uid;
				sharesApi.create(ms.name, ms);
				String inName = ms.name.replace('.', '^');
				mailboxes.add(domainUid + "!" + inName);
				mailboxes.add(domainUid + "!" + inName + ".Sent");
			} else {
				uid = "junit" + Strings.padStart(Integer.toString(i), 5, '0');

				PopulateHelper.addUser(uid, domainUid, Routing.internal);

				mailboxes.add(domainUid + "!user." + uid);
				mailboxes.add(domainUid + "!user." + uid + ".Sent");
				mailboxes.add(domainUid + "!user." + uid + ".Trash");
				mailboxes.add(domainUid + "!user." + uid + ".Drafts");
				mailboxes.add(domainUid + "!user." + uid + ".Outbox");
				mailboxes.add(domainUid + "!user." + uid + ".Junk");
			}
			Thread.sleep(20);
			System.err.println("After " + uid);
		}
		System.err.println("Registered " + mailboxes.size() + " mailboxes.");
	}

	@Override
	public void after() throws Exception {
		super.after();
	}

	@Test
	public void testResolveMany() {
		IReplicatedMailboxesMgmt api = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IReplicatedMailboxesMgmt.class);
		List<ResolvedMailbox> resolved = api.resolve(mailboxes);
		assertNotNull(resolved);
		System.err.println("Resolved " + mailboxes.size() + " returns " + resolved.size());
		assertEquals(mailboxes.size(), resolved.size());
	}

}
