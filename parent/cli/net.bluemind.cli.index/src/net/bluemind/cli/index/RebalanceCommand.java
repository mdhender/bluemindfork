/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.cli.index;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.UpdateByQueryAction;
import org.elasticsearch.index.reindex.UpdateByQueryRequestBuilder;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats.MailboxCount;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy.BoxesCount;
import net.bluemind.lib.elasticsearch.allocations.BoxAllocation;
import net.bluemind.lib.elasticsearch.allocations.rebalance.Rebalance;
import net.bluemind.lib.elasticsearch.allocations.rebalance.RebalanceBoxAllocator;
import net.bluemind.lib.elasticsearch.allocations.rebalance.RebalanceConfig;
import net.bluemind.lib.elasticsearch.allocations.rebalance.RebalanceSourcesCountByRefreshDurationRatio;
import net.bluemind.lib.elasticsearch.allocations.rebalance.RebalanceSpecificationFactory;
import net.bluemind.mailbox.api.IMailboxMgmt;
import net.bluemind.mailbox.api.ShardStats;
import net.bluemind.mailbox.api.ShardStats.MailboxStats;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "rebalance", description = "Rebalance mailspool indices")
public class RebalanceCommand implements ICmdLet, Runnable {
	enum Strategy {
		size, refresh_duration_ratio, refresh_duration_threshold
	}

	@Option(names = "--domain", description = "domain for mailboxes")
	public String domain = "global.virt";

	@Option(names = "--apply", description = "Run the operations on the indices (we default to dry mode)")
	public boolean applyRebalance = false;

	@Option(names = "--strategy", description = "Rebalance strategy to use (default ${DEFAULT-VALUE}): ${COMPLETION-CANDIDATES}")
	public Strategy strategy = Strategy.size;

	@Option(names = "--low-ratio", description = "Low refresh duration ratio to use if --strategy=refresh-duration-ratio (default=${DEFAULT-VALUE})")
	public double lowRatio = 0.2;

	@Option(names = "--high-ratio", description = "High refresh duration ratio to use if --strategy=refresh-duration-ratio (default=${DEFAULT-VALUE})")
	public double highRatio = 0.2;

	@Option(names = "--low-threshold", description = "Low refresh duration threshold to use if --strategy=refresh-duration-threshold (default=${DEFAULT-VALUE})")
	public long lowThreshold = 400;

	@Option(names = "--high-threshold", description = "High refresh duration threshold to use if --strategy=refresh-duration-threshold (default=${DEFAULT-VALUE})")
	public long highThreshold = 800;

	@Option(names = "--no-force-refresh", description = "Don't force indices to refresh to compute refresh duration")
	public boolean noForceRefresh = false;

	@Option(names = "--show-refresh-duration", description = "Show the index refresh duration")
	public boolean showRefreshDuration = false;

	private CliContext ctx;

	@Override
	public void run() {
		IMailboxMgmt globalVirtMboxMgmt = ctx.longRequestTimeoutAdminApi().instance(IMailboxMgmt.class, "global.virt");
		List<ShardStats> existing = globalVirtMboxMgmt.getShardsStats();

		CliUtils utils = new CliUtils(ctx);
		String domUid = utils.getDomainUidByDomain(domain);
		IMailboxMgmt mboxMgmt = ctx.longRequestTimeoutAdminApi().instance(IMailboxMgmt.class, domUid);
		if (strategy.equals(Strategy.size)) {
			bySize(mboxMgmt, existing);
		} else {
			List<AllocationShardStats> statsBefore = toAllocationStats(existing);
			Map<String, Long> refreshDurations = refreshDurations(globalVirtMboxMgmt, statsBefore);
			byRefreshDuration(strategy.name().replace("_", "-"), mboxMgmt, statsBefore, refreshDurations);
		}
	}

	private void bySize(IMailboxMgmt mboxMgmt, List<ShardStats> existing) {
		int startCount = 1;
		int totalBoxes = 0;
		for (ShardStats ss : existing) {
			int idxId = Integer.parseInt(ss.indexName.substring("mailspool_".length()));
			startCount = Math.max(startCount, idxId);
			totalBoxes += ss.mailboxes.size();

		}
		int avgBoxCount = totalBoxes / existing.size();

		ctx.info("average box count: {}", avgBoxCount);

		existing.sort((s1, s2) -> Long.compare(s2.size, s1.size));
		int mid = existing.size() / 2;
		List<ShardStats> sources = existing.subList(0, mid);
		List<ShardStats> targets = existing.subList(mid, existing.size());
		Collections.reverse(targets);

		Iterator<ShardStats> src = sources.iterator();
		Iterator<ShardStats> tgt = targets.iterator();

		int count = Math.min(sources.size(), targets.size());
		int loop = 0;
		while (src.hasNext() && tgt.hasNext()) {
			ShardStats source = src.next();
			ShardStats target = tgt.next();
			ctx.info("[" + (++loop) + "/" + count + "] operation starting.");
			if (source.topMailbox.isEmpty()) {
				continue;
			}
			long srcGb = source.size / 1024 / 1024 / 1024;
			long targetGb = target.size / 1024 / 1024 / 1024;
			long boxSize = Math.round(((double) source.size / source.mailboxes.size()) * 1.5);
			if (Math.abs(srcGb - targetGb) < 1) {
				ctx.warn(source.indexName + " and " + target.indexName + " have similar size (" + srcGb + " vs "
						+ targetGb + ")");
				continue;
			}
			MailboxStats topSrc = source.topMailbox.get(0);
			ctx.info("From {} ({}GB) to {} ({}GB)", source.indexName, srcGb, target.indexName, targetGb);
			ctx.info("Move {} ({}) from {} to {} (size:{})", topSrc.mailboxUid, topSrc.docCount, source.indexName,
					target.indexName, boxSize);

			applyRebalance(mboxMgmt, topSrc.mailboxUid, target.indexName, boxSize);
		}
	}

