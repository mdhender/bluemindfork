package net.bluemind.lib.elasticsearch.allocations.newshard;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;

import net.bluemind.lib.elasticsearch.allocations.AllocationShardStats;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy;
import net.bluemind.lib.elasticsearch.allocations.AllocatorSourcesCountStrategy.BoxesCount;
import net.bluemind.lib.elasticsearch.allocations.BoxAllocation;

public class NewShardBoxAllocatorTest {

	private int indexCount = 21;
	private long minRefresh = 400l;
	private int refreshStep = 20;

	@Test
	public void testOnlyOneAboveThreshold() {
		long refreshDurationThreshold = 800l;
		Map<String, Long> refreshDurations = refreshDurations(indexCount, i -> minRefresh + (refreshStep * i));
		List<AllocationShardStats> existing = shardStats(indexCount);
		long averageDocCount = Math
				.round(existing.stream().mapToLong(stat -> stat.docCount).sum() / (existing.size() + 1));
		NewShard newShard = new NewShard(existing, "" + indexCount, averageDocCount);

		Map<AllocationShardStats, BoxesCount> sourcesBoxes = new NewShardSourcesCountByRefreshDurationRatio(
				refreshDurations, refreshDurationThreshold).apply(newShard);
		List<BoxAllocation> allocations = new NewShardBoxAllocator().apply(newShard, sourcesBoxes);

		long allocatedDocCount = allocations.stream()
				.mapToLong(allocation -> docCount(existing, allocation.sourceIndex, allocation.mbox)).sum();
		assertThat((double) allocatedDocCount).isWithin(0.05 * averageDocCount).of(averageDocCount);
		Set<String> allocatedMbox = allocations.stream().map(allocation -> allocation.mbox).collect(Collectors.toSet());
		assertThat(allocatedMbox.size()).isEqualTo(allocations.size());
		Set<String> sourcesIndex = sourcesBoxes.keySet().stream().map(source -> source.indexName)
				.collect(Collectors.toSet());
		allocations.forEach(allocation -> {
			assertThat(sourcesIndex).contains(allocation.sourceIndex);
			assertThat(allocation.targetIndex).isEqualTo("" + indexCount);
		});
	}

	@Test
	public void testEnoughWithFourIndexAboveThreshold() {
		long refreshDurationThreshold = 740l;
		Map<String, Long> refreshDurations = refreshDurations(indexCount, i -> minRefresh + (refreshStep * i));
		List<AllocationShardStats> existing = shardStats(indexCount);
		long averageDocCount = Math
				.round(existing.stream().mapToLong(stat -> stat.docCount).sum() / (existing.size() + 1));
		NewShard newShard = new NewShard(existing, "" + indexCount, averageDocCount);

		Map<AllocationShardStats, BoxesCount> sourcesBoxes = new NewShardSourcesCountByRefreshDurationRatio(
				refreshDurations, refreshDurationThreshold).apply(newShard);
		List<BoxAllocation> allocations = new NewShardBoxAllocator().apply(newShard, sourcesBoxes);

		Long allocatedDocCount = allocations.stream()
				.mapToLong(allocation -> docCount(existing, allocation.sourceIndex, allocation.mbox)).sum();
		assertThat((double) allocatedDocCount).isWithin(0.05 * averageDocCount).of(averageDocCount);
		Set<String> allocatedMbox = allocations.stream().map(allocation -> allocation.mbox).collect(Collectors.toSet());
		assertThat(allocatedMbox.size()).isEqualTo(allocations.size());
		Set<String> sourcesIndex = sourcesBoxes.keySet().stream().map(source -> source.indexName)
				.collect(Collectors.toSet());
		allocations.forEach(allocation -> {
			assertThat(sourcesIndex).contains(allocation.sourceIndex);
			assertThat(allocation.targetIndex).isEqualTo("" + indexCount);
		});
	}

	@Test
	public void testNoneAboveThreshold() {
		long refreshDurationThreshold = 840l;
		Map<String, Long> refreshDurations = refreshDurations(indexCount, i -> minRefresh + (refreshStep * i));
		List<AllocationShardStats> existing = shardStats(indexCount);
		long averageDocCount = Math
				.round(existing.stream().mapToLong(stat -> stat.docCount).sum() / (existing.size() + 1));
		NewShard newShard = new NewShard(existing, "" + indexCount, averageDocCount);

		AllocatorSourcesCountStrategy<NewShard> strategy = new NewShardSourcesCountByRefreshDurationRatio(
				refreshDurations, refreshDurationThreshold);
		Map<AllocationShardStats, BoxesCount> sourcesBoxes = strategy.apply(newShard);
		List<BoxAllocation> allocations = new NewShardBoxAllocator().apply(newShard, sourcesBoxes);

		Long allocatedDocCount = allocations.stream()
				.mapToLong(allocation -> docCount(existing, allocation.sourceIndex, allocation.mbox)).sum();
		assertThat((double) allocatedDocCount).isWithin(0.05 * averageDocCount).of(averageDocCount);
		Set<String> allocatedMbox = allocations.stream().map(allocation -> allocation.mbox).collect(Collectors.toSet());
		assertThat(allocatedMbox.size()).isEqualTo(allocations.size());
		Set<String> sourcesIndex = sourcesBoxes.keySet().stream().map(source -> source.indexName)
				.collect(Collectors.toSet());
		allocations.forEach(allocation -> {
			assertThat(sourcesIndex).contains(allocation.sourceIndex);
			assertThat(allocation.targetIndex).isEqualTo("" + indexCount);
		});
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
