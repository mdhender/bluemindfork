package net.bluemind.lib.elasticsearch.allocations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface AllocatorSourcesCountStrategy<T> {

	public class BoxesCount {
		public List<String> boxes;
		public Long count;

		public BoxesCount(List<String> boxes, Long count) {
			this.boxes = boxes;
			this.count = count;
		}
	}

	Map<AllocationShardStats, BoxesCount> apply(T rebalanceSpec);

	default BoxesCount boxesCount(AllocationShardStats stat, long docContributed) {
		int boxCount = 0;
		long addedDocCount = 0l;
		List<String> boxes = new ArrayList<>();
		while (boxCount < stat.mailboxesCount.size()) {
			if (addedDocCount + stat.mailboxesCount.get(boxCount).docCount <= docContributed) {
				addedDocCount += stat.mailboxesCount.get(boxCount).docCount;
				boxes.add(stat.mailboxesCount.get(boxCount).name);
			}
			boxCount++;
		}
		return new BoxesCount(boxes, addedDocCount);
	}

}
