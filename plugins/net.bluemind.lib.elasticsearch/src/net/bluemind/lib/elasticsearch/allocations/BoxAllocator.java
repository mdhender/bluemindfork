package net.bluemind.lib.elasticsearch.allocations;

import java.util.List;
import java.util.Map;

import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy.BoxesCount;

public interface BoxAllocator<T> {
	List<BoxAllocation> apply(T rebalance, Map<AllocationShardStats, BoxesCount> sourcesBoxes);
}
