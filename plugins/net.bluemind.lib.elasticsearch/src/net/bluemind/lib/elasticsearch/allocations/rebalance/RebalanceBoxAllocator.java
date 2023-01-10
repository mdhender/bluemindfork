package net.bluemind.lib.elasticsearch.allocations.rebalance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy.BoxesCount;
import net.bluemind.lib.elasticsearch.allocations.BoxAllocation;
import net.bluemind.lib.elasticsearch.allocations.BoxAllocator;

public class RebalanceBoxAllocator implements BoxAllocator<Rebalance> {

	public List<BoxAllocation> apply(Rebalance rebalance, Map<AllocationShardStats, BoxesCount> sourcesBoxes) {
		List<BoxAllocation> allocations = new ArrayList<>();
		int targetIndex = 0;
		long targetAddedDocCount = 0;
		Iterator<AllocationShardStats> sourceIterator = rebalance.sources.iterator();
		while (sourceIterator.hasNext() && targetIndex < rebalance.targets.size()) {
			AllocationShardStats source = sourceIterator.next();
			BoxesCount boxesCount = sourcesBoxes.get(source);
			Map<String, Long> boxCount = source.mailboxesCount.stream()
					.collect(Collectors.toMap(box -> box.name, box -> box.docCount));
			Iterator<String> mboxIterator = boxesCount.boxes.iterator();
			while (mboxIterator.hasNext() && targetIndex < rebalance.targets.size()) {
				String mbox = mboxIterator.next();
				AllocationShardStats target = rebalance.targets.get(targetIndex);
				allocations.add(new BoxAllocation(source.indexName, target.indexName, mbox));
				targetAddedDocCount += boxCount.get(mbox);
				if (target.docCount + targetAddedDocCount >= rebalance.averageDocCount) {
					targetIndex++;
					targetAddedDocCount = 0;
				}
			}
		}
		return allocations;
	}
}
