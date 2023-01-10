package net.bluemind.lib.elasticsearch.allocations.rebalance;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats.MailboxCount;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy.BoxesCount;
import net.bluemind.lib.elasticsearch.allocations.BoxAllocation;

public class RebalanceBoxAllocatorTest {

	private static final Logger logger = LoggerFactory.getLogger(RebalanceBoxAllocatorTest.class);

	private int indexCount = 21;

	@Test
	public void testJustEnoughSpace() {
		List<AllocationShardStats> existing = shardStats(indexCount, 10, 1000l);
		List<AllocationShardStats> sources = existing.subList(16, 21);
		List<AllocationShardStats> targets = existing.subList(0, 5);
		long averageDocCount = Math.round(existing.stream().mapToLong(stat -> stat.docCount).sum() / existing.size());
		long availableDocCount = targets.stream().mapToLong(target -> averageDocCount - target.docCount).sum();

		Rebalance rebalance = new Rebalance(averageDocCount, sources, targets);
		Map<AllocationShardStats, BoxesCount> sourcesCount = new RebalanceSourcesCountByRefreshDurationRatio()
				.apply(rebalance);
		sourcesCount.forEach((source, count) -> {
			logger.info("source:{}, count:{}", source, count);
		});
		List<BoxAllocation> allocations = new RebalanceBoxAllocator().apply(rebalance, sourcesCount);

		long allocatedDocCount = allocations.stream()
				.mapToLong(allocation -> docCount(existing, allocation.sourceIndex, allocation.mbox)).sum();
		assertThat((double) allocatedDocCount).isWithin(0.05 * availableDocCount).of(availableDocCount);
		Set<String> allocatedMbox = allocations.stream().map(allocation -> allocation.mbox).collect(Collectors.toSet());
		assertThat(allocatedMbox).hasSize(allocations.size());
		Set<String> sourcesIndex = rebalance.sources.stream().map(source -> source.indexName)
				.collect(Collectors.toSet());
		Set<String> targetsIndex = rebalance.targets.stream().map(target -> target.indexName)
				.collect(Collectors.toSet());
		allocations.forEach(allocation -> {
			assertThat(sourcesIndex).contains(allocation.sourceIndex);
			assertThat(targetsIndex).contains(allocation.targetIndex);
		});
	}

	@Test
	public void testNotEnoughSpace() {
		List<AllocationShardStats> existing = shardStats(indexCount, 10, 1000l);
		List<AllocationShardStats> sources = existing.subList(16, 21);
		List<AllocationShardStats> targets = existing.subList(0, 2);
		long averageDocCount = Math.round(existing.stream().mapToLong(stat -> stat.docCount).sum() / existing.size());
		long availableDocCount = targets.stream().mapToLong(target -> averageDocCount - target.docCount).sum();

		Rebalance rebalance = new Rebalance(averageDocCount, sources, targets);
		Map<AllocationShardStats, BoxesCount> sourcesCount = new RebalanceSourcesCountByRefreshDurationRatio()
				.apply(rebalance);
		sourcesCount.forEach((source, count) -> {
			logger.info("source:{}, count:{}", source, count);
		});
		List<BoxAllocation> allocations = new RebalanceBoxAllocator().apply(rebalance, sourcesCount);

		long allocatedDocCount = allocations.stream()
				.mapToLong(allocation -> docCount(existing, allocation.sourceIndex, allocation.mbox)).sum();
		assertThat((double) allocatedDocCount).isWithin(0.05 * availableDocCount).of(availableDocCount);
		Set<String> allocatedMbox = allocations.stream().map(allocation -> allocation.mbox).collect(Collectors.toSet());
		assertThat(allocatedMbox).hasSize(allocations.size());
		Set<String> sourcesIndex = rebalance.sources.stream().map(source -> source.indexName)
				.collect(Collectors.toSet());
		Set<String> targetsIndex = rebalance.targets.stream().map(target -> target.indexName)
				.collect(Collectors.toSet());
		allocations.forEach(allocation -> {
			assertThat(sourcesIndex).contains(allocation.sourceIndex);
			assertThat(targetsIndex).contains(allocation.targetIndex);
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

		Rebalance rebalance = new Rebalance(averageDocCount, sources, targets);
		Map<AllocationShardStats, BoxesCount> sourcesCount = new RebalanceSourcesCountByRefreshDurationRatio()
				.apply(rebalance);
		sourcesCount.forEach((source, count) -> {
			logger.info("source:{}, count:{}", source, count);
		});
		List<BoxAllocation> allocations = new RebalanceBoxAllocator().apply(rebalance, sourcesCount);

		long allocatedDocCount = allocations.stream()
				.mapToLong(allocation -> docCount(existing, allocation.sourceIndex, allocation.mbox)).sum();
		assertThat((double) allocatedDocCount).isWithin(0.05 * availableDocCount).of(sourceDocCount);
		Set<String> allocatedMbox = allocations.stream().map(allocation -> allocation.mbox).collect(Collectors.toSet());
		assertThat(allocatedMbox).hasSize(allocations.size());
		Set<String> sourcesIndex = rebalance.sources.stream().map(source -> source.indexName)
				.collect(Collectors.toSet());
		Set<String> targetsIndex = rebalance.targets.stream().map(target -> target.indexName)
				.collect(Collectors.toSet());
		allocations.forEach(allocation -> {
			assertThat(sourcesIndex).contains(allocation.sourceIndex);
			assertThat(targetsIndex).contains(allocation.targetIndex);
		});
	}

	private Long docCount(List<AllocationShardStats> existing, String indexName, String mbox) {
		return docCount(byName(existing, indexName), mbox);
	}

	private Long docCount(AllocationShardStats stat, String mbox) {
		return stat.mailboxesCount.stream().filter(count -> count.name.equals(mbox)).map(count -> count.docCount)
				.findFirst().get();
	}

	private AllocationShardStats byName(List<AllocationShardStats> existing, String indexName) {
		return existing.stream().filter(stat -> stat.indexName.equals(indexName)).findAny().get();
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
