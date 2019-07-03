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

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import net.bluemind.backend.postfix.internal.maps.DomainInfo;
import net.bluemind.backend.postfix.internal.maps.MapRow;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsHelper;

public class VirtualMailboxesMap implements IMapGenerator {
	private static final String MAP_FILENAME = "/etc/postfix/virtual_mailbox";

	private final Collection<ItemValue<Domain>> forwardToSlaveRelayDomains;
	private final Collection<MapRow> mapRows;

	private VirtualMailboxesMap(Collection<ItemValue<Domain>> forwardToSlaveRelayDomains, Collection<MapRow> mapRows) {
		this.forwardToSlaveRelayDomains = forwardToSlaveRelayDomains;
		this.mapRows = mapRows;
	}

	public static VirtualMailboxesMap init(Map<String, DomainInfo> domainInfoByUid, Collection<MapRow> mapRows) {
		Map<String, ItemValue<Domain>> forwardToSlaveRelayDomainsUids = domainInfoByUid.values().stream()
				.filter(domainInfo -> DomainSettingsHelper.isForwardToSlaveRelay(domainInfo.domainSettings))
				.collect(Collectors.toMap(domainInfo -> domainInfo.domain.uid, domainInfo -> domainInfo.domain));

		Collection<MapRow> mapRowsToManage = mapRows.stream()
				.filter(mapRow -> !forwardToSlaveRelayDomainsUids.containsKey(mapRow.domain.uid))
				.collect(Collectors.toList());

		return new VirtualMailboxesMap(forwardToSlaveRelayDomainsUids.values(), mapRowsToManage);
	}

	@Override
	public String generateMap() {
		StringBuilder map = new StringBuilder();

		forwardToSlaveRelayDomains.forEach(d -> {
			map.append("@").append(d.value.name).append(" OK\n");

			if (d.value.aliases != null) {
				d.value.aliases.forEach(da -> {
					map.append("@").append(da).append(" OK\n");
				});
			}
		});

		mapRows.forEach(mapRow -> {
			String mailboxName = mapRow.getMailboxName();
			if (!Strings.isNullOrEmpty(mailboxName)) {
				map.append(mailboxName).append(" OK\n");
			}
		});

		return map.toString();
	}

	@Override
	public String getMapFileName() {
		return MAP_FILENAME;
	}
}
