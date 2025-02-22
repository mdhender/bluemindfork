package net.bluemind.lib.elasticsearch.allocations.newshard;

import java.util.List;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocationSpecification;

public class NewShardSpecificationByBoxAverage implements AllocationSpecification<NewShard> {

	@Override
	public NewShard apply(List<AllocationShardStats> existing) {
		int startCount = 1;
		int totalDocs = 0;
		for (AllocationShardStats stat : existing) {
			int idxId = Integer.parseInt(stat.indexName.substring("mailspool_".length()));
			startCount = Math.max(startCount, idxId);
			totalDocs += stat.docCount;
		}
		return new NewShard(existing, "mailspool_" + (startCount + 1), totalDocs / (existing.size() + 1));
	}
}