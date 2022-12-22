package net.bluemind.lib.elasticsearch.allocations.rebalance;

import java.util.Map;

import net.bluemind.lib.elasticsearch.allocations.AllocationSpecification;

public class RebalanceSpecificationFactory {

	private final RebalanceConfig config;
	private final Map<String, Long> refreshDurations;

	public RebalanceSpecificationFactory(RebalanceConfig config, Map<String, Long> refreshDurations) {
		this.config = config;
		this.refreshDurations = refreshDurations;
	}

	public AllocationSpecification<Rebalance> instance(String name) {
		switch (name) {
		case "refresh-duration-ratio":
			return new RebalanceSpecificationByRatio(refreshDurations, config.refreshDurationLowRatio,
					config.refreshDurationHighRatio);
		case "refresh-duration-threshold":
		default:
			return new RebalanceSpecificationByThreshold(refreshDurations, config.refreshDurationLowThreshold,
					config.refreshDurationHighThreshold);
		}
	}
}
