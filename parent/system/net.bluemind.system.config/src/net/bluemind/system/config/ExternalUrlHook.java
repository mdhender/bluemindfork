/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.system.config;

import java.util.Map;

import com.google.common.base.Strings;

import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationSanitizor;
import net.bluemind.system.hook.ISystemConfigurationValidator;

public class ExternalUrlHook implements ISystemConfigurationSanitizor, ISystemConfigurationValidator {
	@Override
	public void sanitize(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (!modifications.containsKey(SysConfKeys.external_url.name()) && previous != null
				&& previous.values.containsKey("external-url")) {
			modifications.put(SysConfKeys.external_url.name(), previous.stringValue("external-url"));
		}
		// external-url key is forbidden in database
		modifications.put("external-url", null);

		if (!modifications.containsKey(SysConfKeys.external_url.name())
				|| modifications.get(SysConfKeys.external_url.name()) == null) {
			return;
		}

		modifications.put(SysConfKeys.external_url.name(), modifications.get(SysConfKeys.external_url.name()).trim());
	}

	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (!modifications.containsKey(SysConfKeys.external_url.name())) {
			return;
		}

		if (Strings.isNullOrEmpty(modifications.get(SysConfKeys.external_url.name()))) {
			throw new ServerFault("External URL must not be null or empty!", ErrorCode.INVALID_PARAMETER);
		}

		if (!Regex.DOMAIN.validate(modifications.get(SysConfKeys.external_url.name()))) {
			throw new ServerFault(
					String.format("Invalid external URL '%s'", modifications.get(SysConfKeys.external_url.name())),
					ErrorCode.INVALID_PARAMETER);
		}
	}
}
