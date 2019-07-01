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

import java.util.ArrayList;
import java.util.Collection;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

public class VirtualDomainsMap implements IMapGenerator {
	private static final String MAP_FILENAME = "/etc/postfix/virtual_domains";

	private Collection<ItemValue<Domain>> domains;

	public VirtualDomainsMap(Collection<ItemValue<Domain>> domains) {
		this.domains = new ArrayList<>();

		domains.forEach(d -> {
			boolean found = false;
			for (ItemValue<Domain> dIv : this.domains) {
				if (d.uid.equals(dIv.uid)) {
					found = true;
					break;
				}
			}

			if (!found) {
				this.domains.add(d);
			}
		});
	}

	@Override
	public String generateMap() {
		StringBuilder map = new StringBuilder();

		for (ItemValue<Domain> domain : domains) {
			map.append(domain.value.name).append(" OK\n");

			domain.value.aliases.forEach(alias -> map.append(alias).append(" OK\n"));
		}

		return map.toString();
	}

	@Override
	public String getMapFileName() {
		return MAP_FILENAME;
	}
}
