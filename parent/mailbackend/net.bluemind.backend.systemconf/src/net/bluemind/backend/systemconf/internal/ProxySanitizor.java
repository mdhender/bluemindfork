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
package net.bluemind.backend.systemconf.internal;

import java.util.Map;

import com.google.common.base.Strings;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationSanitizor;

public class ProxySanitizor implements ISystemConfigurationSanitizor {
	private static final String DEFAULT_PORT = "3128";

	@Override
	public void sanitize(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		ParametersValidator.notNull(modifications);
		ParametersValidator.notNull(previous);

		if (Strings.isNullOrEmpty(previous.stringValue(SysConfKeys.http_proxy_enabled.name()))
				&& !modifications.containsKey(SysConfKeys.http_proxy_enabled.name())) {
			modifications.put(SysConfKeys.http_proxy_enabled.name(), Boolean.FALSE.toString());
		} else if (modifications.containsKey(SysConfKeys.http_proxy_enabled.name())) {
			modifications.put(SysConfKeys.http_proxy_enabled.name(),
					Boolean.valueOf(modifications.get(SysConfKeys.http_proxy_enabled.name())).toString());
		}

		if (modifications.containsKey(SysConfKeys.http_proxy_hostname.name())
				&& modifications.get(SysConfKeys.http_proxy_hostname.name()) != null) {
			modifications.put(SysConfKeys.http_proxy_hostname.name(),
					modifications.get(SysConfKeys.http_proxy_hostname.name()).trim());
		}

		if (Strings.isNullOrEmpty(previous.stringValue(SysConfKeys.http_proxy_port.name()))
				&& !modifications.containsKey(SysConfKeys.http_proxy_port.name())) {
			modifications.put(SysConfKeys.http_proxy_port.name(), DEFAULT_PORT);
		} else if (modifications.containsKey(SysConfKeys.http_proxy_port.name())) {
			try {
				Integer.parseInt(modifications.get(SysConfKeys.http_proxy_port.name()));
			} catch (NumberFormatException nfe) {
				modifications.put(SysConfKeys.http_proxy_port.name(), DEFAULT_PORT);
			}
		}

		String login = modifications.get(SysConfKeys.http_proxy_login.name());
		if (login != null) {
			modifications.put(SysConfKeys.http_proxy_login.name(), login.trim());
		}

		String password = modifications.get(SysConfKeys.http_proxy_password.name());
		if (password != null) {
			modifications.put(SysConfKeys.http_proxy_password.name(), password.trim());
		}
	}
}
