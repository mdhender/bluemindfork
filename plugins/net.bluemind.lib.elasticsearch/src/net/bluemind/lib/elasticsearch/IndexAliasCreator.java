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
package net.bluemind.lib.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.lib.elasticsearch.ESearchActivator.IndexDefinition;
import net.bluemind.lib.elasticsearch.IndexAliasMode.Mode;

public abstract class IndexAliasCreator {

	private static final Logger logger = LoggerFactory.getLogger(IndexAliasCreator.class);

	protected abstract String getIndexName(String index, int count, int loopIndex);

	protected abstract void addAliases(String index, String indexName, int count) throws ServerFault;

	protected static IndexAliasCreator get(IndexDefinition definition) {
		return !definition.supportsAliasRing || IndexAliasMode.getMode() == Mode.ONE_TO_ONE
				? new OneToOneIndexAliasCreator()
				: new RingIndexAliasCreator();
	}

	public static class OneToOneIndexAliasCreator extends IndexAliasCreator {

		@Override
		protected String getIndexName(String index, int count, int loopIndex) {
			return (count == 1) ? index : index + "_" + loopIndex;
		}

		@Override
		protected void addAliases(String index, String indexName, int count) {
			// TODO one to one does create aliases on the fly
		}

	}

	public static class RingIndexAliasCreator extends IndexAliasCreator {

		@Override
		protected String getIndexName(String index, int totalNumberOfIndexes, int loopIndex) {
			int maxAliasCount = getMaxAliasCount(totalNumberOfIndexes);
			int steps = maxAliasCount / totalNumberOfIndexes;
			int start = steps / 2;
			int indexPosition = start + ((loopIndex - 1) * steps);
			return index + "_ring_" + indexPosition;
		}

		private int getMaxAliasCount(int totalNumberOfIndexes) {
			return totalNumberOfIndexes * ElasticsearchClientConfig.getMaxAliasMultiplier();
		}

		@Override
		protected void addAliases(String index, String indexName, int totalNumberOfIndexes) {
			if (index.contains("_ring_")) {
				index = index.substring(0, index.indexOf("_"));
			}
			int start = Integer.parseInt(indexName.substring(indexName.lastIndexOf("_") + 1));
			int maxAliasCount = getMaxAliasCount(totalNumberOfIndexes);
			int steps = maxAliasCount / totalNumberOfIndexes;
			for (int i = 0; i < steps; i++) {
				int aliasPosition = start--;
				if (aliasPosition < 0) {
					aliasPosition = maxAliasCount + aliasPosition;
				}
				String boxAliasRead = index + "_ring_alias_read" + aliasPosition;
				String boxAliasWrite = index + "_ring_alias_write" + aliasPosition;
				try {
					logger.info("Creating alias {} targeting index {}", aliasPosition, indexName);
					ESearchActivator.getClient().indices().updateAliases(u -> u.actions(a -> a.add(ad -> ad //
							.index(indexName).alias(boxAliasRead))));
					ESearchActivator.getClient().indices().updateAliases(u -> u.actions(a -> a.add(ad -> ad //
							.index(indexName).alias(boxAliasWrite))));
				} catch (Exception e) {
					logger.warn("Cannot create alias {} of index {}", aliasPosition, indexName, e);
				}
			}
		}

	}

}
