package net.bluemind.lib.elasticsearch.allocations.rebalance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy;
import net.bluemind.lib.elasticsearch.allocations.BoxAllocation;
import net.bluemind.lib.elasticsearch.allocations.BoxAllocator;

public class RebalanceBoxAllocatorTest {

	private static final Logger logger = LoggerFactory.getLogger(RebalanceBoxAllocatorTest.class);

	private int indexCount = 21;
	private long minRefresh = 200l;
	private int refreshStep = 20;

	@Test
	public void testJustEnoughSpace() {
		Map<String, Long> refreshDurations = refreshDurations(indexCount, i -> minRefresh + (refreshStep * i));
		List<AllocationShardStats> existing = shardStats(indexCount);
		List<AllocationShardStats> sources = existing.subList(16, 21);
		List<AllocationShardStats> targets = existing.subList(0, 5);
		int averageBoxCount = existing.get(indexCount / 2).mailboxes.size();
		int available = targets.stream().mapToInt(target -> averageBoxCount - target.mailboxes.size()).sum();
		Rebalance rebalance = new Rebalance(averageBoxCount, sources, targets);

		AllocatorSourcesCountStrategy<Rebalance> strategy = new RebalanceSourcesCountByRefreshDurationRatio(
				refreshDurations);
		Map<AllocationShardStats, Integer> sourcesCount = strategy.apply(rebalance);
		sourcesCount.forEach((source, count) -> {
			logger.info("source:{}, count:{}", source, count);
		});

		BoxAllocator<Rebalance> rebalanceAllocator = new RebalanceBoxAllocator();
		List<BoxAllocation> allocations = rebalanceAllocator.apply(rebalance, sourcesCount);

		assertEquals(available, allocations.size());
		Set<String> allocatedMbox = allocations.stream().map(allocation -> allocation.mbox).collect(Collectors.toSet());
		assertEquals(allocatedMbox.size(), allocations.size());
		Set<String> sourcesIndex = rebalance.sources.stream().map(source -> source.indexName)
				.collect(Collectors.toSet());
		Set<String> targetsIndex = rebalance.targets.stream().map(target -> target.indexName)
				.collect(Collectors.toSet());
		allocations.forEach(allocation -> {
			assertTrue(sourcesIndex.contains(allocation.sourceIndex));
			assertTrue(targetsIndex.contains(allocation.targetIndex));
		});
	}

	@Test
	public void testNotEnoughSpace() {
		Map<String, Long> refreshDurations = refreshDurations(indexCount, i -> minRefresh + (refreshStep * i));
		List<AllocationShardStats> existing = shardStats(indexCount);
		List<AllocationShardStats> sources = existing.subList(16, 21);
		List<AllocationShardStats> targets = existing.subList(0, 2);
		int averageBoxCount = existing.get(indexCount / 2).mailboxes.size();
		int available = targets.stream().mapToInt(target -> averageBoxCount - target.mailboxes.size()).sum();
		Rebalance rebalance = new Rebalance(averageBoxCount, sources, targets);
		AllocatorSourcesCountStrategy<Rebalance> strategy = new RebalanceSourcesCountByRefreshDurationRatio(
				refreshDurations);
		Map<AllocationShardStats, Integer> sourcesCount = strategy.apply(rebalance);
		sourcesCount.forEach((source, count) -> {
			logger.info("source:{}, count:{}", source, count);
		});

		BoxAllocator<Rebalance> rebalanceAllocator = new RebalanceBoxAllocator();
		List<BoxAllocation> allocations = rebalanceAllocator.apply(rebalance, sourcesCount);

		assertEquals(available, allocations.size());
		Set<String> allocatedMbox = allocations.stream().map(allocation -> allocation.mbox).collect(Collectors.toSet());
		assertEquals(allocatedMbox.size(), allocations.size());
		Set<String> sourcesIndex = rebalance.sources.stream().map(source -> source.indexName)
				.collect(Collectors.toSet());
		Set<String> targetsIndex = rebalance.targets.stream().map(target -> target.indexName)
				.collect(Collectors.toSet());
		allocations.forEach(allocation -> {
			assertTrue(sourcesIndex.contains(allocation.sourceIndex));
			assertTrue(targetsIndex.contains(allocation.targetIndex));
		});
	}

	@Test
	public void testTooMuchSpace() {
		Map<String, Long> refreshDurations = refreshDurations(indexCount, i -> minRefresh + (refreshStep * i));
		List<AllocationShardStats> existing = shardStats(indexCount);
		List<AllocationShardStats> sources = existing.subList(19, 21);
		List<AllocationShardStats> targets = existing.subList(0, 5);
		int averageBoxCount = existing.get(indexCount / 2).mailboxes.size();
		int available = targets.stream().mapToInt(target -> averageBoxCount - target.mailboxes.size()).sum();
		Rebalance rebalance = new Rebalance(averageBoxCount, sources, targets);

		AllocatorSourcesCountStrategy<Rebalance> strategy = new RebalanceSourcesCountByRefreshDurationRatio(
				refreshDurations);
		Map<AllocationShardStats, Integer> sourcesCount = strategy.apply(rebalance);
		sourcesCount.forEach((source, count) -> {
			logger.info("source:{}, count:{}", source, count);
		});

		BoxAllocator<Rebalance> rebalanceAllocator = new RebalanceBoxAllocator();
		List<BoxAllocation> allocations = rebalanceAllocator.apply(rebalance, sourcesCount);

		assertEquals((19 - 10) * 10l + (20 - 10) * 10l, allocations.size());
		Set<String> allocatedMbox = allocations.stream().map(allocation -> allocation.mbox).collect(Collectors.toSet());
		assertEquals(allocatedMbox.size(), allocations.size());
		Set<String> sourcesIndex = rebalance.sources.stream().map(source -> source.indexName)
				.collect(Collectors.toSet());
		Set<String> targetsIndex = rebalance.targets.stream().map(target -> target.indexName)
				.collect(Collectors.toSet());
		allocations.forEach(allocation -> {
			assertTrue(sourcesIndex.contains(allocation.sourceIndex));
			assertTrue(targetsIndex.contains(allocation.targetIndex));
		});
	}

	private Map<String, Long> refreshDurations(int indexCount, Function<Integer, Long> consumer) {
		Map<String, Long> refreshDurations = new HashMap<>();
		for (int i = 0; i < indexCount; i++) {
			long duration = consumer.apply(i);
			refreshDurations.put("" + i, duration);
		}
		return refreshDurations;
	}

	private List<AllocationShardStats> shardStats(int indexCount) {
		List<AllocationShardStats> existing = new ArrayList<>();
		for (int i = 0; i < indexCount; i++) {
			AllocationShardStats stat = new AllocationShardStats();
			stat.indexName = "" + i;
			int mailboxCount = 10 + (10 * i);
			stat.mailboxes = new HashSet<>();
			for (int j = 0; j < mailboxCount; j++) {
				stat.mailboxes.add("alias_" + i + j);
			}
			existing.add(stat);
		}
		return existing;
	}
}
