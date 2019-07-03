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
package net.bluemind.system.iptables.tools;

import java.util.Map;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationValidator;
import net.bluemind.system.iptables.internal.SubnetUtils;

public class SystemConfigurationValidator implements ISystemConfigurationValidator {
	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (!modifications.containsKey(SysConfKeys.fwAdditionalIPs.name())
				|| modifications.get(SysConfKeys.fwAdditionalIPs.name()) == null
				|| modifications.get(SysConfKeys.fwAdditionalIPs.name()).trim().isEmpty()) {
			return;
		}

		for (String ip : modifications.get(SysConfKeys.fwAdditionalIPs.name()).split(" ")) {
			String[] parts = ip.split("/");

			if (parts.length > 2) {
				throw new ServerFault("Invalid IP: " + ip, ErrorCode.INVALID_PARAMETER);
			} else if (parts.length == 1) {
				try {
					new SubnetUtils(ip + "/32");
				} catch (IllegalArgumentException iae) {
					throw new ServerFault("Invalid IP: " + ip, ErrorCode.INVALID_PARAMETER);
				}
			} else if (parts[1].matches("^\\d\\d$")) {
				try {
					new SubnetUtils(ip);
				} catch (IllegalArgumentException iae) {
					throw new ServerFault("Invalid IP: " + ip, ErrorCode.INVALID_PARAMETER);
				}
			} else {
				try {
					new SubnetUtils(parts[0], parts[1]);
				} catch (IllegalArgumentException iae) {
					throw new ServerFault("Invalid IP: " + ip, ErrorCode.INVALID_PARAMETER);
				}
			}
		}
	}
}
