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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationSanitizor;
import net.bluemind.system.hook.ISystemConfigurationValidator;

public class OtherUrlHook implements ISystemConfigurationSanitizor, ISystemConfigurationValidator {
	@Override
	public void sanitize(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (!modifications.containsKey(SysConfKeys.other_urls.name())
				|| modifications.get(SysConfKeys.other_urls.name()) == null) {
			return;
		}

		String sanitizedUrl = modifications.get(SysConfKeys.other_urls.name()).replaceAll("\\s+", " ").trim();
		modifications.put(SysConfKeys.other_urls.name(), sanitizedUrl.isEmpty() ? null : sanitizedUrl);
	}

	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (!modifications.containsKey(SysConfKeys.other_urls.name())) {
			return;
		}

		Optional.ofNullable(modifications.get(SysConfKeys.other_urls.name())).map(ou -> Arrays.asList(ou.split(" ")))
				.orElseGet(Collections::emptyList).stream().filter(ou -> !Regex.DOMAIN.validate(ou)).findFirst()
				.ifPresent(ou -> {
					throw new ServerFault(String.format("Invalid URL '%s' in other URLs", ou),
							ErrorCode.INVALID_PARAMETER);
				});
	}
}
