package net.bluemind.lib.elasticsearch.allocations.newshard;

import java.util.List;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;

public class NewShard {
	public final List<AllocationShardStats> sources;
	public final String indexName;
	public final int boxCount;

	public NewShard(List<AllocationShardStats> sources, String indexName, int boxCount) {
		this.sources = sources;
		this.indexName = indexName;
		this.boxCount = boxCount;
	}
}