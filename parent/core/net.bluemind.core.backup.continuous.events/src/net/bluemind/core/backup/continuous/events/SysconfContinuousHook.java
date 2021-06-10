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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;

public class SysconfContinuousHook implements ISystemConfigurationObserver {

	private static final Logger logger = LoggerFactory.getLogger(SysconfContinuousHook.class);

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		ContainerDescriptor metaDesc = ContainerDescriptor.create("sysconf", "sysconf", "system", "sysconf", null,
				true);

		ItemValue<SystemConf> metaItem = ItemValue.create("sysconf", conf);
		metaItem.internalId = metaItem.uid.hashCode();
		DefaultBackupStore.get().<SystemConf>forContainer(metaDesc).store(metaItem);
		logger.info("Saved sysconf as {}", metaItem);

	}

}
