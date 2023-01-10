package net.bluemind.lib.elasticsearch.allocations.rebalance;

import java.util.List;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;

public class Rebalance {
	public final long averageDocCount;
	public final List<AllocationShardStats> sources;
	public final List<AllocationShardStats> targets;

	public Rebalance(long averageDocCount, List<AllocationShardStats> sources, List<AllocationShardStats> targets) {
		this.averageDocCount = averageDocCount;
		this.sources = sources;
		this.targets = targets;
	}
}