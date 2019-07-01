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
package net.bluemind.mailbox.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import net.bluemind.backend.cyrus.MigrationPhase;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.imap.Acl;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class CheckAndRepairMigrationPhaseTests extends AbstractMailboxServiceTests {

	@Test
	public void checkAndRepairAcl() throws Exception {
		IMailboxes service = getService(defaultSecurityContext);
		assertNotNull(service);
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IUser userApi = prov.instance(IUser.class, domainUid);
		ItemValue<User> adminUser = userApi.byLogin("admin");
		assertNotNull(adminUser);

		TaskRef ref = service.checkAndRepair(adminUser.uid);
		waitEnd(ref);

		String folderRoot = "h" + System.currentTimeMillis();
		String folderB = folderRoot + "/b";
		String folderC = folderRoot + "/b/c";

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, "admin@" + domainUid, "admin")) {
			assertTrue(sc.login());
			sc.create(folderRoot);
			sc.create(folderB);
			sc.create(folderC);
		}

		IContainerManagement cmgmt = prov.instance(IContainerManagement.class,
				IMailboxAclUids.uidForMailbox(adminUser.uid));
		List<AccessControlEntry> entries = new ArrayList<>();
		entries.add(AccessControlEntry.create(adminUser.uid, Verb.All));
		entries.add(AccessControlEntry.create("admin0", Verb.All));
		cmgmt.setAccessControlList(entries);

		// remove folderB admin@domain acl
		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			String mbox = "user/admin/" + folderB + "@" + domainUid;
			sc.deleteAcl(mbox, "admin@" + domainUid);
			Map<String, Acl> acl = sc.listAcl(mbox);
			assertEquals(1, acl.size());
			assertNull(acl.get("admin@" + domainUid));
			assertTrue(acl.get("admin0").equals(Acl.ALL));

		}

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, "admin@" + domainUid, "admin")) {
			assertTrue(sc.login());
			assertFalse(sc.select(folderB));
		}

		// migration phase, acl should not be fixed
		MigrationPhase.migrationPhase = true;

		ref = service.checkAndRepair(adminUser.uid);
		waitEnd(ref);

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, "admin@" + domainUid, "admin")) {
			assertTrue(sc.login());
			assertFalse(sc.select(folderB));

			Map<String, Acl> acl = sc.listAcl(folderB);
			assertEquals(1, acl.size());
			assertNull(acl.get("admin@" + domainUid));
			assertTrue(acl.get("admin0").equals(Acl.ALL));
		}

		// not migration phase, acl should be fixed
		MigrationPhase.migrationPhase = false;

		ref = service.checkAndRepair(adminUser.uid);
		waitEnd(ref);

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, "admin@" + domainUid, "admin")) {
			assertTrue(sc.login());
			assertTrue(sc.select(folderB));

			Map<String, Acl> holeBAcl = sc.listAcl(folderB);
			assertTrue(holeBAcl.get("admin0").equals(Acl.ALL));
			assertTrue(holeBAcl.get("admin@" + domainUid).equals(Acl.ALL));
		}
	}

	@Override
	protected IMailboxes getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IMailboxes.class, domainUid);
	}

	private TaskStatus waitEnd(TaskRef ref) throws Exception {
		return waitEnd(ref, TaskStatus.State.Success);
	}

	private TaskStatus waitEnd(TaskRef ref, TaskStatus.State expectedState) throws Exception {
		TaskStatus status = null;
		while (true) {
			ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, ref.id);
			status = task.status();
			if (status.state.ended) {
				break;
			}
		}

		assertTrue(status.state == expectedState);
		return status;
	}

}
