package net.bluemind.lib.elasticsearch.allocations.newshard;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy;

public class NewShardSourcesCountByRefreshDurationRatio implements AllocatorSourcesCountStrategy<NewShard> {
	private final Map<String, Long> refreshDurations;
	private final long refreshThreshold;

	public NewShardSourcesCountByRefreshDurationRatio(Map<String, Long> refreshDurations, long refreshThreshold) {
		this.refreshDurations = refreshDurations;
		this.refreshThreshold = refreshThreshold;
	}

	@Override
	public Map<AllocationShardStats, BoxesCount> apply(NewShard newShard) {
		List<AllocationShardStats> shardAboveTarget = newShard.sources.stream()
				.filter(stat -> stat.docCount > newShard.docCount).collect(Collectors.toList());

		long aboveThresholdDocCount = shardAboveTarget.stream()
				.filter(stat -> refreshDurations.get(stat.indexName) >= refreshThreshold)
				.mapToLong(stat -> stat.docCount - newShard.docCount) //
				.sum();

		AtomicLong remainingDocCount = new AtomicLong(newShard.docCount);
		AtomicInteger remainingIndexCount = new AtomicInteger(shardAboveTarget.size());
		return shardAboveTarget.stream()
				.sorted((stat1, stat2) -> refreshDurations.get(stat2.indexName)
						.compareTo(refreshDurations.get(stat1.indexName)))
				.collect(Collectors.toMap(stat -> stat, stat -> {
					long maxdocContribution = Math.round(newShard.docCount
							* ((stat.docCount - newShard.docCount) / (double) aboveThresholdDocCount));
					long docContribution = (refreshDurations.get(stat.indexName) >= refreshThreshold)
							? Math.min(stat.docCount - newShard.docCount, maxdocContribution)
							: remainingDocCount.get() / remainingIndexCount.get();
					docContribution = remainingDocCount.get() < 0 ? 0 : Math.max(0, docContribution);
					BoxesCount boxesCount = boxesCount(stat, docContribution);
					remainingDocCount.addAndGet(-docContribution);
					remainingIndexCount.decrementAndGet();
					return boxesCount;
				}));
	}

}