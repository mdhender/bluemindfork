package net.bluemind.lib.elasticsearch.allocations.rebalance;

import java.util.HashMap;
import java.util.Map;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy;

public class RebalanceSourcesCountByRefreshDurationRatio implements AllocatorSourcesCountStrategy<Rebalance> {

	private final Map<String, Long> refreshDurations;

	public RebalanceSourcesCountByRefreshDurationRatio(Map<String, Long> refreshDurations) {
		this.refreshDurations = refreshDurations;
	}

	@Override
	public Map<AllocationShardStats, Integer> apply(Rebalance rebalanceSpec) {
		long availableBoxes = rebalanceSpec.targets.stream()
				.mapToInt(target -> rebalanceSpec.averageBoxCount - target.mailboxes.size()).sum();

		Map<AllocationShardStats, Integer> sourcesCount = new HashMap<>();
		for (int i = 0; i < rebalanceSpec.sources.size(); i++) {
			AllocationShardStats source = rebalanceSpec.sources.get(i);
			long sourcesTotalRefreshDuration = rebalanceSpec.sources.subList(i, rebalanceSpec.sources.size()).stream()
					.mapToLong(s -> refreshDurations.get(s.indexName)).sum();
			float ratio = refreshDurations.get(source.indexName) / (float) sourcesTotalRefreshDuration;
			int max = Math.round(availableBoxes * ratio);
			int count = Math.min(source.mailboxes.size() - rebalanceSpec.averageBoxCount, max);
			availableBoxes -= count;
			sourcesCount.put(source, count);
		}
		return sourcesCount;
	}
}
