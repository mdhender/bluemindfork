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
import java.nio.file.Path;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.fs.HollowFilesystemBlobRetriever;

import net.bluemind.common.hollow.BmFilesystemAnnoucementWatcher;

public class LocalHollowContext {

	public HollowContext create(File dir) {
		Path asPath = dir.toPath();
		HollowConsumer.BlobRetriever blobRetriever = new HollowFilesystemBlobRetriever(asPath);
		HollowConsumer.AnnouncementWatcher announcementWatcher = new BmFilesystemAnnoucementWatcher(asPath);
		// new HollowFilesystemAnnouncementWatcher(dir.toPath());
		return new HollowContext(blobRetriever, announcementWatcher);
	}

}
