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
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.system.hook.ISystemConfigurationSanitizor;
import net.bluemind.system.hook.ISystemConfigurationValidator;

public class DefaultDomainHook
		implements ISystemConfigurationObserver, ISystemConfigurationSanitizor, ISystemConfigurationValidator {
	private static Logger logger = LoggerFactory.getLogger(DefaultDomainHook.class);

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		String defaultDomain = conf.values.get(SysConfKeys.default_domain.name());
		if ((Strings.isNullOrEmpty(defaultDomain)
				&& Strings.isNullOrEmpty(previous.values.get(SysConfKeys.default_domain.name())))
				|| Strings.nullToEmpty(defaultDomain)
						.equals(Strings.nullToEmpty(previous.values.get(SysConfKeys.default_domain.name())))) {
			return;
		}

		logger.info("System configuration {} has been updated, changed to {}", SysConfKeys.default_domain.name(),
				defaultDomain);
		VertxPlatform.eventBus().publish("bm.defaultdomain.changed", (Object) null);
	}

	@Override
	public void sanitize(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (!modifications.containsKey(SysConfKeys.default_domain.name())) {
			return;
		}

		if (modifications.get(SysConfKeys.default_domain.name()) == null) {
			modifications.put(SysConfKeys.default_domain.name(), "");
		}

		modifications.put(SysConfKeys.default_domain.name(),
				modifications.get(SysConfKeys.default_domain.name()).trim());
	}

	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (!modifications.containsKey(SysConfKeys.default_domain.name())
				|| modifications.get(SysConfKeys.default_domain.name()).isEmpty()) {
			return;
		}

		try {
			if (ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class)
					.findByNameOrAliases(modifications.get(SysConfKeys.default_domain.name())) == null) {
				throw new ServerFault(String.format("Unable to check if default domain '{}' exists",
						modifications.get(SysConfKeys.default_domain.name())), ErrorCode.INVALID_PARAMETER);
			}
		} catch (Exception e) {
			logger.error("Unable to check if default domain '{}' exists",
					modifications.get(SysConfKeys.default_domain.name()), e);
			throw new ServerFault(
					String.format("Unable to check if default domain '%s' exists: %s",
							modifications.get(SysConfKeys.default_domain.name()), e.getMessage()),
					ErrorCode.INVALID_PARAMETER);
		}
	}
}
