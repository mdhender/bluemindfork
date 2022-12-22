package net.bluemind.lib.elasticsearch.allocations;

import java.util.Map;

public interface AllocatorSourcesCountStrategy<T> {

	Map<AllocationShardStats, Integer> apply(T rebalanceSpec);

}
