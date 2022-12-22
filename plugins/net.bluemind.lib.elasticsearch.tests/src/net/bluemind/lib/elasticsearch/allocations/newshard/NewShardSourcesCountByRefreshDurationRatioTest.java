package net.bluemind.lib.elasticsearch.allocations.newshard;

import static java.lang.Long.compare;
import static java.lang.Long.parseLong;
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

public class NewShardSourcesCountByRefreshDurationRatioTest {

	private static final Logger logger = LoggerFactory.getLogger(NewShardSourcesCountByRefreshDurationRatioTest.class);

	private int indexCount = 21;
	private long minRefresh = 400l;
	private int refreshStep = 20;

	@Test
	public void testOnlyOneAboveThreshold() {
		Map<String, Long> refreshDurations = refreshDurations(indexCount, i -> minRefresh + (refreshStep * i));
		List<AllocationShardStats> existing = shardStats(indexCount);
		int averageBoxCount = existing.get(indexCount / 2).mailboxes.size();
		NewShard newShard = new NewShard(existing, "" + indexCount, averageBoxCount);

		AllocatorSourcesCountStrategy<NewShard> strategy = new NewShardSourcesCountByRefreshDurationRatio(
				refreshDurations, 800l);
		Map<AllocationShardStats, Integer> sourcesCount = strategy.apply(newShard);

		sourcesCount.keySet().stream()
				.forEach(source -> assertTrue(Integer.parseInt(source.indexName) > indexCount / 2));
		assertTrue(sourcesCount.values().stream().mapToInt(x -> x).sum() <= averageBoxCount);
		assertTrue(sourcesCount.values().stream().mapToInt(x -> x).sum() >= averageBoxCount - 1);
		AllocationShardStats aboveThresholdShard = byName(existing, "" + (indexCount - 1));
		int count = sourcesCount.get(aboveThresholdShard);
		assertEquals(aboveThresholdShard.mailboxes.size() - averageBoxCount, count);
	}

	@Test
	public void testEnoughWithFourIndexAboveThreshold() {
		long refreshDurationThreshold = 740l;
		Map<String, Long> refreshDurations = refreshDurations(indexCount, i -> minRefresh + (refreshStep * i));
		List<AllocationShardStats> existing = shardStats(indexCount);
		int averageBoxCount = existing.get(indexCount / 2).mailboxes.size();
		NewShard newShard = new NewShard(existing, "" + indexCount, averageBoxCount);

		AllocatorSourcesCountStrategy<NewShard> strategy = new NewShardSourcesCountByRefreshDurationRatio(
				refreshDurations, refreshDurationThreshold);
		Map<AllocationShardStats, Integer> sourcesCount = strategy.apply(newShard);

		sourcesCount.keySet().stream()
				.forEach(source -> assertTrue(Integer.parseInt(source.indexName) > indexCount / 2));
		assertTrue(sourcesCount.values().stream().mapToInt(x -> x).sum() <= averageBoxCount);
		assertTrue(sourcesCount.values().stream().mapToInt(x -> x).sum() >= averageBoxCount - 1);

		existing.subList(indexCount / 2 + 1, indexCount).stream()
				.sorted((stat1, stat2) -> compare(parseLong(stat2.indexName), parseLong(stat1.indexName)))
				.reduce((stat1, stat2) -> {
					assertTrue(sourcesCount.get(stat1) >= sourcesCount.get(stat2));
					if (refreshDurations.get(stat1.indexName) < refreshDurationThreshold) {
						assertEquals(0, (int) sourcesCount.get(stat1));
					}
					if (refreshDurations.get(stat2.indexName) < refreshDurationThreshold) {
						assertEquals(0, (int) sourcesCount.get(stat2));
					}
					return stat2;
				});
	}

	@Test
	public void testNoneAboveThreshold() {
		long refreshDurationThreshold = 840l;
		Map<String, Long> refreshDurations = refreshDurations(indexCount, i -> minRefresh + (refreshStep * i));
		List<AllocationShardStats> existing = shardStats(indexCount);
		int averageBoxCount = existing.get(indexCount / 2).mailboxes.size();
		NewShard newShard = new NewShard(existing, "" + indexCount, averageBoxCount);

		AllocatorSourcesCountStrategy<NewShard> strategy = new NewShardSourcesCountByRefreshDurationRatio(
				refreshDurations, refreshDurationThreshold);
		Map<AllocationShardStats, Integer> sourcesCount = strategy.apply(newShard);

		sourcesCount.keySet().stream()
				.forEach(source -> assertTrue(Integer.parseInt(source.indexName) > indexCount / 2));
		assertTrue(sourcesCount.values().stream().mapToInt(x -> x).sum() <= averageBoxCount);
		assertTrue(sourcesCount.values().stream().mapToInt(x -> x).sum() >= averageBoxCount - 1);

		existing.subList(indexCount / 2 + 1, indexCount).stream()
				.sorted((stat1, stat2) -> compare(parseLong(stat2.indexName), parseLong(stat1.indexName)))
				.reduce((stat1, stat2) -> {
					assertEquals(sourcesCount.get(stat1), sourcesCount.get(stat2));
					return stat2;
				});
	}

	private AllocationShardStats byName(List<AllocationShardStats> existing, String indexName) {
		return existing.stream().filter(stat -> stat.indexName.equals(indexName)).findAny().get();
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
