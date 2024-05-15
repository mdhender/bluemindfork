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
package net.bluemind.imap.vt.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.base.Stopwatch;

import net.bluemind.imap.vt.StoreClient;
import net.bluemind.imap.vt.dto.IdleContext;
import net.bluemind.imap.vt.dto.ListResult;
import net.bluemind.imap.vt.dto.UidFetched;

public class OutlookStyleTests extends BaseClientTests {

	int cnt = 1;

	@Override
	public void before() throws Exception {
		super.before();
		CompletableFuture<?>[] comp = new CompletableFuture<?>[cnt];
		for (int i = 0; i < comp.length; i++) {
			final int uCount = i;
			comp[i] = CompletableFuture.runAsync(() -> addUser("u" + uCount, domUid));
		}
		CompletableFuture.allOf(comp).join();
	}

	@Test
	public void outlookIdleLoop() {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "u0" + "@" + domUid, "u0")) {
			assertTrue(sc.login());
			ListResult folders = sc.list("", "*");
			Stopwatch idling = Stopwatch.createUnstarted();
			Stopwatch total = Stopwatch.createStarted();
			for (int i = 0; i < 1024; i++) {
				outlookLoop(idling, sc, folders);
			}
			System.err.println("idled for " + idling.elapsed(TimeUnit.MILLISECONDS) + "ms out of "
					+ total.elapsed(TimeUnit.MILLISECONDS) + "ms");

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void outlookLoop(Stopwatch idling, StoreClient sc, ListResult folders) throws IOException {
		for (var li : folders) {
			if (li.isSelectable()) {
				sc.select(li.getName());
				timedIdle(sc, idling);
				List<UidFetched> fetch = sc.uidFetchHeaders("1:*", "Message-Id");
				assertNotNull(fetch);
				timedIdle(sc, idling);
			}
		}
	}

	private void timedIdle(StoreClient sc, Stopwatch idling) throws IOException {
		idling.start();
		IdleContext ctx = sc.idle((c, ev) -> {
		});
		ctx.done();
		ctx.join();
		idling.stop();
	}

}
