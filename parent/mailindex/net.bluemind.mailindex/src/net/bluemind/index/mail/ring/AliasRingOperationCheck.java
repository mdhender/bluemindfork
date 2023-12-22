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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.index.MailIndexActivator;
import net.bluemind.index.mail.MailIndexService;
import net.bluemind.index.mail.ring.AliasRing.RingAlias;
import net.bluemind.index.mail.ring.AliasRing.RingIndex;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.IndexAliasMode;
import net.bluemind.lib.elasticsearch.IndexAliasMode.Mode;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.StateContext;

public class AliasRingOperationCheck extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(AliasRingOperationCheck.class);

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new AliasRingOperationCheck();
		}

	}

	@Override
	public void start() {
		if (IndexAliasMode.getMode() == Mode.RING) {
			check();
		}
	}

	private void check() {
		vertx.setTimer(60000, (id -> {
			if (StateContext.getState() == SystemState.CORE_STATE_RUNNING) {
				IMailIndexService service = MailIndexActivator.getService();
				if (!service.isNoop()) {
					checkAliasCoherency(service);
				}
			} else {
				check();
			}
		}));
	}

	private void checkAliasCoherency(IMailIndexService service) {
		MailIndexService serviceImpl = (MailIndexService) service;
		AliasRing ring = AliasRing.create(ESearchActivator.getClient(), serviceImpl);
		if (!ring.isCoherent()) {
			logger.info("Mailspool alias ring is not coherent");
			List<RingIndex> incoherentIndices = ring.getIndices().stream()
					.filter(index -> index.readAliases().size() != index.writeAliases().size()).toList();
			showIndicesInfo(incoherentIndices);

			removeEmptyIndices(ring);
			resumeRebalanceActions(ring);
		}
	}

	private void resumeRebalanceActions(AliasRing ring) {
		Set<RingIndex> sourceIndices = getIndicesHavingMissingWriteIndices(ring.getIndices());

		for (RingIndex source : sourceIndices) {
			Optional<RingIndex> targetIndex = findTargetIndex(ring, source);
			targetIndex.ifPresentOrElse(target -> {
				try {
					logger.info("Detected unfinished index operation from index {} to {}", source, target);
					var isDeletion = source.readAliases().size() == ring.getIndices().stream()
							.filter(index -> index.name().equals(source.name())).findFirst().get().readAliases().size();
					ring.rebalance(source, target.position());
					if (isDeletion) {
						ring.deleteIndex(source.name());
					}
				} catch (Exception e) {
					logger.error("Cannot rebalance indices {} --> {}", source.name(), target.name(), e);
				}
			}, () -> logger.error("Cannot determine which rebalance operation needs to be done for source index {}",
					source.name()));
		}

	}

	private Set<RingIndex> getIndicesHavingMissingWriteIndices(SortedSet<RingIndex> indices) {
		Set<RingIndex> sourceIndices = new HashSet<>();

		for (RingIndex index : indices) {
			SortedSet<RingAlias> brokenReadAliases = new TreeSet<>(index.readAliases().stream().filter(aliasA -> {
				int position = aliasA.position();
				return index.writeAliases().stream().noneMatch(aliasB -> aliasB.position() == position);
			}).toList());
			if (!brokenReadAliases.isEmpty()) {
				sourceIndices.add(new RingIndex(index.name(), brokenReadAliases, new TreeSet<>()));
			}
		}

		return sourceIndices;
	}

	private Optional<RingIndex> findTargetIndex(AliasRing ring, RingIndex source) {
		return ring
				.getIndices().stream().filter(
						index -> index.writeAliases().stream()
								.anyMatch(writeAlias -> source.readAliases().stream()
										.anyMatch(readAlias -> readAlias.position() == writeAlias.position())))
				.findAny();
	}

	private void removeEmptyIndices(AliasRing ring) {
		for (RingIndex emptyIndex : ring.getIndices().stream().filter(index -> index.aliases().isEmpty()).toList()) {
			logger.info("Detected empty index {}", emptyIndex.name());
			try {
				ring.removeIndex(emptyIndex.position());
			} catch (Exception e) {
				logger.info("Cannot delete empty index {}", emptyIndex.name(), e);
			}
		}
	}

	private void showIndicesInfo(List<RingIndex> incoherentIndices) {
		StringBuilder info = new StringBuilder();
		for (RingIndex index : incoherentIndices) {
			info.append(String.format("Index: %s, aliases: %s%n", index.name(),
					String.join(",", index.aliases().stream().map(RingAlias::name).toList())));
		}
		logger.info("Incoherent indices: {}\r\n", info);
	}

}
