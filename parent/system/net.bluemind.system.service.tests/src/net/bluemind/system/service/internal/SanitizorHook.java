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
package net.bluemind.system.service.internal;

import java.util.Map;

import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationSanitizor;

public class SanitizorHook implements ISystemConfigurationSanitizor {

	public static boolean called = false;

	public static final String PARAMETER = "testSanitize";

	public static final String SANITIZED_VALUE = "sanitized";

	@Override
	public void sanitize(SystemConf previous, Map<String, String> modifications) {
		called = true;
		if (modifications.containsKey(PARAMETER) && modifications.get(PARAMETER) != null) {
			modifications.put("testSanitize", SANITIZED_VALUE);
		}
	}

}
