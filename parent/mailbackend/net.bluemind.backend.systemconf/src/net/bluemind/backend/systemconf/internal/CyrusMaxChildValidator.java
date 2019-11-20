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

public class CyrusMaxChildValidator implements ISystemConfigurationValidator {
	private static final String PARAMETER = "imap_max_child";
	private static final int MIN_VALUE = 10;

	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		ParametersValidator.notNull(modifications);

		if (!modifications.containsKey(PARAMETER)) {
			return;
		}

		try {
			ParametersValidator.notNull(modifications.get(PARAMETER));
		} catch (ServerFault sf) {
			throw new ServerFault("Invalid " + PARAMETER + " value", ErrorCode.INVALID_PARAMETER);
		}

		int imapMaxChild = 0;
		try {
			imapMaxChild = Integer.parseInt(modifications.get(PARAMETER).trim());
		} catch (NumberFormatException nfe) {
			throw new ServerFault("Invalid " + PARAMETER + " value", ErrorCode.INVALID_PARAMETER);
		}

		if (imapMaxChild < MIN_VALUE) {
			throw new ServerFault(String.format("Invalid %s value. Must be at least %s", PARAMETER, MIN_VALUE),
					ErrorCode.INVALID_PARAMETER);
		}
	}
}
