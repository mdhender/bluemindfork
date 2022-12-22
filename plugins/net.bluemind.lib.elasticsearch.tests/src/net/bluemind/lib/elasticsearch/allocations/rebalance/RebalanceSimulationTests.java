package net.bluemind.lib.elasticsearch.allocations.rebalance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

public class RebalanceSimulationTests {

	private static final Logger logger = LoggerFactory.getLogger(RebalanceSimulationTests.class);

	private final double lowRefreshDurationRatio = 0.2;
	private final double highRefreshDurationRatio = 0.2;

	@Before
	public void setup() {
	}

	@Test
	public void testRebalanceMarseille() {
		testRebalance("marseille");
	}

	@Test
	public void testRebalanceLaforet() {
		testRebalance("laforet");
	}

	private void testRebalance(String dataset) {
		List<AllocationShardStats> existing = SimulationStatLoader.load(dataset);
		Map<String, Long> refreshDurations = refreshDurations(existing);

		Rebalance rebalance = new RebalanceSpecificationByRatio(refreshDurations, lowRefreshDurationRatio,
				highRefreshDurationRatio).apply(existing);
		Map<AllocationShardStats, Integer> sourcesCount = new RebalanceSourcesCountByRefreshDurationRatio(
				refreshDurations).apply(rebalance);
		List<BoxAllocation> allocations = new RebalanceBoxAllocator().apply(rebalance, sourcesCount);

		assertRebalance(existing, refreshDurations, allocations);
		allocations.forEach(allocation -> {
			logger.info("mbox:{} source:{} target:{}", allocation.mbox, allocation.sourceIndex, allocation.targetIndex);
		});
	}

	@Test
	public void testRebalanceIterationMarseille() {
		testRebalanceIteration("marseille");
	}

	@Test
	public void testRebalanceIterationLaforet() {
		testRebalanceIteration("laforet");
	}

	private void testRebalanceIteration(String dataset) {
		List<AllocationShardStats> existing = SimulationStatLoader.load(dataset);

		boolean iterate = true;
		int iteration = 0;
		while (iterate) {
			Map<String, Long> refreshDurations = refreshDurations(existing);
			long maxRefreshDuration = refreshDurations.values().stream().mapToLong(x -> x).max().orElse(0l);
			long minRefreshDuration = refreshDurations.values().stream().mapToLong(x -> x).min().orElse(0l);
			logger.info(" maxRefreshDuration:{} minRefreshDuration:{}", maxRefreshDuration, minRefreshDuration);
			Rebalance rebalance = new RebalanceSpecificationByRatio(refreshDurations, lowRefreshDurationRatio,
					highRefreshDurationRatio).apply(existing);
			if (!rebalance.sources.isEmpty() && !rebalance.targets.isEmpty()) {
				logger.info("== iteration:{}", iteration);
				Map<AllocationShardStats, Integer> sourcesCount = new RebalanceSourcesCountByRefreshDurationRatio(
						refreshDurations).apply(rebalance);
				List<BoxAllocation> allocations = new RebalanceBoxAllocator().apply(rebalance, sourcesCount);
				assertRebalance(existing, refreshDurations, allocations);
				iteration++;
				logger.info("== allocations:{}", allocations.size());
				allocations.forEach(allocation -> {
					logger.info(" mbox:{} source:{} target:{}", allocation.mbox, allocation.sourceIndex,
							allocation.targetIndex);
				});
				// on considère une contribution égale de chaque box au temps du refresh
				existing = simulateAllocations(existing, allocations);

			} else {
				iterate = false;
			}
		}
		// après 1 itération, on a encore des index au-dessus et en dessous des seuils,
		// mais leur nombre de box ne permet pas de devenir à nouveau des sources de
		// rebalance.
		assertEquals(1, iteration);
		// allocations:1284
		// maxRefreshDuration:1288.0 minRefreshDuration:222.0
		// maxRefreshDuration:1000 minRefreshDuration:450
	}

	private List<AllocationShardStats> simulateAllocations(List<AllocationShardStats> existing,
			List<BoxAllocation> allocations) {
		allocations.forEach(allocation -> {
			AllocationShardStats source = byName(existing, allocation.sourceIndex);
			AllocationShardStats target = byName(existing, allocation.targetIndex);
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

	private void assertRebalance(List<AllocationShardStats> existing, Map<String, Long> refreshDurations,
			List<BoxAllocation> allocations) {
		double averageRefreshDuration = refreshDurations.values().stream().mapToLong(x -> x).average().orElse(0d);
		double averageBoxCount = existing.stream().mapToInt(stat -> stat.mailboxes.size()).average().orElse(0);

		Map<String, Integer> allocationSourcesCount = allocations.stream()
				.collect(Collectors.toMap(allocation -> allocation.sourceIndex, allocation -> 1, (x, y) -> x + y));
		allocationSourcesCount.forEach((sourceIndex, count) -> {
			assertTrue(refreshDurations.get(sourceIndex) >= (1 + highRefreshDurationRatio) * averageRefreshDuration);
			AllocationShardStats source = byName(existing, sourceIndex);
			assertTrue(source.mailboxes.size() - count >= Math.round(averageBoxCount) - 1);
		});

		Map<String, Integer> allocationTargetsCount = allocations.stream()
				.collect(Collectors.toMap(allocation -> allocation.targetIndex, allocation -> 1, (x, y) -> x + y));
		allocationTargetsCount.forEach((targetIndex, count) -> {
			assertTrue(refreshDurations.get(targetIndex) <= (1 - lowRefreshDurationRatio) * averageRefreshDuration);
			AllocationShardStats target = byName(existing, targetIndex);
			assertTrue(target.mailboxes.size() + count <= Math.round(averageBoxCount));
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
