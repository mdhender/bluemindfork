package net.bluemind.lib.elasticsearch.allocations.newshard;

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
import net.bluemind.mailbox.api.SimpleShardStats;

public class NewShardBoxAllocatorTest {

	private static final Logger logger = LoggerFactory.getLogger(NewShardBoxAllocatorTest.class);

	private int indexCount = 21;
	private long minRefresh = 400l;
	private int refreshStep = 20;

	@Test
	public void testOnlyOneAboveThreshold() {
		Map<String, Long> refreshDurations = refreshDurations(indexCount, i -> minRefresh + (refreshStep * i));
		List<AllocationShardStats> existing = shardStats(indexCount);
		int averageBoxCount = existing.get(indexCount / 2).mailboxes.size();
		NewShard newShard = new NewShard(existing, "" + indexCount, averageBoxCount);

		Map<AllocationShardStats, Integer> sourcesCount = new NewShardSourcesCountByRefreshDurationRatio(
				refreshDurations, 800l).apply(newShard);
		List<BoxAllocation> allocations = new NewShardBoxAllocator().apply(newShard, sourcesCount);

		assertEquals(averageBoxCount, allocations.size());
		Set<String> allocatedMbox = allocations.stream().map(allocation -> allocation.mbox).collect(Collectors.toSet());
		assertEquals(allocatedMbox.size(), allocations.size());
		Set<String> sourcesIndex = sourcesCount.keySet().stream().map(source -> source.indexName)
				.collect(Collectors.toSet());
		allocations.forEach(allocation -> {
			assertTrue(sourcesIndex.contains(allocation.sourceIndex));
			assertEquals(allocation.targetIndex, "" + indexCount);
		});
	}

	@Test
	public void testEnoughWithFourIndexAboveThreshold() {
		long refreshDurationThreshold = 740l;
		Map<String, Long> refreshDurations = refreshDurations(indexCount, i -> minRefresh + (refreshStep * i));
		List<AllocationShardStats> existing = shardStats(indexCount);
		int averageBoxCount = existing.get(indexCount / 2).mailboxes.size();
		NewShard newShard = new NewShard(existing, "" + indexCount, averageBoxCount);

		Map<AllocationShardStats, Integer> sourcesCount = new NewShardSourcesCountByRefreshDurationRatio(
				refreshDurations, refreshDurationThreshold).apply(newShard);
		List<BoxAllocation> allocations = new NewShardBoxAllocator().apply(newShard, sourcesCount);

		assertEquals(averageBoxCount, allocations.size());
		Set<String> allocatedMbox = allocations.stream().map(allocation -> allocation.mbox).collect(Collectors.toSet());
		assertEquals(allocatedMbox.size(), allocations.size());
		Set<String> sourcesIndex = sourcesCount.keySet().stream().map(source -> source.indexName)
				.collect(Collectors.toSet());
		allocations.forEach(allocation -> {
			assertTrue(sourcesIndex.contains(allocation.sourceIndex));
			assertEquals(allocation.targetIndex, "" + indexCount);
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
		List<BoxAllocation> allocations = new NewShardBoxAllocator().apply(newShard, sourcesCount);

		assertEquals(averageBoxCount, allocations.size());
		Set<String> allocatedMbox = allocations.stream().map(allocation -> allocation.mbox).collect(Collectors.toSet());
		assertEquals(allocatedMbox.size(), allocations.size());
		Set<String> sourcesIndex = sourcesCount.keySet().stream().map(source -> source.indexName)
				.collect(Collectors.toSet());
		allocations.forEach(allocation -> {
			assertTrue(sourcesIndex.contains(allocation.sourceIndex));
			assertEquals(allocation.targetIndex, "" + indexCount);
		});
	}

	private SimpleShardStats byName(List<SimpleShardStats> existing, String indexName) {
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
