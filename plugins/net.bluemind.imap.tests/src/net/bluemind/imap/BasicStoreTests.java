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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;

import net.bluemind.imap.impl.MailThread;
import net.bluemind.imap.mime.MimePart;
import net.bluemind.imap.mime.MimeTree;

public class BasicStoreTests extends LoggedTestCase {

	private static final int COUNT = 500;

	public void testSelect() throws IMAPException {
		sc.select("INBOX");
	}

	public void testSelectSpeed() throws IMAPException {
		long time;

		time = System.currentTimeMillis();

		for (int i = 0; i < COUNT; i++) {
			sc.select("INBOX");
		}

		time = System.currentTimeMillis() - time;
		System.out.println(COUNT + " iterations in " + time + "ms. " + (time / COUNT) + "ms avg, "
				+ (1000.0 / ((double) time / (double) COUNT)) + " per sec.");

	}

	public void testCapability() throws IMAPException {
		Set<String> caps = sc.capabilities();
		assertNotNull(caps);
		for (String s : caps) {
			System.out.print(s);
			System.out.print(" ");
		}
		System.out.println();
	}

	public void testCreateSubUnsubRenameDelete() throws IMAPException {
		String mbox = "test" + System.currentTimeMillis();
		String newMbox = "rename" + System.currentTimeMillis();
		try {
			boolean b = sc.create(mbox);
			assertTrue(b);
			boolean sub = sc.subscribe(mbox);
			assertTrue(sub);
			sub = sc.unsubscribe(mbox);
			assertTrue(sub);

			boolean renamed = sc.rename(mbox, newMbox);
			System.out.println("Rename success: " + renamed);
			CreateMailboxResult del;
			if (!renamed) {
				del = sc.deleteMailbox(mbox);
			} else {
				del = sc.deleteMailbox(newMbox);
			}
			assertTrue(del.isOk());

		} catch (IMAPException ime) {
			fail("error on mailbox creation");
		}
	}

	public void testNoop() {
		sc.noop();
	}

