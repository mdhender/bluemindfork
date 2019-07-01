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

import com.google.common.base.Strings;

import net.bluemind.backend.postfix.internal.maps.MapRow;

public class VirtualAliasMap implements IMapGenerator {
	private static final String MAP_FILENAME = "/etc/postfix/virtual_alias";

	private final Collection<MapRow> mapRows;

	public VirtualAliasMap(Collection<MapRow> mapRows) {
		this.mapRows = mapRows;
	}

	@Override
	public String generateMap() {
		StringBuilder map = new StringBuilder();

		mapRows.forEach(mapRow -> mapRow.emails.forEach(email -> {
			if (!Strings.isNullOrEmpty(mapRow.getRecipients())) {
				map.append(email).append(" ").append(mapRow.getRecipients()).append("\n");
			}
		}));

		return map.toString();
	}

	@Override
	public String getMapFileName() {
		return MAP_FILENAME;
	}
}
