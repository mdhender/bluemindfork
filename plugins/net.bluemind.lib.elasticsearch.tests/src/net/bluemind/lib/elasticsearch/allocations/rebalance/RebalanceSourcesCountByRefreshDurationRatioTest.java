package net.bluemind.lib.elasticsearch.allocations.rebalance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy;

public class RebalanceSourcesCountByRefreshDurationRatioTest {

	private static final Logger logger = LoggerFactory.getLogger(RebalanceSourcesCountByRefreshDurationRatioTest.class);

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

		AllocatorSourcesCountStrategy<Rebalance> startegy = new RebalanceSourcesCountByRefreshDurationRatio(
				refreshDurations);
		Map<AllocationShardStats, Integer> sourcesCount = startegy.apply(rebalance);

		assertEquals(sourcesCount.size(), sources.size());
		assertTrue(sourcesCount.values().stream().mapToInt(x -> x).sum() <= available);
		assertTrue(sourcesCount.values().stream().mapToInt(x -> x).sum() >= available - 1);
		sources.stream() //
				.map(source -> {
					assertTrue(sourcesCount.get(source) <= source.mailboxes.size() - averageBoxCount);
					return source;
				}).reduce((source1, source2) -> {
					assertTrue(sourcesCount.get(source1) < sourcesCount.get(source2));
					return source2;
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
		AllocatorSourcesCountStrategy<Rebalance> startegy = new RebalanceSourcesCountByRefreshDurationRatio(
				refreshDurations);
		Map<AllocationShardStats, Integer> sourcesCount = startegy.apply(rebalance);

		assertEquals(sourcesCount.size(), sources.size());
		assertTrue(sourcesCount.values().stream().mapToInt(x -> x).sum() <= available);
		assertTrue(sourcesCount.values().stream().mapToInt(x -> x).sum() >= available - 1);
		sources.stream() //
				.map(source -> {
					assertTrue(sourcesCount.get(source) <= source.mailboxes.size() - averageBoxCount);
					return source;
				}).reduce((source1, source2) -> {
					assertTrue(sourcesCount.get(source1) < sourcesCount.get(source2));
					return source2;
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

		AllocatorSourcesCountStrategy<Rebalance> startegy = new RebalanceSourcesCountByRefreshDurationRatio(
				refreshDurations);
		Map<AllocationShardStats, Integer> sourcesCount = startegy.apply(rebalance);

		assertEquals(sourcesCount.size(), sources.size());
		assertTrue(sourcesCount.values().stream().mapToInt(x -> x).sum() <= available);
		assertEquals(sourcesCount.values().stream().mapToInt(x -> x).sum(), (19 - 10) * 10l + (20 - 10) * 10l);
		sources.stream() //
				.map(source -> {
					assertTrue(sourcesCount.get(source) <= source.mailboxes.size() - averageBoxCount);
					return source;
				}).reduce((source1, source2) -> {
					assertTrue(sourcesCount.get(source1) < sourcesCount.get(source2));
					return source2;
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
