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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.fs.HollowFilesystemAnnouncer;
import com.netflix.hollow.api.producer.fs.HollowFilesystemPublisher;

import net.bluemind.directory.hollow.datamodel.AddressBookRecord;
import net.bluemind.directory.hollow.datamodel.Email;
import net.bluemind.directory.hollow.datamodel.OfflineAddressBook;

public class DirectorySearchTest {

	private File file;
	private SerializedDirectorySearch defaultSearch;
	private SerializedDirectorySearch filteredSearch;
	AddressBookRecord rec1;
	AddressBookRecord rec2;
	AddressBookRecord rec3;
	AddressBookRecord rec4;

	@Before
	public void setup() throws Exception {
		file = new File(System.getProperty("java.io.tmpdir"), "" + System.currentTimeMillis() + ".data");
		file.mkdirs();
		rec1 = record(1, "user");
		rec2 = record(2, "user");
		rec3 = record(3, "user");
		rec3.hidden = true;
		rec4 = record(4, "group");

		CountDownLatch wait = new CountDownLatch(1);
		serialize(Arrays.asList(rec1, rec2, rec3, rec4), wait);
		wait.await();

		DirectoryDeserializer deserializer = new DirectoryDeserializer(file);
		this.defaultSearch = new DefaultDirectorySearch(deserializer);
		this.filteredSearch = new FilteredDirectorySearch(deserializer, rec -> !rec.getHidden());
	}

