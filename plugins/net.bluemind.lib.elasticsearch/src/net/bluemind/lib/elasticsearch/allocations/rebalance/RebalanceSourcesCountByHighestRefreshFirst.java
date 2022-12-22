package net.bluemind.lib.elasticsearch.allocations.rebalance;

import static java.util.stream.Collectors.toMap;

import java.util.Map;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy;

public class RebalanceSourcesCountByHighestRefreshFirst implements AllocatorSourcesCountStrategy<Rebalance> {

	@Override
	public Map<AllocationShardStats, Integer> apply(Rebalance rebalanceSpec) {
		return rebalanceSpec.sources.stream() //
				.collect(toMap(source -> source, source -> source.mailboxes.size() - rebalanceSpec.averageBoxCount));
	}
}
