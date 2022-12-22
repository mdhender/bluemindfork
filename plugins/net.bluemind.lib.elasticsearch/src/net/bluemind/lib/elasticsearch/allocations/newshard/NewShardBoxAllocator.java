package net.bluemind.lib.elasticsearch.allocations.newshard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.BoxAllocation;
import net.bluemind.lib.elasticsearch.allocations.BoxAllocator;

public class NewShardBoxAllocator implements BoxAllocator<NewShard> {

	public List<BoxAllocation> apply(NewShard newShard, Map<AllocationShardStats, Integer> sourcesCount) {
		List<BoxAllocation> allocations = new ArrayList<>();
		sourcesCount.forEach((source, boxContributed) -> {
			int movedCount = 0;
			Iterator<String> remaining = source.mailboxes.iterator();
			while (remaining.hasNext() && movedCount < boxContributed) {
				String mbox = remaining.next();
				allocations.add(new BoxAllocation(source.indexName, newShard.indexName, mbox));
				movedCount++;
			}
		});
		return allocations;
	}
}
