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
package net.bluemind.index.mail.ring.actions;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import net.bluemind.index.mail.ring.AliasRing.RingAlias;
import net.bluemind.index.mail.ring.AliasRing.RingIndex;

public class MoveAliasAction implements IndexAction {

	private final RingIndex sourceIndex;
	private final SortedSet<RingAlias> concernedAliases;
	private final String targetIndex;

	public MoveAliasAction(RingIndex sourceIndex, SortedSet<RingAlias> concernedAliases, String targetIndex) {
		this.sourceIndex = sourceIndex;
		this.concernedAliases = concernedAliases;
		this.targetIndex = targetIndex;
	}

	public void execute(ElasticsearchClient esClient) throws ElasticsearchException, IOException {
		List<String> aliases = concernedAliases.stream().map(RingAlias::name).toList();
		if (!aliases.isEmpty()) {
			esClient.indices().updateAliases(u -> u //
					.actions(action -> action
							.remove(removeAction -> removeAction.index(sourceIndex.name()).aliases(aliases))) //
					.actions(action -> action.add(addAction -> addAction.index(targetIndex).aliases(aliases))));
		}
	}

	@Override
	public String info() {
		return String.format("Moving aliases %s from index %s to %s",
				String.join(",", concernedAliases.stream().map(RingAlias::name).toList()), sourceIndex.name(),
				targetIndex);
	}

}
