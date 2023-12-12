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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.lib.elasticsearch.ESearchActivator.IndexDefinition;
import net.bluemind.lib.elasticsearch.IndexAliasMode.Mode;

public abstract class IndexAliasCreator {

	private static final Logger logger = LoggerFactory.getLogger(IndexAliasCreator.class);
	private static final String readAliasString = "_ring_alias_read";
	private static final String writeAliasString = "_ring_alias_write";

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
			if (index.contains("_ring_")) {
				return index;
			}
			int maxAliasCount = getMaxAliasCount(totalNumberOfIndexes);
			int steps = maxAliasCount / totalNumberOfIndexes;
			int start = steps / 2;
			int indexPosition = start + ((loopIndex - 1) * steps);
			return getIndexRingName(index, indexPosition);
		}

		public static String getIndexRingName(String index, int numericIndex) {
			return index + "_ring_" + numericIndex;
		}

		public static String composeRead(String indexName, int aliasPosition) {
			return indexName + readAliasString + aliasPosition;
		}

		public static String composeWrite(String indexName, int aliasPosition) {
			return indexName + writeAliasString + aliasPosition;
		}

		public static int decompose(String indexName) {
			return Integer.parseInt(indexName.substring(indexName.lastIndexOf("_") + 1));
		}

		public static int decomposeAlias(String alias) {
			if (alias.contains(readAliasString)) {
				return Integer.parseInt(alias.substring(alias.indexOf(readAliasString) + readAliasString.length()));
			} else {
				return Integer.parseInt(alias.substring(alias.indexOf(writeAliasString) + writeAliasString.length()));
			}
		}

		public static boolean isReadAlias(String alias) {
			return alias.contains(readAliasString);
		}

		private int getMaxAliasCount(int totalNumberOfIndexes) {
			return totalNumberOfIndexes * ElasticsearchClientConfig.getMaxAliasMultiplier();
		}

		@Override
		protected void addAliases(String index, String indexName, int totalNumberOfIndexes) {
			Set<String> existingAliases = new HashSet<>();
			if (index.contains("_ring_")) {
				index = index.substring(0, index.indexOf("_"));
				try {
					existingAliases = existingAliases();
				} catch (Exception e) {
					logger.warn("Cannot detect existing aliases", e);
				}
			}
			int start = decompose(indexName);
			int maxAliasCount = getMaxAliasCount(totalNumberOfIndexes);
			int steps = maxAliasCount / totalNumberOfIndexes;
			for (int i = 0; i < steps; i++) {
				int aliasPosition = start--;
				if (aliasPosition < 0) {
					aliasPosition = maxAliasCount + aliasPosition;
				}
				String boxAliasRead = composeRead(index, aliasPosition);
				String boxAliasWrite = composeWrite(index, aliasPosition);

				if (existingAliases.contains(boxAliasRead) || existingAliases.contains(boxAliasWrite)) {
					logger.info("Skipping alias creation of {} and {}. Aliases are beeing used by another index",
							boxAliasRead, boxAliasWrite);
				}

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

	public Set<String> existingAliases() throws ElasticsearchException, IOException {
		Set<String> aliases = new HashSet<>();
		GetMappingResponse response = ESearchActivator.getClient().indices().getMapping(b -> b.index("mailspool*"));
		List<String> indices = response.result().entrySet().stream().map(Entry::getKey).sorted().toList();
		for (String indexName : indices) {
			IndexAliases aliasesRsp = ESearchActivator.getClient().indices().getAlias(a -> a.index(indexName))
					.get(indexName);
			aliases.addAll(aliasesRsp.aliases().keySet());
		}
		return aliases;
	}

}
