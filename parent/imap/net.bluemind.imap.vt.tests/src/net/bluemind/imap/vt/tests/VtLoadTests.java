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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.netty.buffer.Unpooled;
import net.bluemind.imap.vt.StoreClient;
import net.bluemind.imap.vt.dto.IdleContext;
import net.bluemind.imap.vt.dto.IdleListener;
import net.bluemind.imap.vt.dto.ListResult;
import net.bluemind.imap.vt.dto.UidFetched;

public class VtLoadTests extends BaseClientTests {

	int cnt = 250;
	int idle_wake_ups = 3;
	private long creates;

	@Override
	public void before() throws Exception {
		super.before();
		long time = System.currentTimeMillis();
		CompletableFuture<?>[] comp = new CompletableFuture<?>[cnt];
		for (int i = 0; i < comp.length; i++) {
			final int uCount = i;
			comp[i] = CompletableFuture.runAsync(() -> addUser("u" + uCount, domUid));
		}
		CompletableFuture.allOf(comp).join();
		this.creates = System.currentTimeMillis() - time;

	}

	@Test
	public void rampUpThenIdle() throws InterruptedException {
		CountDownLatch cdl = new CountDownLatch(cnt);
		CountDownLatch returned = new CountDownLatch(cnt);
		long time = System.currentTimeMillis();
		for (int i = 0; i < cnt; i++) {
			final int uCount = i;
			System.err.println("Start u" + uCount);
			final String tn = "virt-user-" + i;
			Thread.ofVirtual().name(tn).start(() -> {
				try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "u" + uCount + "@" + domUid, "u" + uCount)) {
					sc.login();
					walkFoldersThenIdle(cdl, idle_wake_ups, tn, sc);
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.err.println("Idle loops completed for u" + uCount);
				returned.countDown();
			});

		}
		// ensure all connections are idling on inbox
		int waits = 0;
		while (true) {
			waits++;
			boolean over = cdl.await(20, TimeUnit.SECONDS);
			if (over) {
				break;
			}
			System.err.println("cdl.count " + cdl.getCount());
			assertTrue(waits < 10);
		}

		time = System.currentTimeMillis() - time;
		long append = System.currentTimeMillis() - time;
		for (int i = 0; i < cnt; i++) {
			final int uCount = i;

			byte[] eml = "From: toto@gmail.com\r\n\r\n".getBytes();
			Thread.ofVirtual().name("virt-user-" + i + "-append").start(() -> {

				try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "u" + uCount + "@" + domUid, "u" + uCount)) {
					sc.login();
					while (returned.getCount() > 0) {
						sc.append("INBOX", Unpooled.wrappedBuffer(eml));
						Thread.sleep(100);
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
		}

		assertTrue(returned.await(120, TimeUnit.SECONDS));
		append = System.currentTimeMillis() - time;
		System.err.println(
				"Create took " + creates + " ms, all idle in " + time + "ms, append & wakeups in " + append + "ms.");

	}

	private void walkFoldersThenIdle(CountDownLatch cdl, int loop, String tn, StoreClient sc) throws IOException {
		for (int i = 0; i < loop; i++) {
			ListResult allFolders = sc.list("", "*");
			for (var li : allFolders) {
				sc.select(li.getName());
			}
			sc.select("INBOX");
			final int cur = i;
			IdleContext ctx = sc.idle(new IdleListener() {

				@Override
				public void onEvent(IdleContext ctx, IdleEvent event) {
					System.err
							.println("wake up " + tn + " on " + event.payload() + " (" + (loop - cur) + " remaining)");
					ctx.done();
				}

			});
			cdl.countDown();
			ctx.join();
			List<UidFetched> messages = sc.uidFetchHeaders("1:*");
			System.err.println(tn + " has " + messages.size() + " in INBOX.");
		}
	}

}
