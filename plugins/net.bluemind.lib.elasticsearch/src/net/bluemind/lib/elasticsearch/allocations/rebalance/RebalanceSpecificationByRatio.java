package net.bluemind.lib.elasticsearch.allocations.rebalance;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocationSpecification;

public class RebalanceSpecificationByRatio implements AllocationSpecification<Rebalance> {
	private final Map<String, Long> refreshDurations;
	private final double refreshDurationLowRatio;
	private final double refreshDurationHighRatio;

	public RebalanceSpecificationByRatio(Map<String, Long> refreshDurations, double refreshDurationLowRatio,
			double refreshDurationHighRatio) {
		this.refreshDurations = refreshDurations;
		this.refreshDurationLowRatio = refreshDurationLowRatio;
		this.refreshDurationHighRatio = refreshDurationHighRatio;
	}

	@Override
	public Rebalance apply(List<AllocationShardStats> existing) {
		long averageDocCount = Math.round(existing.stream().mapToLong(stat -> stat.docCount).average().orElse(0d));
		long averageRefreshDuration = (long) existing.stream() //
				.mapToLong(stat -> refreshDurations.get(stat.indexName)) //
				.average().orElse(0);

		double lowRefreshDuration = averageRefreshDuration * (1 - refreshDurationLowRatio);
		double highRefreshDuration = averageRefreshDuration * (1 + refreshDurationHighRatio);

		List<AllocationShardStats> sources = existing.stream() //
				.filter(stat -> refreshDurations.get(stat.indexName) >= highRefreshDuration)
				.filter(stat -> stat.docCount > averageDocCount) //
				.sorted((stat1, stat2) -> refreshDurations.get(stat2.indexName)
						.compareTo(refreshDurations.get(stat1.indexName)))
				.collect(Collectors.toList());

		List<AllocationShardStats> targets = existing.stream() //
				.filter(stat -> refreshDurations.get(stat.indexName) > 0)
				.filter(stat -> refreshDurations.get(stat.indexName) <= lowRefreshDuration)
				.filter(stat -> stat.docCount < averageDocCount) //
				.sorted((stat1, stat2) -> refreshDurations.get(stat1.indexName)
						.compareTo(refreshDurations.get(stat2.indexName)))
				.collect(Collectors.toList());

		return new Rebalance(averageDocCount, sources, targets);
	}
}
