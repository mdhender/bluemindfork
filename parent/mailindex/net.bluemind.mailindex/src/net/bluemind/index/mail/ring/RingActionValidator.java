/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.index.mail.ring;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.index.mail.ring.AliasRing.RingAlias;
import net.bluemind.lib.elasticsearch.IndexAliasCreator.RingIndexAliasCreator;

public class RingActionValidator {

	enum ACTION {
		ADD_INDEX, REMOVE_INDEX
	}

	public static void validate(AliasRing ring, ACTION action, int position) {

		if (ring.getIndices().stream().anyMatch(index -> index.readAliases().size() != index.writeAliases().size())) {
			throw new ServerFault("There is already a rebalance action running");
		}

		switch (action) {
		case ADD_INDEX:
			if (ring.getIndices().stream().map(index -> index.position()).anyMatch(index -> index == position)) {
				throw new ServerFault(
						"Index " + RingIndexAliasCreator.getIndexRingName("mailspool", position) + " already exists");
			}

			if (ring.getIndices().stream().noneMatch(
					index -> index.aliases().stream().map(RingAlias::position).toList().contains(position))) {
				throw new ServerFault("Index " + RingIndexAliasCreator.getIndexRingName("mailspool", position)
						+ " must not extend existing ring");
			}

			if (ring.getIndices().stream()
					.anyMatch(index -> index.readAliases().size() != index.writeAliases().size())) {
				throw new ServerFault("There is already a rebalance action running");
			}

			break;

		case REMOVE_INDEX:
			if (ring.getIndices().stream().map(index -> index.position()).noneMatch(index -> index == position)) {
				throw new ServerFault(
						"Index " + RingIndexAliasCreator.getIndexRingName("mailspool", position) + " does not exist");
			}

			break;
		}

	}

}
