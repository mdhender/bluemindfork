package net.bluemind.lib.elasticsearch.allocations;

import java.util.List;

public interface AllocationSpecification<T> {

	T apply(List<AllocationShardStats> existing);
}