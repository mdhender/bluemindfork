package net.bluemind.lib.elasticsearch.allocations;

import java.util.Set;

public class AllocationShardStats {

	public String indexName;

	public Set<String> mailboxes;

	public long docCount;

	public long deletedCount;

	public long externalRefreshCount;

	public long externalRefreshDuration;

	public long size;

	public AllocationShardStats() {

	}

	public AllocationShardStats(String indexName, Set<String> mailboxes, long docCount, long deletedCount,
			long externalRefreshCount, long externalRefreshDuration, long size) {
		this.indexName = indexName;
		this.mailboxes = mailboxes;
		this.docCount = docCount;
		this.deletedCount = deletedCount;
		this.externalRefreshCount = externalRefreshCount;
		this.externalRefreshDuration = externalRefreshDuration;
		this.size = size;
	}
}
