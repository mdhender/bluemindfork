package net.bluemind.lib.elasticsearch.allocations.rebalance;

import java.util.HashMap;
import java.util.Map;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy;

public class RebalanceSourcesCountByRefreshDurationRatio implements AllocatorSourcesCountStrategy<Rebalance> {

	@Override
	public Map<AllocationShardStats, BoxesCount> apply(Rebalance rebalanceSpec) {
		long availableDocCount = rebalanceSpec.targets.stream()
				.mapToLong(target -> rebalanceSpec.averageDocCount - target.docCount).sum();
		long sourcesDocCount = rebalanceSpec.sources.stream()
				.mapToLong(source -> source.docCount - rebalanceSpec.averageDocCount).sum();

		Map<AllocationShardStats, BoxesCount> sourcesBoxes = new HashMap<>();
		for (int i = 0; i < rebalanceSpec.sources.size(); i++) {
			AllocationShardStats source = rebalanceSpec.sources.get(i);
			float ratio = (source.docCount - rebalanceSpec.averageDocCount) / (float) sourcesDocCount;
			long maxdDocContributed = Math.min(source.docCount - rebalanceSpec.averageDocCount,
					Math.round(availableDocCount * ratio));
			BoxesCount boxes = boxesCount(source, maxdDocContributed);
			sourcesBoxes.put(source, boxes);
			availableDocCount -= boxes.count;
			sourcesDocCount -= (source.docCount - rebalanceSpec.averageDocCount);
		}
		return sourcesBoxes;
	}
}
