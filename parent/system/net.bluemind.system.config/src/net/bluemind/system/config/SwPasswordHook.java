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
package net.bluemind.system.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.system.hook.ISystemConfigurationSanitizor;
import net.bluemind.system.hook.ISystemConfigurationValidator;
import net.bluemind.system.nginx.NginxService;

public class SwPasswordHook
		implements ISystemConfigurationObserver, ISystemConfigurationSanitizor, ISystemConfigurationValidator {
	private static Logger logger = LoggerFactory.getLogger(SwPasswordHook.class);

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		String swPassword = conf.values.get(SysConfKeys.sw_password.name());
		if ((Strings.isNullOrEmpty(swPassword)
				&& Strings.isNullOrEmpty(previous.values.get(SysConfKeys.sw_password.name())))
				|| Strings.nullToEmpty(swPassword)
						.equals(Strings.nullToEmpty(previous.values.get(SysConfKeys.sw_password.name())))) {
			return;
		}

		logger.info("System configuration {} has been updated", SysConfKeys.sw_password.name());
		new NginxService().updateSwPassword(swPassword);
	}

	@Override
	public void sanitize(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (!modifications.containsKey(SysConfKeys.sw_password.name())
				|| modifications.get(SysConfKeys.sw_password.name()) == null) {
			return;
		}

		modifications.put(SysConfKeys.sw_password.name(), modifications.get(SysConfKeys.sw_password.name()).trim());
	}

	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (!modifications.containsKey(SysConfKeys.sw_password.name())) {
			return;
		}

		if (Strings.isNullOrEmpty(modifications.get(SysConfKeys.sw_password.name()))) {
			throw new ServerFault("SetupWizard password must not be null or empty!", ErrorCode.INVALID_PARAMETER);
		}

		if (modifications.get(SysConfKeys.sw_password.name()).contains("'")) {
			throw new ServerFault("\"'\" is a forbidden character in this password", ErrorCode.INVALID_PARAMETER);
		}
	}
}
