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
package net.bluemind.directory.hollow.datamodel.consumer;

import java.io.File;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.fs.HollowFilesystemBlobRetriever;
import com.netflix.hollow.api.consumer.index.UniqueKeyIndex;
import com.netflix.hollow.core.HollowConstants;

public class DirectoryVersionReader {

	private static final Logger logger = LoggerFactory.getLogger(DirectoryVersionReader.class);
	private final String domainUid;
	private long version;

	public DirectoryVersionReader(String domain) {
		this(new File(DirectoryDeserializer.baseDataDir(), domain));
	}

	public DirectoryVersionReader(File dir) {
		this.domainUid = dir.getName();
		logger.info("Consuming from directory {} for domain {}", dir.getAbsolutePath(), domainUid);
		if (!dir.exists()) {
			this.version = 0L;
		} else {
			HollowConsumer.BlobRetriever blobRetriever = new HollowFilesystemBlobRetriever(dir.toPath());
			HollowConsumer consumer = new HollowConsumer.Builder<>()//
					.withBlobRetriever(blobRetriever).withAnnouncementWatcher(null)//
					.withGeneratedAPIClass(OfflineDirectoryAPI.class).build();

			try {
				consumer.triggerRefreshTo(HollowConstants.VERSION_LATEST);
				logger.info("Current version: {}", consumer.getCurrentVersionId());
				UniqueKeyIndex<OfflineAddressBook, String> idx = OfflineAddressBook.uniqueIndex(consumer);
				this.version = Optional.ofNullable(idx.findMatch(domainUid)).map(OfflineAddressBook::getSequence)
						.orElseGet(() -> 0);
			} catch (IllegalArgumentException iea) {
				// this one is thrown when no version is available
				this.version = 0L;
			}

		}
	}

	public long version() {
		return version;
	}

}
