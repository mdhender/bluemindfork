/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap;

import java.util.Arrays;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class CyradmTestCase extends IMAPTestCase {

	protected StoreClient sc;
	protected String mboxCyrusPrefix = "user/";
	protected String mboxName = "u" + System.currentTimeMillis();
	protected String domainUid = "bm.lan";
	protected String mboxLogin = mboxName + "@" + domainUid;

	protected String mboxCyrusName = mboxCyrusPrefix + mboxName + "@" + domainUid;

	public void setUp() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer);

		CyrusService cyrusService = new CyrusService(cyrusIp);
		CyrusPartition partition = cyrusService.createPartition(domainUid);
		cyrusService.refreshPartitions(Arrays.asList(domainUid));
		cyrusService.reload();

		sc = new StoreClient(cyrusIp, 1143, "admin0", "admin");
		try {
			boolean login = sc.login();
			if (!login) {
				fail("login failed for " + login + "/" + testPass);
			}

			System.err.println("creating mailbox: " + mboxCyrusName);

			CreateMailboxResult cmr = sc.createMailbox(mboxCyrusName, partition.name);
			if (!cmr.isOk()) {
				String tmp = mboxCyrusName;
				mboxCyrusName = null;
				fail("create mailbox " + tmp + " failed: " + cmr.getMessage());
			}
		} catch (IMAPException e) {
			e.printStackTrace();
			fail("exception on setup");
		}
	}

	public void tearDown() {
		try {
			if (mboxCyrusName != null) {
				System.err.println("deleting mbox " + mboxCyrusName);
				sc.setAcl(mboxCyrusName, "admin0", Acl.ALL);
				sc.deleteMailbox(mboxCyrusName);
			}
			sc.logout();
		} catch (IMAPException e) {
			e.printStackTrace();
		}
	}
}
