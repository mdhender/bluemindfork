package net.bluemind.lib.elasticsearch.allocations.rebalance;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocationSpecification;

public class RebalanceSpecificationByRatio implements AllocationSpecification<Rebalance> {
	private final Map<String, Long> refreshDurations;
	private final double refreshDurationLowRatio;
	private final double refreshDurationHighRatio;
	private static final Logger logger = LoggerFactory.getLogger(RebalanceSpecificationByRatio.class);

	public RebalanceSpecificationByRatio(Map<String, Long> refreshDurations, double refreshDurationLowRatio,
			double refreshDurationHighRatio) {
		this.refreshDurations = refreshDurations;
		this.refreshDurationLowRatio = refreshDurationLowRatio;
		this.refreshDurationHighRatio = refreshDurationHighRatio;
	}

	@Override
	public Rebalance apply(List<AllocationShardStats> existing) {
		int averageBoxCount = (int) existing.stream().mapToInt(stat -> stat.mailboxes.size()).average().orElse(0);
		long averageRefreshDuration = (long) existing.stream() //
				.mapToLong(stat -> refreshDurations.get(stat.indexName)) //
				.average().orElse(0);
		logger.info("averageRefreshDuration:{} averageBoxCount:{}", averageRefreshDuration, averageBoxCount);

		double lowRefreshDuration = averageRefreshDuration * (1 - refreshDurationLowRatio);
		double highRefreshDuration = averageRefreshDuration * (1 + refreshDurationHighRatio);
		logger.info("lowRefreshDuration:{} highRefreshDuration:{}", lowRefreshDuration, highRefreshDuration);

		List<AllocationShardStats> sources = existing.stream() //
				.filter(stat -> stat.mailboxes.size() > averageBoxCount)
				.filter(stat -> refreshDurations.get(stat.indexName) >= highRefreshDuration)
				.sorted((stat1, stat2) -> refreshDurations.get(stat2.indexName)
						.compareTo(refreshDurations.get(stat1.indexName)))
				.collect(Collectors.toList());

		List<AllocationShardStats> targets = existing.stream() //
				.filter(stat -> stat.mailboxes.size() < averageBoxCount)
				.filter(stat -> refreshDurations.get(stat.indexName) > 0)
				.filter(stat -> refreshDurations.get(stat.indexName) <= lowRefreshDuration)
				.sorted((stat1, stat2) -> refreshDurations.get(stat1.indexName)
						.compareTo(refreshDurations.get(stat2.indexName)))
				.collect(Collectors.toList());

		return new Rebalance(averageBoxCount, sources, targets);
	}
}
