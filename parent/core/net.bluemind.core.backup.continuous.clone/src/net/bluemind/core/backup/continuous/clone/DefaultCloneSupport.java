/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.clone;

import java.util.Map;

import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.IBackupReader;
import net.bluemind.core.backup.continuous.restore.InstallFromBackupTask;
import net.bluemind.core.backup.continuous.restore.SysconfOverride;
import net.bluemind.core.backup.continuous.restore.TopologyMapping;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.system.api.CloneConfiguration;
import net.bluemind.system.service.clone.CloneSupport;

public class DefaultCloneSupport implements CloneSupport {

	@Override
	public IServerTask create(CloneConfiguration conf, IServiceProvider prov, Map<String, String> sysconfOverride) {
		TopologyMapping tm = new TopologyMapping(conf.uidToIpMapping);
		IBackupReader reader = DefaultBackupStore.reader();
		return new InstallFromBackupTask(conf.sourceInstallationId, reader, new SysconfOverride(sysconfOverride), tm,
				prov);
	}

}
