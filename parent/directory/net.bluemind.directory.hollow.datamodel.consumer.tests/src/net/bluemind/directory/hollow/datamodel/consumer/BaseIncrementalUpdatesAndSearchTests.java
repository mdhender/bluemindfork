/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.directory.hollow.datamodel.consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.fs.HollowFilesystemAnnouncer;

import net.bluemind.directory.hollow.datamodel.AddressBookRecord;
import net.bluemind.directory.hollow.datamodel.AnrToken;
import net.bluemind.directory.hollow.datamodel.Email;
import net.bluemind.directory.hollow.datamodel.OfflineAddressBook;

public abstract class BaseIncrementalUpdatesAndSearchTests {
	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	@AfterClass
	public static void dropDir() {
		System.clearProperty(DirectoryDeserializer.BASE_DIR_PROP);
		System.err.println("Cleared prop, DIR is now " + DirectoryDeserializer.baseDataDir());
	}

	private File file;
	private BrowsableDirectorySearch defaultSearch;
	AddressBookRecord rec1;
	AddressBookRecord rec2;
	AddressBookRecord rec3;
	AddressBookRecord rec4;
	private OfflineAddressBook book;
	private IncrementalProductionStrategy strat;

	@Before
	public void setup() throws Exception {
		long time = System.currentTimeMillis();
		file = new File(System.getProperty("java.io.tmpdir"), "" + time + ".data");
		File withDomain = new File(file, "bm.loc");
		withDomain.mkdirs();
		System.setProperty(DirectoryDeserializer.BASE_DIR_PROP, file.getAbsolutePath());
		file = withDomain;

		this.book = new OfflineAddressBook();
		book.domainName = "bm.loc";
		book.domainAliases = new HashSet<>(Arrays.asList("bm.lan"));

		rec1 = record(1, "user");
		rec2 = record(2, "user");
		rec3 = record(3, "user");
		rec3.hidden = true;
		rec4 = record(4, "group");

		CountDownLatch wait = new CountDownLatch(1);

		this.strat = strategy(file, wait);

		serialize(rec1, rec2, rec3, rec4);
		wait.await(60, TimeUnit.SECONDS);

		this.defaultSearch = DirectorySearchFactory.browser("bm.loc");
	}

	protected abstract IncrementalProductionStrategy strategy(File dir, CountDownLatch latch);

