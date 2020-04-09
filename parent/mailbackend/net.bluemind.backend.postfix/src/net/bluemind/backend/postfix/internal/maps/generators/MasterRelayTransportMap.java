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

package net.bluemind.backend.postfix.internal.maps.generators;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.bluemind.backend.postfix.internal.maps.DomainInfo;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsHelper;

public class MasterRelayTransportMap implements IMapGenerator {
	private static final String MAP_FILENAME = "/etc/postfix/master_relay_transport";

	private Map<ItemValue<Domain>, Map<String, String>> domainSettingsByDomains;

	private MasterRelayTransportMap(Map<ItemValue<Domain>, Map<String, String>> domainSettingsByDomains) {
		this.domainSettingsByDomains = domainSettingsByDomains;
	}

	public static MasterRelayTransportMap init(Map<String, DomainInfo> domainInfoByUid) {
		Map<ItemValue<Domain>, Map<String, String>> forwardToSlaveRelayDomains = new HashMap<>();

		domainInfoByUid.values().forEach(domainInfo -> {
			String slaveRelayHost = DomainSettingsHelper.getSlaveRelayHost(domainInfo.domainSettings);

			if (DomainSettingsHelper.isForwardToSlaveRelay(domainInfo.domainSettings) && slaveRelayHost != null
					&& !slaveRelayHost.isEmpty()) {
				forwardToSlaveRelayDomains.put(domainInfo.domain, domainInfo.domainSettings);
			}
		});

		return new MasterRelayTransportMap(forwardToSlaveRelayDomains);
	}

	@Override
	public String generateMap() {
		StringBuilder map = new StringBuilder();

		for (Entry<ItemValue<Domain>, Map<String, String>> e : domainSettingsByDomains.entrySet()) {
			String relay = DomainSettingsHelper.getSlaveRelayHost(e.getValue());
			map.append(e.getKey().value.name).append(" smtp:").append(relay).append(":25\n");

			e.getKey().value.aliases.forEach(alias -> map.append(alias).append(" smtp:").append(relay).append(":25\n"));
		}

		return map.toString();
	}

	@Override
	public String getMapFileName() {
		return MAP_FILENAME;
	}
}
