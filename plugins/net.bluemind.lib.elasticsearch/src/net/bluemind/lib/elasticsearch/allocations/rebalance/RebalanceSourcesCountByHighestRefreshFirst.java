package net.bluemind.lib.elasticsearch.allocations.rebalance;

import java.util.HashMap;
import java.util.Map;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy;

public class RebalanceSourcesCountByHighestRefreshFirst implements AllocatorSourcesCountStrategy<Rebalance> {

	@Override
	public Map<AllocationShardStats, BoxesCount> apply(Rebalance rebalanceSpec) {
		long availableDocCount = rebalanceSpec.targets.stream()
				.mapToLong(target -> rebalanceSpec.averageDocCount - target.docCount).sum();
		int i = 0;
		Map<AllocationShardStats, BoxesCount> sourcesBoxes = new HashMap<>();
		while (i < rebalanceSpec.sources.size() && availableDocCount > 0) {
			AllocationShardStats source = rebalanceSpec.sources.get(i);
			long max = Math.min(source.docCount - rebalanceSpec.averageDocCount, availableDocCount);
			BoxesCount boxes = boxesCount(source, max);
			sourcesBoxes.put(source, boxes);
			availableDocCount -= boxes.count;
			i++;
		}
		return sourcesBoxes;
	}
}