	@After
	public void teardown() {
		Path dir = file.toPath();
		try (Stream<Path> stream = Files.walk(dir, FileVisitOption.FOLLOW_LINKS)) {
			stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void serialize(AddressBookRecord... records) {
		serialize(Collections.emptyList(), records);
	}

	private void serialize(List<Long> toDrop, AddressBookRecord... records) {
		book.sequence++;
		strat.produce(toDrop, book, records);
	}

	private AddressBookRecord record(int minimalId, String kind) {
		AddressBookRecord record = new AddressBookRecord();
		record.uid = "uid" + minimalId;
		record.minimalid = minimalId;
		record.name = "Entity " + minimalId;
		record.distinguishedName = kind + "/" + record.uid;
		record.kind = kind;
		record.email = record.uid + "@bm.loc";
		record.emails = Arrays.asList(new Email(record.email, Arrays.asList(record.email), true, true),
				new Email("alt-" + record.email, Arrays.asList("alt-" + record.email), false, false));
		record.created = new Date();
		record.updated = record.created;
		AnrToken anr = new AnrToken();
		anr.token = record.uid;
		record.anr = Arrays.asList(anr);
		return record;

	}

	public static class Announcer extends HollowFilesystemAnnouncer implements HollowProducer.Announcer {

		private CountDownLatch wait;

		public Announcer(File publishDir) {
			super(publishDir.toPath());
		}

		@Override
		public void announce(long stateVersion) {
			super.announce(stateVersion);
			System.err.println("ANNOUNCE " + stateVersion + "...");
			wait.countDown();
		}

		void setWait(CountDownLatch wait) {
			this.wait = wait;
		}

	}

	@Test
	public void testStableSearchWithConcurrentUpdates() throws InterruptedException {
		int count = 5000;
		int start = 10;
		int existingUserCount = 4;
		int hiddenUserCount = 1;
		System.err.println("testStableSearchWithConcurrentUpdates STARTS...");
		checkByEmail("uid2@bm.loc", 2);
		checkByEmail("uid4@bm.loc", 4);

		System.err.println("Update 1,3,4");
		serialize(rec1, rec3, rec4);
		checkByEmail("uid2@bm.loc", 2);
		checkByEmail("uid4@bm.loc", 4);

		Collection<net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord> anrSearch = defaultSearch
				.byNameOrEmailPrefix("uid4");
		assertFalse(anrSearch.isEmpty());
		for (net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord abr : anrSearch) {
			System.err.println("Got " + abr.getName());
		}

		AddressBookRecord[] empty = new AddressBookRecord[0];
		List<AddressBookRecord> toFlush = new ArrayList<>();
		AtomicBoolean stop = new AtomicBoolean();
		LongAdder errors = new LongAdder();
		LongAdder checks = new LongAdder();
		Runnable r = () -> {
			ThreadLocalRandom rand = ThreadLocalRandom.current();
			while (!stop.get()) {
				checks.increment();
				try {
					int toCheck = rand.nextInt(1, 5);
					checkByEmail("uid" + toCheck + "@bm.loc", toCheck);
				} catch (Throwable t) {
					System.err.println("FAIL: " + t.getMessage());
					errors.increment();
					t.printStackTrace();
					throw t;
				}
				try {
					Thread.sleep(0, 1);
				} catch (InterruptedException e) {
				}
			}
		};
		Thread otherThreadCheck = new Thread(r, "integrity-check");
		otherThreadCheck.setDaemon(true);
		otherThreadCheck.setPriority(Thread.MIN_PRIORITY);
		otherThreadCheck.start();

		int deleted = 0;
		int modulo = 100;
		for (int i = start; i < count; i++) {
			toFlush.add(record(i, "user"));
			if (i % modulo == 0) {
				List<AddressBookRecord> chunk = new ArrayList<>(toFlush);
				chunk.add(refreshed(rec2));
				chunk.add(refreshed(rec1));
				toFlush.clear();
				List<Long> toDelete = Collections.emptyList();
				// drop some entries from previous chunks
				long dropStart = i - (2 * modulo + 10);
				if (dropStart > 0) {
					toDelete = Arrays.asList(dropStart, dropStart + 1);
					deleted += toDelete.size();
				}

				serialize(toDelete, chunk.toArray(empty));
				System.err.println(
						"Serialized " + chunk.size() + " record(s), dropped " + toDelete.size() + " record(s)");

				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}
		}
		AddressBookRecord[] remainder = toFlush.toArray(empty);
		serialize(remainder);
		System.err.println("Serialized " + toFlush.size() + " record(s)");
		System.err.println("last record is " + remainder[remainder.length - 1].uid);

		System.err.println("Waiting for annoucements....");
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
		}

		Optional<net.bluemind.directory.hollow.datamodel.consumer.OfflineAddressBook> parentBook = defaultSearch.root();
		assertTrue(parentBook.isPresent());

		Collection<net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord> allRecs = defaultSearch.all();
		System.err.println("TEST finishes with " + allRecs.size() + " item(s) in hollow");
		System.err.println("deleted " + deleted + " record(s) during test.");
		int total = count - start + (existingUserCount - hiddenUserCount) - deleted;
		assertEquals(total, allRecs.size());

		stop.set(true);
		otherThreadCheck.join(1000);
		System.err.println("Performed " + checks.sum() + " async checks");
		assertEquals(0, errors.sum());

	}

	private AddressBookRecord refreshed(AddressBookRecord r) {
		r.updated = new Date();
		return r;
	}

	private void checkByEmail(String em, long minId) {
		Optional<net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord> lookup = defaultSearch.byEmail(em);
		assertTrue("nothing found for " + em, lookup.isPresent());
		lookup.ifPresent(rec -> {
			assertEquals(minId, rec.getMinimalid());
		});
		assertEquals(Long.valueOf(minId), lookup
				.map(net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord::getMinimalid).orElse(-1L));

	}

}
