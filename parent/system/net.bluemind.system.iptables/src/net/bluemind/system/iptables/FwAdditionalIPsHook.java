/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.system.iptables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;

public class FwAdditionalIPsHook implements ISystemConfigurationObserver {
	private static final Logger logger = LoggerFactory.getLogger(FwAdditionalIPsHook.class);

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		String additionalIps = conf.values.get(SysConfKeys.fwAdditionalIPs.name());
		if ((additionalIps == previous.values.get(SysConfKeys.fwAdditionalIPs.name())) || (additionalIps != null
				&& additionalIps.equalsIgnoreCase(previous.values.get(SysConfKeys.fwAdditionalIPs.name())))) {
			return;
		}

		logger.info("Update firewall rules");
		context.provider().instance(ITasksManager.class).run(new UpdateFirewallRulesTask());
	}
}
