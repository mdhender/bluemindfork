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
package net.bluemind.system.nginx;

import java.util.Map;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationValidator;

public class SwPasswordValidator implements ISystemConfigurationValidator {

	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (modifications.get(SysConfKeys.sw_password.name()) != null) {
			String pwd = modifications.get(SysConfKeys.sw_password.name());
			if (pwd.contains("'")) {
				throw new ServerFault("\"'\" is a forbidden character in this password", ErrorCode.INVALID_PARAMETER);
			}
		}
	}

}
