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

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationValidator;

public class ProxyValidator implements ISystemConfigurationValidator {
	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		String proxyEnabled = getValue(SysConfKeys.http_proxy_enabled.name(), previous, modifications);
		if (!Boolean.parseBoolean(proxyEnabled)) {
			return;
		}

		String proxyHostname = getValue(SysConfKeys.http_proxy_hostname.name(), previous, modifications);
		if (Strings.isNullOrEmpty(proxyHostname)) {
			throw new ServerFault("Proxy hostname must be defined", ErrorCode.INVALID_PARAMETER);
		}

		String proxyPort = getValue(SysConfKeys.http_proxy_port.name(), previous, modifications);
		try {
			int port = Integer.parseInt(proxyPort);
			if (port < 1 || port > 65535) {
				throw new ServerFault("Proxy port must be an integer between 1 and 65535", ErrorCode.INVALID_PARAMETER);
			}
		} catch (NumberFormatException nfe) {
			throw new ServerFault("Proxy port must be an integer", ErrorCode.INVALID_PARAMETER);
		}

		String proxyLogin = getValue(SysConfKeys.http_proxy_login.name(), previous, modifications);
		String proxyPassword = getValue(SysConfKeys.http_proxy_password.name(), previous, modifications);

		if (!Strings.isNullOrEmpty(proxyLogin) && Strings.isNullOrEmpty(proxyPassword)) {
			throw new ServerFault("Proxy password must be defined for login '" + proxyLogin + "'",
					ErrorCode.INVALID_PARAMETER);
		}

		if (Strings.isNullOrEmpty(proxyLogin) && !Strings.isNullOrEmpty(proxyPassword)) {
			throw new ServerFault("Proxy login must be defined", ErrorCode.INVALID_PARAMETER);
		}
	}

	private String getValue(String key, SystemConf previous, Map<String, String> modifications) {
		if (modifications.containsKey(key)) {
			return modifications.get(key);
		}

		return previous.values.get(key);
	}
}
