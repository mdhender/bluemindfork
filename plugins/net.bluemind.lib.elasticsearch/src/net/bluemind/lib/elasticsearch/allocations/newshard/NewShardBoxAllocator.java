package net.bluemind.lib.elasticsearch.allocations.newshard;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy.BoxesCount;
import net.bluemind.lib.elasticsearch.allocations.BoxAllocation;
import net.bluemind.lib.elasticsearch.allocations.BoxAllocator;

public class NewShardBoxAllocator implements BoxAllocator<NewShard> {

	public List<BoxAllocation> apply(NewShard newShard, Map<AllocationShardStats, BoxesCount> sourcesBoxes) {
		return sourcesBoxes.entrySet().stream() //
				.flatMap(entry -> entry.getValue().boxes.stream()
						.map(mbox -> new BoxAllocation(entry.getKey().indexName, newShard.indexName, mbox)))
				.collect(Collectors.toList());
	}
}