	public void testNoopSpeed() {
		int count = 100000;
		long time = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			sc.noop();
		}
		time = System.currentTimeMillis() - time;
		System.out.println("Done " + count + " in " + time + "ms. " + (int) (count / (time / 1000.0)) + "/sec");
	}

	public void testList() throws IMAPException {
		ListResult lr = sc.listAll();
		assertNotNull(lr);
		System.out.println("IMAP SEPARATOR: '" + lr.getImapSeparator() + "'");
		for (ListInfo li : lr) {
			System.out.println(" => " + li.getName() + " selectable: " + li.isSelectable());
		}
	}

	public void testLsub() throws IMAPException {
		ListResult lr = sc.listSubscribed();
		assertNotNull(lr);
		System.out.println("IMAP SEPARATOR: '" + lr.getImapSeparator() + "'");
		for (ListInfo li : lr) {
			System.out.println(" => " + li.getName() + " selectable: " + li.isSelectable());
			if (li.isSelectable()) {
				sc.select(li.getName());
			}
		}
	}

	public void testAppend() throws Exception {
		FlagsList fl = new FlagsList();
		fl.add(Flag.SEEN);
		int uid = sc.append("INBOX", getRfc822Message(), fl);
		assertTrue(uid > 0);
		Date d = new Date(TimeUnit.DAYS.toMillis(5));
		IMAPByteSource msg = getUtf8Rfc822Message();
		int secondUid = sc.append("INBOX", msg.source().openStream(), fl, d);
		msg.close();
		System.out.println("Added uids : " + uid + " " + secondUid);
		assertTrue(secondUid == uid + 1);

		sc.select("INBOX");
		InternalDate[] date = sc.uidFetchInternalDate(ImmutableList.of(secondUid));
		assertTrue(date.length == 1);
		System.out.println(date[0]);
		System.out.println(d);
		assertEquals(date[0], d);
	}

	public void testAppendPerf() throws Exception {
		FlagsList fl = new FlagsList();
		fl.add(Flag.SEEN);
		int CNT = 500;
		long time = System.currentTimeMillis();
		Integer uid = 0;
		for (int i = 0; i < CNT; i++) {
			IMAPByteSource msg = getUtf8Rfc822Message();
			Integer newUid = sc.append("INBOX", msg.source().openStream(), fl);
			msg.close();
			assertTrue(newUid > uid);
			uid = newUid;
		}
		time = System.currentTimeMillis() - time;
		System.err.println(
				CNT + " IMAP append done in " + (time / 1000) + "s. Performing at " + (CNT * 1000.0 / time) + "/sec");
	}

	public void testUidFetchMessage() throws Exception {
		FlagsList fl = new FlagsList();
		fl.add(Flag.SEEN);
		IMAPByteSource msg = getUtf8Rfc822Message();
		int uid = sc.append("INBOX", msg.source().openStream(), fl);
		msg.close();
		sc.select("INBOX");
		IMAPByteSource in = sc.uidFetchMessage(uid);
		assertNotNull(in);
		try {
			System.out.println("Received:\n" + new String(in.source().read()));
		} catch (IOException e) {
			e.printStackTrace();
			fail("error");
		} finally {
			in.close();
		}

		long time = System.currentTimeMillis();

		for (int i = 0; i < COUNT; i++) {
			try (IMAPByteSource ibs = sc.uidFetchMessage(uid)) {
				ibs.source().read();
			} catch (IOException e) {
				fail(e.getMessage());
			}
		}

		time = System.currentTimeMillis() - time;
		System.out.println("time: " + time);
		System.out.println("FETCH: " + COUNT + " iterations in " + time + "ms. " + (time / COUNT) + "ms avg, "
				+ (int) (COUNT / (time / 1000.0)) + " per sec.");
	}

	public void testUidFetchAllMessages() throws IMAPException {
		sc.select("INBOX");
		Collection<Integer> uids = sc.uidSearch(new SearchQuery());

		for (int uid : uids) {
			IMAPByteSource in = sc.uidFetchMessage(uid);
			try {
				byte[] content = in.source().read();
				assertNotNull(content);
				System.out.println("Fetched uid " + uid);
			} catch (IOException e) {
				fail("error reading content " + e.getMessage());
			}
		}
		Collection<Envelope> envs = sc.uidFetchEnvelope(uids);
		assertEquals(uids.size(), envs.size());
		Collection<MimeTree> bss = sc.uidFetchBodyStructure(uids);
		for (MimeTree mt : bss) {
			for (MimePart mp : mt) {
				sc.uidFetchPart(mt.getUid(), mp.getAddress());
			}
		}
		assertEquals(uids.size(), bss.size());

	}

	public void testUidFetchBodyStructure() throws Exception {
		FlagsList fl = new FlagsList();
		fl.add(Flag.SEEN);
		IMAPByteSource ibs = getUtf8Rfc822Message();
		Collection<Integer> uid = Arrays.asList(sc.append("INBOX", ibs.source().openStream(), fl),
				sc.append("INBOX", getRfc822Message(), fl));
		ibs.close();
		sc.select("INBOX");
		sc.uidFetchBodyStructure(uid);

		long time = System.currentTimeMillis();

		for (int i = 0; i < COUNT; i++) {
			sc.uidFetchBodyStructure(uid);
		}

		time = System.currentTimeMillis() - time;
		System.out.println("time: " + time);
		System.out.println("FETCH BS: " + COUNT + " iterations in " + time + "ms. " + (time / COUNT) + "ms avg, "
				+ 1000 / ((time + 0.1) / COUNT) + " per sec.");

		time = System.currentTimeMillis();
		sc.select("INBOX");
		Collection<Integer> allUids = sc.uidSearch(new SearchQuery());
		for (int l : allUids) {
			try {
				sc.uidFetchBodyStructure(Arrays.asList(l));
			} catch (Throwable t) {
				System.err.println("Failure on uid: " + l);
				t.printStackTrace();
				Collection<IMAPHeaders> heads = sc.uidFetchHeaders(Arrays.asList(l), new String[] { "Subject" });
				System.out.println("subject: " + heads.iterator().next().getSubject());
				fail();
			}
		}
		time = System.currentTimeMillis() - time;
		System.out.println("bs for " + allUids.size() + " messages took: " + time + "ms");
	}

	public void testUidSearch() throws Exception {
		SearchQuery sq = new SearchQuery();
		sc.select("INBOX");
		// append a mail to be sure to have one message into inbox
		IMAPByteSource ibs = getUtf8Rfc822Message();
		sc.append("INBOX", ibs.source().openStream(), new FlagsList());
		ibs.close();
		Collection<Integer> uids = sc.uidSearch(sq);
		assertNotNull(uids);
		assertTrue(uids.size() > 0);

		long time = System.currentTimeMillis();

		for (int i = 0; i < COUNT; i++) {
			Collection<Integer> u = sc.uidSearch(sq);
			assertTrue(u.size() > 0);
		}

		time = System.currentTimeMillis() - time;
		System.out.println("time: " + time);
		System.out.println("UID SEARCH: " + COUNT + " iterations in " + time + "ms. " + (time / COUNT) + "ms avg, "
				+ 1000 / ((time + 0.1) / COUNT) + " per sec.");
	}

	public void testUidFetchHeadersPerf() throws Exception {
		final String[] HEADS_LOAD = new String[] { "Subject", "From", "Date", "To", "Cc", "Bcc", "X-Mailer",
				"User-Agent", "Message-ID" };

		sc.select("INBOX");
		// make sure to have at least two mails into INBOX
		IMAPByteSource msg1 = getUtf8Rfc822Message();
		IMAPByteSource msg2 = getUtf8Rfc822Message();
		sc.append("INBOX", msg1.source().openStream(), new FlagsList());
		sc.append("INBOX", msg2.source().openStream(), new FlagsList());
		msg1.close();
		msg2.close();
		Collection<Integer> uids = sc.uidSearch(new SearchQuery());
		Iterator<Integer> iterator = uids.iterator();
		Collection<Integer> firstTwo = Arrays.asList(iterator.next(), iterator.next());

		long nstime = System.nanoTime();
		for (int i = 0; i < COUNT; i++) {
			Collection<IMAPHeaders> h = sc.uidFetchHeaders(firstTwo, HEADS_LOAD);
			assertNotNull(h);
		}
		nstime = System.nanoTime() - nstime;
		System.out.println("fetchHeaders for " + firstTwo.size() + " uids took " + nstime + "ns ("
				+ (nstime / 1000000000.0) + "secs)");

	}

	public void testUidFetchHeaders() throws IMAPException {
		SearchQuery sq = new SearchQuery();
		sc.select("INBOX");
		Collection<Integer> uids = sc.uidSearch(sq);
		String[] headers = new String[] { "date", "from", "subject" };

		long nstime = System.nanoTime();
		Collection<IMAPHeaders> h = sc.uidFetchHeaders(uids, headers);
		nstime = System.nanoTime() - nstime;
		assertEquals(uids.size(), h.size());
		System.out.println(
				"fetchHeaders for " + uids.size() + " uids took " + nstime + "ns (" + (nstime / 1000000) + "ms)");

		for (IMAPHeaders header : h) {
			System.out.println("Subject: " + header.getSubject() + " Date: " + header.getDate() + " FromMail: "
					+ header.getFrom().getMail() + " FromDisp: " + header.getFrom().getDisplayName());
		}

	}

	public void testUidFetchHeadersSpeed() throws IMAPException {
		SearchQuery sq = new SearchQuery();
		sc.select("INBOX");
		String[] headers = new String[] { "x-priority" };

		long time = System.currentTimeMillis();
		Collection<Integer> uids = sc.uidSearch(sq);
		Collection<Envelope> e = sc.uidFetchEnvelope(uids);
		assertNotNull(e);
		assertEquals(uids.size(), e.size());
		Collection<IMAPHeaders> h = sc.uidFetchHeaders(uids, headers);
		assertEquals(uids.size(), h.size());

		time = System.currentTimeMillis() - time;
		System.err.println("Done in " + time + "ms.");
	}

	public void testUidFetchEnvelopePerf() throws Exception {

		sc.select("INBOX");
		IMAPByteSource msg1 = getUtf8Rfc822Message();
		IMAPByteSource msg2 = getUtf8Rfc822Message();
		sc.append("INBOX", msg1.source().openStream(), new FlagsList());
		sc.append("INBOX", msg2.source().openStream(), new FlagsList());
		msg1.close();
		msg2.close();

		Collection<Integer> uids = sc.uidSearch(new SearchQuery());
		Iterator<Integer> it = uids.iterator();
		Collection<Integer> firstTwo = Arrays.asList(it.next(), it.next());

		long nstime = System.nanoTime();
		for (int i = 0; i < COUNT; i++) {
			Collection<Envelope> h = sc.uidFetchEnvelope(firstTwo);
			assertNotNull(h);
		}
		nstime = System.nanoTime() - nstime;
		System.err.println(
				"fetchEnv for " + firstTwo + " uids took " + nstime + "ns (" + (nstime / 1000000000.0) + "secs)");

	}

	public void testUidFetchEnvelopeReliable() throws IMAPException {
		SearchQuery sq = new SearchQuery();
		sc.select("INBOX");
		Collection<Integer> uids = sc.uidSearch(sq);

		for (Integer l : uids) {
			try {
				Collection<Envelope> h = sc.uidFetchEnvelope(Arrays.asList(l));
				assertEquals(1, h.size());
			} catch (Throwable t) {
				System.err.println("failed on uid " + l);
				t.printStackTrace();
				fail();
			}
		}
	}

	public void testUidFetchFlags() throws Exception {
		SearchQuery sq = new SearchQuery();
		sc.select("INBOX");

		IMAPByteSource msg1 = getUtf8Rfc822Message();
		IMAPByteSource msg2 = getUtf8Rfc822Message();
		sc.append("INBOX", msg1.source().openStream(), new FlagsList());
		sc.append("INBOX", msg2.source().openStream(), new FlagsList());
		msg1.close();
		msg2.close();

		Collection<Integer> uids = sc.uidSearch(sq);

		Iterator<Integer> iterator = uids.iterator();
		List<Integer> firstTwo = Arrays.asList(iterator.next(), iterator.next());

		long nstime = System.nanoTime();
		Collection<FlagsList> h = sc.uidFetchFlags(firstTwo);
		nstime = System.nanoTime() - nstime;
		assertEquals(firstTwo.size(), h.size());
		System.out.println(
				"fetchFlags for " + firstTwo.size() + " uids took " + nstime + "ns (" + (nstime / 1000000) + "ms)");

		nstime = System.nanoTime();
		h = sc.uidFetchFlags(uids);
		nstime = System.nanoTime() - nstime;
		assertEquals(uids.size(), h.size());
		System.out.println(
				"fetchFlags for " + uids.size() + " uids took " + nstime + "ns (" + (nstime / 1000000) + "ms)");
	}

	public void testUidExpunge() throws IMAPException {
		FlagsList fl = new FlagsList();
		fl.add(Flag.SEEN);
		fl.add(Flag.DELETED);
		int uid = sc.append("INBOX", getRfc822Message(), fl);
		assertTrue(uid > 0);
		sc.select("INBOX");
		InternalDate[] date = sc.uidFetchInternalDate(ImmutableList.of(uid));
		assertTrue(date.length == 1);
		sc.uidExpunge(ImmutableList.of(uid));
		date = sc.uidFetchInternalDate(ImmutableList.of(uid));
		assertTrue(date.length == 0);
	}

	public void testUidCopy() throws Exception {
		SearchQuery sq = new SearchQuery();
		sc.select("INBOX");

		for (int i = 0; i < 4; i++) {
			IMAPByteSource msg = getUtf8Rfc822Message();
			sc.append("INBOX", msg.source().openStream(), new FlagsList());
			msg.close();
		}

		Collection<Integer> uids = sc.uidSearch(sq);

		Iterator<Integer> it = uids.iterator();
		int one = it.next();
		int two = it.next();
		int three = it.next();
		int four = it.next();
		Collection<Integer> toCopy = Arrays.asList(one, two, four, 99);

		long nstime = System.nanoTime();
		Map<Integer, Integer> result = sc.uidCopy(toCopy, "Sent");
		nstime = System.nanoTime() - nstime;
		assertNotNull(result);
		assertEquals(toCopy.size() - 1, result.size()); // -1 because uid 99 is
														// invalid
		assertFalse(result.keySet().contains(99));
		System.out
				.println("uidCopy for " + toCopy.size() + " uids took " + nstime + "ns (" + (nstime / 1000000) + "ms)");
	}

	public void testUidStore() throws Exception {
		SearchQuery sq = new SearchQuery();
		sc.select("INBOX");

		IMAPByteSource msg1 = getUtf8Rfc822Message();
		IMAPByteSource msg2 = getUtf8Rfc822Message();
		sc.append("INBOX", msg1.source().openStream(), new FlagsList());
		sc.append("INBOX", msg2.source().openStream(), new FlagsList());
		msg1.close();
		msg2.close();

		Collection<Integer> uids = sc.uidSearch(sq);

		Iterator<Integer> it = uids.iterator();
		Collection<Integer> firstTwo = Arrays.asList(it.next(), it.next());

		FlagsList fl = new FlagsList();
		fl.add(Flag.ANSWERED);
		long nstime = System.nanoTime();
		boolean result = sc.uidStore(firstTwo, fl, true);
		nstime = System.nanoTime() - nstime;
		assertTrue(result);
		System.out.println(
				"uidStore for " + firstTwo.size() + " uids took " + nstime + "ns (" + (nstime / 1000000) + "ms)");
		result = sc.uidStore(firstTwo, fl, false);
		assertTrue(result);
	}

	public void testUidFetchPartBroken() throws IMAPException {
		// allows test to be green bar when not running on my computer
		try {
			boolean selection = sc.select("Shared Folders/partage");
			if (!selection) {
				return;
			}
		} catch (IMAPException ime) {
			return;
		}

		Collection<MimeTree> mts = sc.uidFetchBodyStructure(Arrays.asList(1));
		if (mts.size() == 1) {
			System.out.println("mts[0]" + mts.iterator().next().toString());
			IMAPByteSource part = sc.uidFetchPart(1, "1");
			try {
				part.source().read();
			} catch (IOException e) {
				e.printStackTrace();
				fail();
			}
		}
		// InputStream in = sc.uidFetchPart(uid, "1");
	}

	public void testUidThreads() throws Exception {
		sc.select("INBOX");

		IMAPByteSource msg1 = getUtf8Rfc822Message();
		IMAPByteSource msg2 = getUtf8Rfc822Message();
		sc.append("INBOX", msg1.source().openStream(), new FlagsList());
		sc.append("INBOX", msg2.source().openStream(), new FlagsList());
		msg1.close();
		msg2.close();

		List<MailThread> threads = sc.uidThreads();
		assertNotNull(threads);
		assertTrue(threads.size() > 0);
	}

	public void testUidFetchPart() throws Exception {
		SearchQuery sq = new SearchQuery();
		sc.select("INBOX");

		IMAPByteSource msg1 = getUtf8Rfc822Message();
		IMAPByteSource msg2 = getUtf8Rfc822Message();
		sc.append("INBOX", msg1.source().openStream(), new FlagsList());
		sc.append("INBOX", msg2.source().openStream(), new FlagsList());
		msg1.close();
		msg2.close();

		Collection<Integer> uids = sc.uidSearch(sq);
		Integer uid = uids.iterator().next();

		long nstime = System.nanoTime();
		IMAPByteSource in = sc.uidFetchPart(uid, "1");
		nstime = System.nanoTime() - nstime;
		System.out.println(
				"uidFetchPart took took " + nstime + "ns (" + (nstime / 1000000) + "ms) for " + in.size() + "byte(s)");
		assertNotNull(in);
		try {
			in.source().read();
		} catch (IOException e) {
			e.printStackTrace();
			fail("Cannot dump part stream");
		} finally {
			in.close();
		}
	}

	public void testNamespace() {
		NameSpaceInfo nsi = sc.namespace();
		assertNotNull(nsi);
		System.out.println("perso: '" + nsi.getPersonal() + "'");
		System.out.println("other: '" + nsi.getOtherUsers() + "'");
		System.out.println("shared: '" + nsi.getMailShares() + "'");
	}

	public void testUidFetchSummary() throws IMAPException {
		sc.select("INBOX");
		int mailCount = sc.uidSearch(new SearchQuery()).size();
		Collection<Summary> summaries = sc.uidFetchSummary("1:*");
		assertNotNull(summaries);
		assertEquals(mailCount, summaries.size());
	}

}
