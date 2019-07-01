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

import com.google.common.base.Strings;

import net.bluemind.backend.postfix.internal.maps.MapRow;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;

public class TransportMap implements IMapGenerator {
	private static final String MAP_FILENAME = "/etc/postfix/transport";

	private final Map<String, ItemValue<Server>> smtpServerByDomainUid;
	private final Collection<MapRow> mapRows;

	public TransportMap(Map<String, ItemValue<Server>> smtpServerByDomainUid, Collection<MapRow> mapRows) {
		this.smtpServerByDomainUid = smtpServerByDomainUid;
		this.mapRows = mapRows;
	}

	@Override
	public String generateMap() {
		StringBuilder map = new StringBuilder();

		mapRows.forEach(mapRow -> {
			String mailboxName = mapRow.getMailboxName();
			if (mapRow.routing == Routing.none || Strings.isNullOrEmpty(mailboxName)) {
				return;
			}

			if (mapRow.routing == Routing.external && !Strings.isNullOrEmpty(mapRow.dataLocation)) {
				map.append(mailboxName).append(" smtp:").append(mapRow.dataLocation).append(":25\n");
			} else if (smtpServerByDomainUid.containsKey(mapRow.domain.uid)) {
				map.append(mailboxName).append(" smtp:")
						.append(smtpServerByDomainUid.get(mapRow.domain.uid).value.address()).append(":25\n");
			} else if (!Strings.isNullOrEmpty(mapRow.dataLocation)) {
				map.append(mailboxName).append(" lmtp:").append(mapRow.dataLocation).append(":2400\n");
			}
		});

		return map.toString();
	}

	@Override
	public String getMapFileName() {
		return MAP_FILENAME;
	}
}
