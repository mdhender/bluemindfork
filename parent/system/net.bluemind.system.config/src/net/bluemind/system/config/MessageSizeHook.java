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

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.system.api.GlobalSettingsKeys;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.system.hook.ISystemConfigurationValidator;
import net.bluemind.system.nginx.NginxService;

public class MessageSizeHook implements ISystemConfigurationObserver, ISystemConfigurationValidator {
	private static Logger logger = LoggerFactory.getLogger(MessageSizeHook.class);

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		logger.info("System configuration has been updated");
		MessageSizeValue messageSizeLimit = MessageSizeValue.getMessageSizeLimit(SysConfKeys.message_size_limit.name(),
				previous, conf);

		if (valueNotChanged(messageSizeLimit)) {
			logger.debug("Message size limit has not changed or is not set");
			return;
		}
		logger.info("Message size limit has changed to {}", messageSizeLimit.newValue);

		new NginxService().updateMessageSize(messageSizeLimit.newValue);
	}

	private boolean valueNotChanged(MessageSizeValue messageSizeLimit) {
		return !messageSizeLimit.isSet() || !messageSizeLimit.hasChanged();
	}

	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (modifications.containsKey(SysConfKeys.message_size_limit.name())) {
			try {
				Long.parseLong(modifications.get(SysConfKeys.message_size_limit.name()));
			} catch (NumberFormatException nfe) {
				throw new ServerFault(
						String.format("%s must be a valid integer", SysConfKeys.message_size_limit.name()),
						ErrorCode.INVALID_PARAMETER);
			}
		}

		if (modifications.containsKey(GlobalSettingsKeys.filehosting_max_filesize.name())) {
			try {
				Long.parseLong(modifications.get(GlobalSettingsKeys.filehosting_max_filesize.name()));
			} catch (NumberFormatException nfe) {
				throw new ServerFault(
						String.format("%s must be a valid integer", GlobalSettingsKeys.filehosting_max_filesize.name()),
						ErrorCode.INVALID_PARAMETER);
			}
		}
	}
}
