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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.cyrus.replication.server.tests;

import java.util.Arrays;

import org.junit.Test;

import net.bluemind.backend.cyrus.replication.client.SyncClient;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusGUID;

public class SyncClientToCyrusTestsExperiments {

	@Test
	public void testConnectTLS() {
		SyncClient sc = new SyncClient("bm1604.vagrant.vmw", 2501);
		sc.connect().thenCompose(v -> {
			System.out.println("Connected.");
			return sc.startTLS();
		}).thenCompose(tls -> {
			System.out.println("TLS-ed");
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).join();
	}

	@Test
	public void testGetExistingUserSimple() {
		SyncClient sc = new SyncClient("bm1604.vagrant.vmw", 2501);
		sc.connect().thenCompose(v -> {
			System.out.println("Connected.");
			return sc.startTLS();
		}).thenCompose(tlsResp -> {
			System.out.println("TLS-ed");
			return sc.authenticate("admin0", "admin");
		}).thenCompose(authResp -> {
			System.out.println("Authenticated");
			return sc.getMeta("admin@vagrant.vmw");
		}).thenCompose(getMetaResp -> {
			System.out.println("GetMeta: " + getMetaResp);
			return sc.getUser("admin@vagrant.vmw");
		}).thenCompose(getUserResp -> {
			System.out.println("GetUser: " + getUserResp);
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).join();
	}

	@Test
	public void testGetBmhiddenSysadmin() {
		SyncClient sc = new SyncClient("bm1604.vagrant.vmw", 2501);
		sc.connect().thenCompose(v -> {
			System.out.println("Connected.");
			return sc.startTLS();
		}).thenCompose(tlsResp -> {
			System.out.println("TLS-ed");
			return sc.authenticate("admin0", "admin");
		}).thenCompose(authResp -> {
			System.out.println("Authenticated");
			return sc.getMeta("bmhiddensysadmin@vagrant.vmw");
		}).thenCompose(getMetaResp -> {
			System.out.println("getMetaResp: " + getMetaResp);
			return sc.getUser("bmhiddensysadmin@vagrant.vmw");
		}).thenCompose(getUserResp -> {
			System.out.println("GetUser: " + getUserResp);
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).join();
	}

	@Test
	public void testApplyReserve() {
		SyncClient sc = new SyncClient("bm1604.vagrant.vmw", 2501);
		sc.connect().thenCompose(v -> {
			System.out.println("Connected.");
			return sc.startTLS();
		}).thenCompose(tlsResp -> {
			System.out.println("TLS-ed");
			return sc.authenticate("admin0", "admin");
		}).thenCompose(authResp -> {
			System.out.println("Authenticated");
			return sc.applyReserve("vagrant_vmw", Arrays.asList("vagrant.vmw!user.admin"),
					Arrays.asList(CyrusGUID.randomGuid()));
		}).thenCompose(reserve -> {
			System.out.println("reserve: " + reserve);
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).join();
	}

	@Test
	public void testGetMissingUser() {
		SyncClient sc = new SyncClient("bm1604.vagrant.vmw", 2501);
		sc.connect().thenCompose(v -> {
			System.out.println("Connected.");
			return sc.startTLS();
		}).thenCompose(tlsResp -> {
			System.out.println("TLS-ed");
			return sc.authenticate("admin0", "admin");
		}).thenCompose(authResp -> {
			System.out.println("Authenticated");
			return sc.getUser("missing@vagrant.vmw");
		}).thenCompose(getUserResp -> {
			System.out.println("GetUser: " + getUserResp);
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).join();
	}

	@Test
	public void testGetMailboxes() {
		SyncClient sc = new SyncClient("bm1604.vagrant.vmw", 2501);
		sc.connect().thenCompose(v -> {
			System.out.println("Connected.");
			return sc.startTLS();
		}).thenCompose(tlsResp -> {
			System.out.println("TLS-ed");
			return sc.authenticate("admin0", "admin");
		}).thenCompose(authResp -> {
			System.out.println("Authenticated");
			return sc.getMailboxes("vagrant.vmw!user.admin");
		}).thenCompose(getMailboxesResp -> {
			System.out.println("GetMailboxes: " + getMailboxesResp);
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).join();
	}

	@Test
	public void testGetMissingUserOnMissingDomain() {
		SyncClient sc = new SyncClient("bm1604.vagrant.vmw", 2501);
		sc.connect().thenCompose(v -> {
			System.out.println("Connected.");
			return sc.startTLS();
		}).thenCompose(tlsResp -> {
			System.out.println("TLS-ed");
			return sc.authenticate("admin0", "admin");
		}).thenCompose(authResp -> {
			System.out.println("Authenticated");
			return sc.getUser("missing@notexi.st");
		}).thenCompose(getUserResp -> {
			System.out.println("GetUser: " + getUserResp);
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).join();
	}

	@Test
	public void testGetMissingUserLocalPartOnly() {
		SyncClient sc = new SyncClient("bm1604.vagrant.vmw", 2500);
		sc.connect().thenCompose(v -> {
			System.out.println("TLS-ed");
			return sc.authenticate("admin0", "admin");
		}).thenCompose(authResp -> {
			System.out.println("Authenticated");
			return sc.getMailboxes("test.lab!user.missing");
		}).thenCompose(getMailboxes -> {
			System.out.println("Got mailboxes " + getMailboxes);
			return sc.getUser("missing");
		}).thenCompose(getUserResp -> {
			System.out.println("GetUser: " + getUserResp);
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).join();
	}

}
