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
package net.bluemind.imap.vertx.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import net.bluemind.imap.vertx.ImapResponseStatus.Status;
import net.bluemind.imap.vertx.VXStoreClient;
import net.bluemind.imap.vertx.cmd.SelectResponse;

public class ConTests extends WithMailboxTests {

	@Test
	public void testConnectLoginSelectDisconnect() throws Exception {
		VXStoreClient sc = client();

		sc.login().thenCompose(login -> {
			assertEquals(Status.Ok, login.status);
			return sc.select("INBOX");
		}).thenCompose(select -> {
			assertEquals(Status.Ok, select.status);
			assertTrue(select.result.isPresent());
			SelectResponse resp = select.result.get();
			for (String s : resp.untagged) {
				System.out.println("SELECT: '" + s + "'");
			}
			return sc.close();
		}).get(15, TimeUnit.SECONDS);
	}

}
