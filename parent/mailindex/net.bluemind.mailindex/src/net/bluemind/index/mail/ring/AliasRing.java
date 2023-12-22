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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.index.mail.MailIndexService;
import net.bluemind.index.mail.ring.RingActionValidator.ACTION;
import net.bluemind.index.mail.ring.actions.CopyDocumentsAction;
import net.bluemind.index.mail.ring.actions.CreateIndexAction;
import net.bluemind.index.mail.ring.actions.DeleteIndexAction;
import net.bluemind.index.mail.ring.actions.MoveAliasAction;
import net.bluemind.index.mail.statistics.ShardStatistics.RingShardStatistics;
import net.bluemind.lib.elasticsearch.IndexAliasCreator.RingIndexAliasCreator;

public class AliasRing {

	private static final Logger logger = LoggerFactory.getLogger(AliasRing.class);
	private SortedSet<RingIndex> indices;
	private final ElasticsearchClient esClient;
	private final MailIndexService service;

	public AliasRing(ElasticsearchClient esClient, MailIndexService service, SortedSet<RingIndex> indices) {
		this.esClient = esClient;
		this.service = service;
		this.indices = indices;
	}

	public static AliasRing create(ElasticsearchClient esClient, MailIndexService service) {
		return new AliasRing(esClient, service, getRing(esClient, service));
	}

	private static SortedSet<RingIndex> getRing(ElasticsearchClient esClient, MailIndexService service) {
		var ringStatistics = new RingShardStatistics(service.getMetricRegistry(), service.getIdFactory())
				.getRing(esClient);
		return new TreeSet<>(ringStatistics.entrySet().stream().map(entry -> {
			String name = entry.getKey();
			Map<Boolean, List<RingAlias>> aliases = entry.getValue().stream().map(RingAlias::new)
					.collect(Collectors.partitioningBy(RingAlias::isReadAlias));
			return new RingIndex(name, new TreeSet<>(aliases.get(Boolean.TRUE)),
					new TreeSet<>(aliases.get(Boolean.FALSE)));
		}).toList());
	}

	public void addIndex(Integer position) throws ElasticsearchException, IOException {
		var targetIndex = RingIndexAliasCreator.getIndexRingName("mailspool", position);
		logger.info("Adding index {} to mailspool alias ring", targetIndex);

		RingActionValidator.validate(this, ACTION.ADD_INDEX, position);
		var nextIndex = getNextIndex(position);

		createIndex(targetIndex);
		rebalance(nextIndex, position);

		this.indices = getRing(esClient, service);
	}

	public void removeIndex(Integer position) throws ElasticsearchException, IOException {
		var targetIndex = RingIndexAliasCreator.getIndexRingName("mailspool", position);
		logger.info("Removing index {} from mailspool alias ring", targetIndex);

		RingIndex index = getIndex(position);
		if (!index.aliases().isEmpty()) {
			RingActionValidator.validate(this, ACTION.REMOVE_INDEX, position);
			var nextIndex = getNextIndex(position);
			rebalance(index, nextIndex.position());
		}
		deleteIndex(index.name);

		this.indices = getRing(esClient, service);
	}

	public boolean isCoherent() {
		return RingActionValidator.isCoherent(this);
	}

	private RingIndex getIndex(int position) {

		Optional<RingIndex> index = indices.stream().filter(ringIndex -> ringIndex.position() == position).findFirst();

		if (index.isEmpty()) {
			throw new ServerFault(
					"Index " + RingIndexAliasCreator.getIndexRingName("mailspool", position) + " does not exist");
		} else {
			return index.get();
		}
	}

	private RingIndex getNextIndex(int position) {
		for (RingIndex index : indices) {
			if (index.position() > position) {
				return index;
			}
		}
		return indices.getFirst();
	}

	public SortedSet<RingIndex> getIndices() {
		return indices;
	}

	public void createIndex(String indexName) throws ElasticsearchException, IOException {
		new AliasRingOperations.Builder(esClient, "Adding index " + indexName) //
				.action(new CreateIndexAction(indexName)) //
				.execute();
	}

	public void deleteIndex(String indexName) throws ElasticsearchException, IOException {
		new AliasRingOperations.Builder(esClient, "Deleting index " + indexName) //
				.action(new DeleteIndexAction(indexName)) //
				.execute();
	}

	public void rebalance(RingIndex sourceIndex, int targetPosition) throws ElasticsearchException, IOException {
		var targetIndex = RingIndexAliasCreator.getIndexRingName("mailspool", targetPosition);

		var concernedReadAliases = new TreeSet<>(
				sourceIndex.readAliases().stream().filter(alias -> alias.position() <= targetPosition).toList());
		var concerncedWriteAliases = new TreeSet<>(
				sourceIndex.writeAliases().stream().filter(alias -> alias.position() <= targetPosition).toList());

		new AliasRingOperations.Builder(esClient,
				String.format("Rebalancing source index %s with target index %s", sourceIndex.name(), targetIndex)) //
				.action(new MoveAliasAction(sourceIndex, concerncedWriteAliases, targetIndex)) //
				.action(new CopyDocumentsAction(service, sourceIndex, concernedReadAliases, targetIndex)) //
				.action(new MoveAliasAction(sourceIndex, concernedReadAliases, targetIndex)) //
				.execute();
	}

	public static record RingIndex(String name, SortedSet<RingAlias> readAliases, SortedSet<RingAlias> writeAliases)
			implements Comparable<RingIndex> {

		public int position() {
			return RingIndexAliasCreator.decompose(name);
		}

		public Set<RingAlias> aliases() {
			Set<RingAlias> alias = new TreeSet<>();
			alias.addAll(readAliases);
			alias.addAll(writeAliases);
			return alias;
		}

		@Override
		public int compareTo(RingIndex other) {
			return Integer.compare(this.position(), other.position());
		}

	}

	public static record RingAlias(String name) implements Comparable<RingAlias> {

		public int position() {
			return RingIndexAliasCreator.decomposeAlias(name);
		}

		public boolean isReadAlias() {
			return RingIndexAliasCreator.isReadAlias(name);
		}

		public boolean isWriteAlias() {
			return !RingIndexAliasCreator.isReadAlias(name);
		}

		@Override
		public int compareTo(RingAlias other) {
			return Integer.compare(this.position(), other.position());
		}
	}

}
