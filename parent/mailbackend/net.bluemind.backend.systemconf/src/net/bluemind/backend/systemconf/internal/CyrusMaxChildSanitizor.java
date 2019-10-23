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

import com.google.common.base.Strings;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationSanitizor;

public class CyrusMaxChildSanitizor implements ISystemConfigurationSanitizor {
	private static final String PARAMETER = "imap_max_child";
	private static final String DEFAULT_VALUE = "200";

	@Override
	public void sanitize(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		ParametersValidator.notNull(modifications);
		ParametersValidator.notNull(previous);

		if ((!previous.values.containsKey(PARAMETER) || Strings.isNullOrEmpty(previous.values.get(PARAMETER)))
				&& !modifications.containsKey(PARAMETER)) {
			modifications.put(PARAMETER, DEFAULT_VALUE);
			return;
		}

		if (!modifications.containsKey(PARAMETER)) {
			return;
		}

		modifications.put(PARAMETER, modifications.get(PARAMETER).trim());
		if (modifications.get(PARAMETER).isEmpty()) {
			modifications.put(PARAMETER, DEFAULT_VALUE);
		}
	}
}
