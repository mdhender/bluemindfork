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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.HollowProducer.Incremental;
import com.netflix.hollow.api.producer.fs.HollowFilesystemAnnouncer;
import com.netflix.hollow.api.producer.fs.HollowFilesystemPublisher;

import net.bluemind.directory.hollow.datamodel.AddressBookRecord;
import net.bluemind.directory.hollow.datamodel.AnrToken;
import net.bluemind.directory.hollow.datamodel.Email;
import net.bluemind.directory.hollow.datamodel.OfflineAddressBook;
import net.bluemind.directory.hollow.datamodel.producer.EdgeNgram.EmailEdgeNGram;

public class DirectorySearchDoubleSnapshotTest {

	private File file;
	private List<AddressBookRecord> entries;
	private Consumer<List<AddressBookRecord>> producer;
	private int entryCount = 200;
	int entryAddedCount = 200;
	private DirectoryDeserializer deserializer;
	private SerializedDirectorySearch defaultSearch;

	@Before
	public void setup() throws Exception {
		this.entries = new ArrayList<>();
		for (int i = 1; i <= entryCount / 2; i++) {
			this.entries.add(record(i, "user"));
		}
		this.file = new File(System.getProperty("java.io.tmpdir"), "" + System.currentTimeMillis() + ".data");
		initSnapshot();

		deserializer = new DirectoryDeserializer(file);
		this.defaultSearch = new DefaultDirectorySearch(deserializer);

		waitForProducerVersionOnConsumer();

		List<AddressBookRecord> incremetalEntries = new ArrayList<>();
		for (int i = (entryCount / 2) + 1; i <= entryCount; i++) {
			incremetalEntries.add(record(i, "user"));
		}
		this.entries.addAll(incremetalEntries);
		this.producer.accept(incremetalEntries);

		waitForProducerVersionOnConsumer();
	}

	private void waitForProducerVersionOnConsumer() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	}

	@After
	public void teardown() {
		deleteHollowDirectoy();
	}

	private void initSnapshot() throws InterruptedException {
		file.mkdirs();
		this.producer = createProducer();
		this.producer.accept(entries);
	}

	private void recreateSnapshot() throws InterruptedException {
		deleteHollowDirectoy();
		initSnapshot();
	}

	private void deleteHollowDirectoy() {
		Path dir = file.toPath();
		try {
			Files.walk(dir, FileVisitOption.FOLLOW_LINKS) ///
					.sorted(Comparator.reverseOrder()) //
					.map(Path::toFile) //
					.forEach(f -> {
						System.out.println(f.getAbsolutePath());
						f.delete();
					});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testSearchByEmail() throws Exception {
		testSearchBy(entry -> defaultSearch.byEmail(entry.email));
	}

	@Test
	public void testSearchByUid() throws Exception {
		testSearchBy(entry -> defaultSearch.byUid(entry.uid));
	}

	@Test
	public void testSearchByMinimalId() throws Exception {
		testSearchBy(entry -> defaultSearch.byMinimalId(entry.minimalid));
	}

	private interface SearchBy {
		Optional<net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord> by(AddressBookRecord enrty);
	}

	public void testSearchBy(SearchBy search) throws Exception {
		check(search);
		recreateSnapshot();
		waitForProducerVersionOnConsumer();

		check(search);
	}

	private void check(SearchBy search) {
		Optional<net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord> maybeFound;
		for (int i = 1; i <= entryCount; i++) {
			AddressBookRecord entry = entries.get(i - 1);
			maybeFound = search.by(entry);

			assertTrue(maybeFound.isPresent());
			assertEquals(entry.uid, maybeFound.get().getUid());
			assertEquals(entry.email, maybeFound.get().getEmail());
			assertEquals(entry.name, maybeFound.get().getName());
		}
	}

	private Consumer<List<AddressBookRecord>> createProducer() {
		HollowFilesystemPublisher publisher = new HollowFilesystemPublisher(file.toPath());
		Announcer announcer = new Announcer(file);
		Incremental producer = HollowProducer.withPublisher(publisher).withAnnouncer(announcer).buildIncremental();
		producer.initializeDataModel(AddressBookRecord.class);
		producer.initializeDataModel(OfflineAddressBook.class);

		return (List<AddressBookRecord> entries) -> {
			producer.runIncrementalCycle(state -> {
				entries.forEach(state::addOrModify);
			});
		};
	}

	private static class Announcer extends HollowFilesystemAnnouncer implements HollowProducer.Announcer {

		public Announcer(File publishDir) {
			super(publishDir.toPath());
		}

		@Override
		public void announce(long stateVersion) {
			super.announce(stateVersion);
		}

	}

	private AddressBookRecord record(int minimalId, String kind) {
		AddressBookRecord record = new AddressBookRecord();
		record.minimalid = minimalId;
		record.uid = "uid" + minimalId;
		record.name = "Entity " + minimalId;
		record.distinguishedName = kind + "/" + record.uid;
		record.kind = kind;
		record.email = record.uid + "@bm.loc";

		record.emails = Arrays.asList(new Email(record.email, new EmailEdgeNGram().compute(record.email), true, true),
				new Email("alt-" + record.email, new EmailEdgeNGram().compute("alt-" + record.email), false, false));
		record.domain = "bm.loc";
		AnrToken anr = new AnrToken();
		anr.token = record.uid;
		record.anr = Arrays.asList(anr);
		return record;

	}

}
