package net.bluemind.lib.elasticsearch.allocations.rebalance;

public class RebalanceConfig {

	final double refreshDurationLowRatio;
	public final double refreshDurationHighRatio;
	public final long refreshDurationLowThreshold;
	public final long refreshDurationHighThreshold;

	public RebalanceConfig(double refreshDurationLowRatio, double refreshDurationHighRatio,
			long refreshDurationLowThreshold, long refreshDurationHighThreshold) {
		this.refreshDurationLowRatio = refreshDurationLowRatio;
		this.refreshDurationHighRatio = refreshDurationHighRatio;
		this.refreshDurationLowThreshold = refreshDurationLowThreshold;
		this.refreshDurationHighThreshold = refreshDurationHighThreshold;
	}
}
