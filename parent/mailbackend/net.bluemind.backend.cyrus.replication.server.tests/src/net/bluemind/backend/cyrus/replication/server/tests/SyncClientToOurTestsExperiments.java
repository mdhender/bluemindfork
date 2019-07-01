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

import java.io.InputStream;
import java.util.UUID;

import org.junit.Test;

import net.bluemind.backend.cyrus.replication.client.SyncClient;

public class SyncClientToOurTestsExperiments {

	@Test
	public void testConnect() {
		SyncClient sc = new SyncClient("127.0.0.1", 2501);
		sc.connect().thenCompose(v -> {
			System.out.println("Connected");
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).join();
	}

	@Test
	public void testConnectThenApplyMailbox() {
		SyncClient sc = new SyncClient("127.0.0.1", 2501);
		sc.connect().thenCompose(v -> {
			System.out.println("Connected");
			return sc.authenticate("admin0", "admin");
		}).thenCompose(v -> {
			System.out.println("auth, now apply....");
			return sc.rawCommand("APPLY MAILBOX %(UNIQUEID 6399ac9b-a1b7-4b44-a1ea-41700c5c6355 "
					+ "MBOXNAME ex2016.vmw!marketing SYNC_CRC 2018734627 "
					+ "SYNC_CRC_ANNOT 0 LAST_UID 16 HIGHESTMODSEQ 22 "
					+ "RECENTUID 0 RECENTTIME 0 LAST_APPENDDATE 1531761979 " //
					+ "POP3_LAST_LOGIN 0 POP3_SHOW_AFTER 0 UIDVALIDITY 1531392047 " //
					+ "PARTITION bm-master__ex2016_vmw "
					+ "ACL \"anyone\tp\tadmin0\tlrswipkxtecda\t83C21B7E-F4FE-4CF7-9197-4512A7FAFC4C@ex2016.vmw\tlrswipkxtecd\t\" "
					+ "OPTIONS P RECORD ("
					+ "%(UID 12 MODSEQ 18 LAST_UPDATED 1531761976 FLAGS ($NotJunk NotJunk) INTERNALDATE 1230571924 SIZE 6171233 GUID 3d1fb37cb62ac969a51809d2b4e07059a7bdb9b9) "
					+ "%(UID 13 MODSEQ 19 LAST_UPDATED 1531761977 FLAGS ($NotJunk NotJunk) INTERNALDATE 1359235380 SIZE 5799603 GUID f6db90a0590b22658f6c595167db5b54220fd8ab) "
					+ "%(UID 14 MODSEQ 20 LAST_UPDATED 1531761977 FLAGS ($NotJunk NotJunk) INTERNALDATE 1264159159 SIZE 5597518 GUID 52ca48ea8dff77bdc460d498ffd05b28c856eb34) "
					+ "%(UID 15 MODSEQ 21 LAST_UPDATED 1531761978 FLAGS ($NotJunk NotJunk) INTERNALDATE 1235037445 SIZE 6000400 GUID 941f803bc1a9039ed0cec186bac924b113f60824) "
					+ "%(UID 16 MODSEQ 22 LAST_UPDATED 1531761979 FLAGS ($NotJunk NotJunk) INTERNALDATE 1260464837 SIZE 5701216 GUID c1120437ed620ee7ea14ac73d9cfc356df2e33ee)))");
		}).thenCompose(v -> {
			System.out.println("applied");
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).join();
	}

	private InputStream open(String n) {
		return SyncClientToOurTestsExperiments.class.getClassLoader().getResourceAsStream(n);
	}

	@Test
	public void testApplyMessage() {
		SyncClient sc = new SyncClient("127.0.0.1", 2501);
		sc.connect().thenCompose(con -> {
			System.out.println("Connected");
			return sc.authenticate("admin0", "admin");
		}).thenCompose(auth -> {
			System.out.println("Auth");
			return sc.applyMessage("vagrant_vmw", UUID.randomUUID().toString(), open("data/eml/with_inlines.eml"));
		}).thenCompose(v -> {
			System.out.println("Applied");
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).join();
	}

	@Test
	public void testApplyAnnotation() {
		SyncClient sc = new SyncClient("127.0.0.1", 2501);
		sc.connect().thenCompose(con -> {
			System.out.println("Connected");
			return sc.authenticate("admin0", "admin");
		}).thenCompose(auth -> {
			System.out.println("Auth");
			return sc.applyAnnotation("ex2016.vmw!user.tom", "/vendor/blue-mind/replication/id", "tom@ex2016.vmw",
					"43");
		}).thenCompose(v -> {
			System.out.println("Annot");
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).join();
	}

}
