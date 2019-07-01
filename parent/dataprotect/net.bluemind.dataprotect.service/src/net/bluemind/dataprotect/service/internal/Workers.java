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

package net.bluemind.dataprotect.service.internal;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.bluemind.dataprotect.service.IBackupWorker;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public final class Workers {

	private static List<IBackupWorker> workers = workers();

	/**
	 * @return
	 */
	private static List<IBackupWorker> workers() {
		RunnableExtensionLoader<IBackupWorker> rel = new RunnableExtensionLoader<>();
		List<IBackupWorker> exts = rel.loadExtensions("net.bluemind.dataprotect.service", "backupworker",
				"backup_worker", "impl");
		return ImmutableList.copyOf(exts);
	}

	public static List<IBackupWorker> get() {
		return workers;
	}

}
