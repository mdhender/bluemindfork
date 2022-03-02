package net.bluemind.serialization.client;
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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hollow.api.consumer.HollowConsumer;

import net.bluemind.common.hollow.IAnnouncementWatcher;

public class HollowContext {

	public final HollowConsumer.BlobRetriever blobRetriever;
	public final IAnnouncementWatcher announcementWatcher;
	private static final Logger logger = LoggerFactory.getLogger(HollowContext.class);

	public HollowContext(HollowConsumer.BlobRetriever blobRetriever, IAnnouncementWatcher announcementWatcher) {
		this.blobRetriever = blobRetriever;
		this.announcementWatcher = announcementWatcher;
	}

	public static HollowContext get(File dir, String set, boolean watchChanges) {
		if (dir.exists()) {
			logger.info("HOLLOW local strategy selected for set {} and dir {}.", set, dir.getAbsolutePath());
			return new LocalHollowContext().create(dir, watchChanges);
		} else {
			logger.info("HOLLOW remote strategy selected for set {} as '{}' is missing.", set, dir.getAbsolutePath());
			return new BmHollowContext().create(set, dir.getName());
		}
	}

	public void stopWatcher() {
		announcementWatcher.stop();
	}

	public boolean isWatcherListening() {
		return announcementWatcher.isListening();
	}

}
