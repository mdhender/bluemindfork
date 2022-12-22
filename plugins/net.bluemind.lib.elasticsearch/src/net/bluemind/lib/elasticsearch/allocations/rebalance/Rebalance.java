package net.bluemind.lib.elasticsearch.allocations.rebalance;

import java.util.List;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;

public class Rebalance {
	public final int averageBoxCount;
	public final List<AllocationShardStats> sources;
	public final List<AllocationShardStats> targets;

	public Rebalance(int averageBoxCount, List<AllocationShardStats> sources, List<AllocationShardStats> targets) {
		this.averageBoxCount = averageBoxCount;
		this.sources = sources;
		this.targets = targets;
	}
}