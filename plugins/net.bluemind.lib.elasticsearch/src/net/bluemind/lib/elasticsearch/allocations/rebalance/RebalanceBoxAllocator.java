package net.bluemind.lib.elasticsearch.allocations.rebalance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.BoxAllocation;
import net.bluemind.lib.elasticsearch.allocations.BoxAllocator;

public class RebalanceBoxAllocator implements BoxAllocator<Rebalance> {

	public List<BoxAllocation> apply(Rebalance rebalance, Map<AllocationShardStats, Integer> sourcesCount) {
		List<BoxAllocation> allocations = new ArrayList<>();
		int targetIndex = 0;
		int targetAddedBoxCount = 0;
		Iterator<AllocationShardStats> sourceIterator = rebalance.sources.iterator();
		while (sourceIterator.hasNext() && targetIndex < rebalance.targets.size()) {
			AllocationShardStats source = sourceIterator.next();
			int movedCount = 0;
			Iterator<String> remaining = source.mailboxes.iterator();
			while (remaining.hasNext() && targetIndex < rebalance.targets.size()
					&& movedCount < sourcesCount.get(source)) {
				String mbox = remaining.next();
				AllocationShardStats target = rebalance.targets.get(targetIndex);
				allocations.add(new BoxAllocation(source.indexName, target.indexName, mbox));
				movedCount++;
				targetAddedBoxCount++;
				if (target.mailboxes.size() + targetAddedBoxCount >= rebalance.averageBoxCount) {
					targetIndex++;
					targetAddedBoxCount = 0;
				}
			}
		}
		return allocations;
	}
}
