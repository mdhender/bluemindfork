package net.bluemind.lib.elasticsearch.allocations.rebalance;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntToLongFunction;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats.MailboxCount;
import net.bluemind.lib.elasticsearch.allocations.AllocationSpecification;

public class RebalanceSpecificationByRatioTest {
	private static final Logger logger = LoggerFactory.getLogger(RebalanceSpecificationByRatioTest.class);

	private int indexCount = 21;
	private long minRefresh = 200l;
	private int refreshStep = 30;
	private double lowRefreshDurationRatio = 0.2;
	private double highRefreshDurationRatio = 0.2;

	@Before
	public void setup() {
	}

	@Test
	public void testLinearRefreshTime() {
		long averageRefreshDuration = minRefresh + ((indexCount / 2) * 30);
		double low = 0.8 * averageRefreshDuration;
		double high = 1.2 * averageRefreshDuration;

		AtomicInteger lowestSource = new AtomicInteger(-1);
		AtomicInteger highestTarget = new AtomicInteger(-1);
		Map<String, Long> refreshDurations = refreshDurations(indexCount, low, high, lowestSource, highestTarget,
				i -> minRefresh + (refreshStep * i));

		List<AllocationShardStats> existing = shardStats(indexCount, 10, 1000l);
		AllocationSpecification<Rebalance> rebalanceSpec = new RebalanceSpecificationByRatio(refreshDurations,
				lowRefreshDurationRatio, highRefreshDurationRatio);
		Rebalance rebalance = rebalanceSpec.apply(existing);

		assertThat(rebalance.sources).isNotEmpty();
		rebalance.sources.stream()
				.forEach(stat -> assertThat(Integer.valueOf(stat.indexName)).isAtLeast(lowestSource.get()));
		assertThat(rebalance.targets).isNotEmpty();
		rebalance.targets.stream()
				.forEach(stat -> assertThat(Integer.valueOf(stat.indexName)).isAtMost(highestTarget.get()));
	}

	@Test
	public void testTwoBucketsRefreshTime() {
		long averageRefreshDuration = minRefresh + ((indexCount / 2) * 30);
		double low = 0.8 * averageRefreshDuration;
		double high = 1.2 * averageRefreshDuration;

		AtomicInteger lowestSource = new AtomicInteger(-1);
		AtomicInteger highestTarget = new AtomicInteger(-1);
		Map<String, Long> refreshDurations = refreshDurations(indexCount, low, high, lowestSource, highestTarget,
				i -> (i < indexCount / 2) ? minRefresh : minRefresh + (refreshStep * indexCount - 1));
		List<AllocationShardStats> existing = shardStats(indexCount, 10, 1000l);
		AllocationSpecification<Rebalance> rebalanceSpec = new RebalanceSpecificationByRatio(refreshDurations,
				lowRefreshDurationRatio, highRefreshDurationRatio);
		Rebalance rebalance = rebalanceSpec.apply(existing);

		assertThat(rebalance.sources).isNotEmpty();
		rebalance.sources.stream()
				.forEach(stat -> assertThat(Integer.valueOf(stat.indexName)).isAtLeast(lowestSource.get()));
		assertThat(rebalance.targets).isNotEmpty();
		rebalance.targets.stream()
				.forEach(stat -> assertThat(Integer.valueOf(stat.indexName)).isAtMost(highestTarget.get()));
	}

	@Test
	public void testConstantResfreshTime() {
		long averageRefreshDuration = minRefresh + ((indexCount / 2) * 30);
		double low = 0.8 * averageRefreshDuration;
		double high = 1.2 * averageRefreshDuration;

		AtomicInteger lowestSource = new AtomicInteger(-1);
		AtomicInteger highestTarget = new AtomicInteger(-1);
		Map<String, Long> refreshDurations = refreshDurations(indexCount, low, high, lowestSource, highestTarget,
				i -> minRefresh);
		List<AllocationShardStats> existing = shardStats(indexCount, 10, 1000l);
		AllocationSpecification<Rebalance> rebalanceSpec = new RebalanceSpecificationByRatio(refreshDurations,
				lowRefreshDurationRatio, highRefreshDurationRatio);
		Rebalance rebalance = rebalanceSpec.apply(existing);

		assertThat(rebalance.sources).isEmpty();
		assertThat(rebalance.targets).isEmpty();
	}

	private Map<String, Long> refreshDurations(int indexCount, double low, double high, AtomicInteger lowestSource,
			AtomicInteger highestTarget, IntToLongFunction consumer) {
		Map<String, Long> refreshDurations = new HashMap<>();
		for (int i = 0; i < indexCount; i++) {
			long duration = consumer.applyAsLong(i);
			refreshDurations.put("" + i, duration);
			if (lowestSource.get() == -1 && duration > high) {
				lowestSource.set(i);
			}
			if (duration < low) {
				highestTarget.set(i);
			}
		}
		return refreshDurations;
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
