/*
 *
 *  Copyright 2017 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package net.bluemind.common.hollow;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.HollowProducer.Blob.Type;

public class BmFilesystemBlobStorageCleaner extends HollowProducer.BlobStorageCleaner {

	private static final Logger logger = LoggerFactory.getLogger(BmFilesystemBlobStorageCleaner.class);

	private final int numOfSnapshotsToKeep;
	private final File blobStoreDir;

	public BmFilesystemBlobStorageCleaner(File blobStoreDir) {
		this(blobStoreDir, 5);
	}

	public BmFilesystemBlobStorageCleaner(File blobStoreDir, int numOfSnapshotsToKeep) {
		this.blobStoreDir = blobStoreDir;
		this.numOfSnapshotsToKeep = numOfSnapshotsToKeep;
	}

	/**
	 * Cleans snapshot to keep the last 'n' snapshots. Defaults to 5.
	 */
	@Override
	public void cleanSnapshots() {
		cleanImpl(Type.SNAPSHOT, numOfSnapshotsToKeep);
	}

	@Override
	public void cleanDeltas() {
		cleanImpl(Type.DELTA, 2 * numOfSnapshotsToKeep);
	}

	@Override
	public void cleanReverseDeltas() {
		cleanImpl(Type.REVERSE_DELTA, 2 * numOfSnapshotsToKeep);
	}

	private void cleanImpl(HollowProducer.Blob.Type type, int kept) {
		File[] files = getFilesByType(type.prefix);

		if (files == null || files.length <= kept) {
			return;
		}

		sortByLastModified(files);

		for (int i = kept; i < files.length; i++) {
			File file = files[i];
			boolean deleted = file.delete(); // NOSONAR
			if (!deleted) {
				logger.warn("Could not delete delta {}", file.getPath());
			}
		}
	}

	private void sortByLastModified(File[] files) {
		Arrays.sort(files, (File f1, File f2) -> {
			Long lastModifiedF2 = f2.lastModified();
			Long lastModifiedF1 = f1.lastModified();
			return lastModifiedF2.compareTo(lastModifiedF1);
		});
		Arrays.sort(files, Collections.reverseOrder());
	}

	private File[] getFilesByType(final String blobType) {
		return blobStoreDir.listFiles((File dir, String name) -> name.contains(blobType));
	}
}
