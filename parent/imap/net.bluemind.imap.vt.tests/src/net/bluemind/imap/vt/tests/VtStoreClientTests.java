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
package net.bluemind.imap.vt.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.imap.vt.StoreClient;
import net.bluemind.imap.vt.dto.FetchedChunk;
import net.bluemind.imap.vt.dto.IdleContext;
import net.bluemind.imap.vt.dto.IdleListener;
import net.bluemind.imap.vt.dto.ListInfo;
import net.bluemind.imap.vt.dto.ListResult;
import net.bluemind.imap.vt.dto.Mode;
import net.bluemind.imap.vt.dto.UidFetched;

public class VtStoreClientTests extends BaseClientTests {

	@Test
	public void userCannotConnect() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "JOHN")) {
			assertFalse(sc.login());
		}
	}

	@Test
	public void userCanConnect() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			for (int i = 0; i < 10; i++) {
				sc.noop();
			}

		}
	}

	@Test
	public void selectFolders() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			assertTrue(sc.select("INBOX"));
			assertTrue(sc.select("Trash"));
			assertTrue(sc.select("Inbox"));
			assertFalse(sc.select("Fille du bedouin"));
		}
	}

	@Test
	public void listFolders() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			ListResult folders = sc.list("", "*");
			assertNotNull(folders);
			assertFalse(folders.isEmpty());
			Set<String> names = folders.stream().map(ListInfo::getName).map(String::toLowerCase)
					.collect(Collectors.toSet());
			assertTrue(names.contains("inbox"));
			assertTrue(names.contains("trash"));
			int selected = 0;
			for (var li : folders) {
				if (li.isSelectable()) {
					assertTrue(sc.select(li.getName()));
					selected++;
				}
			}
			assertTrue(selected > 0);
			System.err.println("Could select " + selected + " folders");
		}
	}

	@Test
	public void appendOne() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			String source = "From: me@me.com\r\n\r\n";
			ByteBuf eml = Unpooled.wrappedBuffer(source.getBytes());
			int added = sc.append("INBOX", eml);
			System.err.println("Added: " + added);
			assertTrue(added > 0);
			assertTrue(sc.select("INBOX"));
			try (FetchedChunk fetched = sc.uidFetchMessage(added)) {
				assertNotNull(fetched);
				String reread = new String(fetched.open().readAllBytes());
				System.err.println("R: " + reread);
				assertEquals(source, reread);
			}
		}
	}

	@Test
	public void appendOneFetchPart() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			String textPart = "Yeah Yeah\r\n";
			String source = "From: me@me.com\r\n\r\n" + textPart;
			ByteBuf eml = Unpooled.wrappedBuffer(source.getBytes());
			int added = sc.append("INBOX", eml);
			System.err.println("Added: " + added);
			assertTrue(added > 0);
			assertTrue(sc.select("INBOX"));
			try (FetchedChunk fetched = sc.uidFetchPart(added, "1")) {
				assertNotNull(fetched);
				String reread = new String(fetched.open().readAllBytes());
				System.err.println("R: " + reread);
				assertEquals(textPart, reread);
			}
		}
	}

	@Test
	public void appendStoreExpunge() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			int added = addToInbox(sc);
			assertTrue(sc.select("INBOX"));
			assertTrue(sc.uidStore("" + added, Mode.SET, "\\Seen", "\\Deleted"));
			assertTrue(sc.uidStore("" + added, Mode.REMOVE, "\\Seen"));
			assertTrue(sc.uidStore("" + added, Mode.ADD, "\\Seen"));
			assertTrue(sc.expunge());
		}
	}

	@Test
	public void appendThenCopy() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			int added = addToInbox(sc);
			assertTrue(sc.select("INBOX"));
			Map<Integer, Integer> copied = sc.uidCopy("Trash", added);
			assertFalse(copied.isEmpty());
			assertNotNull(copied.get(added));
			int firstCopy = copied.get(added).intValue();
			Map<Integer, Integer> sec = sc.uidCopy("Trash", added);
			assertNotNull(sec.get(added));
			int secCopy = sec.get(added).intValue();
			System.err.println("f: " + firstCopy + ", s: " + secCopy);
			sc.select("Trash");
			Map<Integer, Integer> toInbox = sc.uidCopy("INBOX", firstCopy, secCopy);
			assertNotNull(toInbox);
		}
	}

	@Test
	public void appendStoreUidExpunge() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			int added = addToInbox(sc);
			assertTrue(sc.select("INBOX"));
			assertTrue(sc.uidStore("" + added, Mode.SET, "\\Seen", "\\Deleted"));
			assertTrue(sc.uidExpunge(added));
		}
	}

	@Test
	public void idleChecks() throws Exception {
		try (StoreClient c1 = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john");
				StoreClient c2 = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(c1.login());
			assertTrue(c2.login());

			c2.select("INBOX");
			IdleContext idleContext = c2.idle(new IdleListener() {

				@Override
				public void onEvent(IdleContext ctx, IdleEvent event) {

					System.err.println("[" + Thread.currentThread().getName() + "] I: " + event.payload());
					ctx.done();
				}

			});
			addToInbox(c1);

			System.err.println("Joining idle context");
			idleContext.join();

		}
	}

	@Test
	public void idleQuick() throws Exception {
		try (StoreClient c1 = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(c1.login());

			for (var li : c1.list("", "*")) {
				if (!li.isSelectable()) {
					continue;
				}
				c1.select("INBOX");
				IdleContext idleContext = c1.idle(new IdleListener() {

					@Override
					public void onEvent(IdleContext ctx, IdleEvent event) {
						System.err.println("[" + Thread.currentThread().getName() + "] I: " + event.payload());
					}

				});
				idleContext.done();
				idleContext.join();
				System.err.println("Finished for " + li.getName());
			}
		}
	}

	@Test
	public void fetchInternalDates() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			addToInbox(sc);
			addToInbox(sc);
			int last = addToInbox(sc);
			assertTrue(sc.select("INBOX"));
			assertTrue(sc.uidStore("" + last, Mode.ADD, "yeah"));
			List<UidFetched> fromHeader = sc.uidFetchHeaders("1:*", "From");
			assertEquals(3, fromHeader.size());
			List<UidFetched> noHeader = sc.uidFetchHeaders("1:*");
			assertEquals(3, noHeader.size());
			List<UidFetched> missingHeader = sc.uidFetchHeaders("1:*", "Message-Id");
			assertEquals(3, missingHeader.size());
			for (var f : missingHeader) {
				System.err.println("H: " + f.headers());
			}
			List<UidFetched> twoHeaders = sc.uidFetchHeaders("1:*", "X-Bm-BULLSHIT", "X-BM-FOO");
			assertEquals(3, twoHeaders.size());
			for (var f : twoHeaders) {
				System.err.println(f.headers());
				assertTrue(f.headers().containsKey("x-bm-bullshit"));
				assertEquals("crap", f.headers().get("x-bm-bullshit"));
			}
			List<UidFetched> some = sc.uidFetchHeaders("" + last);
			assertEquals(1, some.size());
			for (var i : some) {
				assertEquals(last, i.uid());
				System.err.println(i);
				assertFalse(i.flags().isEmpty());
			}
		}
	}

	private int addToInbox(StoreClient sc) throws IOException {
		String source = "From: me." + UUID.randomUUID().toString().toLowerCase() + "@me.com\r\n"
				+ "X-Bm-Bullshit: crap\r\n" + "X-Bm-Foo: bar\r\n\r\n";
		ByteBuf eml = Unpooled.wrappedBuffer(source.getBytes());
		int added = sc.append("INBOX", eml);
		assertTrue(added > 0);
		return added;
	}

}
