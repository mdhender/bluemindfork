package net.bluemind.lib.elasticsearch.allocations;

import java.util.List;
import java.util.Map;

public interface BoxAllocator<T> {
	List<BoxAllocation> apply(T rebalance, Map<AllocationShardStats, Integer> sourcesCount);
}
