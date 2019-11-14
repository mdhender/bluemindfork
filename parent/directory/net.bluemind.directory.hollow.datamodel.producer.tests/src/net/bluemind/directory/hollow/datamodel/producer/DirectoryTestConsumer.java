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
package net.bluemind.directory.hollow.datamodel.producer;

import java.io.File;

import com.netflix.hollow.api.consumer.HollowConsumer.AnnouncementWatcher;

import net.bluemind.directory.hollow.datamodel.consumer.DirectoryDeserializer;
import net.bluemind.serialization.client.HollowContext;

public class DirectoryTestConsumer extends DirectoryDeserializer {

	public DirectoryTestConsumer(File file) {
		super(file);
	}

	public void refreshTo(long snapVersion) {
		long cur = getVersion();
		System.err.println("cur: " + cur + " snap: " + snapVersion);
		if (getVersion() != snapVersion) {
			consumer.triggerRefreshTo(snapVersion);
		}
	}

	@Override
	protected AnnouncementWatcher watcher(HollowContext ctx) {
		return null;
	}

	public long getVersion() {
		return consumer.getCurrentVersionId();
	}

}
