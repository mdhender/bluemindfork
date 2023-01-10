package net.bluemind.lib.elasticsearch.allocations.rebalance;

import static com.google.common.truth.Truth.assertThat;

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

public class RebalanceSimulationTests {

	private static final Logger logger = LoggerFactory.getLogger(RebalanceSimulationTests.class);

	private static final double lowRefreshDurationRatio = 0.2;
	private static final double highRefreshDurationRatio = 0.2;

	@Before
	public void setup() {
	}

//	@Test
//	public void testRebalanceMarseille() {
//		testRebalance("marseille");
//	}

	@Test
	public void testRebalanceLaforet() {
		testRebalance("laforet");
	}

	private void testRebalance(String dataset) {
		List<AllocationShardStats> existing = SimulationStatLoader.load(dataset);
		Map<String, Long> refreshDurations = refreshDurations(existing);

		Rebalance rebalance = new RebalanceSpecificationByRatio(refreshDurations, lowRefreshDurationRatio,
				highRefreshDurationRatio).apply(existing);
		Map<AllocationShardStats, BoxesCount> sourcesCount = new RebalanceSourcesCountByRefreshDurationRatio()
				.apply(rebalance);
		List<BoxAllocation> allocations = new RebalanceBoxAllocator().apply(rebalance, sourcesCount);

		assertRebalance(existing, refreshDurations, allocations);
		allocations.forEach(allocation -> {
			logger.info("mbox:{} source:{} target:{}", allocation.mbox, allocation.sourceIndex, allocation.targetIndex);
		});
	}

//	@Test
//	public void testRebalanceIterationMarseille() {
//		testRebalanceIteration("marseille");
//	}

	@Test
	public void testRebalanceIterationLaforet() {
		testRebalanceIteration("laforet");
	}

	private void testRebalanceIteration(String dataset) {
		List<AllocationShardStats> existing = SimulationStatLoader.load(dataset);

		boolean iterate = true;
		int iteration = 0;
		while (iterate && iteration < 5) {
			Map<String, Long> refreshDurations = refreshDurations(existing);
			long maxRefreshDuration = refreshDurations.values().stream().mapToLong(x -> x).max().orElse(0l);
			long minRefreshDuration = refreshDurations.values().stream().mapToLong(x -> x).min().orElse(0l);
			long averageRefreshDuration = (long) existing.stream() //
					.mapToLong(stat -> refreshDurations.get(stat.indexName)) //
					.average().orElse(0);

			double lowRefreshDuration = averageRefreshDuration * (1 - lowRefreshDurationRatio);
			double highRefreshDuration = averageRefreshDuration * (1 + highRefreshDurationRatio);
			logger.info(" maxRefreshDuration:{} ({}) minRefreshDuration:{} ({}) avg:{}", maxRefreshDuration,
					highRefreshDuration, minRefreshDuration, lowRefreshDuration, averageRefreshDuration);

			Rebalance rebalance = new RebalanceSpecificationByRatio(refreshDurations, lowRefreshDurationRatio,
					highRefreshDurationRatio).apply(existing);
			if (!rebalance.sources.isEmpty() && !rebalance.targets.isEmpty()) {
				logger.info("== iteration:{}", iteration);
				Map<AllocationShardStats, BoxesCount> sourcesCount = new RebalanceSourcesCountByRefreshDurationRatio()
						.apply(rebalance);
				List<BoxAllocation> allocations = new RebalanceBoxAllocator().apply(rebalance, sourcesCount);
				assertRebalance(existing, refreshDurations, allocations);
				iteration++;
				logger.info("== allocations:{}", allocations.size());
				allocations.forEach(allocation -> {
					logger.info(" mbox:{} source:{} target:{}", allocation.mbox, allocation.sourceIndex,
							allocation.targetIndex);
				});
				// on considère une contribution égale de chaque box au temps du refresh
				simulateAllocations(existing, allocations);

			} else {
				iterate = false;
			}
		}
		// après 1 itération, on a encore des index au-dessus et en dessous des seuils,
		// mais leur nombre de box ne permet pas de devenir à nouveau des sources de
		// rebalance.
		assertThat(iteration).isEqualTo(1);
		// allocations:1284
		// maxRefreshDuration:1288.0 minRefreshDuration:222.0
		// maxRefreshDuration:1000 minRefreshDuration:450
	}

	private void simulateAllocations(List<AllocationShardStats> existing, List<BoxAllocation> allocations) {
		allocations.forEach(allocation -> {
			AllocationShardStats source = byName(existing, allocation.sourceIndex);
			AllocationShardStats target = byName(existing, allocation.targetIndex);
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

	private void assertRebalance(List<AllocationShardStats> existing, Map<String, Long> refreshDurations,
			List<BoxAllocation> allocations) {
		double averageRefreshDuration = refreshDurations.values().stream().mapToLong(x -> x).average().orElse(0d);
		long averageDocCount = Math.round(existing.stream().mapToLong(stat -> stat.docCount).sum() / existing.size());
		long availableDocCount = allocations.stream().map(allocation -> allocation.targetIndex).distinct()
				.mapToLong(targetIndex -> averageDocCount - byName(existing, targetIndex).docCount).sum();
		long allocatedDocCount = allocations.stream()
				.mapToLong(allocation -> docCount(existing, allocation.sourceIndex, allocation.mbox)).sum();

		Map<String, Long> allocationSourcesCount = allocations.stream()
				.collect(Collectors.toMap(allocation -> allocation.sourceIndex,
						allocation -> docCount(existing, allocation.sourceIndex, allocation.mbox), (x, y) -> x + y));

		allocationSourcesCount.forEach((sourceIndex, count) -> {
			logger.info("source:{} count:{} relocated:{}", sourceIndex, byName(existing, sourceIndex).docCount, count);
			assertThat(refreshDurations.get(sourceIndex))
					.isAtLeast((long) ((1 + highRefreshDurationRatio) * averageRefreshDuration));
		});

		Map<String, Long> allocationTargetsCount = allocations.stream()
				.collect(Collectors.toMap(allocation -> allocation.targetIndex,
						allocation -> docCount(existing, allocation.sourceIndex, allocation.mbox), (x, y) -> x + y));
		allocationTargetsCount.forEach((targetIndex, count) -> {
			logger.info("target:{} count:{} relocated:{}", targetIndex, byName(existing, targetIndex).docCount, count);
			assertThat(refreshDurations.get(targetIndex))
					.isAtMost((long) ((1 - lowRefreshDurationRatio) * averageRefreshDuration));
		});

		assertThat((double) allocatedDocCount).isWithin(0.05 * availableDocCount).of(availableDocCount);
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

	private Map<String, Long> refreshDurations(List<AllocationShardStats> existing) {
		return existing.stream().collect(Collectors.toMap(stat -> stat.indexName,
				stat -> stat.externalRefreshDuration / stat.externalRefreshCount));

	}
}
