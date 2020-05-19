/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.system.auth;

import java.util.Map;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationValidator;

public class HpsConfigChangeValidateHandler implements ISystemConfigurationValidator {
	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (!modifications.containsKey(SysConfKeys.hps_max_sessions_per_user.name())) {
			return;
		}

		int currentHpsMaxSessionsPerUser;
		try {
			currentHpsMaxSessionsPerUser = Integer
					.parseInt(modifications.get(SysConfKeys.hps_max_sessions_per_user.name()));
		} catch (NumberFormatException nfe) {
			throw new ServerFault("Max HPS sessions per user must be an integer", ErrorCode.INVALID_PARAMETER);
		}

		if (currentHpsMaxSessionsPerUser < 1) {
			throw new ServerFault("Max HPS sessions per user must be greater than 0 - default to 5",
					ErrorCode.INVALID_PARAMETER);
		}
	}
}
