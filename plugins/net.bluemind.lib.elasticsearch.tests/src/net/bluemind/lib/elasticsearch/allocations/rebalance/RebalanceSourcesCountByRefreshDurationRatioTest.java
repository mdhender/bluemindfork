package net.bluemind.lib.elasticsearch.allocations.rebalance;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats.MailboxCount;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy.BoxesCount;

public class RebalanceSourcesCountByRefreshDurationRatioTest {

	private int indexCount = 21;

	@Test
	public void testJustEnoughSpace() {
		List<AllocationShardStats> existing = shardStats(indexCount, 10, 1000l);
		List<AllocationShardStats> sources = existing.subList(16, 21);
		List<AllocationShardStats> targets = existing.subList(0, 8);
		long averageDocCount = Math.round(existing.stream().mapToLong(stat -> stat.docCount).sum() / existing.size());
		long availableDocCount = targets.stream().mapToLong(target -> averageDocCount - target.docCount).sum();
		long sourceDocCount = sources.stream().mapToLong(source -> source.docCount - averageDocCount).sum();
		assertThat((double) sourceDocCount).isWithin(0.05 * availableDocCount).of(availableDocCount);

		Rebalance rebalance = new Rebalance(averageDocCount, sources, targets);
		Map<AllocationShardStats, BoxesCount> sourcesBoxes = new RebalanceSourcesCountByRefreshDurationRatio()
				.apply(rebalance);

		long totalDocContributed = sourcesBoxes.values().stream().mapToLong(sourceBoxes -> sourceBoxes.count).sum();
		System.out.println("doc: " + totalDocContributed + "/" + availableDocCount + "(" + sourceDocCount + ")");
		assertThat((double) totalDocContributed).isWithin(0.05 * sourceDocCount).of(sourceDocCount);
		sources.stream() //
				.map(source -> {
					assertThat(sourcesBoxes.get(source).count).isAtMost(source.docCount - averageDocCount);
					return source;
				}).reduce((source1, source2) -> {
					assertThat(sourcesBoxes.get(source1).count).isAtMost(sourcesBoxes.get(source2).count);
					return source2;
				});
	}

	@Test
	public void testNotEnoughSpace() {
		List<AllocationShardStats> existing = shardStats(indexCount, 10, 1000l);
		List<AllocationShardStats> sources = existing.subList(16, 21);
		List<AllocationShardStats> targets = existing.subList(0, 2);
		long averageDocCount = Math.round(existing.stream().mapToLong(stat -> stat.docCount).sum() / existing.size());
		long availableDocCount = targets.stream().mapToLong(target -> averageDocCount - target.docCount).sum();
		long sourceDocCount = sources.stream().mapToLong(source -> source.docCount - averageDocCount).sum();
		assertThat(sourceDocCount).isAtLeast(availableDocCount);

		Rebalance rebalance = new Rebalance(averageDocCount, sources, targets);
		Map<AllocationShardStats, BoxesCount> sourcesBoxes = new RebalanceSourcesCountByRefreshDurationRatio()
				.apply(rebalance);

		long totalDocContributed = sourcesBoxes.values().stream().mapToLong(sourceBoxes -> sourceBoxes.count).sum();
		System.out.println("doc: " + totalDocContributed + "/" + availableDocCount + "(" + sourceDocCount + ")");
		assertThat((double) totalDocContributed).isWithin(0.05 * availableDocCount).of(availableDocCount);
		sources.stream() //
				.map(source -> {
					assertThat(sourcesBoxes.get(source).count).isAtMost(source.docCount - averageDocCount);
					return source;
				}).reduce((source1, source2) -> {
					assertThat(sourcesBoxes.get(source1).count).isAtMost(sourcesBoxes.get(source2).count);
					return source2;
				});
	}

	@Test
	public void testTooMuchSpace() {
		List<AllocationShardStats> existing = shardStats(indexCount, 10, 1000l);
		List<AllocationShardStats> sources = existing.subList(19, 21);
		List<AllocationShardStats> targets = existing.subList(0, 5);
		long averageDocCount = Math.round(existing.stream().mapToLong(stat -> stat.docCount).sum() / existing.size());
		long availableDocCount = targets.stream().mapToLong(target -> averageDocCount - target.docCount).sum();
		long sourceDocCount = sources.stream().mapToLong(source -> source.docCount - averageDocCount).sum();
		assertThat(sourceDocCount).isAtMost(availableDocCount);

		Rebalance rebalance = new Rebalance(averageDocCount, sources, targets);
		Map<AllocationShardStats, BoxesCount> sourcesBoxes = new RebalanceSourcesCountByRefreshDurationRatio()
				.apply(rebalance);

		long totalDocContributed = sourcesBoxes.values().stream().mapToLong(sourceBoxes -> sourceBoxes.count).sum();
		System.out.println("doc: " + totalDocContributed + "/" + availableDocCount + "(" + sourceDocCount + ")");
		assertThat((double) totalDocContributed).isWithin(0.05 * sourceDocCount).of(sourceDocCount);
		sources.stream() //
				.map(source -> {
					assertThat(sourcesBoxes.get(source).count).isAtMost(source.docCount - averageDocCount);
					return source;
				}).reduce((source1, source2) -> {
					assertThat(sourcesBoxes.get(source1).count).isAtMost(sourcesBoxes.get(source2).count);
					return source2;
				});
	}

	private List<AllocationShardStats> shardStats(int indexCount, int baseMailboxCount, long baseDocCount) {
		List<AllocationShardStats> existing = new ArrayList<>();
		for (int i = 0; i < indexCount; i++) {
			AllocationShardStats stat = new AllocationShardStats();
			stat.indexName = "" + i;
			int mailboxCount = baseMailboxCount * (i + 1);
			stat.mailboxes = new HashSet<>();
			stat.docCount = mailboxCount * (mailboxCount * (baseDocCount / 2) + baseDocCount);
			stat.mailboxesCount = new ArrayList<>();
			for (int j = 0; j < mailboxCount; j++) {
				stat.mailboxes.add("alias_" + i + j);
				stat.mailboxesCount.add(new MailboxCount("alias_" + i + j, baseDocCount * (j + 1)));
			}
			existing.add(stat);
		}
		return existing;
	}
}
