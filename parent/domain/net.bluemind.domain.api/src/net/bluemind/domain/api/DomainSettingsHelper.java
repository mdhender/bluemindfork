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
package net.bluemind.domain.api;

import java.util.Map;

import net.bluemind.core.api.fault.ServerFault;

public class DomainSettingsHelper {

	public static boolean isForwardToSlaveRelay(IDomainSettings settingsService) throws ServerFault {
		return isForwardToSlaveRelay(settingsService.get());
	}

	public static String getSlaveRelayHost(IDomainSettings settingsService) throws ServerFault {
		Map<String, String> settings = settingsService.get();
		return getSlaveRelayHost(settings);
	}

	public static String getSlaveRelayHost(Map<String, String> settings) {
		if (null != settings) {
			if (settings.get(DomainSettingsKeys.mail_routing_relay.name()) != null
					&& !settings.get(DomainSettingsKeys.mail_routing_relay.name()).isEmpty()) {
				return settings.get(DomainSettingsKeys.mail_routing_relay.name());
			}
		}
		return null;
	}

	public static boolean isForwardToSlaveRelay(Map<String, String> settings) {
		if (null != settings) {
			if (settings.containsKey(DomainSettingsKeys.mail_forward_unknown_to_relay.name())) {
				return Boolean.valueOf(settings.get(DomainSettingsKeys.mail_forward_unknown_to_relay.name()))
						.booleanValue();
			}
		}
		return false;
	}

}
