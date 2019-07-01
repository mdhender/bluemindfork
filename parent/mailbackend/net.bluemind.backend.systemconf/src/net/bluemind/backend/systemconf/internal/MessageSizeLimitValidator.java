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

package net.bluemind.backend.systemconf.internal;

import java.util.Map;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationValidator;

public class MessageSizeLimitValidator implements ISystemConfigurationValidator {
	private static final String PARAMETER = "message_size_limit";

	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		ParametersValidator.notNull(modifications);

		if (!modifications.containsKey(PARAMETER)) {
			return;
		}

		ParametersValidator.notNullAndNotEmpty(modifications.get(PARAMETER));

		try {
			Integer.parseInt(modifications.get(PARAMETER).trim());
		} catch (NumberFormatException nfe) {
			throw new ServerFault("Invalid " + PARAMETER + " value", ErrorCode.INVALID_PARAMETER);
		}
	}
}
