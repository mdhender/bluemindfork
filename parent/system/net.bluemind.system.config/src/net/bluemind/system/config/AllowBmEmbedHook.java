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
package net.bluemind.system.config;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.system.hook.ISystemConfigurationValidator;
import net.bluemind.system.nginx.NginxService;

public class AllowBmEmbedHook implements ISystemConfigurationObserver, ISystemConfigurationValidator {
	private static Logger logger = LoggerFactory.getLogger(AllowBmEmbedHook.class);
	private Optional<NginxService> nginxService;

	public AllowBmEmbedHook() {
		this.nginxService = Optional.empty();
	}

	public AllowBmEmbedHook(NginxService nginxService) {
		this.nginxService = Optional.of(nginxService);
	}

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		boolean previousAllowSiteEmbed = Boolean.parseBoolean(previous.values.get(SysConfKeys.allow_bm_embed.name()));
		boolean allowSiteEmbed = Boolean.parseBoolean(conf.values.get(SysConfKeys.allow_bm_embed.name()));

		if (allowSiteEmbed == previousAllowSiteEmbed) {
			return;
		}

		logger.info("System configuration {} has been updated", SysConfKeys.allow_bm_embed.name());
		nginxService.orElseGet(() -> new NginxService()).updateAllowBmEmbed(allowSiteEmbed);
	}

	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (modifications.containsKey(SysConfKeys.allow_bm_embed.name())
				&& !Strings.isNullOrEmpty(modifications.get(SysConfKeys.allow_bm_embed.name()))
				&& !modifications.get(SysConfKeys.allow_bm_embed.name()).equals("true")
				&& !modifications.get(SysConfKeys.allow_bm_embed.name()).equals("false")) {
			throw new ServerFault(String.format("%s must be true or false", SysConfKeys.allow_bm_embed.name()),
					ErrorCode.INVALID_PARAMETER);
		}
	}
}
