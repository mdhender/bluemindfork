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
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationSanitizor;

public class MessageSizeLimitSanitizor implements ISystemConfigurationSanitizor {
	private static final String PARAMETER = "message_size_limit";
	private static final String DEFAULT_PARAMETER = "10000000";

	@Override
	public void sanitize(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		ParametersValidator.notNull(modifications);
		ParametersValidator.notNull(previous);

		if (!previous.values.containsKey(PARAMETER) && !modifications.containsKey(PARAMETER)) {
			modifications.put(PARAMETER, DEFAULT_PARAMETER);
			return;
		}

		if (!modifications.containsKey(PARAMETER)) {
			return;
		}

		modifications.put(PARAMETER, modifications.get(PARAMETER).trim());
	}
}
