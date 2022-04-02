/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.backup.continuous.IBackupManager;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.store.ITopicStore;

public class BackupManager implements IBackupManager {

	private final ITopicStore store;
	private static final Logger logger = LoggerFactory.getLogger(BackupManager.class);

	public BackupManager(ITopicStore store) {
		this.store = store;
	}

	@Override
	public void delete(ILiveStream stream) {
		logger.info("Delete {} with fn {}", stream, stream.fullName());
		store.getManager().delete(stream.fullName());
	}

	@Override
	public void flush(ILiveStream stream) {
		logger.info("Flush {} with fn {}", stream, stream.fullName());
		store.getManager().flush(stream.fullName());
	}

}
