package net.bluemind.lib.elasticsearch.allocations.rebalance;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocationSpecification;

public class RebalanceSpecificationByThreshold implements AllocationSpecification<Rebalance> {
	private final Map<String, Long> refreshDurations;
	private final long refreshDurationLowThreshold;
	private final long refreshDurationHighThreshold;

	public RebalanceSpecificationByThreshold(Map<String, Long> refreshDurations, long refreshDurationLowThreshold,
			long refreshDurationHighThreshold) {
		this.refreshDurations = refreshDurations;
		this.refreshDurationLowThreshold = refreshDurationLowThreshold;
		this.refreshDurationHighThreshold = refreshDurationHighThreshold;
	}

	@Override
	public Rebalance apply(List<AllocationShardStats> existing) {
		long averageDocCount = Math.round(existing.stream().mapToLong(stat -> stat.docCount).average().orElse(0d));

		List<AllocationShardStats> sources = existing.stream() //
				.filter(stat -> refreshDurations.get(stat.indexName) >= refreshDurationHighThreshold)
				.filter(stat -> stat.docCount > averageDocCount) //
				.sorted((stat1, stat2) -> refreshDurations.get(stat2.indexName)
						.compareTo(refreshDurations.get(stat1.indexName)))
				.collect(Collectors.toList());

		List<AllocationShardStats> targets = existing.stream() //
				.filter(stat -> refreshDurations.get(stat.indexName) > 0)
				.filter(stat -> refreshDurations.get(stat.indexName) <= refreshDurationLowThreshold)
				.filter(stat -> stat.docCount < averageDocCount) //
				.sorted((stat1, stat2) -> refreshDurations.get(stat1.indexName)
						.compareTo(refreshDurations.get(stat2.indexName)))
				.collect(Collectors.toList());

		return new Rebalance(averageDocCount, sources, targets);
	}
}
