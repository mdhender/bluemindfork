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

import java.util.LinkedHashMap;
import java.util.Map;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationSanitizor;

public class SystemConfigurationSanitizor implements ISystemConfigurationSanitizor {

	@Override
	public void sanitize(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		String fwAdditionalIPs = modifications.get(SysConfKeys.fwAdditionalIPs.name());
		if (fwAdditionalIPs == null || fwAdditionalIPs.isEmpty()) {
			return;
		}

		fwAdditionalIPs = fwAdditionalIPs.trim();
		fwAdditionalIPs = fwAdditionalIPs.replaceAll(" +", " ");

		Map<String, String> unique = new LinkedHashMap<>();
		for (String part : fwAdditionalIPs.split(" ")) {
			unique.put(part, null);
		}

		modifications.put(SysConfKeys.fwAdditionalIPs.name(), String.join(" ", unique.keySet()));
	}
}