	@After
	public void teardown() {
		Path dir = file.toPath();
		try {
			Files.walk(dir, FileVisitOption.FOLLOW_LINKS).sorted(Comparator.reverseOrder()).map(Path::toFile)
					.forEach(File::delete);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testSearchAll() throws Exception {
		Collection<net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord> all = defaultSearch.all();

		assertEquals(4, all.size());

		all = filteredSearch.all();
		assertEquals(3, all.size());
	}

	@Test
	public void testSearchByUid() throws Exception {
		Optional<net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord> byUid = defaultSearch
				.byUid(rec2.uid);

		assertTrue(byUid.isPresent());
		assertEquals(rec2.uid, byUid.get().getUid().getValue());
		assertEquals(rec2.name, byUid.get().getName().getValue());

		byUid = filteredSearch.byUid(rec2.uid);
		assertTrue(byUid.isPresent());
		byUid = filteredSearch.byUid(rec3.uid);
		assertFalse(byUid.isPresent());
	}

	@Test
	public void testSearchByEmail() throws Exception {
		Optional<net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord> byEmail = defaultSearch
				.byEmail(rec3.email);

		assertTrue(byEmail.isPresent());
		assertEquals(rec3.uid, byEmail.get().getUid().getValue());
		assertEquals(rec3.email, byEmail.get().getEmail().getValue());

		byEmail = filteredSearch.byEmail(rec3.email);
		assertFalse(byEmail.isPresent());

		byEmail = filteredSearch.byEmail(rec4.email);
		assertTrue(byEmail.isPresent());
	}

	@Test
	public void testSearchByKind() throws Exception {
		Collection<net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord> byKind = defaultSearch
				.byKind("user");

		assertEquals(3, byKind.size());
		boolean found1 = false;
		boolean found2 = false;
		boolean found3 = false;
		for (net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord ar : byKind) {
			if (ar.getUid().getValue().equals(rec1.uid)) {
				found1 = true;
			} else if (ar.getUid().getValue().equals(rec2.uid)) {
				found2 = true;
			} else if (ar.getUid().getValue().equals(rec3.uid)) {
				found3 = true;
			}
		}
		assertTrue(found1);
		assertTrue(found2);
		assertTrue(found3);

		SearchResults ret = defaultSearch.byKind(Arrays.asList("user"), 1, 1);
		assertEquals(3, ret.totalCount);
		assertEquals(1, ret.records.size());

		byKind = filteredSearch.byKind("user");

		assertEquals(2, byKind.size());
		found1 = false;
		found2 = false;
		for (net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord ar : byKind) {
			if (ar.getUid().getValue().equals(rec1.uid)) {
				found1 = true;
			} else if (ar.getUid().getValue().equals(rec2.uid)) {
				found2 = true;
			}
		}
		assertTrue(found1);
		assertTrue(found2);

		ret = filteredSearch.byKind(Arrays.asList("user"), 1, 1);
		assertEquals(2, ret.totalCount);
		assertEquals(1, ret.records.size());

	}

	@Test
	public void testSimpleQuery() throws Exception {
		Query query = Query.contentQuery("name", rec1.name);
		List<net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord> result = defaultSearch.search(query);
		assertEquals(1, result.size());
		assertEquals(rec1.uid, result.get(0).getUid().getValue());

		query = Query.contentQuery("name", rec2.name);
		result = defaultSearch.search(query);
		assertEquals(1, result.size());
		assertEquals(rec2.uid, result.get(0).getUid().getValue());

		query = Query.contentQuery("name", rec3.name);
		result = defaultSearch.search(query);
		assertEquals(1, result.size());
		assertEquals(rec3.uid, result.get(0).getUid().getValue());

		query = Query.contentQuery("name", rec3.name);
		result = filteredSearch.search(query);
		assertEquals(0, result.size());

		query = Query.contentQuery("name", rec2.name);
		result = filteredSearch.search(query);
		assertEquals(1, result.size());
		assertEquals(rec2.uid, result.get(0).getUid().getValue());
	}

	@Test
	public void testAndQuery() throws Exception {
		Query query1 = Query.contentQuery("kind", rec2.kind);
		Query query2 = Query.contentQuery("anr", rec2.email);

		Query query = Query.andQuery(Arrays.asList(query1, query2));
		List<net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord> result = defaultSearch.search(query);
		assertEquals(1, result.size());
		assertEquals(rec2.uid, result.get(0).getUid().getValue());
	}

	@Test
	public void testOrQuery() throws Exception {
		Query query1 = Query.contentQuery("name", rec1.name);
		Query query2 = Query.contentQuery("name", rec2.name);

		Query query = Query.orQuery(Arrays.asList(query1, query2));
		List<net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord> result = defaultSearch.search(query);
		assertEquals(2, result.size());
	}

	private void serialize(List<AddressBookRecord> records, CountDownLatch wait) {
		HollowFilesystemPublisher publisher = new HollowFilesystemPublisher(file.toPath());
		Announcer announcer = new Announcer(file);
		announcer.setWait(wait);
		HollowProducer producer = HollowProducer.withPublisher(publisher).withAnnouncer(announcer).build();
		producer.initializeDataModel(AddressBookRecord.class);
		producer.initializeDataModel(OfflineAddressBook.class);

		producer.runCycle((state) -> records.forEach(r -> state.add(r)));
	}

	private AddressBookRecord record(int minimalId, String kind) {
		AddressBookRecord record = new AddressBookRecord();
		record.uid = "uid" + minimalId;
		record.name = "Entity " + minimalId;
		record.distinguishedName = kind + "/" + record.uid;
		record.kind = kind;
		record.email = record.uid + "@bm.loc";
		record.emails = Arrays.asList(Email.create(record.email, true, true),
				Email.create("alt-" + record.email, false, false));
		record.addressBook = new OfflineAddressBook();
		record.addressBook.domainName = "bm.loc";
		record.addressBook.domainAliases = new HashSet<>(Arrays.asList("bm.lan"));
		return record;

	}

	private static class Announcer extends HollowFilesystemAnnouncer implements HollowProducer.Announcer {

		private CountDownLatch wait;

		public Announcer(File publishDir) {
			super(publishDir.toPath());
		}

		@Override
		public void announce(long stateVersion) {
			super.announce(stateVersion);
			wait.countDown();
		}

		void setWait(CountDownLatch wait) {
			this.wait = wait;
		}

	}

}
