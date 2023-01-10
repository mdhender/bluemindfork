package net.bluemind.lib.elasticsearch.allocations;

import java.util.List;
import java.util.Set;

public class AllocationShardStats {

	public static class MailboxCount {
		public String name;
		public long docCount;

		public MailboxCount(String name, long docCount) {
			this.name = name;
			this.docCount = docCount;
		}
	}

	public String indexName;

	public long docCount;

	public long deletedCount;

	public long externalRefreshCount;

	public long externalRefreshDuration;

	public long size;

	public Set<String> mailboxes;

	public List<MailboxCount> mailboxesCount;

	public AllocationShardStats() {

	}

	public AllocationShardStats(String indexName, long docCount, long deletedCount, long externalRefreshCount,
			long externalRefreshDuration, long size, Set<String> mailboxes, List<MailboxCount> mailboxesCount) {
		this.indexName = indexName;
		this.docCount = docCount;
		this.deletedCount = deletedCount;
		this.externalRefreshCount = externalRefreshCount;
		this.externalRefreshDuration = externalRefreshDuration;
		this.size = size;
		this.mailboxes = mailboxes;
		this.mailboxesCount = mailboxesCount;
	}
}
