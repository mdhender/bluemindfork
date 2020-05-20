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

import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.system.hook.ISystemConfigurationSanitizor;
import net.bluemind.system.hook.ISystemConfigurationValidator;
import net.bluemind.system.nginx.NginxService;

public class ExternalUrlHook
		implements ISystemConfigurationObserver, ISystemConfigurationSanitizor, ISystemConfigurationValidator {
	private static Logger logger = LoggerFactory.getLogger(ExternalUrlHook.class);

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		String externalUrl = conf.stringValue(SysConfKeys.external_url.name());
		if ((Strings.isNullOrEmpty(externalUrl)
				&& Strings.isNullOrEmpty(previous.values.get(SysConfKeys.external_url.name())))
				|| Strings.nullToEmpty(externalUrl)
						.equals(Strings.nullToEmpty(previous.values.get(SysConfKeys.external_url.name())))) {
			return;
		}

		logger.info("System configuration {} has been updated, changed to {}", SysConfKeys.external_url.name(),
				externalUrl);
		new NginxService().updateExternalUrl(externalUrl);
	}

	@Override
	public void sanitize(SystemConf previous, Map<String, String> modifications) throws ServerFault {
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
