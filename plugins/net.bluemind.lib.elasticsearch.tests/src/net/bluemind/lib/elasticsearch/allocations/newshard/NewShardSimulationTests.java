package net.bluemind.lib.elasticsearch.allocations.newshard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.BoxAllocation;
import net.bluemind.lib.elasticsearch.allocations.SimulationStatLoader;

public class NewShardSimulationTests {

	private static final Logger logger = LoggerFactory.getLogger(NewShardSimulationTests.class);

	private final double lowRefreshDurationRatio = 0.2;
	private final double highRefreshDurationRatio = 0.2;

	@Before
	public void setup() {
		System.setProperty("elasticsearch.maintenance.rebalance.refresh-duration-ratio.low",
				"" + lowRefreshDurationRatio);
		System.setProperty("elasticsearch.maintenance.rebalance.refresh-duration-ratio.high",
				"" + highRefreshDurationRatio);
	}

	@Test
	public void testNewShardMarseille() {
		testNewShard("marseille", 800l);
	}

	@Test
	public void testNewShardLaforet() {
		testNewShard("laforet", 800l);
	}

	private void testNewShard(String dataset, long refreshThreshold) {
		List<AllocationShardStats> existing = SimulationStatLoader.load(dataset);
		Map<String, Long> refreshDurations = refreshDurations(existing);

		NewShard newShard = new NewShardSpecificationByBoxAverage().apply(existing);
		Map<AllocationShardStats, Integer> sourcesCount = new NewShardSourcesCountByRefreshDurationRatio(
				refreshDurations, refreshThreshold).apply(newShard);
		List<BoxAllocation> allocations = new NewShardBoxAllocator().apply(newShard, sourcesCount);

		assertNewShard(existing, refreshDurations, allocations);
		allocations.forEach(allocation -> {
			logger.info("mbox:{} source:{} target:{}", allocation.mbox, allocation.sourceIndex, allocation.targetIndex);
		});
	}

	@Test
	public void testNewShardIterationdMarseille() {
		testNewShardIteration("marseille", 800l, 19);
	}

	@Test
	public void testNewShardIterationLaforet() {
		testNewShardIteration("laforet", 800l, 0);
	}

	private void testNewShardIteration(String dataset, long refreshThreshold, int expectedIteration) {
		List<AllocationShardStats> existing = SimulationStatLoader.load(dataset);
		logger.info("shard:{} boxCount:{}", existing.size(),
				existing.stream().mapToInt(stat -> stat.mailboxes.size()).average().orElse(0d));
		boolean iterate = true;
		int iteration = 0;
		while (iterate) {
			Map<String, Long> refreshDurations = refreshDurations(existing);
			long maxRefreshDuration = refreshDurations.values().stream().mapToLong(x -> x).max().orElse(0l);
			long minRefreshDuration = refreshDurations.values().stream().mapToLong(x -> x).min().orElse(0l);
			logger.info(" maxRefreshDuration:{} minRefreshDuration:{}", maxRefreshDuration, minRefreshDuration);

			if (maxRefreshDuration > refreshThreshold) {
				logger.info("== iteration:{}", iteration);
				NewShard newShard = new NewShardSpecificationByBoxAverage().apply(existing);
				Map<AllocationShardStats, Integer> sourcesCount = new NewShardSourcesCountByRefreshDurationRatio(
						refreshDurations, refreshThreshold).apply(newShard);
				List<BoxAllocation> allocations = new NewShardBoxAllocator().apply(newShard, sourcesCount);
				assertNewShard(existing, refreshDurations, allocations);
				iteration++;
				logger.info("== allocations:{}", allocations.size());
				allocations.forEach(allocation -> {
					logger.info(" mbox:{} source:{} target:{}", allocation.mbox, allocation.sourceIndex,
							allocation.targetIndex);
				});
				// on considère une contribution égale de chaque box au temps du refresh
				simulateNewShardAllocations(existing, allocations);
				logger.info("shard:{} boxCount:{}", existing.size(),
						existing.stream().mapToInt(stat -> stat.mailboxes.size()).average().orElse(0d));
				existing.forEach(stat -> {
					logger.info(" {} boxCount:{} refresh:{}", stat.indexName, stat.mailboxes.size(),
							stat.externalRefreshDuration / stat.externalRefreshCount);
				});
			} else {
				iterate = false;
			}
		}
		assertEquals(expectedIteration, iteration);
	}

	private void assertNewShard(List<AllocationShardStats> existing, Map<String, Long> refreshDurations,
			List<BoxAllocation> allocations) {
		double averageRefreshDuration = refreshDurations.values().stream().mapToLong(x -> x).average().orElse(0d);
		double averageBoxCount = existing.stream().mapToInt(stat -> stat.mailboxes.size()).average().orElse(0);

		Map<String, Integer> allocationSourcesCount = allocations.stream()
				.collect(Collectors.toMap(allocation -> allocation.sourceIndex, allocation -> 1, (x, y) -> x + y));
		allocationSourcesCount.forEach((sourceIndex, count) -> {
			AllocationShardStats source = byName(existing, sourceIndex);
//			assertTrue(source.mailboxes.size() - count >= Math.round(averageBoxCount));
		});

		Map<String, Integer> allocationTargetsCount = allocations.stream()
				.collect(Collectors.toMap(allocation -> allocation.targetIndex, allocation -> 1, (x, y) -> x + y));
		assertEquals(1, allocationTargetsCount.size());
		allocationTargetsCount.forEach((targetIndex, count) -> {
			assertTrue(existing.stream().noneMatch(stat -> stat.indexName.endsWith(targetIndex)));
			assertTrue(count <= Math.round(averageBoxCount));
		});
	}

	private List<AllocationShardStats> simulateNewShardAllocations(List<AllocationShardStats> existing,
			List<BoxAllocation> allocations) {
		AllocationShardStats target = new AllocationShardStats();
		target.indexName = allocations.get(0).targetIndex;
		target.mailboxes = new HashSet<>();
		target.externalRefreshCount = 1;
		target.externalRefreshDuration = 0;
		existing.add(target);
		allocations.forEach(allocation -> {
			AllocationShardStats source = byName(existing, allocation.sourceIndex);
			double boxRefreshTime = ((double) source.externalRefreshDuration / source.externalRefreshCount)
					/ (double) source.mailboxes.size();
			source.externalRefreshDuration -= Math.round(boxRefreshTime * source.externalRefreshCount);
			target.externalRefreshDuration += Math.round(boxRefreshTime * target.externalRefreshCount);
			source.mailboxes = source.mailboxes.stream().filter(mbox -> !mbox.equals(allocation.mbox))
					.collect(Collectors.toSet());
			target.mailboxes.add(allocation.mbox);
		});

		return existing;
	}

	private AllocationShardStats byName(List<AllocationShardStats> existing, String indexName) {
		return existing.stream().filter(stat -> stat.indexName.equals(indexName)).findAny().get();
	}

	private Map<String, Long> refreshDurations(List<AllocationShardStats> existing) {
		return existing.stream().collect(Collectors.toMap(stat -> stat.indexName,
				stat -> stat.externalRefreshDuration / stat.externalRefreshCount));
	}
}