	private void byRefreshDuration(String strategyName, IMailboxMgmt mboxMgmt, List<AllocationShardStats> statsBefore,
			Map<String, Long> refreshDurations) {
		RebalanceConfig rebalanceConfig = new RebalanceConfig(lowRatio, highRatio, lowThreshold, highThreshold);
		Rebalance rebalance = new RebalanceSpecificationFactory(rebalanceConfig, refreshDurations)
				.instance(strategyName).apply(statsBefore);
		if (rebalance.sources.isEmpty() || rebalance.targets.isEmpty()) {
			ctx.info("No rebalance performed given the parameters");
		} else {
			ctx.info("Start rebalancing");
			Map<AllocationShardStats, BoxesCount> sourcesCount = new RebalanceSourcesCountByRefreshDurationRatio()
					.apply(rebalance);
			List<BoxAllocation> allocations = new RebalanceBoxAllocator().apply(rebalance, sourcesCount);
			allocations.forEach(allocation -> {
				AllocationShardStats sourceStat = statsBefore.stream()
						.filter(stat -> stat.indexName.equals(allocation.sourceIndex)).findFirst().orElse(null);
				long boxSize = Math.round(((double) sourceStat.size / sourceStat.mailboxes.size()) * 1.5);
				ctx.info("Move {} from {} to {} (size: {})", allocation.mbox, allocation.sourceIndex,
						allocation.targetIndex, boxSize);
				applyRebalance(mboxMgmt, allocation.mbox, allocation.targetIndex, boxSize);
			});
		}
	}

	protected List<AllocationShardStats> toAllocationStats(List<ShardStats> stats) {
		return stats.stream().map(stat -> {
			List<MailboxCount> mailboxesCount = stat.topMailbox.stream()
					.map(top -> new MailboxCount(top.mailboxUid, top.docCount)).collect(Collectors.toList());
			return new AllocationShardStats(stat.indexName, stat.docCount, stat.deletedCount, stat.externalRefreshCount,
					stat.externalRefreshDuration, stat.size, stat.mailboxes, mailboxesCount);
		}).collect(Collectors.toList());
	}

	private Map<String, Long> refreshDurations(IMailboxMgmt mboxMgmt, List<AllocationShardStats> statsBefore) {
		Map<String, Long> refreshDurations;
		if (noForceRefresh) {
			refreshDurations = refreshDurations(statsBefore);
		} else {
			triggerExternalRefresh(statsBefore);
			List<ShardStats> existingAfter = mboxMgmt.getShardsStats();
			List<AllocationShardStats> statsAfter = toAllocationStats(existingAfter);
			refreshDurations = refreshDurations(statsBefore, statsAfter);
		}
		refreshDurations.forEach((indexName, refreshDuration) -> ctx.info("{}: {}ms", indexName, refreshDuration));
		return refreshDurations;
	}

	private Map<String, Long> refreshDurations(List<AllocationShardStats> shardStats) {
		return shardStats.stream().collect(Collectors.toMap(stat -> stat.indexName,
				stat -> stat.externalRefreshDuration / stat.externalRefreshCount));
	}

	private Map<String, Long> refreshDurations(List<AllocationShardStats> statsBefore,
			List<AllocationShardStats> statsAfter) {
		Map<String, AllocationShardStats> afterByName = statsAfter.stream()
				.collect(Collectors.toMap(stat -> stat.indexName, stat -> stat));
		return statsBefore.stream().collect(Collectors.toMap(statBefore -> statBefore.indexName, statBefore -> {
			AllocationShardStats statAfter = afterByName.get(statBefore.indexName);
			long deltaDuration = statAfter.externalRefreshDuration - statBefore.externalRefreshDuration;
			long deltaCount = statAfter.externalRefreshCount - statBefore.externalRefreshCount;
			return deltaCount != 0 //
					? deltaDuration / deltaCount //
					: statBefore.externalRefreshDuration / statBefore.externalRefreshCount;
		}));
	}

	private void triggerExternalRefresh(List<AllocationShardStats> shardStats) {
		ctx.info("Force refreshing indices");
		shardStats.stream().forEach(stat -> {
			Client client = ESearchActivator.getClient();
			UpdateByQueryRequestBuilder updateByQuery = new UpdateByQueryRequestBuilder(client,
					UpdateByQueryAction.INSTANCE);
			try {
				updateByQuery.source(stat.indexName).maxDocs(1).refresh(true)
						.filter(QueryBuilders.termQuery("body_msg_link", "record")).get();
			} catch (Exception e) {
				ctx.info("Failed to force refresh {} (will use the current stat instead)", stat.indexName);
			}
		});
	}

	private void applyRebalance(IMailboxMgmt mboxMgmt, String mailboxUid, String targetIndexName, long boxSize) {
		if (!applyRebalance) {
			return;
		}

		try {
			TaskRef ref = mboxMgmt.moveIndex(mailboxUid, targetIndexName, true);
			Tasks.follow(ctx, ref, "",
					String.format("Failed to move index from %s to %s", mailboxUid, targetIndexName));
		} catch (Exception e) {
			if (e.getMessage() != null) {
				ctx.warn("WARN " + e.getMessage());
			} else {
				ctx.warn("WARN exception occured", e);
			}
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("index");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return RebalanceCommand.class;
		}
	}

}
