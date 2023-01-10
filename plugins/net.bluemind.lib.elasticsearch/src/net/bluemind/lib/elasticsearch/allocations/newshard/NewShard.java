package net.bluemind.lib.elasticsearch.allocations.newshard;

import java.util.List;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;

public class NewShard {
	public final List<AllocationShardStats> sources;
	public final String indexName;
	public final long docCount;

	public NewShard(List<AllocationShardStats> sources, String indexName, long docCount) {
		this.sources = sources;
		this.indexName = indexName;
		this.docCount = docCount;
	}
}