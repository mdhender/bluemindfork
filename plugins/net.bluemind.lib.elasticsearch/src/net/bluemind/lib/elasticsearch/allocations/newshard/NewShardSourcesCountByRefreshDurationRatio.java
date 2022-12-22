package net.bluemind.lib.elasticsearch.allocations.newshard;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
	public Map<AllocationShardStats, Integer> apply(NewShard newShard) {
		List<AllocationShardStats> aboveTargetBoxCount = newShard.sources.stream()
				.filter(stat -> stat.mailboxes.size() > newShard.boxCount).collect(Collectors.toList());

		long aboveThresholdBoxCount = aboveTargetBoxCount.stream()
				.filter(stat -> refreshDurations.get(stat.indexName) >= refreshThreshold)
				.mapToLong(stat -> stat.mailboxes.size() - newShard.boxCount) //
				.sum();

		AtomicInteger remainingBoxCount = new AtomicInteger(newShard.boxCount);
		AtomicInteger remainingIndexCount = new AtomicInteger(aboveTargetBoxCount.size());
		return aboveTargetBoxCount.stream()
				.sorted((stat1, stat2) -> refreshDurations.get(stat2.indexName)
						.compareTo(refreshDurations.get(stat1.indexName)))
				.collect(Collectors.toMap(stat -> stat, stat -> {
					int boxContributed;
					if (refreshDurations.get(stat.indexName) >= refreshThreshold) {
						int max = Math.max(1, Math.round(newShard.boxCount
								* ((stat.mailboxes.size() - newShard.boxCount) / (float) aboveThresholdBoxCount)));
						boxContributed = stat.mailboxes.size() - max < newShard.boxCount //
								? stat.mailboxes.size() - newShard.boxCount
								: max;
					} else {
						boxContributed = remainingBoxCount.get() / remainingIndexCount.get();
					}
					boxContributed = remainingBoxCount.get() < 0 ? 0 : Math.max(0, boxContributed);
					remainingBoxCount.addAndGet(-boxContributed);
					remainingIndexCount.decrementAndGet();
					return boxContributed;
				}));
	}

}