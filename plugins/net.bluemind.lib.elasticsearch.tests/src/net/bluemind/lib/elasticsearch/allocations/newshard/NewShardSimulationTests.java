package net.bluemind.lib.elasticsearch.allocations.newshard;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats.MailboxCount;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy.BoxesCount;
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

//	@Test
//	public void testNewShardMarseille() {
//		testNewShard("marseille", 800l);
//	}

	@Test
	public void testNewShardLaforet() {
		testNewShard("laforet", 800l);
	}

	private void testNewShard(String dataset, long refreshThreshold) {
		List<AllocationShardStats> existing = SimulationStatLoader.load(dataset);
		Map<String, Long> refreshDurations = refreshDurations(existing);

		NewShard newShard = new NewShardSpecificationByBoxAverage().apply(existing);
		Map<AllocationShardStats, BoxesCount> sourcesCount = new NewShardSourcesCountByRefreshDurationRatio(
				refreshDurations, refreshThreshold).apply(newShard);
		List<BoxAllocation> allocations = new NewShardBoxAllocator().apply(newShard, sourcesCount);

		simulateNewShardAllocations(existing, allocations);
		assertNewShard(existing, refreshDurations, refreshDurations(existing), allocations);
		allocations.forEach(allocation -> {
			logger.info("mbox:{} source:{} target:{}", allocation.mbox, allocation.sourceIndex, allocation.targetIndex);
		});
	}

//	@Test
//	public void testNewShardIterationdMarseille() {
//		testNewShardIteration("marseille", 800l, 19);
//	}

	@Test
	public void testNewShardIterationLaforet() {
		testNewShardIteration("laforet", 500l, 3);
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
			double averageRefreshDuration = refreshDurations.values().stream().mapToLong(x -> x).average().orElse(0d);
			logger.info("averageRefreshDuration:{} maxRefreshDuration:{} minRefreshDuration:{}", averageRefreshDuration,
					maxRefreshDuration, minRefreshDuration);
			existing.forEach(stat -> {
				logger.info("shard:{} docCount:{} refresh:{}", stat.indexName, stat.docCount,
						refreshDurations.get(stat.indexName));
			});
			if (maxRefreshDuration > refreshThreshold) {
				logger.info("== iteration:{}", iteration);
				NewShard newShard = new NewShardSpecificationByBoxAverage().apply(existing);
				Map<AllocationShardStats, BoxesCount> sourcesCount = new NewShardSourcesCountByRefreshDurationRatio(
						refreshDurations, refreshThreshold).apply(newShard);
				List<BoxAllocation> allocations = new NewShardBoxAllocator().apply(newShard, sourcesCount);
				iteration++;
				logger.info("== allocations:{}", allocations.size());
				allocations.forEach(allocation -> {
					logger.info(" mbox:{} source:{} target:{}", allocation.mbox, allocation.sourceIndex,
							allocation.targetIndex);
				});
				// on considère une contribution égale de chaque box au temps du refresh
				simulateNewShardAllocations(existing, allocations);
				Map<String, Long> newRefreshDurations = refreshDurations(existing);
				assertNewShard(existing, refreshDurations, newRefreshDurations, allocations);
				maxRefreshDuration = newRefreshDurations.values().stream().mapToLong(x -> x).max().orElse(0l);
				minRefreshDuration = newRefreshDurations.values().stream().mapToLong(x -> x).min().orElse(0l);
				averageRefreshDuration = newRefreshDurations.values().stream().mapToLong(x -> x).average().orElse(0d);
				logger.info("averageRefreshDuration:{} maxRefreshDuration:{} minRefreshDuration:{}",
						averageRefreshDuration, maxRefreshDuration, minRefreshDuration);
				logger.info("<-- end shard:{} boxCount:{} -->", existing.size(),
						existing.stream().mapToInt(stat -> stat.mailboxes.size()).average().orElse(0d));
			} else {
				iterate = false;
			}
		}
		assertThat(iteration).isEqualTo(expectedIteration);
	}

	private void assertNewShard(List<AllocationShardStats> existing, Map<String, Long> refreshDurations,
			Map<String, Long> newRefreshDurations, List<BoxAllocation> allocations) {
		double averageRefreshDuration = newRefreshDurations.values().stream().mapToLong(x -> x).average().orElse(0d);
		long averageDocCount = Math
				.round(existing.stream().mapToLong(stat -> stat.docCount).sum() / (existing.size() + 1));

		Map<String, Integer> allocationSourcesCount = allocations.stream()
				.collect(Collectors.toMap(allocation -> allocation.sourceIndex, allocation -> 1, (x, y) -> x + y));
		allocationSourcesCount.forEach((sourceIndex, count) -> {
			AllocationShardStats source = byName(existing, sourceIndex);
			assertThat(newRefreshDurations.get(source.indexName)).isAtMost(refreshDurations.get(source.indexName));
			assertThat(source.docCount).isAtLeast(Math.round(0.95 * averageDocCount));
		});

		Map<String, Integer> allocationTargetsCount = allocations.stream()
				.collect(Collectors.toMap(allocation -> allocation.targetIndex, allocation -> 1, (x, y) -> x + y));
		assertThat(allocationTargetsCount).hasSize(1);
		allocationTargetsCount.forEach((targetIndex, count) -> {
			AllocationShardStats target = byName(existing, targetIndex);
			assertThat((double) target.docCount).isWithin(0.05 * averageDocCount).of(averageDocCount);
			assertThat((double) newRefreshDurations.get(target.indexName)).isWithin(0.05 * averageRefreshDuration)
					.of(averageRefreshDuration);
		});
	}

	private void simulateNewShardAllocations(List<AllocationShardStats> existing, List<BoxAllocation> allocations) {
		AllocationShardStats target = new AllocationShardStats();
		target.indexName = allocations.get(0).targetIndex;
		target.mailboxes = new HashSet<>();
		target.mailboxesCount = new ArrayList<>();
		target.externalRefreshCount = 1;
		target.externalRefreshDuration = 0;
		existing.add(target);
		allocations.forEach(allocation -> {
			AllocationShardStats source = byName(existing, allocation.sourceIndex);
			MailboxCount mailboxCount = source.mailboxesCount.stream()
					.filter(mboxCount -> mboxCount.name.equals(allocation.mbox)).findFirst().orElse(null);
			double boxRefreshDuration = (mailboxCount.docCount
					* ((double) source.externalRefreshDuration / source.externalRefreshCount))
					/ (double) source.docCount;
			source.externalRefreshDuration -= Math.round(boxRefreshDuration * source.externalRefreshCount);
			target.externalRefreshDuration += Math.round(boxRefreshDuration * target.externalRefreshCount);

			source.docCount -= mailboxCount.docCount;
			source.mailboxes = source.mailboxes.stream().filter(mbox -> !mbox.equals(allocation.mbox))
					.collect(Collectors.toSet());
			source.mailboxesCount.stream().filter(mboxCount -> !mboxCount.name.equals(allocation.mbox))
					.collect(Collectors.toList());
			target.docCount += mailboxCount.docCount;
			target.mailboxes.add(allocation.mbox);
			target.mailboxesCount.add(mailboxCount);
		});
	}

	private AllocationShardStats byName(List<AllocationShardStats> existing, String indexName) {
		return existing.stream().filter(stat -> stat.indexName.equals(indexName)).findAny().get();
	}

	private Map<String, Long> refreshDurations(List<AllocationShardStats> existing) {
		return existing.stream().collect(Collectors.toMap(stat -> stat.indexName,
				stat -> stat.externalRefreshDuration / stat.externalRefreshCount));
	}
}
