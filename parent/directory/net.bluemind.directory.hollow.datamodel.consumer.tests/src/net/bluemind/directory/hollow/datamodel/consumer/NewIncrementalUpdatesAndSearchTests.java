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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.HollowProducer.Incremental;
import com.netflix.hollow.api.producer.fs.HollowFilesystemPublisher;
import com.netflix.hollow.core.write.objectmapper.RecordPrimaryKey;

import net.bluemind.common.hollow.BmFilesystemBlobStorageCleaner;
import net.bluemind.directory.hollow.datamodel.AddressBookRecord;
import net.bluemind.directory.hollow.datamodel.OfflineAddressBook;

public class NewIncrementalUpdatesAndSearchTests extends BaseIncrementalUpdatesAndSearchTests {

	protected IncrementalProductionStrategy strategy(File dir, CountDownLatch latch) {

		HollowFilesystemPublisher publisher = new HollowFilesystemPublisher(dir.toPath());
		Announcer announcer = new Announcer(dir);
		announcer.setWait(latch);
		BmFilesystemBlobStorageCleaner cleaner = new BmFilesystemBlobStorageCleaner(dir, 4);
		Incremental newIncremental = HollowProducer.withPublisher(publisher).withAnnouncer(announcer)
				.withBlobStorageCleaner(cleaner).buildIncremental();
		newIncremental.initializeDataModel(AddressBookRecord.class);
		newIncremental.initializeDataModel(OfflineAddressBook.class);

		return (List<Long> toDrop, OfflineAddressBook book, AddressBookRecord... recs) -> {
			System.err.println("Running strategy with " + recs.length + " record(s");
			newIncremental.runIncrementalCycle(state -> {
				state.addOrModify(book);

				Arrays.asList(recs).forEach(state::addOrModify);

				toDrop.forEach(
						l -> state.delete(new RecordPrimaryKey("AddressBookRecord", new String[] { "uid" + l })));

				System.err.println("oabRoot SEQ is at " + book.sequence);

			});
		};

	}

}
