package net.bluemind.lib.elasticsearch.allocations.newshard;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.Long.compare;
import static java.lang.Long.parseLong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Test;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats.MailboxCount;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy.BoxesCount;

public class NewShardSourcesCountByRefreshDurationRatioTest {

	private int indexCount = 21;
	private long minRefresh = 400l;
	private int refreshStep = 20;

	@Test
	public void testOnlyOneAboveThreshold() {
		long refreshDurationThreshold = 800l;
		Map<String, Long> refreshDurations = refreshDurations(indexCount, i -> minRefresh + (refreshStep * i));
		List<AllocationShardStats> existing = shardStats(indexCount, 10, 1000l);
		long averageDocCount = Math
				.round(existing.stream().mapToLong(stat -> stat.docCount).sum() / (existing.size() + 1));
		NewShard newShard = new NewShard(existing, "" + indexCount, averageDocCount);

		AllocatorSourcesCountStrategy<NewShard> strategy = new NewShardSourcesCountByRefreshDurationRatio(
				refreshDurations, refreshDurationThreshold);
		Map<AllocationShardStats, BoxesCount> sourcesBoxes = strategy.apply(newShard);

		sourcesBoxes.keySet().stream()
				.forEach(source -> assertThat(Integer.parseInt(source.indexName)).isAtLeast((int) (indexCount / 2)));
		long totalDocContributed = sourcesBoxes.values().stream().mapToLong(sourceBoxes -> sourceBoxes.count).sum();
		assertThat((double) totalDocContributed).isWithin(0.05 * averageDocCount).of(averageDocCount);

		AllocationShardStats shardAboveThreshold = existing.get(indexCount - 1);
		existing.stream() //
				.filter(stat -> stat.docCount > averageDocCount)
				.sorted((stat1, stat2) -> compare(parseLong(stat2.indexName), parseLong(stat1.indexName)))
				.reduce((stat1, stat2) -> {
					if (refreshDurations.get(stat1.indexName) >= refreshDurationThreshold) {
						assertThat(sourcesBoxes.get(stat1).count).isAtLeast(Math.round(averageDocCount * 0.9));
					} else {
						assertThat(sourcesBoxes.get(stat1).count).isAtMost(sourcesBoxes.get(shardAboveThreshold).count);
					}
					return stat2;
				});
	}

	@Test
	public void testEnoughWithFourIndexAboveThreshold() {
		long refreshDurationThreshold = 740l;
		Map<String, Long> refreshDurations = refreshDurations(indexCount, i -> minRefresh + (refreshStep * i));
		List<AllocationShardStats> existing = shardStats(indexCount, 10, 1000l);
		long averageDocCount = Math
				.round(existing.stream().mapToLong(stat -> stat.docCount).sum() / (existing.size() + 1));
		NewShard newShard = new NewShard(existing, "" + indexCount, averageDocCount);

		AllocatorSourcesCountStrategy<NewShard> strategy = new NewShardSourcesCountByRefreshDurationRatio(
				refreshDurations, refreshDurationThreshold);
		Map<AllocationShardStats, BoxesCount> sourcesBoxes = strategy.apply(newShard);

		sourcesBoxes.keySet().stream()
				.forEach(source -> assertThat(Integer.parseInt(source.indexName)).isAtLeast((int) (indexCount / 2)));
		long totalDocContributed = sourcesBoxes.values().stream().mapToLong(sourceBoxes -> sourceBoxes.count).sum();
		assertThat((double) totalDocContributed).isWithin(0.05 * averageDocCount).of(averageDocCount);

		long docCountAboveThreshold = existing.stream()
				.filter(stat -> refreshDurations.get(stat.indexName) >= refreshDurationThreshold)
				.mapToLong(stat -> sourcesBoxes.get(stat).count).sum();
		assertThat(docCountAboveThreshold).isAtLeast(Math.round(averageDocCount * 0.9));
		existing.stream() //
				.filter(stat -> stat.docCount > averageDocCount)
				.sorted((stat1, stat2) -> compare(parseLong(stat2.indexName), parseLong(stat1.indexName)))
				.reduce((stat1, stat2) -> {
					if (refreshDurations.get(stat1.indexName) >= refreshDurationThreshold) {
						assertThat(sourcesBoxes.get(stat1).count).isAtLeast(sourcesBoxes.get(stat2).count);
					} else {
						assertThat(sourcesBoxes.get(stat1).count).isAtMost(docCountAboveThreshold);
					}
					return stat2;
				});
	}

	@Test
	public void testNoneAboveThreshold() {
		long refreshDurationThreshold = 840l;
		Map<String, Long> refreshDurations = refreshDurations(indexCount, i -> minRefresh + (refreshStep * i));
		List<AllocationShardStats> existing = shardStats(indexCount, 10, 1000l);
		long averageDocCount = Math
				.round(existing.stream().mapToLong(stat -> stat.docCount).sum() / (existing.size() + 1));
		NewShard newShard = new NewShard(existing, "" + indexCount, averageDocCount);
		System.out.println("avg:" + averageDocCount);

		AllocatorSourcesCountStrategy<NewShard> strategy = new NewShardSourcesCountByRefreshDurationRatio(
				refreshDurations, refreshDurationThreshold);
		Map<AllocationShardStats, BoxesCount> sourcesBoxes = strategy.apply(newShard);

		sourcesBoxes.keySet().stream()
				.forEach(source -> assertThat(Integer.parseInt(source.indexName)).isAtLeast((int) (indexCount / 2)));
		long totalDocContributed = sourcesBoxes.values().stream().mapToLong(sourceBoxes -> sourceBoxes.count).sum();
		assertThat((double) totalDocContributed).isWithin(0.05 * averageDocCount).of(averageDocCount);

		existing.stream() //
				.filter(stat -> stat.docCount > averageDocCount)
				.sorted((stat1, stat2) -> compare(parseLong(stat2.indexName), parseLong(stat1.indexName)))
				.reduce((stat1, stat2) -> {
					assertThat((double) sourcesBoxes.get(stat1).count).isWithin(0.1 * sourcesBoxes.get(stat2).count)
							.of(sourcesBoxes.get(stat2).count);
					return stat2;
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
